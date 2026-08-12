/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game;

/** Immutable process-local metrics for full content transfers after client ACK. */
public final class ContentTransferMetricsSnapshot {
	private final long ackBatchCount;
	private final long requestedFullContentCount;
	private final long cacheReuseContentCount;
	private final long sentContentCount;
	private final long missingContentCount;
	private final long sentRawPayloadBytes;
	private final long fullSendBatchCount;
	private final long fullSendFailureCount;
	private final long totalFullSendDurationNanos;
	private final long maxFullSendDurationNanos;
	private final int slowestClientId;
	private final int slowestBatchContentCount;
	private final long slowestBatchRawPayloadBytes;

	ContentTransferMetricsSnapshot(long ackBatchCount, long requestedFullContentCount,
			long cacheReuseContentCount, long sentContentCount, long missingContentCount,
			long sentRawPayloadBytes, long fullSendBatchCount, long fullSendFailureCount,
			long totalFullSendDurationNanos, long maxFullSendDurationNanos,
			int slowestClientId, int slowestBatchContentCount,
			long slowestBatchRawPayloadBytes) {
		this.ackBatchCount = ackBatchCount;
		this.requestedFullContentCount = requestedFullContentCount;
		this.cacheReuseContentCount = cacheReuseContentCount;
		this.sentContentCount = sentContentCount;
		this.missingContentCount = missingContentCount;
		this.sentRawPayloadBytes = sentRawPayloadBytes;
		this.fullSendBatchCount = fullSendBatchCount;
		this.fullSendFailureCount = fullSendFailureCount;
		this.totalFullSendDurationNanos = totalFullSendDurationNanos;
		this.maxFullSendDurationNanos = maxFullSendDurationNanos;
		this.slowestClientId = slowestClientId;
		this.slowestBatchContentCount = slowestBatchContentCount;
		this.slowestBatchRawPayloadBytes = slowestBatchRawPayloadBytes;
	}

	public long getAckBatchCount() { return ackBatchCount; }
	public long getRequestedFullContentCount() { return requestedFullContentCount; }
	public long getCacheReuseContentCount() { return cacheReuseContentCount; }
	public long getSentContentCount() { return sentContentCount; }
	public long getMissingContentCount() { return missingContentCount; }
	public long getSentRawPayloadBytes() { return sentRawPayloadBytes; }
	public long getFullSendBatchCount() { return fullSendBatchCount; }
	public long getFullSendFailureCount() { return fullSendFailureCount; }
	public long getTotalFullSendDurationNanos() { return totalFullSendDurationNanos; }
	public long getMaxFullSendDurationNanos() { return maxFullSendDurationNanos; }
	public int getSlowestClientId() { return slowestClientId; }
	public int getSlowestBatchContentCount() { return slowestBatchContentCount; }
	public long getSlowestBatchRawPayloadBytes() { return slowestBatchRawPayloadBytes; }
}
