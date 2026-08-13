/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.common.net.message;

import java.io.IOException;

import marauroa.common.net.Channel;
import marauroa.common.net.InputSerializer;

/** Confirms that the active character was left successfully. */
public class MessageS2CLeaveCharacterACK extends Message {

	/** Constructor for deserialization. */
	public MessageS2CLeaveCharacterACK() {
		super(MessageType.S2C_LEAVECHARACTER_ACK, null);
	}

	/** @param source channel associated with this message */
	public MessageS2CLeaveCharacterACK(Channel source) {
		super(MessageType.S2C_LEAVECHARACTER_ACK, source);
	}

	@Override
	public void readObject(InputSerializer in) throws IOException {
		super.readObject(in);
		if (type != MessageType.S2C_LEAVECHARACTER_ACK) {
			throw new IOException();
		}
	}

	@Override
	public String toString() {
		return "Message (S2C Leave Character ACK) from (" + getAddress() + ") CONTENTS: ()";
	}
}
