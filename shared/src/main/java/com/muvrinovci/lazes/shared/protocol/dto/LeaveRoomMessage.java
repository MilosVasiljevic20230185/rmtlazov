package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Klijent -> Server: napustanje sobe. */
public class LeaveRoomMessage extends Message {

    public LeaveRoomMessage() {
        super(MessageType.LEAVE_ROOM);
    }
}
