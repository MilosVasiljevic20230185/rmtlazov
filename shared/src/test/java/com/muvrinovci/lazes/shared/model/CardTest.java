package com.muvrinovci.lazes.shared.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CardTest {

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
