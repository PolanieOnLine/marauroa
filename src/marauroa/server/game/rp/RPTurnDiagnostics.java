/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import marauroa.common.resource.ResourceReloadMetricsSnapshot;
import marauroa.server.game.ContentTransferMetricsSnapshot;

/**
 * Formats slow RP turn diagnostics from monotonic phase timestamps.
 *
 * The hot path only records timestamps in a preallocated array. String
 * allocation and phase analysis happen only when a turn exceeds its budget.
 */
final class RPTurnDiagnostics {
	static final int WRITE_LOCK = 0;
	static final int ACTION_QUEUE = 1;
	static final int ACTIONS = 2;
	static final int END_TURN = 3;
	static final int TRANSFERS = 4;
	static final int PERCEPTIONS = 5;
	static final int PLAYER_SAVE = 6;
	static final int WORLD_NEXT_TURN = 7;
	static final int RESOURCE_RELOAD = 8;
	static final int WORLD_TASKS = 9;
	static final int BEGIN_TURN = 10;
	static final int UNLOCK = 11;
	static final int STATISTICS = 12;
	static final int TRANSACTION_CLEANUP = 13;
	static final int PHASE_COUNT = 14;

	private static final long NANOS_PER_MILLISECOND = 1000000L;

	private static final String[] PHASE_NAMES = {
		"writeLock",
		"actionQueue",
		"actions",
		"endTurn",
		"transfers",
		"perceptions",
		"playerSave",
		"worldNextTurn",
		"resourceReload",
		"worldTasks",
		"beginTurn",
		"unlock",
		"statistics",
		"transactionCleanup"
	};

	private RPTurnDiagnostics() {
	}

