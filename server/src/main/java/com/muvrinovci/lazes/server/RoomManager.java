package com.muvrinovci.lazes.server;

import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.muvrinovci.lazes.server.util.Log;
import com.muvrinovci.lazes.shared.GameRules;
import com.muvrinovci.lazes.shared.protocol.ErrorCode;
import com.muvrinovci.lazes.shared.protocol.dto.ErrorMessage;

/**
 * Vodi evidenciju o svim sobama na serveru.
 *
 * Sam registar soba je konkurentan jer mu pristupaju niti raznih konekcija,
 * ali se stanje pojedinacne sobe uvek menja iz njene sopstvene niti.
 */
public class RoomManager {

    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    /** Zajednicki tajmer za sve sobe; svaki zadatak se izvrsava u niti svoje sobe. */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "lazes-timer");
        thread.setDaemon(true);
        return thread;
    });

    /** Kreira novu sobu i smesta igraca u nju kao hosta. */
    public void createRoom(ServerPlayer player) {
        String code = generateUniqueCode();
        Room room = new Room(code, this, scheduler);
        rooms.put(code, room);

        Log.info("Kreirana soba %s (ukupno soba: %d)", code, rooms.size());
        room.submit(() -> room.join(player, true));
    }

    /** Smesta igraca u postojecu sobu; sve provere obavlja sama soba u svojoj niti. */
    public void joinRoom(String code, ServerPlayer player) {
        if (code == null || code.isBlank()) {
            player.send(new ErrorMessage(ErrorCode.ROOM_NOT_FOUND, "Soba nije pronadjena."));
            return;
        }

        Room room = rooms.get(code.trim().toUpperCase());
        if (room == null) {
            player.send(new ErrorMessage(ErrorCode.ROOM_NOT_FOUND, "Soba nije pronadjena."));
            return;
        }

        room.submit(() -> room.join(player, false));
    }

    void removeRoom(String code) {
        if (rooms.remove(code) != null) {
            Log.info("Soba %s je zatvorena (ukupno soba: %d)", code, rooms.size());
        }
    }

    public int roomCount() {
        return rooms.size();
    }

    public void shutdown() {
        rooms.values().forEach(Room::shutdown);
        rooms.clear();
        scheduler.shutdownNow();
    }

    private String generateUniqueCode() {
        String code;
        do {
            StringBuilder builder = new StringBuilder(GameRules.ROOM_CODE_LENGTH);
            for (int i = 0; i < GameRules.ROOM_CODE_LENGTH; i++) {
                builder.append(CODE_ALPHABET.charAt(random.nextInt(CODE_ALPHABET.length())));
            }
            code = builder.toString();
        } while (rooms.containsKey(code));

        return code;
    }
}
