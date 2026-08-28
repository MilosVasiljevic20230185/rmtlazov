package com.muvrinovci.lazes.shared.protocol.dto;

import java.util.List;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/**
 * Server -> Klijent: privatno azuriranje ruke.
 *
 * Ova poruka se salje iskljucivo klijentu cija se ruka menja, nikada
 * protivnicima, oni saznaju samo broj karata kroz turn_update
 */
public class HandUpdateMessage extends Message {

    private List<String> cards;

    public HandUpdateMessage() {
        super(MessageType.HAND_UPDATE);
    }

    public HandUpdateMessage(List<String> cards) {
        this();
        this.cards = cards;
    }

    public List<String> getCards() {
        return cards;
    }
}
