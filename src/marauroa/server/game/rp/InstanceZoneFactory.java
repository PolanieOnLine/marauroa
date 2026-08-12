/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import marauroa.common.game.IRPZone;

/**
 * Game supplied factory for an ephemeral zone instance.
 *
 * Marauroa owns instance identity and membership. The game owns creation of
 * the concrete zone contents and any non-persistent cleanup required when an
 * instance is destroyed.
 */
public interface InstanceZoneFactory {
	/**
	 * Creates a complete zone for the descriptor.
	 *
	 * The returned zone id must equal {@link InstanceZoneDescriptor#getRuntimeZoneId()}.
	 */
	IRPZone create(InstanceZoneDescriptor descriptor) throws Exception;

	/**
	 * Releases game-specific transient resources for a destroyed instance.
	 *
	 * The zone has already been detached from RPWorld when this callback runs.
	 * The default implementation intentionally does nothing.
	 */
	default void destroy(InstanceZoneDescriptor descriptor, IRPZone zone) throws Exception {
		// optional game-specific cleanup
	}
}
