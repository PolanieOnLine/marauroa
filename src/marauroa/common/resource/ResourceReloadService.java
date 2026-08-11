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

import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import marauroa.common.Log4J;
import marauroa.common.Logger;

/**
 * Coordinates validated resource reloads and applies prepared candidates at a
 * server safe point.
 *
 * Loading and validation happen synchronously in
 * {@link #requestReload(String)} on the requesting thread. Callers that may
 * perform expensive parsing should therefore invoke reload requests from a
 * control or worker thread, not from RP turn processing. Only a successfully
 * validated candidate is queued. {@link #processPendingReloads()} performs the
 * final state swap and is called by {@code RPServerManager} between world
 * turns.
 *
 * This split keeps parsing and validation out of the safe-point apply phase
 * without introducing a hidden executor or additional lifecycle threads in the
 * engine. Applying a prepared candidate should be a small atomic operation.
 */
public final class ResourceReloadService {
	private static final Logger logger = Log4J.getLogger(ResourceReloadService.class);
	private static final ResourceReloadService INSTANCE = new ResourceReloadService();

	private final ConcurrentMap<String, Registration<?>> registered =
			new ConcurrentHashMap<String, Registration<?>>();
	private final ConcurrentMap<String, PendingReload<?>> pending =
			new ConcurrentHashMap<String, PendingReload<?>>();
	private final Set<String> queued = Collections.newSetFromMap(
			new ConcurrentHashMap<String, Boolean>());
	private final Queue<String> queue = new ConcurrentLinkedQueue<String>();

	private volatile ResourceProvider provider;

	/**
	 * Creates an independent reload service. The singleton returned by
	 * {@link #getInstance()} is used by the server, while separate instances are
	 * useful for tests and isolated embedding.
	 */
	public ResourceReloadService() {
		provider = new ClassPathResourceProvider(Thread.currentThread().getContextClassLoader());
	}

	/**
	 * Returns the server-wide reload service.
	 *
	 * @return shared service
	 */
	public static ResourceReloadService getInstance() {
		return INSTANCE;
	}

	/**
	 * Changes the provider used when preparing future reload candidates.
	 * Already prepared candidates are unaffected.
	 *
	 * @param provider resource provider
	 */
	public void setResourceProvider(ResourceProvider provider) {
		if (provider == null) {
			throw new IllegalArgumentException("provider must not be null");
		}
		this.provider = provider;
	}

	/**
	 * Registers a reloadable resource by its stable id.
	 *
	 * Re-registering the same object is harmless. Registering a different
	 * object under an existing id is rejected so reload requests cannot become
	 * ambiguous.
	 *
	 * @param resource reloadable resource
	 * @param <T> prepared state type
	 */
	public <T> void register(ReloadableResource<T> resource) {
		if (resource == null) {
			throw new IllegalArgumentException("resource must not be null");
		}

		String id = requireId(resource.getId());
		Registration<T> registration = new Registration<T>(resource);
		Registration<?> previous = registered.putIfAbsent(id, registration);
		if (previous != null && previous.resource != resource) {
			throw new IllegalArgumentException("A different resource is already registered as '" + id + "'");
		}
	}

	/**
	 * Unregisters a resource and discards any candidate waiting to be applied.
	 *
	 * @param id resource id
	 * @return true if a resource was registered under this id
	 */
	public boolean unregister(String id) {
		if (id == null) {
			return false;
		}

		Registration<?> removed = registered.remove(id);
		if (removed == null) {
			return false;
		}

		pending.remove(id);
		queued.remove(id);
		queue.remove(id);
		return true;
	}

