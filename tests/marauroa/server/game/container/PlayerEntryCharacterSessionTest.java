/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.container;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import marauroa.common.game.RPObject;

/** Tests resetting an authenticated connection for another character choice. */
public class PlayerEntryCharacterSessionTest {

	@Test
	public void testReturnToCharacterSelectionClearsCharacterState() {
		PlayerEntry entry = new PlayerEntry(null);
		entry.state = ClientState.GAME_BEGIN;
		entry.character = "old-character";
		entry.object = new RPObject();
		entry.perceptionCounter = 17;
		entry.requestedSync = false;
		entry.characterCounter = 4;
		entry.contentToTransfer.add(null);
		entry.creationTime = 0L;

		entry.returnToCharacterSelection();

		assertEquals(ClientState.LOGIN_COMPLETE, entry.state);
		assertNull(entry.character);
		assertNull(entry.object);
		assertEquals(0, entry.perceptionCounter);
		assertTrue(entry.requestedSync);
		assertEquals(0, entry.characterCounter);
		assertTrue(entry.contentToTransfer.isEmpty());
		assertFalse(entry.isRemovable());
	}
}
