package com.muvrinovci.lazes.client;

/** Podaci o tekucoj sesiji koje dele svi ekrani klijenta. */
public class GameSession {

    private String playerName;
    private String playerId;
    private String roomCode;
    private boolean host;

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

    public boolean isMe(String id) {
        return playerId != null && playerId.equals(id);
    }

    public void clearRoom() {
        roomCode = null;
        playerId = null;
        host = false;
    }
}
