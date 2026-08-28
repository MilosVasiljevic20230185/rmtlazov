package com.muvrinovci.lazes.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.muvrinovci.lazes.server.game.GameEngine;
import com.muvrinovci.lazes.server.game.GameException;
import com.muvrinovci.lazes.server.util.Log;
import com.muvrinovci.lazes.shared.GameRules;
import com.muvrinovci.lazes.shared.model.Card;
import com.muvrinovci.lazes.shared.protocol.ErrorCode;
import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;
import com.muvrinovci.lazes.shared.protocol.dto.CallResultMessage;
import com.muvrinovci.lazes.shared.protocol.dto.CardDrawnMessage;
import com.muvrinovci.lazes.shared.protocol.dto.ErrorMessage;
import com.muvrinovci.lazes.shared.protocol.dto.GameOverMessage;
import com.muvrinovci.lazes.shared.protocol.dto.GameStartMessage;
import com.muvrinovci.lazes.shared.protocol.dto.HandUpdateMessage;
import com.muvrinovci.lazes.shared.protocol.dto.LobbyStateMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayAnnouncedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayCardsMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayerDisconnectedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayerReadyMessage;
import com.muvrinovci.lazes.shared.protocol.dto.RoomJoinedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.SetAvatarMessage;
import com.muvrinovci.lazes.shared.protocol.dto.TurnUpdateMessage;

/**
 * Jedna soba, odnosno jedan sto za igru.
 *
 * Sve stanje sobe menja se iskljucivo iz njene jedne niti executor.
 * Svaka poruka i svaki istekli tajmer se prosledjuju kroz submit(Runnable),
 * pa se akcije obradjuju strogo sekvencijalno, zbog toga nema zakljucavanja i
 * nije moguce da dve istovremene call_liar poruke obe prodju.
 */
public class Room {

    /** Boje mesta za stolom koje igraci mogu birati u lobby-ju. */
    private static final List<String> AVATARS = List.of("blue", "red", "green", "gold");

