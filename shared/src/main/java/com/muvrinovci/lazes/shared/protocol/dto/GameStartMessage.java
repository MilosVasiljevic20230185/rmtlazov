package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Server -> Klijent: partija pocinje, uz odbrojavanje 3 - 2 - 1 - Start. */
public class GameStartMessage extends Message {

    private int countdown;
    private String startingPlayerId;
    private int handSize;

    public GameStartMessage() {
        super(MessageType.GAME_START);
    }

    public GameStartMessage(int countdown, String startingPlayerId, int handSize) {
        this();
        this.countdown = countdown;
        this.startingPlayerId = startingPlayerId;
        this.handSize = handSize;
    }

    public int getCountdown() {
        return countdown;
    }

    public String getStartingPlayerId() {
        return startingPlayerId;
    }

    public int getHandSize() {
        return handSize;
    }
}
