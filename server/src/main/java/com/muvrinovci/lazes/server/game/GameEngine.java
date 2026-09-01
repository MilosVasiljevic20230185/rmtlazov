package com.muvrinovci.lazes.server.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.muvrinovci.lazes.shared.GameRules;
import com.muvrinovci.lazes.shared.model.Card;
import com.muvrinovci.lazes.shared.model.Rank;
import com.muvrinovci.lazes.shared.protocol.ErrorCode;

/**
 * Pravila igre "Lazes" , jedini autoritet nad stanjem partije.
 *
 * Klasa je namerno bez ikakve mrezne logike: prima akcije igraca, proverava
 * ih i vraca ishod. Nije thread-safe, ali joj to nije ni potrebno - svaka
 * code Room je poziva iz svoje jedne niti, pa se stanje menja sekvencijalno.
 *
 * Tok jedne runde:
 * 
 *   Igrac na potezu baca karte (playCards) ili vuce kartu (drawCard).
 *   Nakon bacanja partija ulazi u CALL_WINDOW.
 *   Ako neko prozove (callLiar), karte se otkrivaju, gubitnik kupi centar
 *       i runda se zavrsava (vrednost runde se resetuje).
 *   Ako niko ne prozove (closeCallWindow) red ide dalje, a vrednost
 *       runde ostaje ista.
 * 
 */
public class GameEngine {

    /** Odigran potez koji jos uvek ceka na eventualno prozivanje. */
    public record PendingPlay(String playerId, List<Card> cards, int declaredValue) {

        public int count() {
            return cards.size();
        }
    }

    /** Ishod bacanja karata. */
    public record PlayResult(String playerId, int declaredCount, int declaredValue) {
    }

    /** Ishod vucenja karte. */
    public record DrawResult(String playerId, Card card, String nextPlayerId) {
    }

    /** Ishod prozivanja. */
    public record CallResolution(String callerId, String accusedId, boolean wasLying,
                                 List<Card> revealedCards, int declaredValue,
                                 String collectorId, int collectedCount,
                                 String nextPlayerId, String winnerId) {
    }

    /** Ishod isteka prozora za prozivanje bez ijedne prozivke. */
    public record WindowCloseResult(String nextPlayerId, String winnerId) {
    }

    /** Vrednost runde kada je runda otvorena i igrac sme da bira bilo koju vrednost. */
    public static final int OPEN_ROUND = 0;

    private final List<String> playerIds;
    private final Map<String, List<Card>> hands = new HashMap<>();
    private final Deque<Card> drawPile = new ArrayDeque<>();
    private final List<Card> center = new ArrayList<>();

    private int currentIndex;
    private int tableValue = OPEN_ROUND;
    private GamePhase phase = GamePhase.TURN;
    private PendingPlay pendingPlay;
    private String winnerId;

    /**
     * Sastavlja spil, deli po INITIAL_HAND_SIZE karata svakom
     * igracu i nasumicno bira ko zapocinje partiju.
     */
    public GameEngine(List<String> playerIds, Random random) {
        if (playerIds.size() < GameRules.MIN_PLAYERS || playerIds.size() > GameRules.MAX_PLAYERS) {
            throw new IllegalArgumentException("Nedozvoljen broj igraca: " + playerIds.size());
        }

        this.playerIds = new ArrayList<>(playerIds);

        List<Card> deck = Deck.buildShuffled(random);
        int position = 0;
        for (String playerId : this.playerIds) {
            List<Card> hand = new ArrayList<>(deck.subList(position, position + GameRules.INITIAL_HAND_SIZE));
            hands.put(playerId, hand);
            position += GameRules.INITIAL_HAND_SIZE;
        }
        drawPile.addAll(deck.subList(position, deck.size()));

        this.currentIndex = random.nextInt(this.playerIds.size());
    }


    // Akcije igraca


