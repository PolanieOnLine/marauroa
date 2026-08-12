/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ZoneTurnMetricsTest {
	private static final long MS = 1000000L;

	@Test
	public void keepsThreeSlowestZonesAndFailures() {
		ZoneTurnMetrics metrics = new ZoneTurnMetrics();
		metrics.record("fast", true, 2L * MS);
		metrics.record("slowest", true, 9L * MS);
		metrics.record("middle", false, 5L * MS);
		metrics.record("second", true, 7L * MS);

		ZoneTurnMetricsSnapshot snapshot = metrics.snapshot();
		assertEquals(4, snapshot.getZoneCount());
		assertEquals(1, snapshot.getFailureCount());
		assertEquals(23L * MS, snapshot.getTotalDurationNanos());
		assertEquals("slowest", snapshot.getFirstZoneId());
		assertEquals(9L * MS, snapshot.getFirstDurationNanos());
		assertEquals("second", snapshot.getSecondZoneId());
		assertEquals(7L * MS, snapshot.getSecondDurationNanos());
		assertEquals("middle", snapshot.getThirdZoneId());
		assertEquals(5L * MS, snapshot.getThirdDurationNanos());
	}

	@Test
	public void resetClearsPreviousTurn() {
		ZoneTurnMetrics metrics = new ZoneTurnMetrics();
		metrics.record("zone", true, MS);
		metrics.reset();

		ZoneTurnMetricsSnapshot snapshot = metrics.snapshot();
		assertEquals(0, snapshot.getZoneCount());
		assertEquals(0, snapshot.getFailureCount());
		assertEquals(0L, snapshot.getTotalDurationNanos());
		assertNull(snapshot.getFirstZoneId());
		assertNull(snapshot.getSecondZoneId());
		assertNull(snapshot.getThirdZoneId());
	}
}
