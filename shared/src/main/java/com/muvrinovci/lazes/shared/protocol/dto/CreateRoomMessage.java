package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Klijent -> Server: kreiranje nove sobe. */
public class CreateRoomMessage extends Message {

    private String playerName;

    /** Otisak uredjaja, sluzi da server prepozna igraca ako mu pukne veza. */
    private String deviceId;

    public CreateRoomMessage() {
        super(MessageType.CREATE_ROOM);
    }

    public CreateRoomMessage(String playerName) {
        this();
        this.playerName = playerName;
    }

    public CreateRoomMessage(String playerName, String deviceId) {
        this(playerName);
        this.deviceId = deviceId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
