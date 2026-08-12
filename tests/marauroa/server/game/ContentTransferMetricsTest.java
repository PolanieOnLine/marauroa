/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ContentTransferMetricsTest {
	private static final long MS = 1000000L;

	@Test
	public void accumulatesAckAndFullPayloadMetrics() {
		ContentTransferMetrics metrics = new ContentTransferMetrics();

		metrics.recordAckBatch(10, 3, 1, 2, 0, 500, true, true, 5L * MS);
		metrics.recordAckBatch(20, 0, 2, 0, 0, 0, false, true, 0);
		metrics.recordAckBatch(30, 2, 0, 1, 1, 1000, true, false, 8L * MS);

		ContentTransferMetricsSnapshot snapshot = metrics.getMetricsSnapshot();
		assertEquals(3L, snapshot.getAckBatchCount());
		assertEquals(5L, snapshot.getRequestedFullContentCount());
		assertEquals(3L, snapshot.getCacheReuseContentCount());
		assertEquals(3L, snapshot.getSentContentCount());
		assertEquals(1L, snapshot.getMissingContentCount());
		assertEquals(1500L, snapshot.getSentRawPayloadBytes());
		assertEquals(2L, snapshot.getFullSendBatchCount());
		assertEquals(1L, snapshot.getFullSendFailureCount());
		assertEquals(13L * MS, snapshot.getTotalFullSendDurationNanos());
		assertEquals(8L * MS, snapshot.getMaxFullSendDurationNanos());
		assertEquals(30, snapshot.getSlowestClientId());
		assertEquals(1, snapshot.getSlowestBatchContentCount());
		assertEquals(1000L, snapshot.getSlowestBatchRawPayloadBytes());
	}
}
