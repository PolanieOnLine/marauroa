/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game;

import marauroa.common.net.message.Message;

/**
 * Process-local metrics for full content transfers performed after client ACK.
 *
 * Byte counts refer to the uncompressed {@code TransferContent.data} payload,
 * not encoded or compressed wire bytes. The server uses the shared instance
 * returned by {@link #getInstance()}; independent instances may be created for
 * tests or isolated embedding.
 */
public final class ContentTransferMetrics {
	private static final ContentTransferMetrics INSTANCE = new ContentTransferMetrics();

	private long ackBatchCount;
	private long requestedFullContentCount;
	private long cacheReuseContentCount;
	private long sentContentCount;
	private long missingContentCount;
	private long sentRawPayloadBytes;
	private long fullSendBatchCount;
	private long fullSendFailureCount;
	private long totalFullSendDurationNanos;
	private long maxFullSendDurationNanos;
	private int slowestClientId = Message.CLIENTID_INVALID;
	private int slowestBatchContentCount;
	private long slowestBatchRawPayloadBytes;

	public ContentTransferMetrics() {
	}

	public static ContentTransferMetrics getInstance() {
		return INSTANCE;
	}

	public synchronized void recordAckBatch(int clientId, int requestedFullCount,
			int cacheReuseCount, int sentCount, int missingCount, long rawPayloadBytes,
			boolean sentBatch, boolean sendSucceeded, long sendDurationNanos) {
		ackBatchCount++;
		requestedFullContentCount += requestedFullCount;
		cacheReuseContentCount += cacheReuseCount;
		sentContentCount += sentCount;
		missingContentCount += missingCount;
		sentRawPayloadBytes += rawPayloadBytes;

		if (sentBatch) {
			fullSendBatchCount++;
			if (!sendSucceeded) {
				fullSendFailureCount++;
			}
			totalFullSendDurationNanos += sendDurationNanos;
			if (slowestClientId == Message.CLIENTID_INVALID || sendDurationNanos > maxFullSendDurationNanos) {
				slowestClientId = clientId;
				slowestBatchContentCount = sentCount;
				slowestBatchRawPayloadBytes = rawPayloadBytes;
			}
			if (sendDurationNanos > maxFullSendDurationNanos) {
				maxFullSendDurationNanos = sendDurationNanos;
			}
		}
	}

	public synchronized ContentTransferMetricsSnapshot getMetricsSnapshot() {
		return new ContentTransferMetricsSnapshot(ackBatchCount, requestedFullContentCount,
				cacheReuseContentCount, sentContentCount, missingContentCount,
				sentRawPayloadBytes, fullSendBatchCount, fullSendFailureCount,
				totalFullSendDurationNanos, maxFullSendDurationNanos, slowestClientId,
				slowestBatchContentCount, slowestBatchRawPayloadBytes);
	}
}
