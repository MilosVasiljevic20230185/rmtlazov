package com.muvrinovci.lazes.server.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.muvrinovci.lazes.shared.GameRules;
import com.muvrinovci.lazes.shared.model.Card;
import com.muvrinovci.lazes.shared.model.Rank;
import com.muvrinovci.lazes.shared.model.Suit;
import com.muvrinovci.lazes.shared.protocol.ErrorCode;

class GameEngineTest {

    private static final String ANA = "ana";
    private static final String BOB = "bob";
    private static final String CIP = "cip";

    private static Card card(Rank rank, Suit suit) {
        return new Card(rank, suit, 1);
    }

    private static List<String> ids(Card... cards) {
        List<String> result = new ArrayList<>();
        for (Card card : cards) {
            result.add(card.id());
        }
        return result;
    }

    /** Partija sa unapred zadatim rukama; na potezu je prvi navedeni igrac. */
    private static GameEngine engine(Map<String, List<Card>> hands, List<Card> drawPile) {
        return new GameEngine(new ArrayList<>(hands.keySet()), hands, drawPile, 0);
    }

    private static Map<String, List<Card>> hands(String a, List<Card> handA, String b, List<Card> handB) {
        Map<String, List<Card>> hands = new LinkedHashMap<>();
        hands.put(a, handA);
        hands.put(b, handB);
        return hands;
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Pocetak partije")
    class Setup {

        @Test
        @DisplayName("Svaki igrac dobija 7 karata, ostatak ide u centralni spil")
        void dealsSevenCards() {
            GameEngine engine = new GameEngine(List.of(ANA, BOB, CIP), new Random(42));

            assertEquals(GameRules.INITIAL_HAND_SIZE, engine.cardCount(ANA));
            assertEquals(GameRules.INITIAL_HAND_SIZE, engine.cardCount(BOB));
            assertEquals(GameRules.INITIAL_HAND_SIZE, engine.cardCount(CIP));
            assertEquals(GameRules.TOTAL_CARDS - 3 * GameRules.INITIAL_HAND_SIZE, engine.drawPileCount());
            assertEquals(0, engine.centerCount());
        }

        @Test
        @DisplayName("Nijedna karta se ne deli dva puta")
        void noDuplicateCards() {
            GameEngine engine = new GameEngine(List.of(ANA, BOB, CIP, "dea"), new Random(7));

            List<Card> dealt = new ArrayList<>();
            for (String playerId : engine.playerIds()) {
                dealt.addAll(engine.hand(playerId));
            }

            assertEquals(dealt.size(), new HashSet<>(dealt).size());
        }

        @Test
        @DisplayName("Partija pocinje otvorenom rundom i fazom poteza")
        void startsWithOpenRound() {
            GameEngine engine = new GameEngine(List.of(ANA, BOB), new Random(1));

            assertEquals(GameEngine.OPEN_ROUND, engine.tableValue());
            assertEquals(GamePhase.TURN, engine.phase());
            assertTrue(engine.playerIds().contains(engine.currentPlayerId()));
        }

        @Test
        @DisplayName("Broj igraca mora biti izmedju 2 i 4")
        void rejectsInvalidPlayerCount() {
            assertThrows(IllegalArgumentException.class,
                    () -> new GameEngine(List.of(ANA), new Random(1)));
            assertThrows(IllegalArgumentException.class,
                    () -> new GameEngine(List.of("a", "b", "c", "d", "e"), new Random(1)));
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Bacanje karata")
    class Playing {

        @Test
        @DisplayName("Odigrane karte odlaze na centar i otvara se prozor za prozivanje")
        void playMovesCardsToCenter() {
            Card seven = card(Rank.SEVEN, Suit.HEARTS);
            Card two = card(Rank.TWO, Suit.CLUBS);
            GameEngine engine = engine(
                    hands(ANA, List.of(seven, two), BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            GameEngine.PlayResult result = engine.playCards(ANA, ids(seven, two), 7);

            assertEquals(2, result.declaredCount());
            assertEquals(7, result.declaredValue());
            assertEquals(2, engine.centerCount());
            assertEquals(0, engine.cardCount(ANA));
            assertEquals(7, engine.tableValue());
            assertEquals(GamePhase.CALL_WINDOW, engine.phase());
        }

        @Test
        @DisplayName("Igrac ne moze da baci kartu koju nema u ruci")
        void cannotPlayCardNotInHand() {
            Card seven = card(Rank.SEVEN, Suit.HEARTS);
            GameEngine engine = engine(
                    hands(ANA, List.of(seven), BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            GameException error = assertThrows(GameException.class,
                    () -> engine.playCards(ANA, List.of("KS1"), 13));

            assertEquals(ErrorCode.INVALID_CARDS, error.getCode());
        }

        @Test
        @DisplayName("Igrac koji nije na potezu ne moze da baci karte")
        void cannotPlayOutOfTurn() {
            Card king = card(Rank.KING, Suit.SPADES);
            GameEngine engine = engine(
                    hands(ANA, List.of(card(Rank.SEVEN, Suit.HEARTS)), BOB, List.of(king)),
                    List.of());

            GameException error = assertThrows(GameException.class,
                    () -> engine.playCards(BOB, ids(king), 13));

            assertEquals(ErrorCode.NOT_YOUR_TURN, error.getCode());
        }

        @Test
        @DisplayName("Najvise 8 karata po potezu")
        void rejectsMoreThanEightCards() {
            List<Card> bigHand = new ArrayList<>();
            for (Rank rank : Rank.values()) {
                bigHand.add(card(rank, Suit.HEARTS));
            }
            GameEngine engine = engine(
                    hands(ANA, bigHand, BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            List<String> nineCards = ids(bigHand.subList(0, 9).toArray(new Card[0]));

            GameException error = assertThrows(GameException.class,
                    () -> engine.playCards(ANA, nineCards, 5));

            assertEquals(ErrorCode.INVALID_CARDS, error.getCode());
        }

        @Test
        @DisplayName("Deklarisana vrednost mora biti izmedju 1 i 13")
        void rejectsValueOutOfRange() {
            Card seven = card(Rank.SEVEN, Suit.HEARTS);
            GameEngine engine = engine(
                    hands(ANA, List.of(seven), BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            GameException error = assertThrows(GameException.class,
                    () -> engine.playCards(ANA, ids(seven), 14));

            assertEquals(ErrorCode.INVALID_VALUE, error.getCode());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Prozivanje laganja")
    class Calling {

        @Test
        @DisplayName("Lagao je: kupi ceo centar, a prozivac je sledeci na potezu")
        void liarCollectsCenter() {
            Card two = card(Rank.TWO, Suit.CLUBS);
            GameEngine engine = engine(
                    hands(ANA, List.of(two, card(Rank.FIVE, Suit.HEARTS)),
                            BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            engine.playCards(ANA, ids(two), 7);
            GameEngine.CallResolution result = engine.callLiar(BOB);

            assertTrue(result.wasLying());
            assertEquals(ANA, result.collectorId());
            assertEquals(1, result.collectedCount());
            assertEquals(2, engine.cardCount(ANA));
            assertEquals(0, engine.centerCount());
            assertEquals(BOB, engine.currentPlayerId());
            assertNull(result.winnerId());
        }

        @Test
        @DisplayName("Govorio je istinu: prozivac kupi centar, a optuzeni igra ponovo")
        void wrongCallerCollectsCenter() {
            Card seven = card(Rank.SEVEN, Suit.HEARTS);
            GameEngine engine = engine(
                    hands(ANA, List.of(seven, card(Rank.FIVE, Suit.HEARTS)),
                            BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            engine.playCards(ANA, ids(seven), 7);
            GameEngine.CallResolution result = engine.callLiar(BOB);

            assertFalse(result.wasLying());
            assertEquals(BOB, result.collectorId());
            assertEquals(2, engine.cardCount(BOB));
            assertEquals(ANA, engine.currentPlayerId());
        }

        @Test
        @DisplayName("Dovoljna je jedna pogresna karta da potez bude laz")
        void oneWrongCardMakesItALie() {
            Card sevenHearts = card(Rank.SEVEN, Suit.HEARTS);
            Card sevenClubs = card(Rank.SEVEN, Suit.CLUBS);
            Card two = card(Rank.TWO, Suit.SPADES);
            GameEngine engine = engine(
                    hands(ANA, List.of(sevenHearts, sevenClubs, two),
                            BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            engine.playCards(ANA, ids(sevenHearts, sevenClubs, two), 7);
            GameEngine.CallResolution result = engine.callLiar(BOB);

            assertTrue(result.wasLying());
            assertEquals(3, result.revealedCards().size());
        }

        @Test
        @DisplayName("Prozivanje zatvara rundu, pa sledeci igrac bira novu vrednost")
        void callResetsTableValue() {
            Card two = card(Rank.TWO, Suit.CLUBS);
            Card king = card(Rank.KING, Suit.SPADES);
            GameEngine engine = engine(
                    hands(ANA, List.of(two, card(Rank.FIVE, Suit.HEARTS)), BOB, List.of(king)),
                    List.of());

            engine.playCards(ANA, ids(two), 7);
            engine.callLiar(BOB);

            assertEquals(GameEngine.OPEN_ROUND, engine.tableValue());
            engine.playCards(BOB, ids(king), 3);
            assertEquals(3, engine.tableValue());
        }

        @Test
        @DisplayName("Igrac ne moze prozvati sam sebe")
        void cannotCallYourself() {
            Card two = card(Rank.TWO, Suit.CLUBS);
            GameEngine engine = engine(
                    hands(ANA, List.of(two, card(Rank.FIVE, Suit.HEARTS)),
                            BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            engine.playCards(ANA, ids(two), 7);

            GameException error = assertThrows(GameException.class, () -> engine.callLiar(ANA));
            assertEquals(ErrorCode.INVALID_ACTION, error.getCode());
        }

        @Test
        @DisplayName("Ne moze se prozivati dok nema odigranog poteza")
        void cannotCallOutsideWindow() {
            GameEngine engine = engine(
                    hands(ANA, List.of(card(Rank.TWO, Suit.CLUBS)),
                            BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            GameException error = assertThrows(GameException.class, () -> engine.callLiar(BOB));
            assertEquals(ErrorCode.INVALID_ACTION, error.getCode());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Nastavak runde bez prozivanja")
    class NoCall {

        @Test
        @DisplayName("Red prelazi na sledeceg, a vrednost runde ostaje ista")
        void turnAdvancesAndValueStays() {
            Card two = card(Rank.TWO, Suit.CLUBS);
            GameEngine engine = engine(
                    hands(ANA, List.of(two, card(Rank.FIVE, Suit.HEARTS)),
                            BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            engine.playCards(ANA, ids(two), 7);
            GameEngine.WindowCloseResult result = engine.closeCallWindow();

            assertEquals(BOB, result.nextPlayerId());
            assertEquals(7, engine.tableValue());
            assertEquals(1, engine.centerCount());
            assertEquals(GamePhase.TURN, engine.phase());
        }

        @Test
        @DisplayName("Sledeci igrac mora da nastavi istu vrednost runde")
        void nextPlayerMustContinueTableValue() {
            Card two = card(Rank.TWO, Suit.CLUBS);
            Card king = card(Rank.KING, Suit.SPADES);
            GameEngine engine = engine(
                    hands(ANA, List.of(two, card(Rank.FIVE, Suit.HEARTS)), BOB, List.of(king)),
                    List.of());

            engine.playCards(ANA, ids(two), 7);
            engine.closeCallWindow();

            GameException error = assertThrows(GameException.class,
                    () -> engine.playCards(BOB, ids(king), 13));
            assertEquals(ErrorCode.INVALID_VALUE, error.getCode());

            engine.playCards(BOB, ids(king), 7);
            assertEquals(2, engine.centerCount());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Vucenje karte")
    class Drawing {

        @Test
        @DisplayName("Povucena karta ulazi u ruku, red ide dalje, vrednost runde ostaje")
        void drawAddsCardAndAdvances() {
            Card two = card(Rank.TWO, Suit.CLUBS);
            Card drawn = card(Rank.NINE, Suit.DIAMONDS);
            GameEngine engine = engine(
                    hands(ANA, List.of(two, card(Rank.FIVE, Suit.HEARTS)),
                            BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of(drawn));

            engine.playCards(ANA, ids(two), 7);
            engine.closeCallWindow();

            GameEngine.DrawResult result = engine.drawCard(BOB);

            assertEquals(drawn, result.card());
            assertEquals(2, engine.cardCount(BOB));
            assertEquals(0, engine.drawPileCount());
            assertEquals(ANA, engine.currentPlayerId());
            assertEquals(7, engine.tableValue());
        }

        @Test
        @DisplayName("Iz praznog centralnog spila se ne moze vuci")
        void cannotDrawFromEmptyPile() {
            GameEngine engine = engine(
                    hands(ANA, List.of(card(Rank.TWO, Suit.CLUBS)),
                            BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            GameException error = assertThrows(GameException.class, () -> engine.drawCard(ANA));
            assertEquals(ErrorCode.DRAW_PILE_EMPTY, error.getCode());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Uslov pobede")
    class Winning {

        @Test
        @DisplayName("Pobeda se proglasava tek kad istekne prozor za prozivanje")
        void winsOnlyAfterCallWindowCloses() {
            Card seven = card(Rank.SEVEN, Suit.HEARTS);
            GameEngine engine = engine(
                    hands(ANA, List.of(seven), BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            engine.playCards(ANA, ids(seven), 7);
            assertFalse(engine.isFinished(), "Partija ne sme biti gotova dok traje prozor za prozivanje");

            GameEngine.WindowCloseResult result = engine.closeCallWindow();

            assertEquals(ANA, result.winnerId());
            assertEquals(ANA, engine.winnerId());
            assertTrue(engine.isFinished());
        }

        @Test
        @DisplayName("Laz na poslednjem potezu ponistava pobedu - lazov kupi centar")
        void lyingOnLastPlayLosesTheWin() {
            Card two = card(Rank.TWO, Suit.CLUBS);
            GameEngine engine = engine(
                    hands(ANA, List.of(two), BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            engine.playCards(ANA, ids(two), 7);
            GameEngine.CallResolution result = engine.callLiar(BOB);

            assertTrue(result.wasLying());
            assertNull(result.winnerId());
            assertFalse(engine.isFinished());
            assertEquals(1, engine.cardCount(ANA));
        }

        @Test
        @DisplayName("Neopravdano prozivanje poslednjeg poteza ne sprecava pobedu")
        void truthfulLastPlayStillWins() {
            Card seven = card(Rank.SEVEN, Suit.HEARTS);
            GameEngine engine = engine(
                    hands(ANA, List.of(seven), BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            engine.playCards(ANA, ids(seven), 7);
            GameEngine.CallResolution result = engine.callLiar(BOB);

            assertFalse(result.wasLying());
            assertEquals(ANA, result.winnerId());
            assertEquals(BOB, result.collectorId());
            assertTrue(engine.isFinished());
        }

        @Test
        @DisplayName("Posle zavrsetka partije akcije se odbijaju")
        void noActionsAfterGameOver() {
            Card seven = card(Rank.SEVEN, Suit.HEARTS);
            Card king = card(Rank.KING, Suit.SPADES);
            GameEngine engine = engine(hands(ANA, List.of(seven), BOB, List.of(king)), List.of());

            engine.playCards(ANA, ids(seven), 7);
            engine.closeCallWindow();

            GameException error = assertThrows(GameException.class,
                    () -> engine.playCards(BOB, ids(king), 13));
            assertEquals(ErrorCode.INVALID_ACTION, error.getCode());
        }
    }

    // ------------------------------------------------------------------

    @Nested
    @DisplayName("Odspajanje igraca")
    class Disconnect {

        @Test
        @DisplayName("Kada ostane samo jedan igrac, on automatski pobedjuje")
        void lastRemainingPlayerWins() {
            GameEngine engine = engine(
                    hands(ANA, List.of(card(Rank.TWO, Suit.CLUBS)),
                            BOB, List.of(card(Rank.KING, Suit.SPADES))),
                    List.of());

            assertTrue(engine.removePlayer(ANA));

            assertTrue(engine.isFinished());
            assertEquals(BOB, engine.winnerId());
        }

        @Test
        @DisplayName("Odlazak igraca na potezu prebacuje red na sledeceg")
        void removingCurrentPlayerAdvancesTurn() {
            Map<String, List<Card>> hands = new LinkedHashMap<>();
            hands.put(ANA, List.of(card(Rank.TWO, Suit.CLUBS)));
            hands.put(BOB, List.of(card(Rank.KING, Suit.SPADES)));
            hands.put(CIP, List.of(card(Rank.FIVE, Suit.HEARTS)));
            GameEngine engine = new GameEngine(List.of(ANA, BOB, CIP), hands, List.of(), 0);

            engine.removePlayer(ANA);

            assertEquals(BOB, engine.currentPlayerId());
            assertEquals(GamePhase.TURN, engine.phase());
        }

        @Test
        @DisplayName("Odlazak optuzenog prekida prozor za prozivanje")
        void removingAccusedClosesCallWindow() {
            Map<String, List<Card>> hands = new LinkedHashMap<>();
            Card two = card(Rank.TWO, Suit.CLUBS);
            hands.put(ANA, List.of(two, card(Rank.THREE, Suit.CLUBS)));
            hands.put(BOB, List.of(card(Rank.KING, Suit.SPADES)));
            hands.put(CIP, List.of(card(Rank.FIVE, Suit.HEARTS)));
            GameEngine engine = new GameEngine(List.of(ANA, BOB, CIP), hands, List.of(), 0);

            engine.playCards(ANA, ids(two), 7);
            engine.removePlayer(ANA);

            assertEquals(GamePhase.TURN, engine.phase());
            assertEquals(BOB, engine.currentPlayerId());
            assertEquals(1, engine.centerCount(), "Karte odigranog poteza ostaju na centru");
        }
    }
}
