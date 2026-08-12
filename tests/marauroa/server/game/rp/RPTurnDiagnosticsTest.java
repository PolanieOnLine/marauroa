/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import static org.junit.Assert.assertTrue;

import marauroa.common.resource.ResourceReloadMetricsSnapshot;
import marauroa.common.resource.ResourceReloadService;

import org.junit.Test;

public class RPTurnDiagnosticsTest {
	private static final long MS = 1000000L;

	@Test
	public void formatsNamedIncrementalPhasesAndDiagnosticContexts() {
		long start = 100L * MS;
		long[] ends = new long[RPTurnDiagnostics.PHASE_COUNT];
		long cursor = start;
		for (int i = 0; i < ends.length; i++) {
			long duration = i == RPTurnDiagnostics.PERCEPTIONS ? 25L * MS : (i + 1L) * MS;
			cursor += duration;
			ends[i] = cursor;
		}

		ResourceReloadMetricsSnapshot reload = new ResourceReloadService().getMetricsSnapshot();
		WorldTaskMetricsSnapshot tasks = new WorldTaskMetricsSnapshot(
				17, 3, 10, 8, 1, 40L * MS, 12L * MS, 2, 15L * MS, 18L * MS);
		PerceptionTurnMetricsSnapshot perceptions = new PerceptionTurnMetricsSnapshot(
				2, 1, 1, 8L * MS, 12L * MS, 77, 3L * MS, 9L * MS, 4, 1);
		InstanceZoneMetricsSnapshot instances = new InstanceZoneMetricsSnapshot(
				2, 3, 5, 1, 3, 1, 40L * MS, 12L * MS, 30L * MS, 9L * MS);
		String formatted = RPTurnDiagnostics.formatSlowTurn(
				42, 150L * MS, 100, start, ends, reload, tasks, perceptions, instances);

		assertTrue(formatted.contains("turn=42"));
		assertTrue(formatted.contains("elapsedMs=150"));
		assertTrue(formatted.contains("budgetMs=100"));
		assertTrue(formatted.contains("overrunMs=50"));
		assertTrue(formatted.contains("slowestPhase=perceptions"));
		assertTrue(formatted.contains("slowestPhaseMs=25"));
		assertTrue(formatted.contains("writeLock=1"));
		assertTrue(formatted.contains("actionQueue=2"));
		assertTrue(formatted.contains("perceptions=25"));
		assertTrue(formatted.contains("resourceReload=9"));
		assertTrue(formatted.contains("worldTasks=10"));
		assertTrue(formatted.contains("transactionCleanup=14"));
		assertTrue(formatted.contains("reload={registered=0,pending=0,requests=0,prepared=0,coalesced=0,loadFailures=0,validationFailures=0,applySuccess=0,applyFailures=0,maxLoadMs=0,maxValidationMs=0,maxApplyMs=0}"));
		assertTrue(formatted.contains("worldTasks={pending=3,lastBatchTasks=2,lastBatchMs=15,maxTaskMs=12,executedTotal=8,failedTotal=1}"));
		assertTrue(formatted.contains("perceptions={players=2,sync=1,delta=1,buildMs=8,sendMs=12,slowestClient=77,slowestMs=12,slowestBuildMs=3,slowestSendMs=9,cacheHits=4,cacheMisses=1}"));
		assertTrue(formatted.contains("instances={active=2,members=3,created=5,destroyed=3,createFailures=1,destroyFailures=1,maxCreateMs=12,maxDestroyMs=9}"));
	}

	@Test
	public void toleratesMissingDiagnosticContexts() {
		long[] ends = new long[RPTurnDiagnostics.PHASE_COUNT];
		for (int i = 0; i < ends.length; i++) {
			ends[i] = i + 1L;
		}

		String formatted = RPTurnDiagnostics.formatSlowTurn(1, 2L * MS, 1, 0L, ends,
				null, null, null, null);
		assertTrue(formatted.startsWith("Slow RP turn [turn=1"));
		assertTrue(!formatted.contains("reload={"));
		assertTrue(!formatted.contains("worldTasks={"));
		assertTrue(!formatted.contains("perceptions={"));
		assertTrue(!formatted.contains("instances={"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnexpectedPhaseCount() {
		RPTurnDiagnostics.formatSlowTurn(1, MS, 1, 0L, new long[1], null, null, null, null);
	}
}
