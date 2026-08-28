package com.muvrinovci.lazes.server.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.muvrinovci.lazes.shared.GameRules;
import com.muvrinovci.lazes.shared.model.Card;
import com.muvrinovci.lazes.shared.model.Rank;
import com.muvrinovci.lazes.shared.model.Suit;

/** Sastavljanje i mesanje spila za partiju. */
public final class Deck {

    private Deck() {
    }

    /**
     * Sastavlja spil od  GameRules#DECK_COPIES standardna spila
     * (ukupno GameRules#TOTAL_CARDS karata) i mesa ga.
     */
    public static List<Card> buildShuffled(Random random) {
        List<Card> cards = new ArrayList<>(GameRules.TOTAL_CARDS);

        for (int deckIndex = 1; deckIndex <= GameRules.DECK_COPIES; deckIndex++) {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    cards.add(new Card(rank, suit, deckIndex));
                }
            }
        }

        Collections.shuffle(cards, random);
        return cards;
    }
}
