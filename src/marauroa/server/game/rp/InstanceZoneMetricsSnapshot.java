/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

/**
 * Immutable process-local metrics for the {@link InstanceZoneManager}.
 *
 * Metrics describe runtime lifecycle only and are intentionally not persisted.
 */
public final class InstanceZoneMetricsSnapshot {
	private final int activeInstanceCount;
	private final int activeMembershipCount;
	private final long createdInstanceCount;
	private final long createFailureCount;
	private final long destroyedInstanceCount;
	private final long destroyFailureCount;
	private final long totalCreateDurationNanos;
	private final long maxCreateDurationNanos;
	private final long totalDestroyDurationNanos;
	private final long maxDestroyDurationNanos;

	InstanceZoneMetricsSnapshot(int activeInstanceCount, int activeMembershipCount,
			long createdInstanceCount, long createFailureCount,
			long destroyedInstanceCount, long destroyFailureCount,
			long totalCreateDurationNanos, long maxCreateDurationNanos,
			long totalDestroyDurationNanos, long maxDestroyDurationNanos) {
		this.activeInstanceCount = activeInstanceCount;
		this.activeMembershipCount = activeMembershipCount;
		this.createdInstanceCount = createdInstanceCount;
		this.createFailureCount = createFailureCount;
		this.destroyedInstanceCount = destroyedInstanceCount;
		this.destroyFailureCount = destroyFailureCount;
		this.totalCreateDurationNanos = totalCreateDurationNanos;
		this.maxCreateDurationNanos = maxCreateDurationNanos;
		this.totalDestroyDurationNanos = totalDestroyDurationNanos;
		this.maxDestroyDurationNanos = maxDestroyDurationNanos;
	}

	public int getActiveInstanceCount() {
		return activeInstanceCount;
	}

	/** Number of unique member registrations across all active instances. */
	public int getActiveMembershipCount() {
		return activeMembershipCount;
	}

	/** Number of instances successfully created and registered. */
	public long getCreatedInstanceCount() {
		return createdInstanceCount;
	}

	/** Number of create attempts which failed before registration completed. */
	public long getCreateFailureCount() {
		return createFailureCount;
	}

	/** Number of managed instances removed from the runtime registry. */
	public long getDestroyedInstanceCount() {
		return destroyedInstanceCount;
	}

	/** Number of destroy attempts which completed with an exception. */
	public long getDestroyFailureCount() {
		return destroyFailureCount;
	}

	/** Total duration of create attempts, both successful and failed. */
	public long getTotalCreateDurationNanos() {
		return totalCreateDurationNanos;
	}

	public long getMaxCreateDurationNanos() {
		return maxCreateDurationNanos;
	}

	/** Total duration of destroy attempts, both successful and failed. */
	public long getTotalDestroyDurationNanos() {
		return totalDestroyDurationNanos;
	}

	public long getMaxDestroyDurationNanos() {
		return maxDestroyDurationNanos;
	}
}
