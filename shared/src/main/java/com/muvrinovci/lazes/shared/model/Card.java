package com.muvrinovci.lazes.shared.model;

import java.util.Comparator;
import java.util.Objects;

/**
 * Jedna karta iz kombinovanog spila.
 *
 * Posto se igra sa dva standardna spila, ista kombinacija ranga i boje
 * postoji dva puta. Zbog toga svaka karta nosi i deckIndex (1 ili 2),
 * pa je njen id jedinstven u celoj partiji. Server po tom
 * identifikatoru proverava da li igrac zaista poseduje karte koje baca.
 *
 */
public record Card(Rank rank, Suit suit, int deckIndex) {

    /**
     * Redosled za prikaz ruke: prvo po vrednosti (1-13), pa po boji, pa po
     * spilu. Time iste vrednosti stoje jedna uz drugu, sto igracu olaksava da
     * vidi koliko ima karata trazene vrednosti.
     */
    public static final Comparator<Card> BY_VALUE = Comparator
            .comparingInt(Card::value)
            .thenComparing(Card::suit)
            .thenComparingInt(Card::deckIndex);

    public Card {
        Objects.requireNonNull(rank, "rank");
        Objects.requireNonNull(suit, "suit");
        if (deckIndex < 1) {
            throw new IllegalArgumentException("Redni broj spila mora biti pozitivan: " + deckIndex);
        }
    }


    public String id() {
        return rank.label() + suit.code() + deckIndex;
    }


    public int value() {
        return rank.value();
    }

    /** Citljiv zapis za log poruke, npr. "7♥". */
    public String display() {
        return rank.label() + suit.symbol();
    }

    /** Rekonstruise kartu iz identifikatora dobijenog kroz protokol. */
    public static Card fromId(String id) {
        Objects.requireNonNull(id, "id");
        if (id.length() < 3) {
            throw new IllegalArgumentException("Neispravan identifikator karte: " + id);
        }

        char deckChar = id.charAt(id.length() - 1);
        if (!Character.isDigit(deckChar)) {
            throw new IllegalArgumentException("Neispravan identifikator karte: " + id);
        }

        int deckIndex = Character.getNumericValue(deckChar);
        Suit suit = Suit.fromCode(id.charAt(id.length() - 2));
        Rank rank = Rank.fromLabel(id.substring(0, id.length() - 2));

        return new Card(rank, suit, deckIndex);
    }

    @Override
    public String toString() {
        return id();
    }
}
