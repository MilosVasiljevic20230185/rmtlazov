package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Klijent -> Server: pridruzivanje postojecoj sobi preko koda. */
public class JoinRoomMessage extends Message {

    private String roomCode;
    private String playerName;

    public JoinRoomMessage() {
        super(MessageType.JOIN_ROOM);
    }

    public JoinRoomMessage(String roomCode, String playerName) {
        this();
        this.roomCode = roomCode;
        this.playerName = playerName;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getPlayerName() {
        return playerName;
    }
}
