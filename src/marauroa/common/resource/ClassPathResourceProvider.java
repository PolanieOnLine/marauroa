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
 * Opens resources from an application class loader.
 */
public class ClassPathResourceProvider implements ResourceProvider {
	private final ClassLoader classLoader;

	/**
	 * Creates a provider using the supplied loader. If it is null, the current
	 * thread context loader and then the system loader are tried.
	 *
	 * @param classLoader class loader, or null
	 */
	public ClassPathResourceProvider(ClassLoader classLoader) {
		ClassLoader loader = classLoader;
		if (loader == null) {
			loader = Thread.currentThread().getContextClassLoader();
		}
		if (loader == null) {
			loader = ClassLoader.getSystemClassLoader();
		}
		this.classLoader = loader;
	}

	@Override
	public InputStream open(String path) throws IOException {
		if (path == null) {
			throw new IllegalArgumentException("path must not be null");
		}
		InputStream stream = classLoader.getResourceAsStream(path);
		if (stream == null) {
			throw new IOException("Unable to locate resource '" + path + "' on the classpath");
		}
		return stream;
	}
}
