package com.muvrinovci.lazes.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.muvrinovci.lazes.shared.GameRules;
import com.muvrinovci.lazes.shared.protocol.ErrorCode;
import com.muvrinovci.lazes.shared.protocol.dto.CreateRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.ErrorMessage;
import com.muvrinovci.lazes.shared.protocol.dto.GameOverMessage;
import com.muvrinovci.lazes.shared.protocol.dto.GameSnapshotMessage;
import com.muvrinovci.lazes.shared.protocol.dto.GameStartMessage;
import com.muvrinovci.lazes.shared.protocol.dto.JoinRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.LobbyStateMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayerDisconnectedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayerReadyMessage;
import com.muvrinovci.lazes.shared.protocol.dto.ReconnectMessage;
import com.muvrinovci.lazes.shared.protocol.dto.RoomJoinedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.StartGameMessage;
import com.muvrinovci.lazes.shared.protocol.dto.TurnUpdateMessage;

/**
 * Cuvanje mesta posle prekida veze i povratak u partiju, kroz pravi TCP soket.
 *
 * Povratak je moguc iskljucivo sa uredjaja sa koga je igrac i ispao, pa se u
 * testovima otisak uredjaja salje kao obican string.
 */
@Timeout(30)
class ServerReconnectTest {

    private static final String HOST_DEVICE = "device-host";
    private static final String GUEST_DEVICE = "device-guest";

    private GameServer server;
    private int port;

    private String hostId;
    private String guestId;
    private String roomCode;

    @BeforeEach
    void startServer() throws IOException {
        server = new GameServer();
        server.start(0);
        port = server.getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    // ------------------------------------------------------------------

    @Test
    @DisplayName("Prekid veze ne izbacuje igraca - mesto ostaje za stolom i cuva se")
    void seatIsHeldAfterConnectionDrops() throws IOException {
        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            String starterId = startGame(host, guest);

            // Ispada onaj ko je na potezu, pa soba odmah javi novo stanje stola.
            TestClient leaving = starterId.equals(hostId) ? host : guest;
            TestClient staying = leaving == host ? guest : host;
            leaving.close();

            PlayerDisconnectedMessage dropped = staying.await(PlayerDisconnectedMessage.class);
            assertEquals(starterId, dropped.getPlayerId());
            assertTrue(dropped.isTemporary(), "Mesto se cuva, igrac nije izbacen");
            assertEquals(GameRules.DISCONNECT_GRACE_SECONDS, dropped.getGraceSeconds());

            TurnUpdateMessage turn = staying.await(TurnUpdateMessage.class);
            assertEquals(2, turn.getPlayers().size(), "Odspojeno mesto ostaje za stolom");
            assertFalse(connectedFlagOf(turn, starterId), "Mesto je oznaceno kao odspojeno");
            assertEquals(GameRules.DISCONNECTED_TURN_SECONDS, turn.getTurnSeconds(),
                    "Server ne ceka pun minut na igraca koga nema");
        }
    }

