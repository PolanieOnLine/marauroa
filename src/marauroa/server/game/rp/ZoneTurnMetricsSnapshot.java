/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

/** Immutable metrics for one completed {@link RPWorld#nextTurn()} zone pass. */
final class ZoneTurnMetricsSnapshot {
	private final int zoneCount;
	private final int failureCount;
	private final long totalDurationNanos;
	private final String firstZoneId;
	private final long firstDurationNanos;
	private final String secondZoneId;
	private final long secondDurationNanos;
	private final String thirdZoneId;
	private final long thirdDurationNanos;

	ZoneTurnMetricsSnapshot(int zoneCount, int failureCount, long totalDurationNanos,
			String firstZoneId, long firstDurationNanos,
			String secondZoneId, long secondDurationNanos,
			String thirdZoneId, long thirdDurationNanos) {
		this.zoneCount = zoneCount;
		this.failureCount = failureCount;
		this.totalDurationNanos = totalDurationNanos;
		this.firstZoneId = firstZoneId;
		this.firstDurationNanos = firstDurationNanos;
		this.secondZoneId = secondZoneId;
		this.secondDurationNanos = secondDurationNanos;
		this.thirdZoneId = thirdZoneId;
		this.thirdDurationNanos = thirdDurationNanos;
	}

	int getZoneCount() { return zoneCount; }
	int getFailureCount() { return failureCount; }
	long getTotalDurationNanos() { return totalDurationNanos; }
	String getFirstZoneId() { return firstZoneId; }
	long getFirstDurationNanos() { return firstDurationNanos; }
	String getSecondZoneId() { return secondZoneId; }
	long getSecondDurationNanos() { return secondDurationNanos; }
	String getThirdZoneId() { return thirdZoneId; }
	long getThirdDurationNanos() { return thirdDurationNanos; }
}
