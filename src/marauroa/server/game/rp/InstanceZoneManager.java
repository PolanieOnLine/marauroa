/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import marauroa.common.game.IRPZone;

/**
 * Runtime registry and membership lifecycle for ephemeral zone instances.
 *
 * The manager is intentionally generic. It does not know how a game creates a
 * map, what a player or group identifier means, or how portals are routed.
 * Games provide those pieces through opaque scope/member identifiers and an
 * {@link InstanceZoneFactory}.
 *
 * Instance registry state is memory-only. Managed zones are detached without
 * invoking normal zone persistence when the final member leaves or the world
 * shuts down.
 */
public final class InstanceZoneManager {
	private final RPWorld world;
	private final Map<InstanceZoneDescriptor, ManagedInstance> byDescriptor;
	private final Map<IRPZone.ID, ManagedInstance> byZoneId;

	InstanceZoneManager(RPWorld world) {
		if (world == null) {
			throw new IllegalArgumentException("world must not be null");
		}
		this.world = world;
		this.byDescriptor = new LinkedHashMap<InstanceZoneDescriptor, ManagedInstance>();
		this.byZoneId = new HashMap<IRPZone.ID, ManagedInstance>();
	}

	/**
	 * Acquires a member's place in a logical zone instance, creating the zone
	 * on first use.
	 *
	 * Repeated acquisition by the same member is idempotent.
	 */
	public synchronized IRPZone acquire(String baseZoneId, String instanceId, InstanceScope scope,
			String memberId, InstanceZoneFactory factory) throws Exception {
		if (factory == null) {
			throw new IllegalArgumentException("instance zone factory must not be null");
		}
		String normalizedMember = InstanceScope.requireValue(memberId, "instance member id");
		InstanceZoneDescriptor descriptor = new InstanceZoneDescriptor(baseZoneId, instanceId, scope);

		ManagedInstance existing = byDescriptor.get(descriptor);
		if (existing != null) {
			existing.members.add(normalizedMember);
			return existing.zone;
		}

		IRPZone.ID runtimeId = descriptor.getRuntimeZoneId();
		if (world.hasRPZone(runtimeId)) {
			throw new IllegalStateException("Runtime zone id is already in use outside the instance registry: "
					+ runtimeId.getID());
		}

		IRPZone zone = null;
		try {
			zone = factory.create(descriptor);
			if (zone == null) {
				throw new IllegalArgumentException("instance zone factory returned null for " + descriptor);
			}
			if (!runtimeId.equals(zone.getID())) {
				throw new IllegalArgumentException("instance zone factory returned zone id '"
						+ zone.getID().getID() + "' but expected '" + runtimeId.getID() + "'");
			}

			world.addRPZone(zone);
			ManagedInstance managed = new ManagedInstance(descriptor, zone, factory);
			managed.members.add(normalizedMember);
			byDescriptor.put(descriptor, managed);
			byZoneId.put(runtimeId, managed);
			return zone;
		} catch (Exception e) {
			if (zone != null) {
				try {
					factory.destroy(descriptor, zone);
				} catch (Exception cleanupError) {
					e.addSuppressed(cleanupError);
				}
			}
			throw e;
		}
	}

	/**
	 * Releases one member. The instance is destroyed immediately when no
	 * members remain.
	 *
	 * @return true if the member was registered in the instance
	 */
	public synchronized boolean release(IRPZone.ID runtimeZoneId, String memberId) throws Exception {
		if (runtimeZoneId == null) {
			throw new IllegalArgumentException("runtime zone id must not be null");
		}
		String normalizedMember = InstanceScope.requireValue(memberId, "instance member id");
		ManagedInstance managed = byZoneId.get(runtimeZoneId);
		if (managed == null || !managed.members.remove(normalizedMember)) {
			return false;
		}

		if (managed.members.isEmpty()) {
			destroy(managed);
		}
		return true;
	}