    /**
     * Igrac na potezu baca karte i deklarise njihovu vrednost.
     * Karte odlaze na centar okrenute nadole, istinitost izjave se proverava
     * tek ako ga neko prozove.
     */
    public PlayResult playCards(String playerId, List<String> cardIds, int declaredValue) {
        requirePhase(GamePhase.TURN);
        requireCurrentPlayer(playerId);

        if (cardIds == null || cardIds.isEmpty()) {
            throw new GameException(ErrorCode.INVALID_CARDS, "Morate izabrati bar jednu kartu.");
        }
        if (cardIds.size() > GameRules.MAX_CARDS_PER_PLAY) {
            throw new GameException(ErrorCode.INVALID_CARDS,
                    "Najvise " + GameRules.MAX_CARDS_PER_PLAY + " karata po potezu.");
        }
        if (new HashSet<>(cardIds).size() != cardIds.size()) {
            throw new GameException(ErrorCode.INVALID_CARDS, "Ista karta je navedena vise puta.");
        }
        if (!Rank.isValidValue(declaredValue)) {
            throw new GameException(ErrorCode.INVALID_VALUE,
                    "Vrednost karte mora biti izmedju " + Rank.MIN_VALUE + " i " + Rank.MAX_VALUE + ".");
        }
        if (tableValue != OPEN_ROUND && declaredValue != tableValue) {
            throw new GameException(ErrorCode.INVALID_VALUE,
                    "U ovoj rundi se igra vrednost " + tableValue + ".");
        }

        List<Card> hand = hands.get(playerId);
        List<Card> played = new ArrayList<>(cardIds.size());
        for (String cardId : cardIds) {
            Card card = findInHand(hand, cardId);
            if (card == null) {
                throw new GameException(ErrorCode.INVALID_CARDS, "Nemate kartu " + cardId + " u ruci.");
            }
            played.add(card);
        }

        hand.removeAll(played);
        center.addAll(played);
        tableValue = declaredValue;
        pendingPlay = new PendingPlay(playerId, List.copyOf(played), declaredValue);
        phase = GamePhase.CALL_WINDOW;

        return new PlayResult(playerId, played.size(), declaredValue);
    }

    /**
     * Igrac na potezu vuce kartu sa centralnog spila umesto bacanja.
     * Vrednost runde se pritom ne menja.
     */
    public DrawResult drawCard(String playerId) {
        requirePhase(GamePhase.TURN);
        requireCurrentPlayer(playerId);

        if (drawPile.isEmpty()) {
            throw new GameException(ErrorCode.DRAW_PILE_EMPTY, "Centralni spil je prazan.");
        }

        Card card = drawPile.pop();
        hands.get(playerId).add(card);

        advanceFrom(playerId);
        return new DrawResult(playerId, card, currentPlayerId());
    }

    /**
     * Igrac proziva onoga ko je upravo odigrao potez.
     *
     * Karte se otkrivaju i porede sa deklarisanom vrednoscu: ako je optuzeni
     * lagao, on kupi centar i prozivac je sledeci na potezu, ako je govorio
     * istinu, prozivac kupi centar, a optuzeni igra ponovo.
     */
    public CallResolution callLiar(String callerId) {
        requirePhase(GamePhase.CALL_WINDOW);

        if (!playerIds.contains(callerId)) {
            throw new GameException(ErrorCode.NOT_IN_ROOM, "Niste za ovim stolom.");
        }
        if (callerId.equals(pendingPlay.playerId())) {
            throw new GameException(ErrorCode.INVALID_ACTION, "Ne mozete prozvati sami sebe.");
        }

        String accusedId = pendingPlay.playerId();
        int declaredValue = pendingPlay.declaredValue();
        List<Card> revealed = pendingPlay.cards();

        boolean wasLying = revealed.stream().anyMatch(card -> card.value() != declaredValue);
        String collectorId = wasLying ? accusedId : callerId;

        int collectedCount = center.size();
        hands.get(collectorId).addAll(center);
        center.clear();

        // Prozivanje zatvara rundu, sledeci igrac slobodno bira novu vrednost.
        tableValue = OPEN_ROUND;
        pendingPlay = null;

        // Ako je optuzeni govorio istinu i ostao bez karata, pobedio je uprkos prozivanju.
        String winner = !wasLying && hands.get(accusedId).isEmpty() ? accusedId : null;
        String nextPlayerId;

        if (winner != null) {
            finish(winner);
            nextPlayerId = null;
        } else {
            // Lagao je, prozivac je sledeci. Govorio je istinu, optuzeni igra ponovo.
            setCurrentPlayer(wasLying ? callerId : accusedId);
            phase = GamePhase.TURN;
            nextPlayerId = currentPlayerId();
        }

        return new CallResolution(callerId, accusedId, wasLying, revealed, declaredValue,
                collectorId, collectedCount, nextPlayerId, winner);
    }

