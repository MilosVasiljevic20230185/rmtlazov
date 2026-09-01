package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Server -> Klijent: igrac je napustio partiju. */
public class PlayerDisconnectedMessage extends Message {

    private String playerId;
    private String playerName;
    private String reason;

    /** true kada se mesto jos uvek cuva i igrac moze da se vrati. */
    private boolean temporary;

    /** Koliko sekundi se mesto cuva; ima smisla samo uz temporary. */
    private int graceSeconds;

    public PlayerDisconnectedMessage() {
        super(MessageType.PLAYER_DISCONNECTED);
    }

    public PlayerDisconnectedMessage(String playerId, String playerName, String reason) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
        this.reason = reason;
    }

    public PlayerDisconnectedMessage(String playerId, String playerName, String reason,
                                     boolean temporary, int graceSeconds) {
        this(playerId, playerName, reason);
        this.temporary = temporary;
        this.graceSeconds = graceSeconds;
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

    public boolean isTemporary() {
        return temporary;
    }

    public int getGraceSeconds() {
        return graceSeconds;
    }
}
