package com.muvrinovci.lazes.shared.protocol.dto;

import java.util.List;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/**
 * Server -> Klijent: ishod prozivanja.
 *
 * <p>{@code wasLying} govori da li je optuzeni igrac zaista lagao,
 * a {@code cardsCollectedBy} ko zbog toga uzima sve karte sa centra stola.</p>
 */
public class CallResultMessage extends Message {

    private String callerId;
    private String callerName;
    private String accusedId;
    private String accusedName;
    private int declaredValue;
    private boolean wasLying;

    /** Karte koje su bile odigrane u poslednjem potezu - sada otkrivene svima. */
    private List<String> revealedCards;

    private String cardsCollectedBy;
    private int collectedCount;
    private String nextPlayerId;

    public CallResultMessage() {
        super(MessageType.CALL_RESULT);
    }

    public CallResultMessage(String callerId, String callerName, String accusedId, String accusedName,
                             int declaredValue, boolean wasLying, List<String> revealedCards,
                             String cardsCollectedBy, int collectedCount, String nextPlayerId) {
        this();
        this.callerId = callerId;
        this.callerName = callerName;
        this.accusedId = accusedId;
        this.accusedName = accusedName;
        this.declaredValue = declaredValue;
        this.wasLying = wasLying;
        this.revealedCards = revealedCards;
        this.cardsCollectedBy = cardsCollectedBy;
        this.collectedCount = collectedCount;
        this.nextPlayerId = nextPlayerId;
    }

    public String getCallerId() {
        return callerId;
    }

    public String getCallerName() {
        return callerName;
    }

    public String getAccusedId() {
        return accusedId;
    }

    public String getAccusedName() {
        return accusedName;
    }

    public int getDeclaredValue() {
        return declaredValue;
    }

    public boolean isWasLying() {
        return wasLying;
    }

    public List<String> getRevealedCards() {
        return revealedCards;
    }

    public String getCardsCollectedBy() {
        return cardsCollectedBy;
    }

    public int getCollectedCount() {
        return collectedCount;
    }

    public String getNextPlayerId() {
        return nextPlayerId;
    }
}
