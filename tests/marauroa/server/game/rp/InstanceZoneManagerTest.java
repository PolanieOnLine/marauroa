/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import marauroa.common.game.IRPZone;
import marauroa.common.game.Perception;
import marauroa.common.game.RPObject;

import org.junit.Before;
import org.junit.Test;

public class InstanceZoneManagerTest {
	private RPWorld world;
	private InstanceZoneManager manager;
	private CountingFactory factory;

	@Before
	public void setUp() {
		world = new RPWorld();
		manager = world.getInstanceZoneManager();
		factory = new CountingFactory();
	}

	@Test
	public void descriptorProducesStableCollisionSafeRuntimeIds() {
		InstanceZoneDescriptor first = new InstanceZoneDescriptor("int/maze", "run 1",
				InstanceScope.player("Alice Example"));
		InstanceZoneDescriptor same = new InstanceZoneDescriptor("int/maze", "run 1",
				InstanceScope.player("Alice Example"));
		InstanceZoneDescriptor group = new InstanceZoneDescriptor("int/maze", "run 1",
				InstanceScope.group("Alice Example"));

		assertEquals(first, same);
		assertEquals(first.getRuntimeZoneId(), same.getRuntimeZoneId());
		assertFalse(first.getRuntimeZoneId().equals(group.getRuntimeZoneId()));
		assertTrue(first.getRuntimeZoneIdString().startsWith("__instance__."));
		assertFalse(first.getRuntimeZoneIdString().contains("Alice Example"));
		assertFalse(first.getRuntimeZoneIdString().contains("int/maze"));
	}

	@Test
	public void repeatedAcquireIsIdempotentForSameMemberAndDescriptor() throws Exception {
		IRPZone first = manager.acquire("maze", "default", InstanceScope.player("alice"),
				"alice", factory);
		IRPZone second = manager.acquire("maze", "default", InstanceScope.player("alice"),
				"alice", factory);

		assertSame(first, second);
		assertEquals(1, factory.createCount);
		assertEquals(1, manager.getInstanceCount());
		assertEquals(1, manager.getMemberCount(first.getID()));
	}

	@Test
	public void differentScopesAndInstanceIdsAreIsolated() throws Exception {
		TestZone playerZone = (TestZone) manager.acquire("arena", "one",
				InstanceScope.player("same-key"), "alice", factory);
		TestZone groupZone = (TestZone) manager.acquire("arena", "one",
				InstanceScope.group("same-key"), "alice", factory);
		TestZone secondRun = (TestZone) manager.acquire("arena", "two",
				InstanceScope.player("same-key"), "alice", factory);

		assertFalse(playerZone.getID().equals(groupZone.getID()));
		assertFalse(playerZone.getID().equals(secondRun.getID()));
		assertEquals(3, manager.getInstanceCount());

		RPObject playerObject = new RPObject();
		playerZone.assignRPObjectID(playerObject);
		playerZone.add(playerObject);
		RPObject groupObject = new RPObject();
		groupZone.assignRPObjectID(groupObject);
		groupZone.add(groupObject);

		assertTrue(playerZone.has(playerObject.getID()));
		assertFalse(groupZone.has(playerObject.getID()));
		assertTrue(groupZone.has(groupObject.getID()));
		assertFalse(playerZone.has(groupObject.getID()));
		assertEquals(1, playerZone.getPerception(null, Perception.SYNC).addedList.size());
		assertEquals(1, groupZone.getPerception(null, Perception.SYNC).addedList.size());
	}

	@Test
	public void twoMembersKeepZoneUntilLastRelease() throws Exception {
		IRPZone zone = manager.acquire("dungeon", "party-run", InstanceScope.group("party-7"),
				"alice", factory);
		manager.acquire("dungeon", "party-run", InstanceScope.group("party-7"),
				"bob", factory);

		assertEquals(2, manager.getMemberCount(zone.getID()));
		assertTrue(manager.release(zone.getID(), "alice"));
		assertTrue(world.hasRPZone(zone.getID()));
		assertEquals(1, manager.getMemberCount(zone.getID()));
		assertEquals(0, factory.destroyCount);

		assertTrue(manager.release(zone.getID(), "bob"));
		assertFalse(world.hasRPZone(zone.getID()));
		assertEquals(0, manager.getInstanceCount());
		assertEquals(1, factory.destroyCount);
		assertEquals(0, ((TestZone) zone).finishCount);
	}

