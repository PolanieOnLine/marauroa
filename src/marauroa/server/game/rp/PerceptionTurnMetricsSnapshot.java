/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

/** Immutable metrics for one completed perception pass of the RP turn. */
final class PerceptionTurnMetricsSnapshot {
	private final int playerCount;
	private final int syncCount;
	private final int deltaCount;
	private final long totalBuildDurationNanos;
	private final long totalSendDurationNanos;
	private final int slowestClientId;
	private final long slowestBuildDurationNanos;
	private final long slowestSendDurationNanos;
	private final long cacheHitCount;
	private final long cacheMissCount;

	PerceptionTurnMetricsSnapshot(int playerCount, int syncCount, int deltaCount,
			long totalBuildDurationNanos, long totalSendDurationNanos,
			int slowestClientId, long slowestBuildDurationNanos,
			long slowestSendDurationNanos, long cacheHitCount, long cacheMissCount) {
		this.playerCount = playerCount;
		this.syncCount = syncCount;
		this.deltaCount = deltaCount;
		this.totalBuildDurationNanos = totalBuildDurationNanos;
		this.totalSendDurationNanos = totalSendDurationNanos;
		this.slowestClientId = slowestClientId;
		this.slowestBuildDurationNanos = slowestBuildDurationNanos;
		this.slowestSendDurationNanos = slowestSendDurationNanos;
		this.cacheHitCount = cacheHitCount;
		this.cacheMissCount = cacheMissCount;
	}

	int getPlayerCount() {
		return playerCount;
	}

	int getSyncCount() {
		return syncCount;
	}

	int getDeltaCount() {
		return deltaCount;
	}

	long getTotalBuildDurationNanos() {
		return totalBuildDurationNanos;
	}

	long getTotalSendDurationNanos() {
		return totalSendDurationNanos;
	}

	int getSlowestClientId() {
		return slowestClientId;
	}

	long getSlowestBuildDurationNanos() {
		return slowestBuildDurationNanos;
	}

	long getSlowestSendDurationNanos() {
		return slowestSendDurationNanos;
	}

	long getSlowestPlayerDurationNanos() {
		return slowestBuildDurationNanos + slowestSendDurationNanos;
	}

	long getCacheHitCount() {
		return cacheHitCount;
	}

	long getCacheMissCount() {
		return cacheMissCount;
	}
}
