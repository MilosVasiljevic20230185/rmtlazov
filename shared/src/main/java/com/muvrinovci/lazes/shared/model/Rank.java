package com.muvrinovci.lazes.shared.model;

/**
 * Vrednost (rang) karte, 1-13. As je 1, kralj je 13, dzokera nema.
 * Ovaj broj je ujedno i vrednost koju igrac deklarise kada baca karte.
 */
public enum Rank {

    ACE(1, "A", "asa"),
    TWO(2, "2", "dvojke"),
    THREE(3, "3", "trojke"),
    FOUR(4, "4", "cetvorke"),
    FIVE(5, "5", "petice"),
    SIX(6, "6", "sestice"),
    SEVEN(7, "7", "sedmice"),
    EIGHT(8, "8", "osmice"),
    NINE(9, "9", "devetke"),
    TEN(10, "10", "desetke"),
    JACK(11, "J", "zandara"),
    QUEEN(12, "Q", "dame"),
    KING(13, "K", "kralja");

    public static final int MIN_VALUE = 1;
    public static final int MAX_VALUE = 13;

    private final int value;
    private final String label;
    private final String serbianPlural;

    Rank(int value, String label, String serbianPlural) {
        this.value = value;
        this.label = label;
        this.serbianPlural = serbianPlural;
    }

    public int value() {
        return value;
    }

    /** Oznaka koja se ispisuje na karti: A, 2-10, J, Q, K. */
    public String label() {
        return label;
    }

    /** Koristi se u log porukama, npr. "dve sedmice". */
    public String serbianPlural() {
        return serbianPlural;
    }

    public static Rank fromValue(int value) {
        for (Rank rank : values()) {
            if (rank.value == value) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Nepostojeca vrednost karte: " + value);
    }

    public static Rank fromLabel(String label) {
        for (Rank rank : values()) {
            if (rank.label.equals(label)) {
                return rank;
            }
        }
        throw new IllegalArgumentException("Nepoznata oznaka karte: " + label);
    }

    public static boolean isValidValue(int value) {
        return value >= MIN_VALUE && value <= MAX_VALUE;
    }
}
