/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.common.net.message;

import java.io.IOException;

import marauroa.common.net.Channel;
import marauroa.common.net.InputSerializer;

/**
 * Requests leaving the active character while keeping the authenticated
 * account session alive for another character selection.
 */
public class MessageC2SLeaveCharacter extends Message {

	/** Constructor for deserialization. */
	public MessageC2SLeaveCharacter() {
		super(MessageType.C2S_LEAVECHARACTER, null);
	}

	/**
	 * Constructor with a TCP/IP source/destination.
	 *
	 * @param source channel associated with this message
	 */
	public MessageC2SLeaveCharacter(Channel source) {
		super(MessageType.C2S_LEAVECHARACTER, source);
	}

	@Override
	public void readObject(InputSerializer in) throws IOException {
		super.readObject(in);
		if (type != MessageType.C2S_LEAVECHARACTER) {
			throw new IOException();
		}
	}

	@Override
	public String toString() {
		return "Message (C2S Leave Character) from (" + getAddress() + ") CONTENTS: ()";
	}
}
