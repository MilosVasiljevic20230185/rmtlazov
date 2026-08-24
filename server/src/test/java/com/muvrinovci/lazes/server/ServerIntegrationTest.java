package com.muvrinovci.lazes.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.muvrinovci.lazes.shared.GameRules;
import com.muvrinovci.lazes.shared.protocol.ErrorCode;
import com.muvrinovci.lazes.shared.protocol.dto.CallLiarMessage;
import com.muvrinovci.lazes.shared.protocol.dto.CallResultMessage;
import com.muvrinovci.lazes.shared.protocol.dto.CreateRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.ErrorMessage;
import com.muvrinovci.lazes.shared.protocol.dto.GameStartMessage;
import com.muvrinovci.lazes.shared.protocol.dto.HandUpdateMessage;
import com.muvrinovci.lazes.shared.protocol.dto.JoinRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.LobbyStateMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayAnnouncedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayCardsMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayerReadyMessage;
import com.muvrinovci.lazes.shared.protocol.dto.RoomJoinedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.StartGameMessage;
import com.muvrinovci.lazes.shared.protocol.dto.TurnUpdateMessage;

/** Provera celog puta poruka kroz pravi TCP soket, od kreiranja sobe do prozivanja. */
@Timeout(30)
class ServerIntegrationTest {

