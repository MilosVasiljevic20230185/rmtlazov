package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Klijent -> Server: host pokrece partiju kada su svi spremni. */
public class StartGameMessage extends Message {

    public StartGameMessage() {
        super(MessageType.START_GAME);
    }
}
