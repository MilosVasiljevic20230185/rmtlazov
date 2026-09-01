package com.muvrinovci.lazes.client;

import com.muvrinovci.lazes.shared.protocol.dto.GameSnapshotMessage;
import com.muvrinovci.lazes.shared.protocol.dto.LobbyStateMessage;

/** Podaci o tekucoj sesiji koje dele svi ekrani klijenta. */
public class GameSession {

    private String playerName;
    private String playerId;
    private String roomCode;
    private boolean host;

    /**
     * Stanje partije zateceno pri povratku u igru.
     *
     * Glavni meni ga ostavlja ovde, a sto ga pokupi u init i iscrta
     * partiju u toku umesto uobicajenog odbrojavanja od pocetka.
     */
    private GameSnapshotMessage pendingSnapshot;

    /** Isto to, za slucaj da se partija zavrsila dok igraca nije bilo. */
    private LobbyStateMessage pendingLobbyState;

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    /** Identifikator koji je server dodelio ovom igracu. */
    public String getPlayerId() {
        return playerId;
    }

    public void setPlayerId(String playerId) {
        this.playerId = playerId;
    }

    public String getRoomCode() {
        return roomCode;
    }

    public void setRoomCode(String roomCode) {
        this.roomCode = roomCode;
    }

    public boolean isHost() {
        return host;
    }

    public void setHost(boolean host) {
        this.host = host;
    }

    public GameSnapshotMessage getPendingSnapshot() {
        return pendingSnapshot;
    }

    public void setPendingSnapshot(GameSnapshotMessage pendingSnapshot) {
        this.pendingSnapshot = pendingSnapshot;
    }

    /** Vraca snapshot i odmah ga zaboravlja, da se ne bi primenio dva puta. */
    public GameSnapshotMessage takePendingSnapshot() {
        GameSnapshotMessage snapshot = pendingSnapshot;
        pendingSnapshot = null;
        return snapshot;
    }

    public void setPendingLobbyState(LobbyStateMessage pendingLobbyState) {
        this.pendingLobbyState = pendingLobbyState;
    }

    public LobbyStateMessage takePendingLobbyState() {
        LobbyStateMessage state = pendingLobbyState;
        pendingLobbyState = null;
        return state;
    }

    public boolean isMe(String id) {
        return playerId != null && playerId.equals(id);
    }

    public void clearRoom() {
        roomCode = null;
        playerId = null;
        host = false;
        pendingSnapshot = null;
        pendingLobbyState = null;
    }
}
