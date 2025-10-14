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

import marauroa.common.ConfigurationParams;
import marauroa.common.io.Persistence;

/**
 * Opens resources using the configured persistence layer.
 */
public class PersistenceResourceProvider implements ResourceProvider {
	private final boolean relativeToHome;
	private final String basedir;

	public PersistenceResourceProvider(ConfigurationParams params) {
		if (params == null) {
			throw new IllegalArgumentException("params must not be null");
		}
		relativeToHome = params.isRelativeToHome();
		basedir = params.getBasedir();
	}

	@Override
	public InputStream open(String path) throws IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		return Persistence.get().getInputStream(relativeToHome, basedir, path);
	}
}
