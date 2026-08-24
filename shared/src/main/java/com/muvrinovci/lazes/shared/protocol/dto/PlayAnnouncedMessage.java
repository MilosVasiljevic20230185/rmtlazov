package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/**
 * Server -> Klijent: emitovanje odigranog poteza, bez otkrivanja karata.
 *
 * <p>Nakon ove poruke otvara se prozor od {@code callWindowMs} milisekundi
 * u kom ostali igraci mogu poslati {@code call_liar}.</p>
 */
public class PlayAnnouncedMessage extends Message {

    private String playerId;
    private String playerName;
    private int declaredCount;
    private int declaredValue;
    private int callWindowMs;

    public PlayAnnouncedMessage() {
        super(MessageType.PLAY_ANNOUNCED);
    }

    public PlayAnnouncedMessage(String playerId, String playerName, int declaredCount,
                                int declaredValue, int callWindowMs) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
        this.declaredCount = declaredCount;
        this.declaredValue = declaredValue;
        this.callWindowMs = callWindowMs;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public int getDeclaredCount() {
        return declaredCount;
    }

    public int getDeclaredValue() {
        return declaredValue;
    }

    public int getCallWindowMs() {
        return callWindowMs;
    }
}
