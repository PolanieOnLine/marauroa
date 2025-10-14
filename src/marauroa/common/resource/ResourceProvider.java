/***************************************************************************
 *                   (C) Copyright 2024 - Marauroa                    *
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
 * Provides access to external resources such as XML or configuration files.
 */
public interface ResourceProvider {
	InputStream open(String path) throws IOException;
}
