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

    private GameRules() {
    }
}
