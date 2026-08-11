/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import marauroa.common.game.Perception;
import marauroa.common.game.RPObject;

public class MarauroaRPZonePlayerVisibilityTest {

	private MarauroaRPZone zone;
	private RPObject alice;
	private RPObject bob;

	@Before
	public void setUp() {
		zone = new MarauroaRPZone("test") {
			@Override
			public void onInit() {
			}

			@Override
			public void onFinish() {
			}
		};
		alice = viewer("Alice");
		bob = viewer("Bob");
	}

	@Test
	public void syncPerceptionOnlyContainsMatchingScopedObject() throws Exception {
		final RPObject publicObject = object();
		final RPObject aliceObject = scopedObject("Alice");
		final RPObject bobObject = scopedObject("Bob");
		zone.add(publicObject);
		zone.add(aliceObject);
		zone.add(bobObject);

		final Perception alicePerception = zone.getPerception(alice, Perception.SYNC);
		final Perception bobPerception = zone.getPerception(bob, Perception.SYNC);

		assertEquals(2, alicePerception.addedList.size());
		assertTrue(alicePerception.addedList.contains(publicObject));
		assertTrue(alicePerception.addedList.contains(aliceObject));
		assertEquals(2, bobPerception.addedList.size());
		assertTrue(bobPerception.addedList.contains(publicObject));
		assertTrue(bobPerception.addedList.contains(bobObject));
	}

	@Test
	public void deltaModificationIsOnlySentToMatchingViewer() throws Exception {
		final RPObject aliceObject = scopedObject("Alice");
		zone.add(aliceObject);
		zone.nextTurn();

		aliceObject.put("value", 1);
		zone.modify(aliceObject);

		assertEquals(1, zone.getPerception(alice, Perception.DELTA).modifiedAddedList.size());
		assertEquals(0, zone.getPerception(bob, Perception.DELTA).modifiedAddedList.size());
	}

	@Test
	public void deletionIsOnlySentToMatchingViewer() throws Exception {
		final RPObject aliceObject = scopedObject("Alice");
		zone.add(aliceObject);
		zone.nextTurn();

		zone.remove(aliceObject.getID());

		assertEquals(1, zone.getPerception(alice, Perception.DELTA).deletedList.size());
		assertEquals(0, zone.getPerception(bob, Perception.DELTA).deletedList.size());
	}

	private RPObject viewer(final String name) {
		final RPObject player = new RPObject();
		player.put("name", name);
		return player;
	}

	private RPObject object() {
		final RPObject object = new RPObject();
		zone.assignRPObjectID(object);
		return object;
	}

	private RPObject scopedObject(final String owner) {
		final RPObject object = object();
		object.put(MarauroaRPZone.PERCEPTION_KEY_ATTRIBUTE, "name");
		object.put(MarauroaRPZone.PERCEPTION_VALUE_ATTRIBUTE, owner);
		return object;
	}
}
