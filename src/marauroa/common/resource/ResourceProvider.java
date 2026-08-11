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

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides access to external resources used by reloadable components.
 *
 * Implementations decide where resources are stored. The resource path is
 * supplied by registered code, not directly by the reload request, so callers
 * can request only known resource ids instead of arbitrary filesystem paths.
 */
public interface ResourceProvider {

	/**
	 * Opens a resource for reading.
	 *
	 * @param path resource path understood by this provider
	 * @return input stream for the resource
	 * @throws IOException if the resource cannot be opened
	 */
	InputStream open(String path) throws IOException;
}
