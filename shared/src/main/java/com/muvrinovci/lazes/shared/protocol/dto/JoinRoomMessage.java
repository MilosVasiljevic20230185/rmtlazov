package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Klijent -> Server: pridruzivanje postojecoj sobi preko koda. */
public class JoinRoomMessage extends Message {

    private String roomCode;
    private String playerName;

    /** Otisak uredjaja; sluzi da server prepozna igraca ako mu pukne veza. */
    private String deviceId;

    public JoinRoomMessage() {
        super(MessageType.JOIN_ROOM);
    }

    public JoinRoomMessage(String roomCode, String playerName) {
        this();
        this.roomCode = roomCode;
        this.playerName = playerName;
    }

    public JoinRoomMessage(String roomCode, String playerName, String deviceId) {
        this(roomCode, playerName);
        this.deviceId = deviceId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getDeviceId() {
        return deviceId;
    }
}
