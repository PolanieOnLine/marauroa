/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.common.net.message;

import java.io.IOException;

import marauroa.common.net.Channel;
import marauroa.common.net.InputSerializer;

/** Rejects leaving the active character while keeping the account session. */
public class MessageS2CLeaveCharacterNACK extends Message {

	/** Constructor for deserialization. */
	public MessageS2CLeaveCharacterNACK() {
		super(MessageType.S2C_LEAVECHARACTER_NACK, null);
	}

	/** @param source channel associated with this message */
	public MessageS2CLeaveCharacterNACK(Channel source) {
		super(MessageType.S2C_LEAVECHARACTER_NACK, source);
	}

	@Override
	public void readObject(InputSerializer in) throws IOException {
		super.readObject(in);
		if (type != MessageType.S2C_LEAVECHARACTER_NACK) {
			throw new IOException();
		}
	}

	@Override
	public String toString() {
		return "Message (S2C Leave Character NACK) from (" + getAddress() + ") CONTENTS: ()";
	}
}
