/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class InstanceZoneDescriptorRuntimeIdTest {
	@Test
	public void runtimeIdIsStableCompactAndSafeForLegacyZoneConsumers() {
		InstanceZoneDescriptor first = new InstanceZoneDescriptor("maze", "daily",
				InstanceScope.player("PolanieOnLine"));
		InstanceZoneDescriptor same = new InstanceZoneDescriptor("maze", "daily",
				InstanceScope.player("PolanieOnLine"));

		String runtimeId = first.getRuntimeZoneIdString();
		assertEquals(runtimeId, same.getRuntimeZoneIdString());
		assertTrue(runtimeId.startsWith("instance_"));
		assertTrue(runtimeId.length() <= 32);
		assertFalse(runtimeId.contains("."));
		assertFalse(runtimeId.contains("PolanieOnLine"));
	}

	@Test
	public void logicalIdentityStillSeparatesScopeAndInstance() {
		String player = new InstanceZoneDescriptor("maze", "daily",
				InstanceScope.player("same")).getRuntimeZoneIdString();
		String group = new InstanceZoneDescriptor("maze", "daily",
				InstanceScope.group("same")).getRuntimeZoneIdString();
		String otherRun = new InstanceZoneDescriptor("maze", "other",
				InstanceScope.player("same")).getRuntimeZoneIdString();

		assertFalse(player.equals(group));
		assertFalse(player.equals(otherRun));
	}
}
