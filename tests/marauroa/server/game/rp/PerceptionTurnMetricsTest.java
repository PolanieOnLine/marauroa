/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import marauroa.common.game.Perception;
import marauroa.common.net.message.Message;

public class PerceptionTurnMetricsTest {
	private PerceptionTurnMetrics metrics;

	@Before
	public void setUp() {
		metrics = new PerceptionTurnMetrics();
	}

	@Test
	public void tracksTypesDurationsAndSlowestClient() {
		metrics.record(10, Perception.DELTA, 3, 5);
		metrics.record(20, Perception.SYNC, 10, 2);

		PerceptionTurnMetricsSnapshot snapshot = metrics.snapshot(4, 1);
		assertEquals(2, snapshot.getPlayerCount());
		assertEquals(1, snapshot.getSyncCount());
		assertEquals(1, snapshot.getDeltaCount());
		assertEquals(13, snapshot.getTotalBuildDurationNanos());
		assertEquals(7, snapshot.getTotalSendDurationNanos());
		assertEquals(20, snapshot.getSlowestClientId());
		assertEquals(10, snapshot.getSlowestBuildDurationNanos());
		assertEquals(2, snapshot.getSlowestSendDurationNanos());
		assertEquals(12, snapshot.getSlowestPlayerDurationNanos());
		assertEquals(4, snapshot.getCacheHitCount());
		assertEquals(1, snapshot.getCacheMissCount());
	}

	@Test
	public void resetStartsFreshTurnWithoutAllocatingNewTracker() {
		metrics.record(10, Perception.SYNC, 10, 20);
		metrics.reset();

		PerceptionTurnMetricsSnapshot snapshot = metrics.snapshot(0, 0);
		assertEquals(0, snapshot.getPlayerCount());
		assertEquals(0, snapshot.getSyncCount());
		assertEquals(0, snapshot.getDeltaCount());
		assertEquals(0, snapshot.getTotalBuildDurationNanos());
		assertEquals(0, snapshot.getTotalSendDurationNanos());
		assertEquals(Message.CLIENTID_INVALID, snapshot.getSlowestClientId());
		assertEquals(0, snapshot.getSlowestPlayerDurationNanos());
	}
}
