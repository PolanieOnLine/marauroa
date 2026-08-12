/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import marauroa.common.game.Perception;
import marauroa.common.net.message.Message;

/**
 * Allocation-free accumulator for the perception pass of one RP turn.
 *
 * The RP thread is the only writer. An immutable snapshot is created only
 * when diagnostics explicitly request one.
 */
final class PerceptionTurnMetrics {
	private int playerCount;
	private int syncCount;
	private int deltaCount;
	private long totalBuildDurationNanos;
	private long totalSendDurationNanos;
	private int slowestClientId = Message.CLIENTID_INVALID;
	private long slowestBuildDurationNanos;
	private long slowestSendDurationNanos;

	void reset() {
		playerCount = 0;
		syncCount = 0;
		deltaCount = 0;
		totalBuildDurationNanos = 0;
		totalSendDurationNanos = 0;
		slowestClientId = Message.CLIENTID_INVALID;
		slowestBuildDurationNanos = 0;
		slowestSendDurationNanos = 0;
	}

	void record(int clientId, byte perceptionType, long buildDurationNanos,
			long sendDurationNanos) {
		playerCount++;
		if (perceptionType == Perception.SYNC) {
			syncCount++;
		} else if (perceptionType == Perception.DELTA) {
			deltaCount++;
		}

		totalBuildDurationNanos += buildDurationNanos;
		totalSendDurationNanos += sendDurationNanos;

		long playerDurationNanos = buildDurationNanos + sendDurationNanos;
		long slowestDurationNanos = slowestBuildDurationNanos + slowestSendDurationNanos;
		if (slowestClientId == Message.CLIENTID_INVALID || playerDurationNanos > slowestDurationNanos) {
			slowestClientId = clientId;
			slowestBuildDurationNanos = buildDurationNanos;
			slowestSendDurationNanos = sendDurationNanos;
		}
	}

	PerceptionTurnMetricsSnapshot snapshot(long cacheHitCount, long cacheMissCount) {
		return new PerceptionTurnMetricsSnapshot(playerCount, syncCount, deltaCount,
				totalBuildDurationNanos, totalSendDurationNanos,
				slowestClientId, slowestBuildDurationNanos, slowestSendDurationNanos,
				cacheHitCount, cacheMissCount);
	}
}
