/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.common.resource;

/**
 * Immutable process-local metrics for {@link ResourceReloadService}.
 *
 * Timing values describe durations measured with {@link System#nanoTime()}.
 * Metrics are runtime diagnostics only and are intentionally not persisted.
 */
public final class ResourceReloadMetricsSnapshot {
	private final int registeredResourceCount;
	private final int pendingCandidateCount;
	private final long requestCount;
	private final long unknownResourceRequestCount;
	private final long loadSuccessCount;
	private final long loadFailureCount;
	private final long validationSuccessCount;
	private final long validationFailureCount;
	private final long preparedCandidateCount;
	private final long coalescedCandidateCount;
	private final long stalePreparedCandidateCount;
	private final long applySuccessCount;
	private final long applyFailureCount;
	private final long stalePendingCandidateCount;
	private final long totalLoadDurationNanos;
	private final long maxLoadDurationNanos;
	private final long totalValidationDurationNanos;
	private final long maxValidationDurationNanos;
	private final long totalApplyDurationNanos;
	private final long maxApplyDurationNanos;

	ResourceReloadMetricsSnapshot(int registeredResourceCount, int pendingCandidateCount,
			long requestCount, long unknownResourceRequestCount,
			long loadSuccessCount, long loadFailureCount,
			long validationSuccessCount, long validationFailureCount,
			long preparedCandidateCount, long coalescedCandidateCount,
			long stalePreparedCandidateCount, long applySuccessCount,
			long applyFailureCount, long stalePendingCandidateCount,
			long totalLoadDurationNanos, long maxLoadDurationNanos,
			long totalValidationDurationNanos, long maxValidationDurationNanos,
			long totalApplyDurationNanos, long maxApplyDurationNanos) {
		this.registeredResourceCount = registeredResourceCount;
		this.pendingCandidateCount = pendingCandidateCount;
		this.requestCount = requestCount;
		this.unknownResourceRequestCount = unknownResourceRequestCount;
		this.loadSuccessCount = loadSuccessCount;
		this.loadFailureCount = loadFailureCount;
		this.validationSuccessCount = validationSuccessCount;
		this.validationFailureCount = validationFailureCount;
		this.preparedCandidateCount = preparedCandidateCount;
		this.coalescedCandidateCount = coalescedCandidateCount;
		this.stalePreparedCandidateCount = stalePreparedCandidateCount;
		this.applySuccessCount = applySuccessCount;
		this.applyFailureCount = applyFailureCount;
		this.stalePendingCandidateCount = stalePendingCandidateCount;
		this.totalLoadDurationNanos = totalLoadDurationNanos;
		this.maxLoadDurationNanos = maxLoadDurationNanos;
		this.totalValidationDurationNanos = totalValidationDurationNanos;
		this.maxValidationDurationNanos = maxValidationDurationNanos;
		this.totalApplyDurationNanos = totalApplyDurationNanos;
		this.maxApplyDurationNanos = maxApplyDurationNanos;
	}

	public int getRegisteredResourceCount() { return registeredResourceCount; }
	public int getPendingCandidateCount() { return pendingCandidateCount; }
	public long getRequestCount() { return requestCount; }
	public long getUnknownResourceRequestCount() { return unknownResourceRequestCount; }
	public long getLoadSuccessCount() { return loadSuccessCount; }
	public long getLoadFailureCount() { return loadFailureCount; }
	public long getValidationSuccessCount() { return validationSuccessCount; }
	public long getValidationFailureCount() { return validationFailureCount; }
	public long getPreparedCandidateCount() { return preparedCandidateCount; }
	public long getCoalescedCandidateCount() { return coalescedCandidateCount; }
	public long getStalePreparedCandidateCount() { return stalePreparedCandidateCount; }
	public long getApplySuccessCount() { return applySuccessCount; }
	public long getApplyFailureCount() { return applyFailureCount; }
	public long getStalePendingCandidateCount() { return stalePendingCandidateCount; }
	public long getTotalLoadDurationNanos() { return totalLoadDurationNanos; }
	public long getMaxLoadDurationNanos() { return maxLoadDurationNanos; }
	public long getTotalValidationDurationNanos() { return totalValidationDurationNanos; }
	public long getMaxValidationDurationNanos() { return maxValidationDurationNanos; }
	public long getTotalApplyDurationNanos() { return totalApplyDurationNanos; }
	public long getMaxApplyDurationNanos() { return maxApplyDurationNanos; }
}
