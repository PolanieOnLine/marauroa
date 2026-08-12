/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Schedules generic tasks for deterministic execution on RP safe boundaries.
 * Scheduling may be requested from other threads. Execution happens only from
 * the RP thread through {@link #processNextTurn()}.
 */
public final class WorldTaskScheduler {
	private static final Logger logger = Logger.getLogger(WorldTaskScheduler.class);
	private final Map<Long, List<ScheduledTask>> tasksByTurn = new LinkedHashMap<Long, List<ScheduledTask>>();
	private final Map<WorldTaskOwner, OwnerEpoch> ownerEpochs = new LinkedHashMap<WorldTaskOwner, OwnerEpoch>();
	private long currentTurn;
	private long sequence;
	private long scheduledTaskCount;
	private long executedTaskCount;
	private long failedTaskCount;
	private long totalTaskDurationNanos;
	private long maxTaskDurationNanos;
	private int lastBatchTaskCount;
	private long lastBatchDurationNanos;
	private long maxBatchDurationNanos;

	public synchronized WorldTaskHandle scheduleNextTurn(WorldTaskOwner owner, WorldTask task) {
		return scheduleInTurns(owner, 1, task);
	}

	public synchronized WorldTaskHandle scheduleInTurns(WorldTaskOwner owner, long turns, WorldTask task) {
		if (owner == null) {
			throw new IllegalArgumentException("world task owner must not be null");
		}
		if (task == null) {
			throw new IllegalArgumentException("world task must not be null");
		}
		if (turns < 1) {
			throw new IllegalArgumentException("world task delay must be at least one turn");
		}
		if (Long.MAX_VALUE - currentTurn < turns) {
			throw new IllegalArgumentException("world task due turn overflow");
		}

		OwnerEpoch epoch = getOrCreateOwnerEpoch(owner);
		ScheduledTask scheduled = new ScheduledTask(++sequence, owner, epoch, currentTurn + turns, task);
		List<ScheduledTask> turnTasks = tasksByTurn.get(scheduled.dueTurn);
		if (turnTasks == null) {
			turnTasks = new ArrayList<ScheduledTask>();
			tasksByTurn.put(scheduled.dueTurn, turnTasks);
		}
		turnTasks.add(scheduled);
		scheduledTaskCount++;
		return scheduled;
	}

	/**
	 * Cancels the current lifecycle generation of one owner. Tasks already
	 * detached for execution keep the old epoch and will therefore be skipped
	 * unless they have already started. A later schedule for the same owner
	 * receives a fresh epoch.
	 */
	public synchronized int cancelOwner(WorldTaskOwner owner) {
		if (owner == null) {
			throw new IllegalArgumentException("world task owner must not be null");
		}
		OwnerEpoch epoch = ownerEpochs.remove(owner);
		if (epoch != null) {
			epoch.cancelled = true;
		}

		int removed = 0;
		Iterator<Map.Entry<Long, List<ScheduledTask>>> turns = tasksByTurn.entrySet().iterator();
		while (turns.hasNext()) {
			List<ScheduledTask> turnTasks = turns.next().getValue();
			Iterator<ScheduledTask> tasks = turnTasks.iterator();
			while (tasks.hasNext()) {
				ScheduledTask scheduled = tasks.next();
				if (owner.equals(scheduled.owner)) {
					scheduled.cancelled = true;
					tasks.remove();
					removed++;
				}
			}
			if (turnTasks.isEmpty()) {
				turns.remove();
			}
		}
		return removed;
	}

	public synchronized int getPendingTaskCount() {
		return getPendingTaskCountUnsafe();
	}

	public synchronized long getCurrentTurn() {
		return currentTurn;
	}

	/**
	 * Returns one coherent, immutable view of the scheduler runtime metrics.
	 *
	 * Timing is collected only when at least one callback actually starts. An
	 * unused scheduler therefore does not add nanoTime calls to the RP turn.
	 */
	public synchronized WorldTaskMetricsSnapshot getMetricsSnapshot() {
		return new WorldTaskMetricsSnapshot(currentTurn, getPendingTaskCountUnsafe(),
				scheduledTaskCount, executedTaskCount, failedTaskCount,
				totalTaskDurationNanos, maxTaskDurationNanos,
				lastBatchTaskCount, lastBatchDurationNanos, maxBatchDurationNanos);
	}

	/** Advances one safe-boundary turn and executes a detached snapshot. */
	public void processNextTurn() {
		final long processingTurn;
		final List<ScheduledTask> due = new ArrayList<ScheduledTask>();
		synchronized (this) {
			currentTurn++;
			processingTurn = currentTurn;
			Iterator<Map.Entry<Long, List<ScheduledTask>>> turns = tasksByTurn.entrySet().iterator();
			while (turns.hasNext()) {
				Map.Entry<Long, List<ScheduledTask>> entry = turns.next();
				if (entry.getKey().longValue() <= processingTurn) {
					due.addAll(entry.getValue());
					turns.remove();
				}
			}
		}

		long batchStartNanos = 0;
		int batchTaskCount = 0;
		for (ScheduledTask scheduled : due) {
			if (!beginExecution(scheduled)) {
				continue;
			}
			if (batchTaskCount == 0) {
				batchStartNanos = System.nanoTime();
			}
			batchTaskCount++;
			long taskStartNanos = System.nanoTime();
			boolean failed = false;
			try {
				scheduled.task.run();
			} catch (Exception e) {
				failed = true;
				logger.error("World task failed [owner=" + scheduled.owner + ", sequence="
						+ scheduled.sequence + ", turn=" + processingTurn + "]", e);
			} finally {
				recordTaskExecution(System.nanoTime() - taskStartNanos, failed);
			}
		}
		cleanupUnusedOwnerEpochs();
		recordBatch(batchTaskCount,
				batchTaskCount == 0 ? 0 : System.nanoTime() - batchStartNanos);
	}

	/**
	 * Atomically claims one detached task for execution. Cancellation and task
	 * start use the same scheduler monitor, so a successful handle cancellation
	 * guarantees that the task cannot begin afterwards.
	 */
	private synchronized boolean beginExecution(ScheduledTask task) {
		if (task.cancelled || task.epoch.cancelled || task.started) {
			return false;
		}
		task.started = true;
		return true;
	}

	/**
	 * Cancels one task and removes it from the queued turn when it has not yet
	 * been detached for execution. A detached task is still marked cancelled so
	 * {@link #beginExecution(ScheduledTask)} will reject it atomically.
	 */
	private synchronized boolean cancelTask(ScheduledTask task) {
		if (task.started || task.cancelled || task.epoch.cancelled) {
			return false;
		}
		task.cancelled = true;

		Long dueTurn = Long.valueOf(task.dueTurn);
		List<ScheduledTask> turnTasks = tasksByTurn.get(dueTurn);
		if (turnTasks != null) {
			turnTasks.remove(task);
			if (turnTasks.isEmpty()) {
				tasksByTurn.remove(dueTurn);
			}
		}
		return true;
	}

	private int getPendingTaskCountUnsafe() {
		int total = 0;
		for (List<ScheduledTask> tasks : tasksByTurn.values()) {
			for (ScheduledTask task : tasks) {
				if (!task.cancelled && !task.epoch.cancelled) {
					total++;
				}
			}
		}
		return total;
	}

	private synchronized void recordTaskExecution(long durationNanos, boolean failed) {
		executedTaskCount++;
		if (failed) {
			failedTaskCount++;
		}
		totalTaskDurationNanos += durationNanos;
		if (durationNanos > maxTaskDurationNanos) {
			maxTaskDurationNanos = durationNanos;
		}
	}

	private synchronized void recordBatch(int taskCount, long durationNanos) {
		lastBatchTaskCount = taskCount;
		lastBatchDurationNanos = durationNanos;
		if (durationNanos > maxBatchDurationNanos) {
			maxBatchDurationNanos = durationNanos;
		}
	}

	private OwnerEpoch getOrCreateOwnerEpoch(WorldTaskOwner owner) {
		OwnerEpoch epoch = ownerEpochs.get(owner);
		if (epoch == null || epoch.cancelled) {
			epoch = new OwnerEpoch();
			ownerEpochs.put(owner, epoch);
		}
		return epoch;
	}

	private synchronized void cleanupUnusedOwnerEpochs() {
		Iterator<Map.Entry<WorldTaskOwner, OwnerEpoch>> epochs = ownerEpochs.entrySet().iterator();
		while (epochs.hasNext()) {
			Map.Entry<WorldTaskOwner, OwnerEpoch> entry = epochs.next();
			boolean used = false;
			for (List<ScheduledTask> tasks : tasksByTurn.values()) {
				for (ScheduledTask task : tasks) {
					if (task.epoch == entry.getValue()) {
						used = true;
						break;
					}
				}
				if (used) {
					break;
				}
			}
			if (!used) {
				epochs.remove();
			}
		}
	}

	private static final class OwnerEpoch {
		private boolean cancelled;
	}

	private final class ScheduledTask implements WorldTaskHandle {
		private final long sequence;
		private final WorldTaskOwner owner;
		private final OwnerEpoch epoch;
		private final long dueTurn;
		private final WorldTask task;
		private boolean cancelled;
		private boolean started;

		private ScheduledTask(long sequence, WorldTaskOwner owner, OwnerEpoch epoch,
				long dueTurn, WorldTask task) {
			this.sequence = sequence;
			this.owner = owner;
			this.epoch = epoch;
			this.dueTurn = dueTurn;
			this.task = task;
		}

		@Override
		public boolean cancel() {
			return cancelTask(this);
		}

		@Override
		public boolean isCancelled() {
			synchronized (WorldTaskScheduler.this) {
				return cancelled || epoch.cancelled;
			}
		}
	}
}
