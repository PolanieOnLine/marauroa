/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.server.game.messagehandler;

import marauroa.common.Log4J;
import marauroa.common.game.RPObject;
import marauroa.common.net.message.Message;
import marauroa.common.net.message.MessageC2SLeaveCharacter;
import marauroa.common.net.message.MessageS2CLeaveCharacterACK;
import marauroa.common.net.message.MessageS2CLeaveCharacterNACK;
import marauroa.server.db.command.DBCommand;
import marauroa.server.db.command.DBCommandPriority;
import marauroa.server.db.command.DBCommandQueue;
import marauroa.server.game.container.ClientState;
import marauroa.server.game.container.PlayerEntry;
import marauroa.server.game.dbcommand.LoadAllActiveCharactersCommand;

/**
 * Leaves the active character but keeps the authenticated account connection
 * alive so another character can be selected without a new login handshake.
 */
class LeaveCharacterHandler extends MessageHandler {
	private static final marauroa.common.Logger logger = Log4J.getLogger(LeaveCharacterHandler.class);

	@Override
	public void process(Message message) {
		MessageC2SLeaveCharacter msg = (MessageC2SLeaveCharacter) message;
		int clientid = msg.getClientID();
		PlayerEntry entry = playerContainer.get(clientid);

		if (!isValidEvent(msg, entry, ClientState.GAME_BEGIN)) {
			return;
		}

		boolean accepted = false;
		try {
			playerContainer.getLock().requestWriteLock();
			try {
				RPObject object = entry.object;
				if (object == null) {
					logger.warn("Cannot leave character because PlayerEntry has no active object: " + entry);
				} else if (rpMan.onExit(object)) {
					entry.storeRPObject(object);
					entry.returnToCharacterSelection();
					accepted = true;
				}
			} finally {
				playerContainer.getLock().releaseLock();
			}

			if (!accepted) {
				sendNACK(msg, entry);
				return;
			}

			DBCommand command = new LoadAllActiveCharactersCommand(entry.username,
					new SendCharacterListHandler(netMan, entry.getProtocolVersion()),
					clientid, msg.getChannel(), entry.getProtocolVersion());
			DBCommandQueue.get().enqueue(command, DBCommandPriority.CRITICAL);

			stats.add("Players leave character", 1);
			MessageS2CLeaveCharacterACK ack = new MessageS2CLeaveCharacterACK(msg.getChannel());
			ack.setClientID(clientid);
			ack.setProtocolVersion(entry.getProtocolVersion());
			netMan.sendMessage(ack);
		} catch (Exception e) {
			logger.error("Error while leaving active character", e);
			if (entry != null && entry.state == ClientState.GAME_BEGIN) {
				sendNACK(msg, entry);
			}
		}
	}

	private void sendNACK(MessageC2SLeaveCharacter msg, PlayerEntry entry) {
		MessageS2CLeaveCharacterNACK nack = new MessageS2CLeaveCharacterNACK(msg.getChannel());
		nack.setClientID(msg.getClientID());
		nack.setProtocolVersion(entry.getProtocolVersion());
		netMan.sendMessage(nack);
	}
}
