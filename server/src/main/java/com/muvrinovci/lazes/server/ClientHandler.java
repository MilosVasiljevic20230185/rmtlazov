package com.muvrinovci.lazes.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.muvrinovci.lazes.server.util.Log;
import com.muvrinovci.lazes.shared.protocol.ErrorCode;
import com.muvrinovci.lazes.shared.protocol.JsonCodec;
import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;
import com.muvrinovci.lazes.shared.protocol.ProtocolException;
import com.muvrinovci.lazes.shared.protocol.dto.CreateRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.ErrorMessage;
import com.muvrinovci.lazes.shared.protocol.dto.JoinRoomMessage;

/**
 * Jedna nit po konekciji: cita poruke sa soketa i prosledjuje ih sobi igraca.
 *
 * <p>Poruke {@code create_room} i {@code join_room} obradjuje sam handler jer
 * igrac tada jos nije ni u jednoj sobi; sve ostalo se prosledjuje sobi, koja ih
 * izvrsava u svojoj niti.</p>
 */
public class ClientHandler implements Runnable {

    private static final int MAX_NAME_LENGTH = 16;

    private final Socket socket;
    private final RoomManager roomManager;
    private final PrintWriter writer;
    private final BufferedReader reader;

    private ServerPlayer player;

    public ClientHandler(Socket socket, RoomManager roomManager) throws IOException {
        this.socket = socket;
        this.roomManager = roomManager;
        this.reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        this.writer = new PrintWriter(
                new java.io.OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    @Override
    public void run() {
        Log.info("Nova konekcija sa %s", socket.getRemoteSocketAddress());
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                dispatch(line);
            }
        } catch (IOException e) {
            Log.info("Konekcija %s prekinuta: %s", socket.getRemoteSocketAddress(), e.getMessage());
        } finally {
            disconnect("connection_lost");
        }
    }

    private void dispatch(String line) {
        Message message;
        try {
            message = JsonCodec.decode(line);
        } catch (ProtocolException e) {
            Log.warn("Neispravna poruka sa %s: %s", socket.getRemoteSocketAddress(), e.getMessage());
            send(new ErrorMessage(ErrorCode.MALFORMED_MESSAGE, e.getMessage()));
            return;
        }

        switch (message.getType()) {
            case MessageType.CREATE_ROOM -> onCreateRoom((CreateRoomMessage) message);
            case MessageType.JOIN_ROOM -> onJoinRoom((JoinRoomMessage) message);
            default -> forwardToRoom(message);
        }
    }

    private void onCreateRoom(CreateRoomMessage message) {
        if (!ensurePlayer(message.getPlayerName())) {
            return;
        }
        roomManager.createRoom(player);
    }

    private void onJoinRoom(JoinRoomMessage message) {
        if (!ensurePlayer(message.getPlayerName())) {
            return;
        }
        roomManager.joinRoom(message.getRoomCode(), player);
    }

    private void forwardToRoom(Message message) {
        if (player == null || player.getRoom() == null) {
            send(new ErrorMessage(ErrorCode.NOT_IN_ROOM, "Niste ni u jednoj sobi."));
            return;
        }

        Room room = player.getRoom();
        room.submit(() -> room.handle(player, message));
    }

    /** Kreira igraca pri prvoj akciji ili odbija neispravno ime. */
    private boolean ensurePlayer(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            send(new ErrorMessage(ErrorCode.INVALID_NAME, "Unesite ime igraca."));
            return false;
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            trimmed = trimmed.substring(0, MAX_NAME_LENGTH);
        }

        if (player == null) {
            player = new ServerPlayer(this, trimmed);
        } else {
            if (player.getRoom() != null) {
                send(new ErrorMessage(ErrorCode.INVALID_ACTION, "Vec ste u sobi."));
                return false;
            }
            player.setName(trimmed);
        }
        return true;
    }

    /** Salje poruku klijentu. Sinhronizovano jer vise soba/niti moze pisati istom klijentu. */
    public synchronized void send(Message message) {
        writer.println(JsonCodec.encode(message));
    }

    private void disconnect(String reason) {
        if (player != null && player.getRoom() != null) {
            Room room = player.getRoom();
            room.submit(() -> room.leave(player, reason));
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // Konekcija je vec zatvorena.
        }
    }
}
