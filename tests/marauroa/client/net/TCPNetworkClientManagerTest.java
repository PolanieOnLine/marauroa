/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package marauroa.client.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import marauroa.common.net.Encoder;
import marauroa.common.net.message.Message;
import marauroa.common.net.message.MessageC2SLogout;
import marauroa.common.net.message.MessageS2CLeaveCharacterACK;

/** Exercises TCP shutdown with real loopback sockets and captures its logs. */
public class TCPNetworkClientManagerTest {

	private ServerSocket server;
	private Socket peer;
	private TCPNetworkClientManager manager;
	private Thread reader;
	private final Logger logger = Logger.getLogger("marauroa.client.net");
	private Level previousLevel;
	private final RecordingAppender logs = new RecordingAppender();

	@Before
	public void setUp() throws Exception {
		previousLevel = logger.getLevel();
		logger.setLevel(Level.DEBUG);
		logger.addAppender(logs);
		server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
		manager = new TCPNetworkClientManager("127.0.0.1", server.getLocalPort());
		peer = server.accept();
		Field field = TCPNetworkClientManager.class.getDeclaredField("readManager");
		field.setAccessible(true);
		reader = (Thread) field.get(manager);
	}

	@After
	public void tearDown() throws Exception {
		try {
			if (manager != null) {
				manager.finish();
			}
			if (peer != null) {
				peer.close();
			}
			if (server != null) {
				server.close();
			}
		} finally {
			logger.removeAppender(logs);
			logger.setLevel(previousLevel);
		}
	}

	@Test(timeout = 5000)
	public void closesIdleConnectionWithoutWarnings() throws Exception {
		assertTrue(manager.getConnectionState());
		manager.finish();
		assertStoppedWithoutWarnings();
	}

	@Test(timeout = 5000)
	public void closesWhileReceivingPartialHeaderWithoutWarnings() throws Exception {
		peer.getOutputStream().write(new byte[] {32, 0});
		peer.getOutputStream().flush();
		Thread.sleep(20);
		manager.finish();
		assertStoppedWithoutWarnings();
	}

	@Test(timeout = 5000)
	public void closesWhileReceivingPartialBodyWithoutWarnings() throws Exception {
		peer.getOutputStream().write(new byte[] {32, 0, 0, 0, 1, 2});
		peer.getOutputStream().flush();
		Thread.sleep(20);
		manager.finish();
		assertStoppedWithoutWarnings();
	}

	@Test(timeout = 5000)
	public void repeatedCloseAndLateLogoutAreQuiet() throws Exception {
		manager.finish();
		manager.finish();
		manager.addMessage(new MessageC2SLogout(1));
		assertStoppedWithoutWarnings();
	}

	@Test(timeout = 5000)
	public void closePreservesInterruptedCaller() throws Exception {
		Thread.currentThread().interrupt();
		try {
			manager.finish();
			assertTrue(Thread.currentThread().isInterrupted());
			assertStoppedWithoutWarnings();
		} finally {
			Thread.interrupted();
		}
	}

	@Test(timeout = 5000)
	public void receivesFragmentedHeaderBodyAndConsecutiveMessages() throws Exception {
		byte[] encoded = Encoder.get().encode(new MessageS2CLeaveCharacterACK());
		for (int i = 0; i < encoded.length; i++) {
			peer.getOutputStream().write(encoded[i]);
			peer.getOutputStream().flush();
			Thread.sleep(2);
		}
		peer.getOutputStream().write(encoded);
		peer.getOutputStream().write(encoded);
		peer.getOutputStream().flush();
		for (int i = 0; i < 3; i++) {
			Message message = manager.getMessage(2000);
			assertNotNull(message);
			assertEquals(Message.MessageType.S2C_LEAVECHARACTER_ACK, message.getType());
		}
		manager.finish();
		assertStoppedWithoutWarnings();
	}

	@Test(timeout = 5000)
	public void unexpectedPeerCloseStillReportsConnectionFailure() throws Exception {
		peer.close();
		assertUnexpectedDisconnection();
	}

	@Test(timeout = 5000)
	public void unexpectedCloseDuringBodyStillReportsConnectionFailure() throws Exception {
		peer.getOutputStream().write(new byte[] {32, 0, 0, 0, 1, 2});
		peer.getOutputStream().flush();
		peer.close();
		assertUnexpectedDisconnection();
	}

	@Test(timeout = 5000)
	public void unexpectedSocketCloseIsNotMistakenForIntentionalShutdown() throws Exception {
		manager.socket.close();
		assertUnexpectedDisconnection();
	}

	@Test(timeout = 5000)
	public void invalidPacketSizeStillReportsConnectionFailure() throws Exception {
		peer.getOutputStream().write(new byte[] {0, 0, 0, 0});
		peer.getOutputStream().flush();
		assertUnexpectedDisconnection();
	}

	private void assertUnexpectedDisconnection() throws Exception {
		reader.join(2000);
		assertFalse("Reader must terminate on a failed connection", reader.isAlive());
		assertTrue(manager.isfinished);
		assertFalse(manager.getConnectionState());
		assertTrue("Unexpected connection failures must remain visible", logs.hasConnectionWarning());
	}

	private void assertStoppedWithoutWarnings() {
		assertFalse("finish() must wait for the reader to exit", reader.isAlive());
		assertTrue(manager.isfinished);
		assertFalse(manager.getConnectionState());
		for (LoggingEvent event : logs.events) {
			assertFalse("Unexpected shutdown log: " + event.getRenderedMessage(),
					event.getLevel().isGreaterOrEqual(Level.WARN));
		}
	}

	private static class RecordingAppender extends AppenderSkeleton {
		final List<LoggingEvent> events = new CopyOnWriteArrayList<LoggingEvent>();

		@Override
		protected void append(LoggingEvent event) {
			events.add(event);
		}

		boolean hasConnectionWarning() {
			for (LoggingEvent event : events) {
				if (event.getLevel().isGreaterOrEqual(Level.WARN)
						&& "Connection broken.".equals(event.getRenderedMessage())) {
					return true;
				}
			}
			return false;
		}

		@Override
		public void close() {
		}

		@Override
		public boolean requiresLayout() {
			return false;
		}
	}
}
