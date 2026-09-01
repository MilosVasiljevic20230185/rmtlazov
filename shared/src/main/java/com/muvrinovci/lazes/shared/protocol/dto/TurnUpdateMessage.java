package com.muvrinovci.lazes.shared.protocol.dto;

import java.util.List;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Server -> Klijent: stanje stola i trenutnog poteza. */
public class TurnUpdateMessage extends Message {

    /** Javno vidljivi podaci o jednom igracu za stolom. */
    public static class PlayerInfo {

        private String id;
        private String name;
        private String avatar;
        private int cardCount;

        /** false dok se mesto cuva zbog prekinute veze. */
        private boolean connected = true;

        public PlayerInfo() {
        }

        public PlayerInfo(String id, String name, String avatar, int cardCount) {
            this(id, name, avatar, cardCount, true);
        }

        public PlayerInfo(String id, String name, String avatar, int cardCount, boolean connected) {
            this.id = id;
            this.name = name;
            this.avatar = avatar;
            this.cardCount = cardCount;
            this.connected = connected;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getAvatar() {
            return avatar;
        }

        public int getCardCount() {
            return cardCount;
        }

        public boolean isConnected() {
            return connected;
        }
    }

    private String currentPlayerId;

    /** Vrednost koju svi igraci u tekucoj rundi moraju deklarisati; 0 kada je runda otvorena. */
    private int tableValue;

    private int centerCount;
    private int drawPileCount;
    private int turnSeconds;
    private List<PlayerInfo> players;

    public TurnUpdateMessage() {
        super(MessageType.TURN_UPDATE);
    }

    public TurnUpdateMessage(String currentPlayerId, int tableValue, int centerCount,
                             int drawPileCount, int turnSeconds, List<PlayerInfo> players) {
        this();
        this.currentPlayerId = currentPlayerId;
        this.tableValue = tableValue;
        this.centerCount = centerCount;
        this.drawPileCount = drawPileCount;
        this.turnSeconds = turnSeconds;
        this.players = players;
    }

    public String getCurrentPlayerId() {
        return currentPlayerId;
    }

    public int getTableValue() {
        return tableValue;
    }

    public int getCenterCount() {
        return centerCount;
    }

    public int getDrawPileCount() {
        return drawPileCount;
    }

    public int getTurnSeconds() {
        return turnSeconds;
    }

    public List<PlayerInfo> getPlayers() {
        return players;
    }
}
