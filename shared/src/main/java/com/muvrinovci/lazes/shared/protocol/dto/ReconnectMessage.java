package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/**
 * Klijent -> Server: povratak na mesto koje se cuva posle prekida veze.
 *
 * Kod sobe se ne salje, server sam zna u kojoj sobi je mesto ovog uredjaja.
 * Povratak je moguc iskljucivo sa uredjaja sa koga je igrac i ispao.
 */
public class ReconnectMessage extends Message {

    private String deviceId;
    private String playerName;

    public ReconnectMessage() {
        super(MessageType.RECONNECT);
    }

    public ReconnectMessage(String deviceId, String playerName) {
        this();
        this.deviceId = deviceId;
        this.playerName = playerName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getPlayerName() {
        return playerName;
    }
}
