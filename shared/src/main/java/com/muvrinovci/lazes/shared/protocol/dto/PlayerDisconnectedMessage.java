package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Server -> Klijent: igrac je napustio partiju. */
public class PlayerDisconnectedMessage extends Message {

    private String playerId;
    private String playerName;
    private String reason;

    public PlayerDisconnectedMessage() {
        super(MessageType.PLAYER_DISCONNECTED);
    }

    public PlayerDisconnectedMessage(String playerId, String playerName, String reason) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
        this.reason = reason;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getReason() {
        return reason;
    }
}