	/**
	 * Loads and validates a candidate for a registered resource.
	 *
	 * This method performs loading and validation synchronously on the caller's
	 * thread. Callers should avoid invoking it from RP turn processing when the
	 * resource may require non-trivial I/O or parsing.
	 *
	 * The active resource state is not changed by this method. On success the
	 * prepared candidate is queued for the next server safe point. If another
	 * valid candidate for the same id is prepared before that safe point, the
	 * newest candidate replaces the older one. A failed later request does not
	 * discard an already prepared valid candidate.
	 *
	 * @param id registered resource id
	 * @return true if a valid candidate was prepared and queued
	 */
	public boolean requestReload(String id) {
		if (id == null) {
			return false;
		}

		Registration<?> registration = registered.get(id);
		if (registration == null) {
			logger.warn("Ignoring reload request for unknown resource '" + id + "'");
			return false;
		}

		try {
			PendingReload<?> candidate = prepare(registration, provider);

			/* The resource may have been replaced or removed while it loaded. */
			if (registered.get(id) != registration) {
				logger.warn("Ignoring prepared reload for stale resource '" + id + "'");
				return false;
			}

			pending.put(id, candidate);
			if (queued.add(id)) {
				queue.add(id);
			}
			logger.info("Prepared and validated reload candidate for resource '" + id + "'");
			return true;
		} catch (Exception e) {
			logger.error("Failed to prepare reload for resource '" + id
					+ "'; active state was left unchanged", e);
			return false;
		}
	}

	/**
	 * Requests reloads for every currently registered resource.
	 *
	 * @return number of resources for which a valid candidate was queued
	 */
	public int requestReloadAll() {
		int prepared = 0;
		for (String id : registered.keySet()) {
			if (requestReload(id)) {
				prepared++;
			}
		}
		return prepared;
	}

	/**
	 * Applies all currently prepared candidates.
	 *
	 * This method is intentionally limited to the final apply step. Resource
	 * loading and validation have already happened in the requesting thread.
	 * The RP server calls this method at its safe point between turns.
	 */
	public void processPendingReloads() {
		String id;
		while ((id = queue.poll()) != null) {
			queued.remove(id);
			PendingReload<?> candidate = pending.remove(id);
			if (candidate == null) {
				continue;
			}

			if (registered.get(id) != candidate.registration) {
				logger.warn("Ignoring stale pending reload for resource '" + id + "'");
				continue;
			}

			apply(candidate);
		}
	}

	/**
	 * Returns whether at least one validated candidate is waiting for the safe
	 * point.
	 *
	 * @return true if a candidate is pending
	 */
	public boolean hasPendingReloads() {
		return !pending.isEmpty();
	}

	/**
	 * Returns a stable snapshot of registered ids. This is intended for admin
	 * tooling so external callers can expose known ids instead of arbitrary file
	 * paths.
	 *
	 * @return sorted immutable id set
	 */
	public Set<String> getRegisteredResourceIds() {
		return Collections.unmodifiableSet(new TreeSet<String>(registered.keySet()));
	}

	private static String requireId(String id) {
		if (id == null || id.trim().length() == 0) {
			throw new IllegalArgumentException("resource id must not be empty");
		}
		return id;
	}

	private static <T> PendingReload<T> prepare(Registration<T> registration,
			ResourceProvider provider) throws Exception {
		T candidate = registration.resource.load(provider);
		registration.resource.validate(candidate);
		return new PendingReload<T>(registration, candidate);
	}

	private static <T> void apply(PendingReload<T> pendingReload) {
		String id = pendingReload.registration.resource.getId();
		try {
			pendingReload.registration.resource.apply(pendingReload.candidate);
			logger.info("Applied reload for resource '" + id + "'");
		} catch (RuntimeException e) {
			logger.error("Failed to apply validated reload for resource '" + id
					+ "'. ReloadableResource.apply() must be an atomic, non-failing state swap", e);
		}
	}

	private static final class Registration<T> {
		private final ReloadableResource<T> resource;

		private Registration(ReloadableResource<T> resource) {
			this.resource = resource;
		}
	}

	private static final class PendingReload<T> {
		private final Registration<T> registration;
		private final T candidate;

		private PendingReload(Registration<T> registration, T candidate) {
			this.registration = registration;
			this.candidate = candidate;
		}
	}
}
