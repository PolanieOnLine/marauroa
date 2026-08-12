/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import marauroa.common.net.message.Message;

/**
 * Allocation-free RP-thread accumulator for one transfer-offer phase.
 * Byte counts describe uncompressed {@code TransferContent.data} payloads.
 */
final class TransferTurnMetrics {
	private int offerBatchCount;
	private int offerSendFailureCount;
	private int offeredContentCount;
	private int cacheableContentCount;
	private long offeredRawPayloadBytes;
	private long totalSendDurationNanos;
	private int slowestClientId = Message.CLIENTID_INVALID;
	private long slowestSendDurationNanos;
	private int slowestBatchContentCount;
	private long slowestBatchRawPayloadBytes;

	void reset() {
		offerBatchCount = 0;
		offerSendFailureCount = 0;
		offeredContentCount = 0;
		cacheableContentCount = 0;
		offeredRawPayloadBytes = 0;
		totalSendDurationNanos = 0;
		slowestClientId = Message.CLIENTID_INVALID;
		slowestSendDurationNanos = 0;
		slowestBatchContentCount = 0;
		slowestBatchRawPayloadBytes = 0;
	}

	void recordOfferBatch(int clientId, int contentCount, int cacheableCount,
			long rawPayloadBytes, boolean sendSucceeded, long sendDurationNanos) {
		offerBatchCount++;
		if (!sendSucceeded) {
			offerSendFailureCount++;
		}
		offeredContentCount += contentCount;
		cacheableContentCount += cacheableCount;
		offeredRawPayloadBytes += rawPayloadBytes;
		totalSendDurationNanos += sendDurationNanos;
		if (slowestClientId == Message.CLIENTID_INVALID || sendDurationNanos > slowestSendDurationNanos) {
			slowestClientId = clientId;
			slowestSendDurationNanos = sendDurationNanos;
			slowestBatchContentCount = contentCount;
			slowestBatchRawPayloadBytes = rawPayloadBytes;
		}
	}

	TransferTurnMetricsSnapshot snapshot() {
		return new TransferTurnMetricsSnapshot(offerBatchCount, offerSendFailureCount,
				offeredContentCount, cacheableContentCount, offeredRawPayloadBytes,
				totalSendDurationNanos, slowestClientId, slowestSendDurationNanos,
				slowestBatchContentCount, slowestBatchRawPayloadBytes);
	}
}
