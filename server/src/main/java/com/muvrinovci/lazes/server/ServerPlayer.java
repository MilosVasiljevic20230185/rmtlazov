package com.muvrinovci.lazes.server;

import java.util.UUID;

import com.muvrinovci.lazes.shared.protocol.Message;

/** Igrac na serverskoj strani: identitet, stanje u lobby-ju i veza ka njegovoj konekciji. */
public class ServerPlayer {

    private final String id = UUID.randomUUID().toString();
    private final ClientHandler handler;

    private String name;
    private String avatar;
    private boolean ready;
    private Room room;

    public ServerPlayer(ClientHandler handler, String name) {
        this.handler = handler;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public void send(Message message) {
        handler.send(message);
    }

    @Override
    public String toString() {
        return name + "(" + id.substring(0, 8) + ")";
    }
}
