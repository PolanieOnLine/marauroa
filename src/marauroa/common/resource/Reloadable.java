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

/**
 * Represents a component that can reload its state from an external resource.
 */
public interface Reloadable {
	String resourcePath();
	void reload(ResourceProvider provider) throws Exception;
}
