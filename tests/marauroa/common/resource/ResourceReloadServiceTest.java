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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class ResourceReloadServiceTest {
	private MemoryResourceProvider provider;
	private ResourceReloadService service;
	private IntegerResource resource;

	@Before
	public void setUp() {
		provider = new MemoryResourceProvider();
		service = new ResourceReloadService();
		service.setResourceProvider(provider);
		resource = new IntegerResource("items", "items.txt");
		service.register(resource);
	}

	@Test
	public void loadingAndValidationHappenBeforeSafePointButApplyDoesNot() {
		provider.put("items.txt", "5");

		assertTrue(service.requestReload("items"));
		assertEquals(1, resource.loadCount);
		assertEquals(1, resource.validateCount);
		assertEquals(0, resource.applyCount);
		assertEquals(0, resource.activeValue);
		assertTrue(service.hasPendingReloads());

		service.processPendingReloads();

		assertEquals(1, resource.applyCount);
		assertEquals(5, resource.activeValue);
		assertFalse(service.hasPendingReloads());
	}

	@Test
	public void invalidCandidateLeavesActiveStateUnchanged() {
		provider.put("items.txt", "5");
		assertTrue(service.requestReload("items"));
		service.processPendingReloads();

		provider.put("items.txt", "-1");
		assertFalse(service.requestReload("items"));
		service.processPendingReloads();

		assertEquals(5, resource.activeValue);
		assertEquals(1, resource.applyCount);
	}

	@Test
	public void invalidLaterRequestDoesNotDiscardValidPendingCandidate() {
		provider.put("items.txt", "6");
		assertTrue(service.requestReload("items"));

		provider.put("items.txt", "-1");
		assertFalse(service.requestReload("items"));

		service.processPendingReloads();
		assertEquals(6, resource.activeValue);
		assertEquals(1, resource.applyCount);
	}

	@Test
	public void newestValidCandidateWinsBeforeSafePoint() {
		provider.put("items.txt", "7");
		assertTrue(service.requestReload("items"));
		provider.put("items.txt", "8");
		assertTrue(service.requestReload("items"));

		service.processPendingReloads();

		assertEquals(2, resource.loadCount);
		assertEquals(2, resource.validateCount);
		assertEquals(1, resource.applyCount);
		assertEquals(8, resource.activeValue);
	}

	@Test
	public void unregisterDropsPreparedCandidate() {
		provider.put("items.txt", "9");
		assertTrue(service.requestReload("items"));
		assertTrue(service.unregister("items"));

		service.processPendingReloads();

		assertEquals(0, resource.activeValue);
		assertEquals(0, resource.applyCount);
		assertFalse(service.hasPendingReloads());
	}

	@Test
	public void unknownResourceIsRejectedWithoutCreatingPendingWork() {
		assertFalse(service.requestReload("missing"));
		assertFalse(service.hasPendingReloads());
	}

	private static final class MemoryResourceProvider implements ResourceProvider {
		private final Map<String, String> values = new HashMap<String, String>();

		private void put(String path, String value) {
			values.put(path, value);
		}

		@Override
		public InputStream open(String path) throws IOException {
			String value = values.get(path);
			if (value == null) {
				throw new IOException("Missing test resource " + path);
			}
			return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
		}
	}

	private static final class IntegerResource implements ReloadableResource<Integer> {
		private final String id;
		private final String path;
		private int activeValue;
		private int loadCount;
		private int validateCount;
		private int applyCount;

		private IntegerResource(String id, String path) {
			this.id = id;
			this.path = path;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public Integer load(ResourceProvider provider) throws Exception {
			loadCount++;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(
					provider.open(path), StandardCharsets.UTF_8))) {
				return Integer.valueOf(reader.readLine());
			}
		}

		@Override
		public void validate(Integer candidate) {
			validateCount++;
			if (candidate.intValue() < 0) {
				throw new IllegalArgumentException("value must not be negative");
			}
		}

		@Override
		public void apply(Integer candidate) {
			applyCount++;
			activeValue = candidate.intValue();
		}
	}
}
