/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import marauroa.common.game.IRPZone;

/**
 * Immutable logical identity of an ephemeral zone instance.
 */
public final class InstanceZoneDescriptor {
	private static final String RUNTIME_PREFIX = "instance_";
	private static final int HASH_BYTES = 16;

	private final String baseZoneId;
	private final String instanceId;
	private final InstanceScope scope;
	private final String runtimeZoneId;

	/**
	 * Creates a descriptor and derives a compact collision-resistant runtime zone id.
	 *
	 * @param baseZoneId logical/template zone identifier
	 * @param instanceId stable id of this logical instance during its lifetime
	 * @param scope opaque player or group scope
	 */
	public InstanceZoneDescriptor(String baseZoneId, String instanceId, InstanceScope scope) {
		this.baseZoneId = InstanceScope.requireValue(baseZoneId, "base zone id");
		this.instanceId = InstanceScope.requireValue(instanceId, "instance id");
		if (scope == null) {
			throw new IllegalArgumentException("instance scope must not be null");
		}
		this.scope = scope;
		this.runtimeZoneId = buildRuntimeZoneId();
	}

	public String getBaseZoneId() {
		return baseZoneId;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public InstanceScope getScope() {
		return scope;
	}

	/** Returns the concrete zone id used by the current runtime. */
	public IRPZone.ID getRuntimeZoneId() {
		return new IRPZone.ID(runtimeZoneId);
	}

	/** Returns the concrete zone id as a string. */
	public String getRuntimeZoneIdString() {
		return runtimeZoneId;
	}

	private String buildRuntimeZoneId() {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			updateDigest(digest, baseZoneId);
			updateDigest(digest, scope.getType().name());
			updateDigest(digest, scope.getKey());
			updateDigest(digest, instanceId);
			byte[] full = digest.digest();
			byte[] compact = new byte[HASH_BYTES];
			System.arraycopy(full, 0, compact, 0, HASH_BYTES);
			return RUNTIME_PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(compact);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	private static void updateDigest(MessageDigest digest, String value) {
		byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
		digest.update((byte) (bytes.length >>> 24));
		digest.update((byte) (bytes.length >>> 16));
		digest.update((byte) (bytes.length >>> 8));
		digest.update((byte) bytes.length);
		digest.update(bytes);
	}

	@Override
	public int hashCode() {
		int result = baseZoneId.hashCode();
		result = 31 * result + instanceId.hashCode();
		result = 31 * result + scope.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof InstanceZoneDescriptor)) {
			return false;
		}
		InstanceZoneDescriptor other = (InstanceZoneDescriptor) object;
		return baseZoneId.equals(other.baseZoneId)
				&& instanceId.equals(other.instanceId)
				&& scope.equals(other.scope);
	}

	@Override
	public String toString() {
		return runtimeZoneId;
	}
}