    @Test
    @DisplayName("Server automatski igra umesto odspojenog igraca")
    void serverPlaysForDisconnectedSeat() throws IOException {
        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            String starterId = startGame(host, guest);

            TestClient leaving = starterId.equals(hostId) ? host : guest;
            TestClient staying = leaving == host ? guest : host;
            leaving.close();

            staying.await(PlayerDisconnectedMessage.class);

            // Po isteku skracenog tajmera red mora sam da predje na preostalog igraca.
            String currentPlayerId = starterId;
            while (currentPlayerId.equals(starterId)) {
                currentPlayerId = staying.await(TurnUpdateMessage.class).getCurrentPlayerId();
            }

            assertEquals(staying == host ? hostId : guestId, currentPlayerId);
        }
    }

    @Test
    @DisplayName("Isti uredjaj se vraca na svoje mesto i dobija celo stanje partije")
    void sameDeviceReturnsToItsSeat() throws IOException {
        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            startGame(host, guest);
            guest.close();
            host.await(PlayerDisconnectedMessage.class);

            try (TestClient returning = new TestClient(port)) {
                returning.send(new ReconnectMessage(GUEST_DEVICE, "Mika"));

                RoomJoinedMessage joined = returning.await(RoomJoinedMessage.class);
                assertEquals(roomCode, joined.getRoomCode());
                assertEquals(guestId, joined.getPlayerId(), "Vraca se na isto mesto, ne pravi novo");

                GameSnapshotMessage snapshot = returning.await(GameSnapshotMessage.class);
                assertEquals(guestId, snapshot.getPlayerId());
                assertEquals(roomCode, snapshot.getRoomCode());
                assertFalse(snapshot.getHand().isEmpty(), "Ruka se vraca onakva kakva je ostala");
                assertEquals(2, snapshot.getPlayers().size());
                assertNotNull(snapshot.getCurrentPlayerId());
                assertTrue(snapshot.getPlayers().stream().allMatch(TurnUpdateMessage.PlayerInfo::isConnected));
            }
        }
    }

    @Test
    @DisplayName("Drugi uredjaj ne moze da uskoci na tudje mesto")
    void otherDeviceCannotTakeTheSeat() throws IOException {
        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            startGame(host, guest);
            guest.close();
            host.await(PlayerDisconnectedMessage.class);

            try (TestClient stranger = new TestClient(port)) {
                stranger.send(new ReconnectMessage("device-telefon", "Mika"));

                ErrorMessage error = stranger.await(ErrorMessage.class);
                assertEquals(ErrorCode.RECONNECT_FAILED, error.getCode());
            }
        }
    }

    @Test
    @DisplayName("Mesto sa zivom konekcijom se ne preuzima")
    void activeSeatIsNotTakenOver() throws IOException {
        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            startGame(host, guest);

            try (TestClient second = new TestClient(port)) {
                second.send(new ReconnectMessage(GUEST_DEVICE, "Mika"));

                ErrorMessage error = second.await(ErrorMessage.class);
                assertEquals(ErrorCode.SESSION_ACTIVE, error.getCode());
            }

            // Prva konekcija je netaknuta i dalje prima stanje partije.
            guest.send(new PlayerReadyMessage(true));
            assertNotNull(guest.await(ErrorMessage.class), "Partija je u toku, ali veza radi");
        }
    }

    @Test
    @DisplayName("Kada rok istekne, mesto se konacno gasi i partija se zavrsava")
    void seatIsRemovedWhenGraceExpires() throws IOException {
        server.getRoomManager().setTimeoutsForTest(1, 30);

        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            startGame(host, guest);
            guest.close();

            PlayerDisconnectedMessage held = host.await(PlayerDisconnectedMessage.class);
            assertTrue(held.isTemporary());

            // Sa samo jednim preostalim igracem partija se zavrsava.
            GameOverMessage over = host.await(GameOverMessage.class);
            assertEquals(hostId, over.getWinnerId());
        }
    }

    @Test
    @DisplayName("Soba bez ijednog povezanog igraca gasi se po isteku roka")
    void abandonedRoomIsClosed() throws IOException, InterruptedException {
        server.getRoomManager().setTimeoutsForTest(30, 1);

        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            startGame(host, guest);
            host.close();
            guest.close();
        }

        Thread.sleep(2500);
        assertEquals(0, server.getRoomManager().roomCount(), "Napustena soba se ugasila");

        try (TestClient returning = new TestClient(port)) {
            returning.send(new ReconnectMessage(HOST_DEVICE, "Pera"));
            assertEquals(ErrorCode.RECONNECT_FAILED, returning.await(ErrorMessage.class).getCode());
        }
    }

    @Test
    @DisplayName("Povratak pre isteka roka odmrzava partiju")
    void returningBeforeTimeoutResumesGame() throws IOException {
        server.getRoomManager().setTimeoutsForTest(30, 10);

        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            startGame(host, guest);
            host.close();
            guest.close();
        }

        try (TestClient returning = new TestClient(port)) {
            returning.send(new ReconnectMessage(HOST_DEVICE, "Pera"));

            returning.await(RoomJoinedMessage.class);
            GameSnapshotMessage snapshot = returning.await(GameSnapshotMessage.class);

            assertEquals(hostId, snapshot.getPlayerId());
            assertEquals(2, snapshot.getPlayers().size());
            assertEquals(1, server.getRoomManager().roomCount(), "Soba je docekala povratak");
        }
    }

    // ------------------------------------------------------------------

    /** Otvara sobu sa dva igraca, pokrece partiju i vraca ko je prvi na potezu. */
    private String startGame(TestClient host, TestClient guest) throws IOException {
        host.send(new CreateRoomMessage("Pera", HOST_DEVICE));
        RoomJoinedMessage hostJoined = host.await(RoomJoinedMessage.class);
        hostId = hostJoined.getPlayerId();
        roomCode = hostJoined.getRoomCode();

        guest.send(new JoinRoomMessage(roomCode, "Mika", GUEST_DEVICE));
        guestId = guest.await(RoomJoinedMessage.class).getPlayerId();

        host.send(new PlayerReadyMessage(true));
        guest.send(new PlayerReadyMessage(true));

        while (!host.await(LobbyStateMessage.class).isCanStart()) {
            // Ceka se dok obe potvrde spremnosti ne stignu do sobe.
        }

        host.send(new StartGameMessage());
        return host.await(GameStartMessage.class).getStartingPlayerId();
    }

    private boolean connectedFlagOf(TurnUpdateMessage turn, String playerId) {
        return turn.getPlayers().stream()
                .filter(player -> player.getId().equals(playerId))
                .findFirst()
                .orElseThrow()
                .isConnected();
    }
}