    private final String code;
    private final RoomManager roomManager;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    });
    private final Random random = new Random();

    private final List<ServerPlayer> players = new ArrayList<>();
    private final Map<String, String> knownNames = new HashMap<>();

    private RoomState state = RoomState.LOBBY;
    private String hostId;
    private GameEngine engine;

    /**
     * Stiti od zakasnelih tajmera: svaki zakazani zadatak pamti vrednost koju je
     * imao u trenutku zakazivanja i odustaje ako se stanje u medjuvremenu promenilo.
     */
    private long actionToken;
    private ScheduledFuture<?> pendingTimer;

    public Room(String code, RoomManager roomManager, ScheduledExecutorService scheduler) {
        this.code = code;
        this.roomManager = roomManager;
        this.scheduler = scheduler;
    }

    public String getCode() {
        return code;
    }

    /** Izvrsava zadatak u niti ove sobe. */
    public Future<?> submit(Runnable task) {
        return executor.submit(() -> {
            try {
                task.run();
            } catch (RuntimeException e) {
                Log.error("Soba %s - greska pri obradi: %s", code, e);
            }
        });
    }

    public void shutdown() {
        cancelTimer();
        executor.shutdownNow();
    }


    // Ulazak i izlazak


    /** Pokusaj ulaska u sobu; odgovor (potvrda ili greska) salje se samom igracu. */
    void join(ServerPlayer player, boolean asHost) {
        if (state != RoomState.LOBBY) {
            player.send(new ErrorMessage(ErrorCode.GAME_IN_PROGRESS, "Igra je u toku, ne moze se prikljuciti."));
            return;
        }
        if (players.size() >= GameRules.MAX_PLAYERS) {
            player.send(new ErrorMessage(ErrorCode.ROOM_FULL, "Soba je puna."));
            return;
        }

        players.add(player);
        knownNames.put(player.getId(), player.getName());
        player.setRoom(this);
        player.setAvatar(firstFreeAvatar());

        if (asHost || hostId == null) {
            hostId = player.getId();
        }

        Log.info("Soba %s - %s se pridruzio (%d/%d)", code, player, players.size(), GameRules.MAX_PLAYERS);

        player.send(new RoomJoinedMessage(code, player.getId(), player.getId().equals(hostId)));
        broadcastLobbyState();
    }

    /** Igrac je napustio sobu ili mu je pukla konekcija. */
    void leave(ServerPlayer player, String reason) {
        if (!players.remove(player)) {
            return;
        }
        player.setRoom(null);
        Log.info("Soba %s - %s je napustio partiju (%s)", code, player, reason);

        if (players.isEmpty()) {
            roomManager.removeRoom(code);
            shutdown();
            return;
        }

        if (player.getId().equals(hostId)) {
            hostId = players.get(0).getId();
            Log.info("Soba %s - novi host je %s", code, players.get(0));
        }

        if (state == RoomState.IN_GAME) {
            broadcast(new PlayerDisconnectedMessage(player.getId(), player.getName(), reason));
            engine.removePlayer(player.getId());

            if (engine.isFinished()) {
                finishGame();
            } else {
                broadcastTurnUpdate();
                startTurnTimer();
            }
        } else {
            broadcastLobbyState();
        }
    }


    // Obrada poruka


    void handle(ServerPlayer player, Message message) {
        if (!players.contains(player)) {
            player.send(new ErrorMessage(ErrorCode.NOT_IN_ROOM, "Niste u sobi."));
            return;
        }

        try {
            switch (message.getType()) {
                case MessageType.PLAYER_READY -> onReady(player, (PlayerReadyMessage) message);
                case MessageType.SET_AVATAR -> onSetAvatar(player, (SetAvatarMessage) message);
                case MessageType.START_GAME -> onStartGame(player);
                case MessageType.PLAY_CARDS -> onPlayCards(player, (PlayCardsMessage) message);
                case MessageType.CALL_LIAR -> onCallLiar(player);
                case MessageType.DRAW_CARD -> onDrawCard(player, false);
                case MessageType.LEAVE_ROOM -> leave(player, "left");
                default -> player.send(new ErrorMessage(ErrorCode.INVALID_ACTION,
                        "Nepodrzana akcija: " + message.getType()));
            }
        } catch (GameException e) {
            player.send(new ErrorMessage(e.getCode(), e.getMessage()));
            Log.warn("Soba %s - odbijena akcija %s od %s: %s", code, message.getType(), player, e.getMessage());
        }
    }

    private void onReady(ServerPlayer player, PlayerReadyMessage message) {
        requireLobby();
        player.setReady(message.isReady());
        broadcastLobbyState();
    }

    private void onSetAvatar(ServerPlayer player, SetAvatarMessage message) {
        requireLobby();

        String wanted = message.getAvatar();
        if (!AVATARS.contains(wanted)) {
            throw new GameException(ErrorCode.INVALID_ACTION, "Nepoznat avatar.");
        }
        boolean taken = players.stream()
                .anyMatch(other -> other != player && wanted.equals(other.getAvatar()));
        if (taken) {
            throw new GameException(ErrorCode.INVALID_ACTION, "Taj avatar je vec zauzet.");
        }

        player.setAvatar(wanted);
        broadcastLobbyState();
    }

    private void onStartGame(ServerPlayer player) {
        requireLobby();

        if (!player.getId().equals(hostId)) {
            throw new GameException(ErrorCode.NOT_HOST, "Samo host moze da pokrene partiju.");
        }
        if (players.size() < GameRules.MIN_PLAYERS) {
            throw new GameException(ErrorCode.NOT_ENOUGH_PLAYERS,
                    "Potrebna su najmanje " + GameRules.MIN_PLAYERS + " igraca.");
        }
        if (!players.stream().allMatch(ServerPlayer::isReady)) {
            throw new GameException(ErrorCode.PLAYERS_NOT_READY, "Nisu svi igraci spremni.");
        }

        startGame();
    }

    private void onPlayCards(ServerPlayer player, PlayCardsMessage message) {
        requireInGame();

        GameEngine.PlayResult result =
                engine.playCards(player.getId(), message.getCardIds(), message.getDeclaredValue());

        Log.info("Soba %s - %s baca %d karata i najavljuje vrednost %d",
                code, player, result.declaredCount(), result.declaredValue());

        sendHand(player);
        // Prvo novo stanje stola, pa tek onda najava, klijentu je tako poslednja
        // primljena poruka uvek ona koja odredjuje fazu u kojoj se nalazi.
        broadcastTurnUpdate();
        broadcast(new PlayAnnouncedMessage(player.getId(), player.getName(),
                result.declaredCount(), result.declaredValue(), GameRules.CALL_WINDOW_MS));
        startCallWindow();
    }

    private void onCallLiar(ServerPlayer player) {
        requireInGame();

        GameEngine.CallResolution result = engine.callLiar(player.getId());
        cancelTimer();

        Log.info("Soba %s - %s proziva %s: %s", code, player,
                knownNames.get(result.accusedId()), result.wasLying() ? "LAGAO" : "ISTINA");

        broadcast(new CallResultMessage(
                result.callerId(), knownNames.get(result.callerId()),
                result.accusedId(), knownNames.get(result.accusedId()),
                result.declaredValue(), result.wasLying(),
                result.revealedCards().stream().map(Card::id).toList(),
                result.collectorId(), result.collectedCount(), result.nextPlayerId()));

        sendHandTo(result.collectorId());

        if (result.winnerId() != null) {
            finishGame();
        } else {
            broadcastTurnUpdate();
            startTurnTimer();
        }
    }

    private void onDrawCard(ServerPlayer player, boolean automatic) {
        requireInGame();

        engine.drawCard(player.getId());
        Log.info("Soba %s - %s vuce kartu%s", code, player, automatic ? " (automatski)" : "");

        sendHand(player);
        broadcast(new CardDrawnMessage(player.getId(), player.getName(), automatic));
        broadcastTurnUpdate();
        startTurnTimer();
    }


    // Tok partije


    private void startGame() {
        state = RoomState.IN_GAME;
        engine = new GameEngine(players.stream().map(ServerPlayer::getId).toList(), random);

        Log.info("Soba %s - partija pocinje, %d igraca, prvi na potezu: %s",
                code, players.size(), knownNames.get(engine.currentPlayerId()));

        broadcast(new GameStartMessage(GameRules.START_COUNTDOWN_SECONDS,
                engine.currentPlayerId(), GameRules.INITIAL_HAND_SIZE));

        // Klijenti prikazuju odbrojavanje 3 - 2 - 1, pa tek onda krece prvi potez.
        long token = ++actionToken;
        pendingTimer = scheduler.schedule(() -> submit(() -> beginFirstTurn(token)),
                GameRules.START_COUNTDOWN_SECONDS, TimeUnit.SECONDS);
    }

    private void beginFirstTurn(long token) {
        if (token != actionToken || state != RoomState.IN_GAME) {
            return;
        }
        players.forEach(this::sendHand);
        broadcastTurnUpdate();
        startTurnTimer();
    }

    /** Istekao je tajmer poteza server igra umesto igraca koji nije reagovao. */
    private void onTurnTimeout(long token) {
        if (token != actionToken || state != RoomState.IN_GAME) {
            return;
        }

        ServerPlayer current = findPlayer(engine.currentPlayerId());
        if (current == null) {
            return;
        }

        try {
            if (engine.drawPileCount() > 0) {
                onDrawCard(current, true);
            } else {
                autoPlayLowestCard(current);
            }
        } catch (GameException e) {
            Log.error("Soba %s - automatska akcija nije uspela: %s", code, e.getMessage());
        }
    }

    /** Kada je centralni spil prazan, igrac koji je istekao baca prvu kartu iz ruke. */
    private void autoPlayLowestCard(ServerPlayer player) {
        List<Card> hand = engine.hand(player.getId());
        if (hand.isEmpty()) {
            return;
        }

        Card card = hand.get(0);
        int declared = engine.tableValue() == GameEngine.OPEN_ROUND ? card.value() : engine.tableValue();
        onPlayCards(player, new PlayCardsMessage(List.of(card.id()), declared));
    }

    /** Istekao je prozor za prozivanje, a niko nije prozvao. */
    private void onCallWindowExpired(long token) {
        if (token != actionToken || state != RoomState.IN_GAME) {
            return;
        }

        GameEngine.WindowCloseResult result = engine.closeCallWindow();

        if (result.winnerId() != null) {
            finishGame();
        } else {
            broadcastTurnUpdate();
            startTurnTimer();
        }
    }

    private void finishGame() {
        state = RoomState.FINISHED;
        cancelTimer();

        String winnerId = engine.winnerId();
        Log.info("Soba %s - partiju je pobedio %s", code, knownNames.get(winnerId));

        List<Map.Entry<String, Integer>> standings = new ArrayList<>(engine.remainingCards().entrySet());
        standings.sort(Comparator.comparingInt(
                entry -> entry.getKey().equals(winnerId) ? -1 : entry.getValue()));

        List<GameOverMessage.RankingEntry> ranking = new ArrayList<>(standings.size());
        for (int i = 0; i < standings.size(); i++) {
            Map.Entry<String, Integer> entry = standings.get(i);
            ranking.add(new GameOverMessage.RankingEntry(
                    i + 1, entry.getKey(), knownNames.get(entry.getKey()), entry.getValue()));
        }

        broadcast(new GameOverMessage(winnerId, knownNames.get(winnerId), ranking));
    }


    // Tajmeri


    private void startTurnTimer() {
        cancelTimer();
        long token = ++actionToken;
        pendingTimer = scheduler.schedule(() -> submit(() -> onTurnTimeout(token)),
                GameRules.TURN_SECONDS, TimeUnit.SECONDS);
    }

    private void startCallWindow() {
        cancelTimer();
        long token = ++actionToken;
        pendingTimer = scheduler.schedule(() -> submit(() -> onCallWindowExpired(token)),
                GameRules.CALL_WINDOW_MS, TimeUnit.MILLISECONDS);
    }

    private void cancelTimer() {
        if (pendingTimer != null) {
            pendingTimer.cancel(false);
            pendingTimer = null;
        }
    }


    // Slanje stanja


    private void broadcast(Message message) {
        players.forEach(player -> player.send(message));
    }

    private void broadcastLobbyState() {
        List<LobbyStateMessage.LobbyPlayer> lobbyPlayers = players.stream()
                .map(player -> new LobbyStateMessage.LobbyPlayer(
                        player.getId(), player.getName(), player.isReady(), player.getAvatar()))
                .toList();

        boolean canStart = players.size() >= GameRules.MIN_PLAYERS
                && players.stream().allMatch(ServerPlayer::isReady);

        broadcast(new LobbyStateMessage(lobbyPlayers, hostId, canStart));
    }

    private void broadcastTurnUpdate() {
        List<TurnUpdateMessage.PlayerInfo> infos = new ArrayList<>();
        for (String playerId : engine.playerIds()) {
            ServerPlayer player = findPlayer(playerId);
            infos.add(new TurnUpdateMessage.PlayerInfo(
                    playerId,
                    knownNames.get(playerId),
                    player == null ? null : player.getAvatar(),
                    engine.cardCount(playerId)));
        }

        broadcast(new TurnUpdateMessage(engine.currentPlayerId(), engine.tableValue(),
                engine.centerCount(), engine.drawPileCount(), GameRules.TURN_SECONDS, infos));
    }

    private void sendHand(ServerPlayer player) {
        player.send(new HandUpdateMessage(engine.hand(player.getId()).stream().map(Card::id).toList()));
    }

    private void sendHandTo(String playerId) {
        ServerPlayer player = findPlayer(playerId);
        if (player != null) {
            sendHand(player);
        }
    }


    // Pomocne metode


    private ServerPlayer findPlayer(String playerId) {
        return players.stream().filter(p -> p.getId().equals(playerId)).findFirst().orElse(null);
    }

    private String firstFreeAvatar() {
        Map<String, Boolean> taken = new LinkedHashMap<>();
        AVATARS.forEach(avatar -> taken.put(avatar, false));
        players.forEach(player -> {
            if (player.getAvatar() != null) {
                taken.put(player.getAvatar(), true);
            }
        });
        return taken.entrySet().stream()
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(AVATARS.get(0));
    }

    private void requireLobby() {
        if (state != RoomState.LOBBY) {
            throw new GameException(ErrorCode.INVALID_ACTION, "Partija je vec pocela.");
        }
    }

    private void requireInGame() {
        if (state != RoomState.IN_GAME) {
            throw new GameException(ErrorCode.INVALID_ACTION, "Partija nije u toku.");
        }
    }
}
