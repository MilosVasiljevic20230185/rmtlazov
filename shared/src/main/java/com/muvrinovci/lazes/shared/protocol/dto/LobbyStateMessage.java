package com.muvrinovci.lazes.shared.protocol.dto;

import java.util.List;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Server -> Klijent: azurirano stanje lobby-ja. */
public class LobbyStateMessage extends Message {

    /** Jedan igrac u lobby listi. */
    public static class LobbyPlayer {

        private String id;
        private String name;
        private boolean ready;
        private String avatar;

        public LobbyPlayer() {
        }

        public LobbyPlayer(String id, String name, boolean ready, String avatar) {
            this.id = id;
            this.name = name;
            this.ready = ready;
            this.avatar = avatar;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isReady() {
            return ready;
        }

        public String getAvatar() {
            return avatar;
        }
    }

    private List<LobbyPlayer> players;
    private String hostId;

    /** true kada u sobi ima najmanje dva igraca i svi su spremni. */
    private boolean canStart;

    public LobbyStateMessage() {
        super(MessageType.LOBBY_STATE);
    }

    public LobbyStateMessage(List<LobbyPlayer> players, String hostId, boolean canStart) {
        this();
        this.players = players;
        this.hostId = hostId;
        this.canStart = canStart;
    }

    public List<LobbyPlayer> getPlayers() {
        return players;
    }

    public String getHostId() {
        return hostId;
    }

    public boolean isCanStart() {
        return canStart;
    }
}
