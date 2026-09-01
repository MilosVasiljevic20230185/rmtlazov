package com.muvrinovci.lazes.shared.model;

/*
  Boja karte. Kod boje ( H, D, C, S) koristi se
  u identifikatoru karte koji putuje kroz mrezni protokol.
 */
public enum Suit {

    HEARTS('H', '♥', true, "herc"),
    DIAMONDS('D', '♦', true, "karo"),
    CLUBS('C', '♣', false, "tref"),
    SPADES('S', '♠', false, "pik");

    private final char code;
    private final char symbol;
    private final boolean red;
    private final String serbianName;

    Suit(char code, char symbol, boolean red, String serbianName) {
        this.code = code;
        this.symbol = symbol;
        this.red = red;
        this.serbianName = serbianName;
    }

    public char code() {
        return code;
    }

    /* Unicode simbol boje,, klijent ga crta na karti umesto slike. */
    public char symbol() {
        return symbol;
    }

    public boolean isRed() {
        return red;
    }

    public String serbianName() {
        return serbianName;
    }

    public static Suit fromCode(char code) {
        for (Suit suit : values()) {
            if (suit.code == code) {
                return suit;
            }
        }
        throw new IllegalArgumentException("Nepoznata boja karte: " + code);
    }
}