	@Test
	public void releaseMemberCleansDisconnectedMemberAcrossInstances() throws Exception {
		IRPZone solo = manager.acquire("maze", "solo", InstanceScope.player("alice"),
				"alice", factory);
		IRPZone party = manager.acquire("arena", "party", InstanceScope.group("group-1"),
				"alice", factory);
		manager.acquire("arena", "party", InstanceScope.group("group-1"),
				"bob", factory);

		assertEquals(2, manager.releaseMember("alice"));
		assertFalse(world.hasRPZone(solo.getID()));
		assertTrue(world.hasRPZone(party.getID()));
		assertEquals(1, manager.getMemberCount(party.getID()));
		assertEquals(1, factory.destroyCount);
	}

	@Test
	public void factoryMustReturnExpectedRuntimeZoneId() throws Exception {
		try {
			manager.acquire("maze", "one", InstanceScope.player("alice"), "alice",
					new InstanceZoneFactory() {
						@Override
						public IRPZone create(InstanceZoneDescriptor descriptor) {
							return new TestZone("wrong-zone-id");
						}
					});
			fail("Expected factory zone id validation to reject the candidate");
		} catch (IllegalArgumentException expected) {
			assertEquals(0, manager.getInstanceCount());
		}
	}

	@Test
	public void ordinaryZoneCollisionIsRejectedWithoutReplacement() throws Exception {
		InstanceZoneDescriptor descriptor = new InstanceZoneDescriptor("maze", "one",
				InstanceScope.player("alice"));
		TestZone ordinary = new TestZone(descriptor.getRuntimeZoneIdString());
		world.addRPZone(ordinary);

		try {
			manager.acquire("maze", "one", InstanceScope.player("alice"), "alice", factory);
			fail("Expected runtime id collision to be rejected");
		} catch (IllegalStateException expected) {
			assertSame(ordinary, world.getRPZone(descriptor.getRuntimeZoneId()));
			assertEquals(0, factory.createCount);
		}
	}

	@Test
	public void managedInstanceCannotBeRemovedThroughPersistentWorldPath() throws Exception {
		IRPZone zone = manager.acquire("maze", "one", InstanceScope.player("alice"),
				"alice", factory);
		try {
			world.removeRPZone(zone.getID());
			fail("Expected managed instance removal guard");
		} catch (IllegalStateException expected) {
			assertTrue(world.hasRPZone(zone.getID()));
			assertEquals(1, manager.getInstanceCount());
		}
	}

	@Test
	public void worldShutdownDestroysInstancesWithoutCallingPersistentZoneFinish() throws Exception {
		TestZone zone = (TestZone) manager.acquire("maze", "one", InstanceScope.player("alice"),
				"alice", factory);

		world.onFinish();

		assertFalse(world.hasRPZone(zone.getID()));
		assertEquals(0, manager.getInstanceCount());
		assertEquals(1, factory.destroyCount);
		assertEquals(0, zone.finishCount);
	}

	private static final class CountingFactory implements InstanceZoneFactory {
		private int createCount;
		private int destroyCount;

		@Override
		public IRPZone create(InstanceZoneDescriptor descriptor) {
			createCount++;
			return new TestZone(descriptor.getRuntimeZoneIdString());
		}

		@Override
		public void destroy(InstanceZoneDescriptor descriptor, IRPZone zone) {
			destroyCount++;
		}
	}

	private static final class TestZone extends MarauroaRPZone {
		private int finishCount;

		private TestZone(String id) {
			super(id);
		}

		@Override
		public void onFinish() {
			finishCount++;
		}
	}
}
