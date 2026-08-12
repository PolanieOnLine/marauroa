/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

/**
 * Opaque lifecycle owner for tasks scheduled on the RP world boundary.
 */
public final class WorldTaskOwner {
	public enum Type {
		GLOBAL,
		ZONE,
		ENTITY,
		INSTANCE
	}

	private final Type type;
	private final String key;

	private WorldTaskOwner(Type type, String key) {
		if (type == null) {
			throw new IllegalArgumentException("world task owner type must not be null");
		}
		if (key == null || key.trim().length() == 0) {
			throw new IllegalArgumentException("world task owner key must not be empty");
		}
		this.type = type;
		this.key = key.trim();
	}

	public static WorldTaskOwner global(String key) {
		return new WorldTaskOwner(Type.GLOBAL, key);
	}

	public static WorldTaskOwner zone(String zoneId) {
		return new WorldTaskOwner(Type.ZONE, zoneId);
	}

	public static WorldTaskOwner entity(String entityId) {
		return new WorldTaskOwner(Type.ENTITY, entityId);
	}

	public static WorldTaskOwner instance(String runtimeZoneId) {
		return new WorldTaskOwner(Type.INSTANCE, runtimeZoneId);
	}

	public Type getType() {
		return type;
	}

	public String getKey() {
		return key;
	}

	@Override
	public int hashCode() {
		return 31 * type.hashCode() + key.hashCode();
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof WorldTaskOwner)) {
			return false;
		}
		WorldTaskOwner other = (WorldTaskOwner) object;
		return type == other.type && key.equals(other.key);
	}

	@Override
	public String toString() {
		return type.name().toLowerCase() + ":" + key;
	}
}
