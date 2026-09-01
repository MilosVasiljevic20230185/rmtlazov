package com.muvrinovci.lazes.shared.protocol.dto;

import java.util.List;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/**
 * Server -> Klijent: celo stanje partije u jednoj poruci.
 *
 * Salje se igracu koji se vratio na svoje mesto, umesto niza poruka koje je
 * propustio dok je bio odspojen. Klijent na osnovu ove poruke iscrtava sto od
 * nule i nastavlja partiju iz zatecene faze.
 */
public class GameSnapshotMessage extends Message {

    /** Faza u kojoj je partija zatecena: ceka se potez. */
    public static final String PHASE_TURN = "TURN";

    /** Faza u kojoj je partija zatecena: otvoren je prozor za prozivanje. */
    public static final String PHASE_CALL_WINDOW = "CALL_WINDOW";

    private String roomCode;
    private String playerId;
    private String phase;

    /** Karte u ruci igraca koji se vratio. */
    private List<String> hand;

    private String currentPlayerId;
    private int tableValue;
    private int centerCount;
    private int drawPileCount;

    /** Koliko jos traje tajmer tekuce faze. */
    private long remainingMs;

    private List<TurnUpdateMessage.PlayerInfo> players;

    // Popunjeno samo kada je faza CALL_WINDOW.
    private String announcerId;
    private String announcerName;
    private int declaredCount;
    private int declaredValue;

    public GameSnapshotMessage() {
        super(MessageType.GAME_SNAPSHOT);
    }

    public GameSnapshotMessage(String roomCode, String playerId, String phase, List<String> hand,
                               String currentPlayerId, int tableValue, int centerCount,
                               int drawPileCount, long remainingMs,
                               List<TurnUpdateMessage.PlayerInfo> players) {
        this();
        this.roomCode = roomCode;
        this.playerId = playerId;
        this.phase = phase;
        this.hand = hand;
        this.currentPlayerId = currentPlayerId;
        this.tableValue = tableValue;
        this.centerCount = centerCount;
        this.drawPileCount = drawPileCount;
        this.remainingMs = remainingMs;
        this.players = players;
    }

    /** Dopunjava snapshot podacima o potezu koji ceka na prozivanje. */
    public void setPendingPlay(String announcerId, String announcerName,
                               int declaredCount, int declaredValue) {
        this.announcerId = announcerId;
        this.announcerName = announcerName;
        this.declaredCount = declaredCount;
        this.declaredValue = declaredValue;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public String getPlayerId() {
        return playerId;
    }

    public String getPhase() {
        return phase;
    }

    public boolean isCallWindow() {
        return PHASE_CALL_WINDOW.equals(phase);
    }

    public List<String> getHand() {
        return hand;
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

    public long getRemainingMs() {
        return remainingMs;
    }

    public List<TurnUpdateMessage.PlayerInfo> getPlayers() {
        return players;
    }

    public String getAnnouncerId() {
        return announcerId;
    }

    public String getAnnouncerName() {
        return announcerName;
    }

    public int getDeclaredCount() {
        return declaredCount;
    }

    public int getDeclaredValue() {
        return declaredValue;
    }
}
