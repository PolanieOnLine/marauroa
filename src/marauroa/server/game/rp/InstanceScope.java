/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

/**
 * Opaque ownership scope for an ephemeral zone instance.
 *
 * Marauroa intentionally does not interpret the key. Games decide what
 * identifies a player or a group and pass that stable value to the instance
 * zone manager.
 */
public final class InstanceScope {
	/** Supported generic scope kinds. */
	public enum Type {
		PLAYER,
		GROUP
	}

	private final Type type;
	private final String key;

	private InstanceScope(Type type, String key) {
		if (type == null) {
			throw new IllegalArgumentException("instance scope type must not be null");
		}
		this.type = type;
		this.key = requireValue(key, "instance scope key");
	}

	/** Creates a player-scoped instance key. */
	public static InstanceScope player(String key) {
		return new InstanceScope(Type.PLAYER, key);
	}

	/** Creates a group-scoped instance key. */
	public static InstanceScope group(String key) {
		return new InstanceScope(Type.GROUP, key);
	}

	/** Creates an instance scope of the requested generic type. */
	public static InstanceScope of(Type type, String key) {
		return new InstanceScope(type, key);
	}

	public Type getType() {
		return type;
	}

	public String getKey() {
		return key;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + key.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (!(object instanceof InstanceScope)) {
			return false;
		}
		InstanceScope other = (InstanceScope) object;
		return type == other.type && key.equals(other.key);
	}

	@Override
	public String toString() {
		return type.name().toLowerCase() + ":" + key;
	}

	static String requireValue(String value, String description) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(description + " must not be empty");
		}
		return value.trim();
	}
}
