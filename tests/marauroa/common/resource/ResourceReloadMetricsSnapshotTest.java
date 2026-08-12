/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
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

public class ResourceReloadMetricsSnapshotTest {
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
	public void tracksSuccessfulPrepareAndApply() {
		provider.put("items.txt", "5");
		assertTrue(service.requestReload("items"));

		ResourceReloadMetricsSnapshot prepared = service.getMetricsSnapshot();
		assertEquals(1, prepared.getRegisteredResourceCount());
		assertEquals(1, prepared.getPendingCandidateCount());
		assertEquals(1, prepared.getRequestCount());
		assertEquals(1, prepared.getLoadSuccessCount());
		assertEquals(0, prepared.getLoadFailureCount());
		assertEquals(1, prepared.getValidationSuccessCount());
		assertEquals(0, prepared.getValidationFailureCount());
		assertEquals(1, prepared.getPreparedCandidateCount());
		assertEquals(0, prepared.getCoalescedCandidateCount());
		assertEquals(0, prepared.getApplySuccessCount());
		assertTrue(prepared.getTotalLoadDurationNanos() >= 0);
		assertTrue(prepared.getMaxLoadDurationNanos() >= 0);
		assertTrue(prepared.getTotalValidationDurationNanos() >= 0);

		service.processPendingReloads();

		ResourceReloadMetricsSnapshot applied = service.getMetricsSnapshot();
		assertEquals(0, applied.getPendingCandidateCount());
		assertEquals(1, applied.getApplySuccessCount());
		assertEquals(0, applied.getApplyFailureCount());
		assertTrue(applied.getTotalApplyDurationNanos() >= 0);
		assertTrue(applied.getMaxApplyDurationNanos() >= 0);
	}

	@Test
	public void validCandidateCoalescesButInvalidLaterCandidateDoesNotReplaceIt() {
		provider.put("items.txt", "6");
		assertTrue(service.requestReload("items"));
		provider.put("items.txt", "7");
		assertTrue(service.requestReload("items"));
		provider.put("items.txt", "-1");
		assertFalse(service.requestReload("items"));

		ResourceReloadMetricsSnapshot snapshot = service.getMetricsSnapshot();
		assertEquals(3, snapshot.getRequestCount());
		assertEquals(3, snapshot.getLoadSuccessCount());
		assertEquals(2, snapshot.getValidationSuccessCount());
		assertEquals(1, snapshot.getValidationFailureCount());
		assertEquals(2, snapshot.getPreparedCandidateCount());
		assertEquals(1, snapshot.getCoalescedCandidateCount());
		assertEquals(1, snapshot.getPendingCandidateCount());

		service.processPendingReloads();
		assertEquals(7, resource.activeValue);
	}

	@Test
	public void failedLoadAndUnknownResourceAreDistinguished() {
		assertFalse(service.requestReload("items"));
		assertFalse(service.requestReload("missing"));

		ResourceReloadMetricsSnapshot snapshot = service.getMetricsSnapshot();
		assertEquals(2, snapshot.getRequestCount());
		assertEquals(1, snapshot.getUnknownResourceRequestCount());
		assertEquals(0, snapshot.getLoadSuccessCount());
		assertEquals(1, snapshot.getLoadFailureCount());
		assertEquals(0, snapshot.getValidationSuccessCount());
		assertEquals(0, snapshot.getValidationFailureCount());
		assertEquals(0, snapshot.getPreparedCandidateCount());
	}

	@Test
	public void stalePreparedCandidateIsNotQueued() {
		provider.put("items.txt", "8");
		service.unregister("items");
		ReloadableResource<Integer> unregistering = new ReloadableResource<Integer>() {
			@Override
			public String getId() {
				return "items";
			}

			@Override
			public Integer load(ResourceProvider resourceProvider) {
				return Integer.valueOf(8);
			}

			@Override
			public void validate(Integer candidate) {
				service.unregister("items");
			}

			@Override
			public void apply(Integer candidate) {
				throw new AssertionError("stale candidate must not apply");
			}
		};
		service.register(unregistering);

		assertFalse(service.requestReload("items"));

		ResourceReloadMetricsSnapshot snapshot = service.getMetricsSnapshot();
		assertEquals(0, snapshot.getRegisteredResourceCount());
		assertEquals(0, snapshot.getPendingCandidateCount());
		assertEquals(1, snapshot.getLoadSuccessCount());
		assertEquals(1, snapshot.getValidationSuccessCount());
		assertEquals(1, snapshot.getStalePreparedCandidateCount());
		assertEquals(0, snapshot.getPreparedCandidateCount());
	}

	@Test
	public void applyFailureIsCountedWithoutChangingRequestSemantics() {
		provider.put("items.txt", "9");
		resource.failApply = true;
		assertTrue(service.requestReload("items"));
		service.processPendingReloads();

		ResourceReloadMetricsSnapshot snapshot = service.getMetricsSnapshot();
		assertEquals(0, snapshot.getApplySuccessCount());
		assertEquals(1, snapshot.getApplyFailureCount());
		assertEquals(0, snapshot.getPendingCandidateCount());
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
		private boolean failApply;

		private IntegerResource(String id, String path) {
			this.id = id;
			this.path = path;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public Integer load(ResourceProvider resourceProvider) throws Exception {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(
					resourceProvider.open(path), StandardCharsets.UTF_8))) {
				return Integer.valueOf(reader.readLine());
			}
		}

		@Override
		public void validate(Integer candidate) {
			if (candidate.intValue() < 0) {
				throw new IllegalArgumentException("value must not be negative");
			}
		}

		@Override
		public void apply(Integer candidate) {
			if (failApply) {
				throw new IllegalStateException("expected apply failure");
			}
			activeValue = candidate.intValue();
		}
	}
}
