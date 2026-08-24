package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Klijent -> Server: oznacavanje spremnosti u lobby-ju. */
public class PlayerReadyMessage extends Message {

    private boolean ready;

    public PlayerReadyMessage() {
        super(MessageType.PLAYER_READY);
    }

    public PlayerReadyMessage(boolean ready) {
        this();
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }
}
