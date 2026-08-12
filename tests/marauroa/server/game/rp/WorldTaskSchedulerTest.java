/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

public class WorldTaskSchedulerTest {
	private WorldTaskScheduler scheduler;
	private WorldTaskOwner owner;
	private List<String> calls;

	@Before
	public void setUp() {
		scheduler = new WorldTaskScheduler();
		owner = WorldTaskOwner.zone("test-zone");
		calls = new ArrayList<String>();
	}

	@Test
	public void nextTurnRunsAtNextBoundary() {
		scheduler.scheduleNextTurn(owner, task("next"));
		assertEquals(1, scheduler.getPendingTaskCount());
		scheduler.processNextTurn();
		assertEquals(1, calls.size());
		assertEquals("next", calls.get(0));
		assertEquals(0, scheduler.getPendingTaskCount());
	}

	@Test
	public void delayedTaskRunsAfterRequestedTurns() {
		scheduler.scheduleInTurns(owner, 3, task("later"));
		scheduler.processNextTurn();
		scheduler.processNextTurn();
		assertTrue(calls.isEmpty());
		scheduler.processNextTurn();
		assertEquals(1, calls.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void zeroDelayIsRejected() {
		scheduler.scheduleInTurns(owner, 0, task("bad"));
	}

	@Test
	public void taskScheduledFromTaskDoesNotRunReentrantly() {
		scheduler.scheduleNextTurn(owner, new WorldTask() {
			@Override
			public void run() {
				calls.add("first");
				scheduler.scheduleNextTurn(owner, task("second"));
			}
		});
		scheduler.processNextTurn();
		assertEquals(1, calls.size());
		assertEquals(1, scheduler.getPendingTaskCount());
		scheduler.processNextTurn();
		assertEquals(2, calls.size());
		assertEquals("second", calls.get(1));
	}

	@Test
	public void individualHandleCancelsOneTask() {
		WorldTaskHandle handle = scheduler.scheduleInTurns(owner, 1000, task("cancelled"));
		assertTrue(handle.cancel());
		assertTrue(handle.isCancelled());
		assertEquals(0, scheduler.getPendingTaskCount());
		assertFalse(handle.cancel());
		scheduler.processNextTurn();
		assertTrue(calls.isEmpty());
	}

	@Test
	public void successfulHandleCancellationPreventsDetachedTaskFromStarting() throws Exception {
		final CountDownLatch firstStarted = new CountDownLatch(1);
		final CountDownLatch releaseFirst = new CountDownLatch(1);
		scheduler.scheduleNextTurn(owner, new WorldTask() {
			@Override
			public void run() throws Exception {
				calls.add("first");
				firstStarted.countDown();
				if (!releaseFirst.await(5, TimeUnit.SECONDS)) {
					throw new IllegalStateException("test timed out waiting to release first task");
				}
			}
		});
		final WorldTaskHandle second = scheduler.scheduleNextTurn(owner, task("second"));

		Thread processing = new Thread(new Runnable() {
			@Override
			public void run() {
				scheduler.processNextTurn();
			}
		}, "WorldTaskSchedulerTest");
		processing.start();
		assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
		assertTrue(second.cancel());
		releaseFirst.countDown();
		processing.join(5000);

		assertFalse(processing.isAlive());
		assertEquals(1, calls.size());
		assertEquals("first", calls.get(0));
		assertTrue(second.isCancelled());
	}

	@Test
	public void cancelOwnerRemovesAllQueuedTasksButNotOtherOwners() {
		WorldTaskOwner other = WorldTaskOwner.zone("other-zone");
		scheduler.scheduleInTurns(owner, 2, task("one"));
		scheduler.scheduleInTurns(owner, 3, task("two"));
		scheduler.scheduleNextTurn(other, task("other"));
		assertEquals(2, scheduler.cancelOwner(owner));
		assertEquals(1, scheduler.getPendingTaskCount());
		scheduler.processNextTurn();
		assertEquals(1, calls.size());
		assertEquals("other", calls.get(0));
	}

	@Test
	public void cancellationDuringBoundaryInvalidatesAlreadyDrainedTask() {
		scheduler.scheduleNextTurn(owner, new WorldTask() {
			@Override
			public void run() {
				calls.add("cancel");
				scheduler.cancelOwner(owner);
			}
		});
		scheduler.scheduleNextTurn(owner, task("stale"));
		scheduler.processNextTurn();
		assertEquals(1, calls.size());
		assertEquals("cancel", calls.get(0));
	}

	@Test
	public void newLifecycleAfterOwnerCancellationCanScheduleAgain() {
		scheduler.scheduleInTurns(owner, 5, task("old"));
		scheduler.cancelOwner(owner);
		scheduler.scheduleNextTurn(owner, task("new"));
		scheduler.processNextTurn();
		assertEquals(1, calls.size());
		assertEquals("new", calls.get(0));
	}

	@Test
	public void failingTaskDoesNotPreventFollowingTask() {
		scheduler.scheduleNextTurn(owner, new WorldTask() {
			@Override
			public void run() throws Exception {
				throw new Exception("expected test failure");
			}
		});
		scheduler.scheduleNextTurn(WorldTaskOwner.global("test"), task("after"));
		scheduler.processNextTurn();
		assertEquals(1, calls.size());
		assertEquals("after", calls.get(0));
	}

	private WorldTask task(final String value) {
		return new WorldTask() {
			@Override
			public void run() {
				calls.add(value);
			}
		};
	}
}