    /**
     * Istekao je prozor za prozivanje, a niko nije prozvao.
     * Karte ostaju na centru, vrednost runde ostaje ista, red ide dalje,
     * osim ako je igrac odigrao svoje poslednje karte, u kom slucaju je pobedio.
     */
    public WindowCloseResult closeCallWindow() {
        requirePhase(GamePhase.CALL_WINDOW);

        String lastPlayerId = pendingPlay.playerId();
        pendingPlay = null;

        if (hands.get(lastPlayerId).isEmpty()) {
            finish(lastPlayerId);
            return new WindowCloseResult(null, lastPlayerId);
        }

        advanceFrom(lastPlayerId);
        phase = GamePhase.TURN;
        return new WindowCloseResult(currentPlayerId(), null);
    }

    /**
     * Uklanja odspojenog igraca iz partije; njegove karte izlaze iz igre.
     * Ako ostane samo jedan igrac, on automatski pobedjuje.
     *
     */
    public boolean removePlayer(String playerId) {
        int removedIndex = playerIds.indexOf(playerId);
        if (removedIndex < 0) {
            return false;
        }

        boolean wasAccused = pendingPlay != null && pendingPlay.playerId().equals(playerId);

        playerIds.remove(removedIndex);
        hands.remove(playerId);

        if (phase == GamePhase.FINISHED) {
            return true;
        }
        if (playerIds.size() == 1) {
            finish(playerIds.get(0));
            return true;
        }

        if (wasAccused) {
            // Potez onoga ko je otisao vise nema ko da proziva, karte ostaju na centru.
            pendingPlay = null;
            phase = GamePhase.TURN;
            currentIndex = removedIndex % playerIds.size();
        } else if (removedIndex < currentIndex) {
            currentIndex--;
        } else if (removedIndex == currentIndex) {
            currentIndex = removedIndex % playerIds.size();
        }

        return true;
    }


    // Citanje stanja


    public List<String> playerIds() {
        return Collections.unmodifiableList(playerIds);
    }

    public String currentPlayerId() {
        return playerIds.isEmpty() ? null : playerIds.get(currentIndex);
    }

    public List<Card> hand(String playerId) {
        List<Card> hand = hands.get(playerId);
        return hand == null ? List.of() : Collections.unmodifiableList(hand);
    }

    public int cardCount(String playerId) {
        return hand(playerId).size();
    }

    public int centerCount() {
        return center.size();
    }

    public int drawPileCount() {
        return drawPile.size();
    }

    /** Vrednost koju svi u tekucoj rundi moraju deklarisati OPEN_ROUND ako je runda otvorena. */
    public int tableValue() {
        return tableValue;
    }

    public GamePhase phase() {
        return phase;
    }

    public PendingPlay pendingPlay() {
        return pendingPlay;
    }

    public String winnerId() {
        return winnerId;
    }

    public boolean isFinished() {
        return phase == GamePhase.FINISHED;
    }

    /** Konacan poredak: pobednik prvi, ostali po broju preostalih karata. */
    public Map<String, Integer> remainingCards() {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String playerId : playerIds) {
            result.put(playerId, hands.get(playerId).size());
        }
        return result;
    }


    // Pomocne metode


    private void requirePhase(GamePhase expected) {
        if (phase != expected) {
            throw new GameException(ErrorCode.INVALID_ACTION, "Akcija nije moguca u ovom trenutku.");
        }
    }

    private void requireCurrentPlayer(String playerId) {
        if (!playerId.equals(currentPlayerId())) {
            throw new GameException(ErrorCode.NOT_YOUR_TURN, "Niste na potezu.");
        }
    }

    private Card findInHand(List<Card> hand, String cardId) {
        for (Card card : hand) {
            if (card.id().equals(cardId)) {
                return card;
            }
        }
        return null;
    }

    private void advanceFrom(String playerId) {
        int index = playerIds.indexOf(playerId);
        currentIndex = (index + 1) % playerIds.size();
    }

    private void setCurrentPlayer(String playerId) {
        int index = playerIds.indexOf(playerId);
        if (index >= 0) {
            currentIndex = index;
        }
    }

    private void finish(String winner) {
        this.winnerId = winner;
        this.phase = GamePhase.FINISHED;
        this.pendingPlay = null;
    }
}
