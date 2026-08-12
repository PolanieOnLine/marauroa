/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

import static org.junit.Assert.assertEquals;

import marauroa.common.net.message.Message;

import org.junit.Test;

public class TransferTurnMetricsTest {
	private static final long MS = 1000000L;

	@Test
	public void accumulatesOfferBatchesAndTracksSlowestClient() {
		TransferTurnMetrics metrics = new TransferTurnMetrics();

		metrics.recordOfferBatch(10, 2, 1, 300, true, 5L * MS);
		metrics.recordOfferBatch(20, 3, 2, 700, false, 8L * MS);

		TransferTurnMetricsSnapshot snapshot = metrics.snapshot();
		assertEquals(2, snapshot.getOfferBatchCount());
		assertEquals(1, snapshot.getOfferSendFailureCount());
		assertEquals(5, snapshot.getOfferedContentCount());
		assertEquals(3, snapshot.getCacheableContentCount());
		assertEquals(1000L, snapshot.getOfferedRawPayloadBytes());
		assertEquals(13L * MS, snapshot.getTotalSendDurationNanos());
		assertEquals(20, snapshot.getSlowestClientId());
		assertEquals(8L * MS, snapshot.getSlowestSendDurationNanos());
		assertEquals(3, snapshot.getSlowestBatchContentCount());
		assertEquals(700L, snapshot.getSlowestBatchRawPayloadBytes());
	}

	@Test
	public void resetClearsCompletedTurnState() {
		TransferTurnMetrics metrics = new TransferTurnMetrics();
		metrics.recordOfferBatch(10, 1, 1, 20, true, MS);

		metrics.reset();

		TransferTurnMetricsSnapshot snapshot = metrics.snapshot();
		assertEquals(0, snapshot.getOfferBatchCount());
		assertEquals(0, snapshot.getOfferSendFailureCount());
		assertEquals(0, snapshot.getOfferedContentCount());
		assertEquals(0, snapshot.getCacheableContentCount());
		assertEquals(0L, snapshot.getOfferedRawPayloadBytes());
		assertEquals(0L, snapshot.getTotalSendDurationNanos());
		assertEquals(Message.CLIENTID_INVALID, snapshot.getSlowestClientId());
		assertEquals(0L, snapshot.getSlowestSendDurationNanos());
	}
}
