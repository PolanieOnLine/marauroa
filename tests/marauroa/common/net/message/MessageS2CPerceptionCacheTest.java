/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package marauroa.common.net.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import marauroa.common.game.IRPZone;
import marauroa.common.game.Perception;
import marauroa.common.game.RPObject;

public class MessageS2CPerceptionCacheTest {

	@Before
	public void clearCacheBeforeTest() {
		MessageS2CPerception.clearPrecomputedPerception();
	}

	@After
	public void clearCache() {
		MessageS2CPerception.clearPrecomputedPerception();
	}

	@Test
	public void differentPerceptionInstancesDoNotShareStaticPayload() throws IOException {
		final Perception alice = perceptionWithObject(1);
		final Perception bob = perceptionWithObject(2);
		final MessageS2CPerception aliceMessage = new MessageS2CPerception(null, alice);
		final MessageS2CPerception bobMessage = new MessageS2CPerception(null, bob);
		aliceMessage.setProtocolVersion(1);
		bobMessage.setProtocolVersion(1);

		final byte[] alicePayload = MessageS2CPerception.CachedCompressedPerception
				.get().get(aliceMessage);
		final byte[] bobPayload = MessageS2CPerception.CachedCompressedPerception
				.get().get(bobMessage);

		assertNotSame(alicePayload, bobPayload);
		assertEquals(0, MessageS2CPerception.getPrecomputedPerceptionCacheHitCount());
		assertEquals(2, MessageS2CPerception.getPrecomputedPerceptionCacheMissCount());
	}

	@Test
	public void samePerceptionInstanceStillSharesStaticPayload() throws IOException {
		final Perception shared = perceptionWithObject(1);
		final MessageS2CPerception first = new MessageS2CPerception(null, shared);
		final MessageS2CPerception second = new MessageS2CPerception(null, shared);
		first.setProtocolVersion(1);
		second.setProtocolVersion(1);

		final byte[] firstPayload = MessageS2CPerception.CachedCompressedPerception
				.get().get(first);
		final byte[] secondPayload = MessageS2CPerception.CachedCompressedPerception
				.get().get(second);

		assertSame(firstPayload, secondPayload);
		assertEquals(1, MessageS2CPerception.getPrecomputedPerceptionCacheHitCount());
		assertEquals(1, MessageS2CPerception.getPrecomputedPerceptionCacheMissCount());
	}

	private Perception perceptionWithObject(final int id) {
		final Perception perception = new Perception(Perception.SYNC, new IRPZone.ID("test"));
		final RPObject object = new RPObject();
		object.put("id", id);
		object.put("type", "test");
		perception.added(object);
		return perception;
	}
}
