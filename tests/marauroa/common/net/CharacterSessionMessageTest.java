/***************************************************************************
 *                   (C) Copyright 2003-2026 - Marauroa                    *
 ***************************************************************************/
package marauroa.common.net;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import marauroa.common.net.message.Message;
import marauroa.common.net.message.Message.MessageType;
import marauroa.common.net.message.MessageC2SLeaveCharacter;
import marauroa.common.net.message.MessageS2CLeaveCharacterACK;
import marauroa.common.net.message.MessageS2CLeaveCharacterNACK;

/** Tests the character-session protocol extension. */
public class CharacterSessionMessageTest {

	@Test
	public void testProtocolAndMessageOrdinalsAreAppendOnly() {
		assertEquals(36, NetConst.NETWORK_PROTOCOL_VERSION);
		assertEquals(38, MessageType.C2S_CREATE_ACCOUNT_WITH_TOKEN.ordinal());
		assertEquals(39, MessageType.C2S_LEAVECHARACTER.ordinal());
		assertEquals(40, MessageType.S2C_LEAVECHARACTER_ACK.ordinal());
		assertEquals(41, MessageType.S2C_LEAVECHARACTER_NACK.ordinal());
	}

	@Test
	public void testNewMessagesRoundTrip() throws Exception {
		assertRoundTrip(new MessageC2SLeaveCharacter(), MessageC2SLeaveCharacter.class);
		assertRoundTrip(new MessageS2CLeaveCharacterACK(), MessageS2CLeaveCharacterACK.class);
		assertRoundTrip(new MessageS2CLeaveCharacterNACK(), MessageS2CLeaveCharacterNACK.class);
	}

	private void assertRoundTrip(Message message, Class<?> expectedClass) throws Exception {
		byte[] encoded = Encoder.get().encode(message);
		List<Message> decoded = Decoder.get().decode(null, encoded);
		assertEquals(1, decoded.size());
		assertTrue(expectedClass.isInstance(decoded.get(0)));
		assertEquals(message.getType(), decoded.get(0).getType());
		assertEquals(NetConst.NETWORK_PROTOCOL_VERSION, decoded.get(0).getProtocolVersion());
	}
}