    private GameServer server;
    private int port;

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
    @DisplayName("Igrac kreira sobu i dobija kod, drugi se pridruzuje tim kodom")
    void createAndJoinRoom() throws IOException {
        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            host.send(new CreateRoomMessage("Pera"));
            RoomJoinedMessage joined = host.await(RoomJoinedMessage.class);

            assertEquals(GameRules.ROOM_CODE_LENGTH, joined.getRoomCode().length());
            assertTrue(joined.isHost());
            assertNotNull(joined.getPlayerId());

            guest.send(new JoinRoomMessage(joined.getRoomCode(), "Mika"));
            RoomJoinedMessage guestJoined = guest.await(RoomJoinedMessage.class);
            assertEquals(joined.getRoomCode(), guestJoined.getRoomCode());
            assertTrue(!guestJoined.isHost());

            LobbyStateMessage lobby = guest.await(LobbyStateMessage.class);
            assertEquals(2, lobby.getPlayers().size());
            assertEquals(joined.getPlayerId(), lobby.getHostId());
        }
    }

    @Test
    @DisplayName("Nepostojeci kod sobe vraca gresku ROOM_NOT_FOUND")
    void joinUnknownRoom() throws IOException {
        try (TestClient client = new TestClient(port)) {
            client.send(new JoinRoomMessage("XXXXXX", "Pera"));

            ErrorMessage error = client.await(ErrorMessage.class);
            assertEquals(ErrorCode.ROOM_NOT_FOUND, error.getCode());
        }
    }

    @Test
    @DisplayName("Peti igrac ne moze u punu sobu")
    void roomIsFullAfterFourPlayers() throws IOException {
        try (TestClient host = new TestClient(port)) {
            host.send(new CreateRoomMessage("Pera"));
            String code = host.await(RoomJoinedMessage.class).getRoomCode();

            List<TestClient> guests = List.of(new TestClient(port), new TestClient(port), new TestClient(port));
            for (int i = 0; i < guests.size(); i++) {
                guests.get(i).send(new JoinRoomMessage(code, "Gost" + i));
                guests.get(i).await(RoomJoinedMessage.class);
            }

            try (TestClient fifth = new TestClient(port)) {
                fifth.send(new JoinRoomMessage(code, "Peti"));
                ErrorMessage error = fifth.await(ErrorMessage.class);
                assertEquals(ErrorCode.ROOM_FULL, error.getCode());
            } finally {
                for (TestClient guest : guests) {
                    guest.close();
                }
            }
        }
    }

    @Test
    @DisplayName("Partija se pokrece tek kada su svi spremni, pa svako dobije 7 karata")
    void startGameDealsSevenCards() throws IOException {
        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            String code = openRoomWithTwoPlayers(host, guest);

            host.send(new PlayerReadyMessage(true));
            guest.send(new PlayerReadyMessage(true));

            LobbyStateMessage ready = awaitStartableLobby(host);
            assertTrue(ready.isCanStart());

            host.send(new StartGameMessage());

            GameStartMessage start = host.await(GameStartMessage.class);
            assertEquals(GameRules.INITIAL_HAND_SIZE, start.getHandSize());
            assertNotNull(start.getStartingPlayerId());

            HandUpdateMessage hand = host.await(HandUpdateMessage.class);
            assertEquals(GameRules.INITIAL_HAND_SIZE, hand.getCards().size());

            TurnUpdateMessage turn = host.await(TurnUpdateMessage.class);
            assertEquals(2, turn.getPlayers().size());
            assertEquals(GameRules.TOTAL_CARDS - 2 * GameRules.INITIAL_HAND_SIZE, turn.getDrawPileCount());
            assertEquals(0, turn.getCenterCount());

            assertTrue(code.length() == GameRules.ROOM_CODE_LENGTH);
        }
    }

    @Test
    @DisplayName("Odigran potez se najavljuje protivniku bez otkrivanja karata, pa prozivanje otkriva istinu")
    void playAndCallLiar() throws IOException {
        try (TestClient host = new TestClient(port); TestClient guest = new TestClient(port)) {
            openRoomWithTwoPlayers(host, guest);

            host.send(new PlayerReadyMessage(true));
            guest.send(new PlayerReadyMessage(true));
            awaitStartableLobby(host);
            host.send(new StartGameMessage());

            String startingPlayerId = host.await(GameStartMessage.class).getStartingPlayerId();

            // Ko je na potezu, taj baca; drugi ga proziva.
            TestClient mover = startingPlayerId.equals(hostId) ? host : guest;
            TestClient caller = mover == host ? guest : host;

            List<String> hand = mover.await(HandUpdateMessage.class).getCards();
            mover.await(TurnUpdateMessage.class);

            // Deklarise vrednost koja sigurno ne odgovara odigranoj karti - dakle laze.
            String played = hand.get(0);
            int actualValue = com.muvrinovci.lazes.shared.model.Card.fromId(played).value();
            int lie = actualValue == 1 ? 2 : 1;

            mover.send(new PlayCardsMessage(List.of(played), lie));

            PlayAnnouncedMessage announced = caller.await(PlayAnnouncedMessage.class);
            assertEquals(1, announced.getDeclaredCount());
            assertEquals(lie, announced.getDeclaredValue());
            assertEquals(GameRules.CALL_WINDOW_MS, announced.getCallWindowMs());

            caller.send(new CallLiarMessage());

            CallResultMessage result = caller.await(CallResultMessage.class);
            assertTrue(result.isWasLying(), "Deklarisana vrednost se ne poklapa sa odigranom kartom");
            assertEquals(List.of(played), result.getRevealedCards());
            assertEquals(result.getAccusedId(), result.getCardsCollectedBy());
            assertEquals(result.getCallerId(), result.getNextPlayerId());
        }
    }

    // ------------------------------------------------------------------

    private String hostId;

    /** Kreira sobu, ubacuje drugog igraca i vraca kod sobe. */
    private String openRoomWithTwoPlayers(TestClient host, TestClient guest) throws IOException {
        host.send(new CreateRoomMessage("Pera"));
        RoomJoinedMessage joined = host.await(RoomJoinedMessage.class);
        hostId = joined.getPlayerId();

        guest.send(new JoinRoomMessage(joined.getRoomCode(), "Mika"));
        guest.await(RoomJoinedMessage.class);

        return joined.getRoomCode();
    }

    /** Ceka lobby stanje u kome partija moze da se pokrene. */
    private LobbyStateMessage awaitStartableLobby(TestClient client) throws IOException {
        while (true) {
            LobbyStateMessage lobby = client.await(LobbyStateMessage.class);
            if (lobby.isCanStart()) {
                return lobby;
            }
        }
    }
}
