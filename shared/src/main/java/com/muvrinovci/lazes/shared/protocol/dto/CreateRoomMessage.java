package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Klijent -> Server: kreiranje nove sobe. */
public class CreateRoomMessage extends Message {

    private String playerName;

    public CreateRoomMessage() {
        super(MessageType.CREATE_ROOM);
    }

    public CreateRoomMessage(String playerName) {
        this();
        this.playerName = playerName;
    }

    public String getPlayerName() {
        return playerName;
    }
}
