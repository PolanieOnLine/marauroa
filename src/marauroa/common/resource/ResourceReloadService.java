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

import java.util.Collection;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import marauroa.common.Log4J;
import marauroa.common.Logger;

/**
 * Coordinates safe reloading of {@link Reloadable} components.
 */
public final class ResourceReloadService {
	private static final Logger logger = Log4J.getLogger(ResourceReloadService.class);
	private static final ResourceReloadService INSTANCE = new ResourceReloadService();

	private final Set<Reloadable> registered;
	private final Set<Reloadable> queued;
	private final Queue<Reloadable> queue;
	private volatile ResourceProvider provider;

	private ResourceReloadService() {
		registered = createSet();
		queued = createSet();
		queue = new ConcurrentLinkedQueue<Reloadable>();
		provider = new ClassPathResourceProvider(Thread.currentThread().getContextClassLoader());
	}

	private static Set<Reloadable> createSet() {
		return java.util.Collections.newSetFromMap(new ConcurrentHashMap<Reloadable, Boolean>());
	}

	public static ResourceReloadService getInstance() {
		return INSTANCE;
	}

	public void setResourceProvider(ResourceProvider provider) {
		if (provider == null) {
			throw new IllegalArgumentException("provider must not be null");
		}
		this.provider = provider;
	}

	public void register(Reloadable reloadable) {
		if (reloadable == null) {
			throw new IllegalArgumentException("reloadable must not be null");
		}
		registered.add(reloadable);
	}

	public void registerAll(Collection<? extends Reloadable> reloadables) {
		if (reloadables == null) {
			return;
		}
		for (Reloadable reloadable : reloadables) {
			if (reloadable != null) {
				register(reloadable);
			}
		}
	}

	public void unregister(Reloadable reloadable) {
		if (reloadable == null) {
			return;
		}
		registered.remove(reloadable);
		queued.remove(reloadable);
		queue.remove(reloadable);
	}

	public void requestReload(Reloadable reloadable) {
		if (reloadable == null) {
			return;
		}
		if (queued.add(reloadable)) {
			queue.add(reloadable);
		}
	}

	public void requestReloadAll() {
		for (Reloadable reloadable : registered) {
			requestReload(reloadable);
		}
	}

	public void processPendingReloads() {
		Reloadable reloadable;
		while ((reloadable = queue.poll()) != null) {
			queued.remove(reloadable);
			try {
				reload(reloadable);
			} catch (Exception e) {
				logger.error("Failed to reload resource '" + safePath(reloadable) + "'", e);
			}
		}
	}

	private void reload(Reloadable reloadable) throws Exception {
		reloadable.reload(provider);
		logger.info("Reloaded resource '" + safePath(reloadable) + "'");
	}

	private static String safePath(Reloadable reloadable) {
		try {
			String path = reloadable.resourcePath();
			return path != null ? path : "<unknown>";
		} catch (Exception e) {
			return "<error>";
		}
	}
}
