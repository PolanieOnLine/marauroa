/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

/** Handle used to cancel one scheduled world task. */
public interface WorldTaskHandle {
	/**
	 * Cancels the task if it has not started yet.
	 *
	 * @return true when this call changed the task from active to cancelled
	 */
	boolean cancel();

	boolean isCancelled();
}
