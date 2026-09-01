package com.muvrinovci.lazes.server;

import java.util.UUID;

import com.muvrinovci.lazes.shared.protocol.Message;

/**
 * Igrac na serverskoj strani: identitet, stanje u lobby-ju i veza ka njegovoj konekciji.
 *
 * Mesto sme da nadzivi svoju konekciju: dok traje grace period posle prekida
 * veze handler je null, mesto ostaje za stolom, a poruke koje bi mu
 * isle se preskacu. Celo stanje mu se vrati snapshotom kada se ponovo javi.
 */
public class ServerPlayer {

    private final String id = UUID.randomUUID().toString();

    private ClientHandler handler;

    private String name;
    private String avatar;
    private boolean ready;
    private Room room;

    /** Otisak uredjaja sa koga je igrac usao  */
    private String deviceId;

    /** Trenutak prekida veze, sluzi da se razlikuju mesta koja cekaju povratak. */
    private long disconnectedAt;

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

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public long getDisconnectedAt() {
        return disconnectedAt;
    }

    public void setDisconnectedAt(long disconnectedAt) {
        this.disconnectedAt = disconnectedAt;
    }

    public ClientHandler getHandler() {
        return handler;
    }

    public void setHandler(ClientHandler handler) {
        this.handler = handler;
    }

    /** false dok mesto ceka povratak igraca i nema aktivnu konekciju. */
    public boolean isConnected() {
        return handler != null;
    }

    /** Salje poruku igracu, dok mesto nema konekciju poruka se tiho preskace. */
    public void send(Message message) {
        ClientHandler target = handler;
        if (target != null) {
            target.send(message);
        }
    }

    @Override
    public String toString() {
        return name + "(" + id.substring(0, 8) + ")";
    }
}
