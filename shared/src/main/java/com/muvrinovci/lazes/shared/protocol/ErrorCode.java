package com.muvrinovci.lazes.shared.protocol;

/**
 * Kodovi gresaka koje server salje klijentu u  error poruci.
 * Klijent na osnovu koda prikazuje odgovarajucu poruku korisniku.
 */
public final class ErrorCode {

    public static final String ROOM_NOT_FOUND = "ROOM_NOT_FOUND";
    public static final String ROOM_FULL = "ROOM_FULL";
    public static final String GAME_IN_PROGRESS = "GAME_IN_PROGRESS";
    public static final String NOT_IN_ROOM = "NOT_IN_ROOM";
    public static final String NOT_HOST = "NOT_HOST";
    public static final String NOT_ENOUGH_PLAYERS = "NOT_ENOUGH_PLAYERS";
    public static final String PLAYERS_NOT_READY = "PLAYERS_NOT_READY";
    public static final String NOT_YOUR_TURN = "NOT_YOUR_TURN";
    public static final String INVALID_ACTION = "INVALID_ACTION";
    public static final String INVALID_CARDS = "INVALID_CARDS";
    public static final String INVALID_VALUE = "INVALID_VALUE";
    public static final String DRAW_PILE_EMPTY = "DRAW_PILE_EMPTY";
    public static final String INVALID_NAME = "INVALID_NAME";
    public static final String MALFORMED_MESSAGE = "MALFORMED_MESSAGE";

    private ErrorCode() {
    }
}
