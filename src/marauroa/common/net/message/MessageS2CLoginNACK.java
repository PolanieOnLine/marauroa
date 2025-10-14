/***************************************************************************
 *                   (C) Copyright 2003-2016 - Marauroa                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package marauroa.common.net.message;

import static marauroa.common.i18n.I18N.translate;

import java.io.IOException;

import marauroa.common.net.Channel;
import marauroa.common.net.InputSerializer;
import marauroa.common.net.OutputSerializer;

/**
 * This message indicate the client that the server has reject its login Message
 *
 * @see marauroa.common.net.message.Message
 */

public class MessageS2CLoginNACK extends Message {

	/**
	 * reason for login failure
	 */
	public enum Reasons {
		/** the username or password is wrong */
		USERNAME_WRONG,
		/** @since will be replaced by TOO_MANY_TRIES_USERNAME and TOO_MANY_TRIES_IP in the future */
		TOO_MANY_TRIES,
		/** the account is banned */
		USERNAME_BANNED,
		/** there are too many active clients */
		SERVER_IS_FULL,
		/** the client is for a different game than the server */
		GAME_MISMATCH,
		/** the protocol version is incompatible */
		PROTOCOL_MISMATCH,
		/** the nonce, which is used during setup of the encryption, is invalid */
		INVALID_NONCE,
		/** @since 3.0 */
		USERNAME_INACTIVE,
		/** @since 3.0 */
		TOO_MANY_TRIES_USERNAME,
		/** @since 3.0 */
		TOO_MANY_TRIES_IP,
		/** @since 3.7 */
		SEED_WRONG,
		/** @since 3.7.1 */
		ACCOUNT_MERGED;
	}

	static private String[] text = {
	        "Niepoprawna nazwa użytkownika/hasła. ",
	        "Wystąpiło zbyt wiele niepoprawnych prób zalogowania do twojego konta lub z twojej sieci. "
	        	+ "Odczekaj kilka minut lub skontaktuj się ze wsparciem.",
		    "Konto jest zablokowane.",
	        "Serwer jest pełny.",
	        "Serwer działa na niekompatybilnej wersji gry. Zaktualizuj.",
	        "Nieprawidłowa wersja protokołu.",
	        "Hash, który wysyłasz jest nie zgodny z tym co wysłałeś wcześniej.",
	        "Twoje konto jest nieaktywne. Musisz potwierdzić utworzenie konta klikając na odnośnik "
	        	+ "przesłany w e-mailu. Jeśli go nie otrzymałeś to zaloguj się na stronie i naciśnij na przycisk "
	        	+ "ponownego wysłania lub skontaktuj się ze wsparciem. Masz 24 godziny na potwierdzenie utworzenia " 
	        	+ "konta ponieważ po tym czasie konto zostanie automatycznie usunięte.",
	        "Wystąpiło zbyt wiele nieudanych prób zalogowania do twojego konta. "
	        	+ "Odczekaj kilka minut lub skontaktuj się ze wsparciem.",
	        "Wystąpiło zbyt wiele nieudanych prób zalogowania z twojej sieci. "
	        	+ "Odczekaj kilka minut lub skontaktuj się ze wsparciem.",
        	"Nie powiodło się wstępne uwierzytelnienie. Spróbuj ponownie.",
        	"To konto zostało połączone z innym kontem. Użyj loginu innego konta, "
        		+"aby zalogować lub skontaktuj się ze wsparciem."};

	/** The reason of login rejection */
	private Reasons reason;

	/** Constructor for allowing creation of an empty message */
	public MessageS2CLoginNACK() {
		super(MessageType.S2C_LOGIN_NACK, null);
	}

	/**
	 * Constructor with a TCP/IP source/destination of the message
	 *
	 * @param source
	 *            The TCP/IP address associated to this message
	 * @param resolution
	 *            the reason to deny the login
	 */
	public MessageS2CLoginNACK(Channel source, Reasons resolution) {
		super(MessageType.S2C_LOGIN_NACK, source);
		reason = resolution;
	}

	/**
	 * This method returns the resolution of the login event
	 *
	 * @return a byte representing the resolution given.
	 */
	public Reasons getResolutionCode() {
		return reason;
	}

	/**
	 * This method returns a String that represent the resolution given to the
	 * login event
	 *
	 * @return a string representing the resolution.
	 */
	public String getResolution() {
		return translate(text[reason.ordinal()]);
	}

	/**
	 * This method returns a String that represent the object
	 *
	 * @return a string representing the object.
	 */
	@Override
	public String toString() {
		return "Message (S2C Login NACK) from (" + getAddress() + ") CONTENTS: (" + getResolution()
		        + ")";
	}

	@Override
	public void writeObject(OutputSerializer out) throws IOException {
		super.writeObject(out);
		out.write((byte) reason.ordinal());
	}

	@Override
	public void readObject(InputSerializer in) throws IOException {
		super.readObject(in);
		reason = Reasons.values()[in.readByte()];

		if (type != MessageType.S2C_LOGIN_NACK) {
			throw new IOException();
		}
	}

	@Override
	public void writeToJson(StringBuilder out) {
		super.writeToJson(out);
		out.append(",\"reason\":\"");
		out.append(reason.name());
		out.append("\",\"text\":");
		OutputSerializer.writeJson(out, text[reason.ordinal()]);
	}

}