	static String formatSlowTurn(long turnNumber, long elapsedNanos, long budgetMillis,
			long turnStartNanos, long[] phaseEndsNanos,
			ResourceReloadMetricsSnapshot reloadMetrics,
			WorldTaskMetricsSnapshot worldTaskMetrics,
			PerceptionTurnMetricsSnapshot perceptionMetrics,
			TransferTurnMetricsSnapshot transferMetrics,
			ContentTransferMetricsSnapshot contentTransferMetrics,
			ZoneTurnMetricsSnapshot zoneTurnMetrics,
			InstanceZoneMetricsSnapshot instanceMetrics) {
		if (phaseEndsNanos == null || phaseEndsNanos.length != PHASE_COUNT) {
			throw new IllegalArgumentException("RP turn phase timestamp count must be " + PHASE_COUNT);
		}

		long budgetNanos = budgetMillis * NANOS_PER_MILLISECOND;
		long overrunNanos = Math.max(0L, elapsedNanos - budgetNanos);
		long previous = turnStartNanos;
		long slowestDuration = -1L;
		int slowestPhase = -1;

		long[] phaseDurations = new long[PHASE_COUNT];
		for (int i = 0; i < PHASE_COUNT; i++) {
			long end = phaseEndsNanos[i];
			long duration = end >= previous ? end - previous : 0L;
			phaseDurations[i] = duration;
			if (duration > slowestDuration) {
				slowestDuration = duration;
				slowestPhase = i;
			}
			if (end >= previous) {
				previous = end;
			}
		}

		StringBuilder result = new StringBuilder(1000);
		result.append("Slow RP turn [turn=").append(turnNumber)
				.append(", elapsedMs=").append(toMillis(elapsedNanos))
				.append(", budgetMs=").append(budgetMillis)
				.append(", overrunMs=").append(toMillis(overrunNanos))
				.append(", slowestPhase=").append(PHASE_NAMES[slowestPhase])
				.append(", slowestPhaseMs=").append(toMillis(slowestDuration))
				.append(", phasesMs={");

		for (int i = 0; i < PHASE_COUNT; i++) {
			if (i > 0) {
				result.append(',');
			}
			result.append(PHASE_NAMES[i]).append('=').append(toMillis(phaseDurations[i]));
		}
		result.append('}');

		if (reloadMetrics != null) {
			result.append(", reload={registered=")
					.append(reloadMetrics.getRegisteredResourceCount())
					.append(",pending=").append(reloadMetrics.getPendingCandidateCount())
					.append(",requests=").append(reloadMetrics.getRequestCount())
					.append(",prepared=").append(reloadMetrics.getPreparedCandidateCount())
					.append(",coalesced=").append(reloadMetrics.getCoalescedCandidateCount())
					.append(",loadFailures=").append(reloadMetrics.getLoadFailureCount())
					.append(",validationFailures=").append(reloadMetrics.getValidationFailureCount())
					.append(",applySuccess=").append(reloadMetrics.getApplySuccessCount())
					.append(",applyFailures=").append(reloadMetrics.getApplyFailureCount())
					.append(",maxLoadMs=").append(toMillis(reloadMetrics.getMaxLoadDurationNanos()))
					.append(",maxValidationMs=").append(toMillis(reloadMetrics.getMaxValidationDurationNanos()))
					.append(",maxApplyMs=").append(toMillis(reloadMetrics.getMaxApplyDurationNanos()))
					.append('}');
		}

		if (worldTaskMetrics != null) {
			result.append(", worldTasks={pending=")
					.append(worldTaskMetrics.getPendingTaskCount())
					.append(",lastBatchTasks=").append(worldTaskMetrics.getLastBatchTaskCount())
					.append(",lastBatchMs=").append(toMillis(worldTaskMetrics.getLastBatchDurationNanos()))
					.append(",maxTaskMs=").append(toMillis(worldTaskMetrics.getMaxTaskDurationNanos()))
					.append(",executedTotal=").append(worldTaskMetrics.getExecutedTaskCount())
					.append(",failedTotal=").append(worldTaskMetrics.getFailedTaskCount())
					.append('}');
		}

		if (perceptionMetrics != null) {
			result.append(", perceptions={players=")
					.append(perceptionMetrics.getPlayerCount())
					.append(",sync=").append(perceptionMetrics.getSyncCount())
					.append(",delta=").append(perceptionMetrics.getDeltaCount())
					.append(",buildMs=").append(toMillis(perceptionMetrics.getTotalBuildDurationNanos()))
					.append(",sendMs=").append(toMillis(perceptionMetrics.getTotalSendDurationNanos()))
					.append(",slowestClient=").append(perceptionMetrics.getSlowestClientId())
					.append(",slowestMs=").append(toMillis(perceptionMetrics.getSlowestPlayerDurationNanos()))
					.append(",slowestBuildMs=").append(toMillis(perceptionMetrics.getSlowestBuildDurationNanos()))
					.append(",slowestSendMs=").append(toMillis(perceptionMetrics.getSlowestSendDurationNanos()))
					.append(",cacheHits=").append(perceptionMetrics.getCacheHitCount())
					.append(",cacheMisses=").append(perceptionMetrics.getCacheMissCount())
					.append('}');
		}

		if (transferMetrics != null) {
			result.append(", transfers={offerBatches=")
					.append(transferMetrics.getOfferBatchCount())
					.append(",offerFailures=").append(transferMetrics.getOfferSendFailureCount())
					.append(",offered=").append(transferMetrics.getOfferedContentCount())
					.append(",cacheable=").append(transferMetrics.getCacheableContentCount())
					.append(",rawPayloadBytes=").append(transferMetrics.getOfferedRawPayloadBytes())
					.append(",sendMs=").append(toMillis(transferMetrics.getTotalSendDurationNanos()))
					.append(",slowestClient=").append(transferMetrics.getSlowestClientId())
					.append(",slowestSendMs=").append(toMillis(transferMetrics.getSlowestSendDurationNanos()))
					.append(",slowestBatchItems=").append(transferMetrics.getSlowestBatchContentCount())
					.append(",slowestBatchRawBytes=").append(transferMetrics.getSlowestBatchRawPayloadBytes())
					.append('}');
		}

		if (contentTransferMetrics != null) {
			result.append(", contentTransfers={ackBatchesTotal=")
					.append(contentTransferMetrics.getAckBatchCount())
					.append(",requestedFullTotal=").append(contentTransferMetrics.getRequestedFullContentCount())
					.append(",cacheReuseTotal=").append(contentTransferMetrics.getCacheReuseContentCount())
					.append(",sentTotal=").append(contentTransferMetrics.getSentContentCount())
					.append(",missingTotal=").append(contentTransferMetrics.getMissingContentCount())
					.append(",rawPayloadBytesTotal=").append(contentTransferMetrics.getSentRawPayloadBytes())
					.append(",sendBatchesTotal=").append(contentTransferMetrics.getFullSendBatchCount())
					.append(",sendFailuresTotal=").append(contentTransferMetrics.getFullSendFailureCount())
					.append(",maxSendMs=").append(toMillis(contentTransferMetrics.getMaxFullSendDurationNanos()))
					.append(",slowestClient=").append(contentTransferMetrics.getSlowestClientId())
					.append(",slowestBatchItems=").append(contentTransferMetrics.getSlowestBatchContentCount())
					.append(",slowestBatchRawBytes=").append(contentTransferMetrics.getSlowestBatchRawPayloadBytes())
					.append('}');
		}

		if (zoneTurnMetrics != null) {
			result.append(", zoneTurns={count=")
					.append(zoneTurnMetrics.getZoneCount())
					.append(",failures=").append(zoneTurnMetrics.getFailureCount())
					.append(",totalMs=").append(toMillis(zoneTurnMetrics.getTotalDurationNanos()))
					.append(",top=[");
			appendZoneCost(result, zoneTurnMetrics.getFirstZoneId(), zoneTurnMetrics.getFirstDurationNanos());
			appendZoneCost(result, zoneTurnMetrics.getSecondZoneId(), zoneTurnMetrics.getSecondDurationNanos());
			appendZoneCost(result, zoneTurnMetrics.getThirdZoneId(), zoneTurnMetrics.getThirdDurationNanos());
			result.append("]}");
		}

		if (instanceMetrics != null) {
			result.append(", instances={active=")
					.append(instanceMetrics.getActiveInstanceCount())
					.append(",members=").append(instanceMetrics.getActiveMembershipCount())
					.append(",created=").append(instanceMetrics.getCreatedInstanceCount())
					.append(",destroyed=").append(instanceMetrics.getDestroyedInstanceCount())
					.append(",createFailures=").append(instanceMetrics.getCreateFailureCount())
					.append(",destroyFailures=").append(instanceMetrics.getDestroyFailureCount())
					.append(",maxCreateMs=").append(toMillis(instanceMetrics.getMaxCreateDurationNanos()))
					.append(",maxDestroyMs=").append(toMillis(instanceMetrics.getMaxDestroyDurationNanos()))
					.append('}');
		}

		result.append(']');
		return result.toString();
	}

	private static void appendZoneCost(StringBuilder result, String zoneId, long durationNanos) {
		if (zoneId == null) {
			return;
		}
		if (result.charAt(result.length() - 1) != '[') {
			result.append(',');
		}
		result.append(zoneId).append(':').append(toMillis(durationNanos));
	}

	private static long toMillis(long nanos) {
		return nanos / NANOS_PER_MILLISECOND;
	}
}
