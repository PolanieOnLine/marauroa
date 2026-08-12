/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

/** Immutable metrics for one completed RP content-transfer offer phase. */
final class TransferTurnMetricsSnapshot {
	private final int offerBatchCount;
	private final int offerSendFailureCount;
	private final int offeredContentCount;
	private final int cacheableContentCount;
	private final long offeredRawPayloadBytes;
	private final long totalSendDurationNanos;
	private final int slowestClientId;
	private final long slowestSendDurationNanos;
	private final int slowestBatchContentCount;
	private final long slowestBatchRawPayloadBytes;

	TransferTurnMetricsSnapshot(int offerBatchCount, int offerSendFailureCount,
			int offeredContentCount, int cacheableContentCount,
			long offeredRawPayloadBytes, long totalSendDurationNanos,
			int slowestClientId, long slowestSendDurationNanos,
			int slowestBatchContentCount, long slowestBatchRawPayloadBytes) {
		this.offerBatchCount = offerBatchCount;
		this.offerSendFailureCount = offerSendFailureCount;
		this.offeredContentCount = offeredContentCount;
		this.cacheableContentCount = cacheableContentCount;
		this.offeredRawPayloadBytes = offeredRawPayloadBytes;
		this.totalSendDurationNanos = totalSendDurationNanos;
		this.slowestClientId = slowestClientId;
		this.slowestSendDurationNanos = slowestSendDurationNanos;
		this.slowestBatchContentCount = slowestBatchContentCount;
		this.slowestBatchRawPayloadBytes = slowestBatchRawPayloadBytes;
	}

	int getOfferBatchCount() { return offerBatchCount; }
	int getOfferSendFailureCount() { return offerSendFailureCount; }
	int getOfferedContentCount() { return offeredContentCount; }
	int getCacheableContentCount() { return cacheableContentCount; }
	long getOfferedRawPayloadBytes() { return offeredRawPayloadBytes; }
	long getTotalSendDurationNanos() { return totalSendDurationNanos; }
	int getSlowestClientId() { return slowestClientId; }
	long getSlowestSendDurationNanos() { return slowestSendDurationNanos; }
	int getSlowestBatchContentCount() { return slowestBatchContentCount; }
	long getSlowestBatchRawPayloadBytes() { return slowestBatchRawPayloadBytes; }
}
