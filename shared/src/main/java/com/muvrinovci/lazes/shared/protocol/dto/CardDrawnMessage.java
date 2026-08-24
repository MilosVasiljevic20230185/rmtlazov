package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/**
 * Server -> Klijent: igrac je povukao kartu sa centralnog spila.
 *
 * <p>Salje se svima radi log prikaza; sama karta se ne otkriva - vlasnik je
 * saznaje kroz privatnu {@code hand_update} poruku.</p>
 */
public class CardDrawnMessage extends Message {

    private String playerId;
    private String playerName;
    private boolean automatic;

    public CardDrawnMessage() {
        super(MessageType.CARD_DRAWN);
    }

    public CardDrawnMessage(String playerId, String playerName, boolean automatic) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
        this.automatic = automatic;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    /** {@code true} kada je server povukao kartu umesto igraca po isteku tajmera. */
    public boolean isAutomatic() {
        return automatic;
    }
}
