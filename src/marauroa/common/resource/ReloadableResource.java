/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package marauroa.common.resource;

/**
 * A resource whose active state can be replaced after a candidate has been
 * loaded and validated.
 *
 * Implementations must not mutate their active state from {@link #load} or
 * {@link #validate}. {@link #apply} is executed by the RP server at a safe
 * point between turns and should therefore be a small, atomic state swap.
 *
 * @param <T> prepared resource state
 */
public interface ReloadableResource<T> {

	/**
	 * Stable id used to register and request this resource.
	 *
	 * @return non-empty resource id
	 */
	String getId();

	/**
	 * Loads a candidate state without changing the active state.
	 *
	 * @param provider source of external resources
	 * @return candidate state
	 * @throws Exception if the resource cannot be loaded or parsed
	 */
	T load(ResourceProvider provider) throws Exception;

	/**
	 * Validates a candidate state without changing the active state.
	 *
	 * @param candidate candidate returned by {@link #load}
	 * @throws Exception if the candidate must not become active
	 */
	void validate(T candidate) throws Exception;

	/**
	 * Replaces the active state with a previously loaded and validated
	 * candidate. Implementations should make this operation atomic and should
	 * not perform parsing, I/O or expensive validation here.
	 *
	 * @param candidate validated candidate state
	 */
	void apply(T candidate);
}
