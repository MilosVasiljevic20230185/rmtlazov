package com.muvrinovci.lazes.shared.protocol.dto;

import java.util.List;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/**
 * Klijent -> Server: odigravanje karata sa deklarisanom vrednoscu.
 *
 * Broj odigranih karata izvodi se iz duzine liste  cardIds.
 * Vrednost declaredValue ne mora odgovarati stvarnom sadrzaju karata,
 * to je izjava cija se istinitost proverava tek prilikom prozivanja.
 */
public class PlayCardsMessage extends Message {

    private List<String> cardIds;
    private int declaredValue;

    public PlayCardsMessage() {
        super(MessageType.PLAY_CARDS);
    }

    public PlayCardsMessage(List<String> cardIds, int declaredValue) {
        this();
        this.cardIds = cardIds;
        this.declaredValue = declaredValue;
    }

    public List<String> getCardIds() {
        return cardIds;
    }

    public int getDeclaredValue() {
        return declaredValue;
    }
}
