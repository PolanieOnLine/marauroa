/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

/**
 * Allocation-free accumulator for zone {@code nextTurn()} work in one world turn.
 * Keeps the three slowest zones without creating a per-turn map.
 */
final class ZoneTurnMetrics {
	private static final int TOP_COUNT = 3;

	private int zoneCount;
	private int failureCount;
	private long totalDurationNanos;
	private final String[] slowestZoneIds = new String[TOP_COUNT];
	private final long[] slowestDurationsNanos = new long[TOP_COUNT];

	void reset() {
		zoneCount = 0;
		failureCount = 0;
		totalDurationNanos = 0;
		for (int i = 0; i < TOP_COUNT; i++) {
			slowestZoneIds[i] = null;
			slowestDurationsNanos[i] = 0;
		}
	}

	void record(String zoneId, boolean succeeded, long durationNanos) {
		zoneCount++;
		if (!succeeded) {
			failureCount++;
		}
		totalDurationNanos += durationNanos;

		for (int i = 0; i < TOP_COUNT; i++) {
			if (slowestZoneIds[i] == null || durationNanos > slowestDurationsNanos[i]) {
				for (int j = TOP_COUNT - 1; j > i; j--) {
					slowestZoneIds[j] = slowestZoneIds[j - 1];
					slowestDurationsNanos[j] = slowestDurationsNanos[j - 1];
				}
				slowestZoneIds[i] = zoneId;
				slowestDurationsNanos[i] = durationNanos;
				break;
			}
		}
	}

	ZoneTurnMetricsSnapshot snapshot() {
		return new ZoneTurnMetricsSnapshot(zoneCount, failureCount, totalDurationNanos,
				slowestZoneIds[0], slowestDurationsNanos[0],
				slowestZoneIds[1], slowestDurationsNanos[1],
				slowestZoneIds[2], slowestDurationsNanos[2]);
	}
}
