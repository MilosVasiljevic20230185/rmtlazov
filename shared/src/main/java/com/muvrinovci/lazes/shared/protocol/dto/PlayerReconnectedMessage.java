package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Server -> Klijent: igrac kome je pukla veza vratio se na svoje mesto. */
public class PlayerReconnectedMessage extends Message {

    private String playerId;
    private String playerName;

    public PlayerReconnectedMessage() {
        super(MessageType.PLAYER_RECONNECTED);
    }

    public PlayerReconnectedMessage(String playerId, String playerName) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }
}
