package com.muvrinovci.lazes.shared;

/**
 * Konstante pravila igre, zajednicke za server i klijent.
 * Server ih koristi za validaciju, klijent za prikaz i onemogucavanje kontrola.
 */
public final class GameRules {

    /** Podrazumevani port na kome server slusa. */
    public static final int DEFAULT_PORT = 5555;

    /** Broj standardnih spilova od kojih se sastavlja spil za partiju. */
    public static final int DECK_COPIES = 2;

    /** Ukupan broj karata u partiji: 2 x 52. */
    public static final int TOTAL_CARDS = DECK_COPIES * 52;

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 4;

    /** Broj karata koji svaki igrac dobija na pocetku partije. */
    public static final int INITIAL_HAND_SIZE = 7;

    public static final int MIN_CARDS_PER_PLAY = 1;
    public static final int MAX_CARDS_PER_PLAY = 8;

    /** Trajanje jednog poteza u sekundama; po isteku server automatski vuce kartu. */
    public static final int TURN_SECONDS = 30;

    /** Trajanje prozora za prozivanje nakon odigranog poteza, u milisekundama. */
    public static final int CALL_WINDOW_MS = 5000;

    /** Odbrojavanje pre pocetka partije: 3 - 2 - 1 - Start. */
    public static final int START_COUNTDOWN_SECONDS = 3;

    /** Duzina alfanumerickog koda sobe. */
    public static final int ROOM_CODE_LENGTH = 6;

    private GameRules() {
    }
}
