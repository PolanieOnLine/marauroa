/***************************************************************************
 *                   (C) Copyright 2003-2011 - Marauroa                    *
 ***************************************************************************
 ***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/
package marauroa.common.game;

import static marauroa.common.i18n.I18N.translate;

/**
 * This enum represent the possible values returned by the create account
 * process. Caller should verify that the process ended in OK_ACCOUNT_CREATED.
 *
 * @author miguel
 */
public enum Result {

	/** Account was created correctly. */
	OK_CREATED(true, "Konto zostało utworzone."),

	/**
	 * Account was not created because one of the important parameters was
	 * missing.
	 */
	FAILED_EMPTY_STRING(false, "Konto nie zostało utworzone ponieważ brakuje jedno z ważnych parametrów."),

	/**
	 * Account was not created because an invalid characters (letter, sign,
	 * number) was used
	 */
	FAILED_INVALID_CHARACTER_USED (false, "Konto nie zostało utworzone ponieważ zostały użyte nieprawidłowe znaki (litera, znak, numer)"),

	/**
	 * Account was not created because any of the parameters are either too long
	 * or too short.
	 */
	FAILED_STRING_SIZE(false, "Konto nie zostało utworzone ponieważ jakieś parametry są za krótkie lub za długie"),

	/** Account was not created because this account already exists. */
	FAILED_PLAYER_EXISTS(false, "Konto nie zostało utworzone ponieważ już istnieje."),

	/** Account was not created because there was an unspecified exception. */
	FAILED_EXCEPTION(false, "Konto nie zostało utworzone ponieważ wystąpił nieokreślony wyjątek."),

	/** Character was not created because this character already exists. */
	FAILED_CHARACTER_EXISTS(false, "Postać nie została utworzna ponieważ już istnieje."),

	/**
	 * The template passed to the create character method is not valid because
	 * it fails to pass the RP rules.
	 */
	FAILED_INVALID_TEMPLATE(false, "Szablon wcześniej użyty do tworzenia postaci jest nieprawidłowy ponieważ nie zaliczył zasad (formuły) zawartej w RP."),

	/**
	 * String is too short
	 *
	 * @since 2.1
	 */
	FAILED_STRING_TOO_SHORT(false, "Konto nie zostało utworzone ponieważ jeden z parametrów był zbyt krótki."),

	/**
	 * String is too long
	 *
	 * @since 2.1
	 */
	FAILED_STRING_TOO_LONG(false, "Konto nie zostało utworzone ponieważ jeden z parametrów jest zbyt długi."),

	/**
	 * Name is reserved
	 *
	 * @since 2.1
	 */
	FAILED_RESERVED_NAME(false, "Konto nie zostało utworzone ponieważ nazwa jest zarezerwowana (lub zawiera zarezerwowaną nazwę)."),

	/**
	 * Password is too close to the username
	 *
	 * @since 2.1
	 */
	FAILED_PASSWORD_TOO_CLOSE_TO_USERNAME(false, "Konto nie zostało utworzone ponieważ hasło jest zbyt podobne do loginu lub nazwy wojownika."),

	/**
	 * Password is too weak.
	 *
	 * @since 2.1
	 */
	FAILED_PASSWORD_TO_WEAK(false, "Konto nie zostało utworzone ponieważ hasło jest zbyt słabe."),

	/**
	 * Too many accounts were created from this ip-address recently.
	 *
	 * @since 3.5
	 */
	FAILED_TOO_MANY(false, "Konto nie zostało utworzone ponieważ limit tworzenia konta dla Twojej sieci został osiągnięty.\nSpróbuj później."),

	/**
	 * Server is offline, obviously not returned by the server but can be generated client side.
	 *
	 * @since 3.8.2
	 */
	FAILED_OFFLINE(false, "Tworzenie nie powiodło się ponieważ serwer jest tymczasowo niedostępny. Spróbuj później."),

	/**
	 * This server does not accept account creation
	 *
	 * @since 3.8.4
	 */
	FAILED_CREATE_ON_MAIN_INSTEAD(false, "Konta nie mogą zostać utworzone na tym serwerze. Utwórz konto na głównym serwerze i poczekaj chwilę.");

	/**
	 * Textual description of the result
	 */
	private String text;

	/**
	 * True if the account is successfully created.
	 */
	private boolean created;

	Result(boolean created, String text) {
		this.created=created;
		this.text=text;
	}

	/**
	 * checks state of account creation represented by this result.
	 *
	 * @return  true if account creation failed. false if account creation was successful.
	 */
	public boolean failed() {
		return !created;
	}

	/**
	 * Returns a textual description of the result.
	 * @return a textual description of the result.
	 */
	public String getText() {
		return translate(text);
	}
}
