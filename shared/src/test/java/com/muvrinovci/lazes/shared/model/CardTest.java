package com.muvrinovci.lazes.shared.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CardTest {

    @Test
    @DisplayName("BY_VALUE redja karte od najmanje do najvece vrednosti")
    void sortsByValueAscending() {
        List<Card> cards = new ArrayList<>(List.of(
                new Card(Rank.KING, Suit.SPADES, 1),
                new Card(Rank.ACE, Suit.HEARTS, 1),
                new Card(Rank.TEN, Suit.CLUBS, 2),
                new Card(Rank.TWO, Suit.DIAMONDS, 1)));

        cards.sort(Card.BY_VALUE);

        assertEquals(List.of(1, 2, 10, 13), cards.stream().map(Card::value).toList());
    }

    @Test
    @DisplayName("BY_VALUE grupise karte iste vrednosti jednu uz drugu")
    void groupsEqualValues() {
        List<Card> cards = new ArrayList<>(List.of(
                new Card(Rank.SEVEN, Suit.SPADES, 1),
                new Card(Rank.THREE, Suit.HEARTS, 1),
                new Card(Rank.SEVEN, Suit.HEARTS, 2),
                new Card(Rank.THREE, Suit.CLUBS, 2),
                new Card(Rank.SEVEN, Suit.DIAMONDS, 1)));

        cards.sort(Card.BY_VALUE);

        // Sve trojke pre svih sedmica, i nijedna vrednost nije prekinuta.
        assertEquals(List.of(3, 3, 7, 7, 7), cards.stream().map(Card::value).toList());
    }

    @Test
    @DisplayName("Identifikator karte spaja rang, boju i redni broj spila")
    void idFormat() {
        assertEquals("7H1", new Card(Rank.SEVEN, Suit.HEARTS, 1).id());
        assertEquals("10D2", new Card(Rank.TEN, Suit.DIAMONDS, 2).id());
        assertEquals("AS1", new Card(Rank.ACE, Suit.SPADES, 1).id());
        assertEquals("KC2", new Card(Rank.KING, Suit.CLUBS, 2).id());
    }

    @Test
    @DisplayName("Karta se moze rekonstruisati iz svog identifikatora")
    void roundTrip() {
        for (Rank rank : Rank.values()) {
            for (Suit suit : Suit.values()) {
                for (int deck = 1; deck <= 2; deck++) {
                    Card original = new Card(rank, suit, deck);
                    assertEquals(original, Card.fromId(original.id()));
                }
            }
        }
    }

    @Test
    @DisplayName("Iste karte iz razlicitih spilova nisu jednake")
    void duplicatesFromTwoDecksAreDistinct() {
        Card first = new Card(Rank.SEVEN, Suit.HEARTS, 1);
        Card second = new Card(Rank.SEVEN, Suit.HEARTS, 2);

        assertNotEquals(first, second);
        assertNotEquals(first.id(), second.id());
        assertEquals(first.value(), second.value());
    }

    @Test
    @DisplayName("Neispravan identifikator baca gresku")
    void invalidId() {
        assertThrows(IllegalArgumentException.class, () -> Card.fromId("7H"));
        assertThrows(IllegalArgumentException.class, () -> Card.fromId("7HX"));
        assertThrows(IllegalArgumentException.class, () -> Card.fromId("7Z1"));
        assertThrows(IllegalArgumentException.class, () -> Card.fromId("15H1"));
    }

    @Test
    @DisplayName("Rangovi idu od 1 do 13, bez dzokera")
    void rankRange() {
        assertEquals(13, Rank.values().length);
        assertEquals(1, Rank.ACE.value());
        assertEquals(13, Rank.KING.value());
        assertEquals(Rank.QUEEN, Rank.fromValue(12));
    }
}
