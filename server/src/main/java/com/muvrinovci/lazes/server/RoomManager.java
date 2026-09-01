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

    /**
     * Uredjaji koji drze mesto za nekim stolom, mapirani na kod sobe.
     *
     * Zahvaljujuci ovom registru klijent pri povratku ne mora da pamti ni kod
     * sobe - dovoljan je otisak uredjaja sa koga je ispao. Upis traje od ulaska
     * u sobu do trenutka kada mesto konacno nestane.
     */
    private final Map<String, String> deviceRooms = new ConcurrentHashMap<>();

    private final int graceSeconds = GameRules.DISCONNECT_GRACE_SECONDS;
    private final int emptyRoomSeconds = GameRules.EMPTY_ROOM_SECONDS;

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

    /**
     * Vraca igraca na mesto koje ga ceka, ako takvo mesto postoji.
     *
     * Sobu pronalazi sam server preko otiska uredjaja, pa klijent ne salje kod
     * sobe. Povratak je moguc iskljucivo sa uredjaja sa koga je igrac i ispao.
     */
    public void reconnect(String deviceId, String playerName, ClientHandler handler) {
        String roomCode = deviceId == null ? null : deviceRooms.get(deviceId);
        Room room = roomCode == null ? null : rooms.get(roomCode);

        if (room == null) {
            handler.send(new ErrorMessage(ErrorCode.RECONNECT_FAILED,
                    "Nema partije u koju mozete da se vratite."));
            return;
        }

        room.submit(() -> room.tryReconnect(handler, deviceId, playerName));
    }

    int graceSeconds() {
        return graceSeconds;
    }

    int emptyRoomSeconds() {
        return emptyRoomSeconds;
    }

    void registerDevice(String deviceId, String roomCode) {
        deviceRooms.put(deviceId, roomCode);
    }

    void unregisterDevice(String deviceId, String roomCode) {
        deviceRooms.remove(deviceId, roomCode);
    }

    void removeRoom(String code) {
        if (rooms.remove(code) != null) {
            deviceRooms.values().removeIf(code::equals);
            Log.info("Soba %s je zatvorena (ukupno soba: %d)", code, rooms.size());
        }
    }

    public void shutdown() {
        rooms.values().forEach(Room::shutdown);
        rooms.clear();
        deviceRooms.clear();
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
