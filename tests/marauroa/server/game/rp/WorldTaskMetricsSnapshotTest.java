/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class WorldTaskMetricsSnapshotTest {
	private WorldTaskScheduler scheduler;
	private WorldTaskOwner owner;

	@Before
	public void setUp() {
		scheduler = new WorldTaskScheduler();
		owner = WorldTaskOwner.zone("metrics-zone");
	}

	@Test
	public void snapshotTracksScheduledAndExecutedTasks() {
		scheduler.scheduleNextTurn(owner, new WorldTask() {
			@Override
			public void run() {
				// no-op
			}
		});

		WorldTaskMetricsSnapshot queued = scheduler.getMetricsSnapshot();
		assertEquals(1, queued.getScheduledTaskCount());
		assertEquals(1, queued.getPendingTaskCount());
		assertEquals(0, queued.getExecutedTaskCount());

		scheduler.processNextTurn();

		WorldTaskMetricsSnapshot executed = scheduler.getMetricsSnapshot();
		assertEquals(1, executed.getCurrentTurn());
		assertEquals(0, executed.getPendingTaskCount());
		assertEquals(1, executed.getScheduledTaskCount());
		assertEquals(1, executed.getExecutedTaskCount());
		assertEquals(0, executed.getFailedTaskCount());
		assertEquals(1, executed.getLastBatchTaskCount());
		assertTrue(executed.getTotalTaskDurationNanos() >= 0);
		assertTrue(executed.getMaxTaskDurationNanos() >= 0);
		assertTrue(executed.getLastBatchDurationNanos() >= 0);
		assertTrue(executed.getMaxBatchDurationNanos() >= executed.getLastBatchDurationNanos());
	}

	@Test
	public void failingTaskIsCountedWithoutStoppingMetrics() {
		scheduler.scheduleNextTurn(owner, new WorldTask() {
			@Override
			public void run() throws Exception {
				throw new Exception("expected metrics test failure");
			}
		});
		scheduler.scheduleNextTurn(WorldTaskOwner.global("metrics"), new WorldTask() {
			@Override
			public void run() {
				// no-op
			}
		});

		scheduler.processNextTurn();

		WorldTaskMetricsSnapshot snapshot = scheduler.getMetricsSnapshot();
		assertEquals(2, snapshot.getExecutedTaskCount());
		assertEquals(1, snapshot.getFailedTaskCount());
		assertEquals(2, snapshot.getLastBatchTaskCount());
	}

	@Test
	public void cancelledTaskNeverContributesExecutionTime() {
		WorldTaskHandle handle = scheduler.scheduleNextTurn(owner, new WorldTask() {
			@Override
			public void run() {
				throw new AssertionError("cancelled task must not run");
			}
		});
		assertTrue(handle.cancel());

		scheduler.processNextTurn();

		WorldTaskMetricsSnapshot snapshot = scheduler.getMetricsSnapshot();
		assertEquals(1, snapshot.getScheduledTaskCount());
		assertEquals(0, snapshot.getPendingTaskCount());
		assertEquals(0, snapshot.getExecutedTaskCount());
		assertEquals(0, snapshot.getFailedTaskCount());
		assertEquals(0, snapshot.getLastBatchTaskCount());
		assertEquals(0, snapshot.getLastBatchDurationNanos());
	}
}
