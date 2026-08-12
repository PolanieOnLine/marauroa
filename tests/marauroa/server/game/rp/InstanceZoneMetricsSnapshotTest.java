/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import marauroa.common.game.IRPZone;

import org.junit.Before;
import org.junit.Test;

public class InstanceZoneMetricsSnapshotTest {
	private RPWorld world;
	private InstanceZoneManager manager;

	@Before
	public void setUp() {
		world = new RPWorld();
		manager = world.getInstanceZoneManager();
	}

	@Test
	public void tracksActiveInstancesMembershipsAndLifecycle() throws Exception {
		CountingFactory factory = new CountingFactory(false);
		IRPZone zone = manager.acquire("dungeon", "run", InstanceScope.group("party"),
				"alice", factory);
		manager.acquire("dungeon", "run", InstanceScope.group("party"), "alice", factory);
		manager.acquire("dungeon", "run", InstanceScope.group("party"), "bob", factory);

		InstanceZoneMetricsSnapshot active = manager.getMetricsSnapshot();
		assertEquals(1, active.getActiveInstanceCount());
		assertEquals(2, active.getActiveMembershipCount());
		assertEquals(1, active.getCreatedInstanceCount());
		assertEquals(0, active.getCreateFailureCount());
		assertEquals(0, active.getDestroyedInstanceCount());
		assertEquals(0, active.getDestroyFailureCount());
		assertTrue(active.getTotalCreateDurationNanos() >= 0);
		assertTrue(active.getMaxCreateDurationNanos() >= 0);

		manager.release(zone.getID(), "alice");
		assertEquals(1, manager.getMetricsSnapshot().getActiveMembershipCount());
		manager.release(zone.getID(), "bob");

		InstanceZoneMetricsSnapshot destroyed = manager.getMetricsSnapshot();
		assertEquals(0, destroyed.getActiveInstanceCount());
		assertEquals(0, destroyed.getActiveMembershipCount());
		assertEquals(1, destroyed.getCreatedInstanceCount());
		assertEquals(1, destroyed.getDestroyedInstanceCount());
		assertEquals(0, destroyed.getDestroyFailureCount());
		assertTrue(destroyed.getTotalDestroyDurationNanos() >= 0);
		assertTrue(destroyed.getMaxDestroyDurationNanos() >= 0);
	}

	@Test
	public void failedCreateIsCountedWithoutChangingActiveState() throws Exception {
		try {
			manager.acquire("maze", "run", InstanceScope.player("alice"), "alice",
					new InstanceZoneFactory() {
						@Override
						public IRPZone create(InstanceZoneDescriptor descriptor) {
							return new TestZone("wrong-zone-id");
						}
					});
			fail("Expected invalid runtime id");
		} catch (IllegalArgumentException expected) {
			// expected
		}

		InstanceZoneMetricsSnapshot snapshot = manager.getMetricsSnapshot();
		assertEquals(0, snapshot.getActiveInstanceCount());
		assertEquals(0, snapshot.getActiveMembershipCount());
		assertEquals(0, snapshot.getCreatedInstanceCount());
		assertEquals(1, snapshot.getCreateFailureCount());
		assertEquals(0, snapshot.getDestroyedInstanceCount());
	}

	@Test
	public void failedDestroyStillRemovesRuntimeInstance() throws Exception {
		CountingFactory factory = new CountingFactory(true);
		IRPZone zone = manager.acquire("maze", "run", InstanceScope.player("alice"),
				"alice", factory);

		try {
			manager.release(zone.getID(), "alice");
			fail("Expected destroy failure");
		} catch (Exception expected) {
			// expected
		}

		InstanceZoneMetricsSnapshot snapshot = manager.getMetricsSnapshot();
		assertEquals(0, snapshot.getActiveInstanceCount());
		assertEquals(0, snapshot.getActiveMembershipCount());
		assertEquals(1, snapshot.getDestroyedInstanceCount());
		assertEquals(1, snapshot.getDestroyFailureCount());
	}

	@Test
	public void shutdownDropsOutstandingMemberships() throws Exception {
		CountingFactory factory = new CountingFactory(false);
		manager.acquire("arena", "run", InstanceScope.group("party"), "alice", factory);
		manager.acquire("arena", "run", InstanceScope.group("party"), "bob", factory);

		world.onFinish();

		InstanceZoneMetricsSnapshot snapshot = manager.getMetricsSnapshot();
		assertEquals(0, snapshot.getActiveInstanceCount());
		assertEquals(0, snapshot.getActiveMembershipCount());
		assertEquals(1, snapshot.getCreatedInstanceCount());
		assertEquals(1, snapshot.getDestroyedInstanceCount());
	}

	private static final class CountingFactory implements InstanceZoneFactory {
		private final boolean failDestroy;

		private CountingFactory(boolean failDestroy) {
			this.failDestroy = failDestroy;
		}

		@Override
		public IRPZone create(InstanceZoneDescriptor descriptor) {
			return new TestZone(descriptor.getRuntimeZoneIdString());
		}

		@Override
		public void destroy(InstanceZoneDescriptor descriptor, IRPZone zone) throws Exception {
			if (failDestroy) {
				throw new Exception("expected destroy failure");
			}
		}
	}

	private static final class TestZone extends MarauroaRPZone {
		private TestZone(String id) {
			super(id);
		}
	}
}
