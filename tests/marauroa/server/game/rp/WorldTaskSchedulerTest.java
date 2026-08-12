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

import marauroa.common.game.IRPZone;

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

	@Test
	public void removingZoneCancelsTasksOwnedByZone() throws Exception {
		RPWorld world = new RPWorld();
		MarauroaRPZone zone = new MarauroaRPZone("temporary-zone");
		world.addRPZone(zone);
		WorldTaskScheduler worldScheduler = world.getWorldTaskScheduler();
		worldScheduler.scheduleNextTurn(WorldTaskOwner.zone(zone.getID().getID()), task("stale-zone"));
		assertEquals(1, worldScheduler.getPendingTaskCount());

		world.removeRPZone(zone.getID());
		assertFalse(world.hasRPZone(zone.getID()));
		assertEquals(0, worldScheduler.getPendingTaskCount());
		worldScheduler.processNextTurn();
		assertTrue(calls.isEmpty());
	}

	@Test
	public void destroyingInstanceCancelsInstanceAndZoneOwnedTasks() throws Exception {
		final RPWorld world = new RPWorld();
		final InstanceZoneManager instances = world.getInstanceZoneManager();
		IRPZone zone = instances.acquire("maze", "run", InstanceScope.player("alice"),
				"alice", new InstanceZoneFactory() {
					@Override
					public IRPZone create(InstanceZoneDescriptor descriptor) {
						return new MarauroaRPZone(descriptor.getRuntimeZoneIdString());
					}
				});
		String runtimeZoneId = zone.getID().getID();
		world.getWorldTaskScheduler().scheduleNextTurn(
				WorldTaskOwner.instance(runtimeZoneId), task("stale-instance"));
		world.getWorldTaskScheduler().scheduleNextTurn(
				WorldTaskOwner.zone(runtimeZoneId), task("stale-zone"));
		assertEquals(2, world.getWorldTaskScheduler().getPendingTaskCount());

		instances.release(zone.getID(), "alice");
		assertFalse(world.hasRPZone(zone.getID()));
		assertEquals(0, world.getWorldTaskScheduler().getPendingTaskCount());
		world.getWorldTaskScheduler().processNextTurn();
		assertTrue(calls.isEmpty());
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
