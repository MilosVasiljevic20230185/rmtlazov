package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/**
 * Server -> Klijent: potvrda ulaska u sobu.
 *
 * Ovom porukom klijent saznaje SVOJ  playerId, koji mu je dalje
 * neophodan da bi u turn_update i  lobby_state porukama
 * prepoznao sebe medju ostalim igracima.
 */
public class RoomJoinedMessage extends Message {

    private String roomCode;
    private String playerId;
    private boolean host;

    public RoomJoinedMessage() {
        super(MessageType.ROOM_JOINED);
    }

    public RoomJoinedMessage(String roomCode, String playerId, boolean host) {
        this();
        this.roomCode = roomCode;
        this.playerId = playerId;
        this.host = host;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getPlayerId() {
        return playerId;
    }

    public boolean isHost() {
        return host;
    }
}
