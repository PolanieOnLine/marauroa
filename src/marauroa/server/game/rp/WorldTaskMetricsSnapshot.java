/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

/**
 * Immutable runtime metrics snapshot for {@link WorldTaskScheduler}.
 *
 * Values are process-local and intentionally not persisted. Timing values use
 * {@link System#nanoTime()} and are therefore suitable only for durations.
 */
public final class WorldTaskMetricsSnapshot {
	private final long currentTurn;
	private final int pendingTaskCount;
	private final long scheduledTaskCount;
	private final long executedTaskCount;
	private final long failedTaskCount;
	private final long totalTaskDurationNanos;
	private final long maxTaskDurationNanos;
	private final int lastBatchTaskCount;
	private final long lastBatchDurationNanos;
	private final long maxBatchDurationNanos;

	WorldTaskMetricsSnapshot(long currentTurn, int pendingTaskCount,
			long scheduledTaskCount, long executedTaskCount, long failedTaskCount,
			long totalTaskDurationNanos, long maxTaskDurationNanos,
			int lastBatchTaskCount, long lastBatchDurationNanos,
			long maxBatchDurationNanos) {
		this.currentTurn = currentTurn;
		this.pendingTaskCount = pendingTaskCount;
		this.scheduledTaskCount = scheduledTaskCount;
		this.executedTaskCount = executedTaskCount;
		this.failedTaskCount = failedTaskCount;
		this.totalTaskDurationNanos = totalTaskDurationNanos;
		this.maxTaskDurationNanos = maxTaskDurationNanos;
		this.lastBatchTaskCount = lastBatchTaskCount;
		this.lastBatchDurationNanos = lastBatchDurationNanos;
		this.maxBatchDurationNanos = maxBatchDurationNanos;
	}

	public long getCurrentTurn() {
		return currentTurn;
	}

	public int getPendingTaskCount() {
		return pendingTaskCount;
	}

	public long getScheduledTaskCount() {
		return scheduledTaskCount;
	}

	/** Number of callbacks which actually started, including failed callbacks. */
	public long getExecutedTaskCount() {
		return executedTaskCount;
	}

	public long getFailedTaskCount() {
		return failedTaskCount;
	}

	public long getTotalTaskDurationNanos() {
		return totalTaskDurationNanos;
	}

	public long getMaxTaskDurationNanos() {
		return maxTaskDurationNanos;
	}

	public long getAverageTaskDurationNanos() {
		return executedTaskCount == 0 ? 0 : totalTaskDurationNanos / executedTaskCount;
	}

	public int getLastBatchTaskCount() {
		return lastBatchTaskCount;
	}

	public long getLastBatchDurationNanos() {
		return lastBatchDurationNanos;
	}

	public long getMaxBatchDurationNanos() {
		return maxBatchDurationNanos;
	}
}