	/**
	 * Releases a member from every managed instance.
	 *
	 * This is intended as a generic disconnect/timeout safety net.
	 *
	 * @return number of memberships released
	 */
	public synchronized int releaseMember(String memberId) throws Exception {
		String normalizedMember = InstanceScope.requireValue(memberId, "instance member id");
		List<ManagedInstance> snapshot = new ArrayList<ManagedInstance>(byDescriptor.values());
		int released = 0;
		Exception failure = null;

		for (ManagedInstance managed : snapshot) {
			if (!managed.members.remove(normalizedMember)) {
				continue;
			}
			released++;
			if (managed.members.isEmpty()) {
				try {
					destroy(managed);
				} catch (Exception e) {
					if (failure == null) {
						failure = e;
					} else {
						failure.addSuppressed(e);
					}
				}
			}
		}

		if (failure != null) {
			throw failure;
		}
		return released;
	}

	public synchronized boolean isInstanceZone(IRPZone.ID runtimeZoneId) {
		return runtimeZoneId != null && byZoneId.containsKey(runtimeZoneId);
	}

	public synchronized InstanceZoneDescriptor getDescriptor(IRPZone.ID runtimeZoneId) {
		ManagedInstance managed = runtimeZoneId == null ? null : byZoneId.get(runtimeZoneId);
		return managed == null ? null : managed.descriptor;
	}

	public synchronized int getMemberCount(IRPZone.ID runtimeZoneId) {
		ManagedInstance managed = runtimeZoneId == null ? null : byZoneId.get(runtimeZoneId);
		return managed == null ? 0 : managed.members.size();
	}

	public synchronized int getInstanceCount() {
		return byDescriptor.size();
	}

	/** Returns an immutable snapshot of active instance descriptors. */
	public synchronized Collection<InstanceZoneDescriptor> getInstances() {
		return java.util.Collections.unmodifiableList(
				new ArrayList<InstanceZoneDescriptor>(byDescriptor.keySet()));
	}

	/**
	 * Destroys all managed instances without normal RPZone persistence.
	 * Called by RPWorld before ordinary zones are finished during shutdown.
	 */
	synchronized void destroyAll() throws Exception {
		List<ManagedInstance> snapshot = new ArrayList<ManagedInstance>(byDescriptor.values());
		Exception failure = null;
		for (ManagedInstance managed : snapshot) {
			try {
				destroy(managed);
			} catch (Exception e) {
				if (failure == null) {
					failure = e;
				} else {
					failure.addSuppressed(e);
				}
			}
		}
		if (failure != null) {
			throw failure;
		}
	}

	private void destroy(ManagedInstance managed) throws Exception {
		IRPZone.ID runtimeId = managed.descriptor.getRuntimeZoneId();
		world.getWorldTaskScheduler().cancelOwner(WorldTaskOwner.instance(runtimeId.getID()));
		world.getWorldTaskScheduler().cancelOwner(WorldTaskOwner.zone(runtimeId.getID()));
		byDescriptor.remove(managed.descriptor);
		byZoneId.remove(runtimeId);

		IRPZone detached = world.detachRPZone(runtimeId);
		Exception failure = null;
		if (detached != managed.zone) {
			failure = new IllegalStateException("Managed instance zone was not attached as expected: "
					+ runtimeId.getID());
		}

		try {
			managed.factory.destroy(managed.descriptor, managed.zone);
		} catch (Exception e) {
			if (failure == null) {
				failure = e;
			} else {
				failure.addSuppressed(e);
			}
		}

		if (failure != null) {
			throw failure;
		}
	}

	private static final class ManagedInstance {
		private final InstanceZoneDescriptor descriptor;
		private final IRPZone zone;
		private final InstanceZoneFactory factory;
		private final Set<String> members = new LinkedHashSet<String>();

		private ManagedInstance(InstanceZoneDescriptor descriptor, IRPZone zone,
				InstanceZoneFactory factory) {
			this.descriptor = descriptor;
			this.zone = zone;
			this.factory = factory;
		}
	}
}
