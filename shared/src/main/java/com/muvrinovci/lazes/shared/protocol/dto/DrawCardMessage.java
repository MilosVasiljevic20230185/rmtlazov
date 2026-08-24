package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Klijent -> Server: vucenje karte sa centralnog spila umesto bacanja. */
public class DrawCardMessage extends Message {

    public DrawCardMessage() {
        super(MessageType.DRAW_CARD);
    }
}
