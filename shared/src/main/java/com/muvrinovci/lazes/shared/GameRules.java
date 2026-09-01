package com.muvrinovci.lazes.shared;

public final class GameRules {

    public static final int DEFAULT_PORT = 5555;

    public static final int DECK_COPIES = 2;

    public static final int TOTAL_CARDS = DECK_COPIES * 52;

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;

    public static final int INITIAL_HAND_SIZE = 7;

    public static final int MIN_CARDS_PER_PLAY = 1;
    public static final int MAX_CARDS_PER_PLAY = DECK_COPIES*4;

    public static final int TURN_SECONDS = 60;

    public static final int CALL_WINDOW_MS = 5000;

    public static final int START_COUNTDOWN_SECONDS = 3;

    public static final int ROOM_CODE_LENGTH = 6;

    /** Koliko dugo se cuva mesto igraca kome je pukla veza. */
    public static final int DISCONNECT_GRACE_SECONDS = 120;

    /** Tajmer poteza za odspojeno mesto; server umesto njega igra automatski. */
    public static final int DISCONNECTED_TURN_SECONDS = 3;

    /** Koliko soba ceka bez ijednog povezanog igraca pre nego sto se ugasi. */
    public static final int EMPTY_ROOM_SECONDS = 60;

    private GameRules() {
    }
}
