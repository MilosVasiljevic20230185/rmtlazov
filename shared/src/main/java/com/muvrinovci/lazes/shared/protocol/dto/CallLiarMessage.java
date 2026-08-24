package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Klijent -> Server: prozivanje igraca na potezu da laze. */
public class CallLiarMessage extends Message {

    public CallLiarMessage() {
        super(MessageType.CALL_LIAR);
    }
}
