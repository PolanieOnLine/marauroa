/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.rp;

/** A task executed on the RP world's safe turn boundary. */
public interface WorldTask {
	void run() throws Exception;
}
