package com.muvrinovci.lazes.shared.protocol;

import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.muvrinovci.lazes.shared.protocol.dto.CallLiarMessage;
import com.muvrinovci.lazes.shared.protocol.dto.CallResultMessage;
import com.muvrinovci.lazes.shared.protocol.dto.CardDrawnMessage;
import com.muvrinovci.lazes.shared.protocol.dto.CreateRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.DrawCardMessage;
import com.muvrinovci.lazes.shared.protocol.dto.ErrorMessage;
import com.muvrinovci.lazes.shared.protocol.dto.GameOverMessage;
import com.muvrinovci.lazes.shared.protocol.dto.GameStartMessage;
import com.muvrinovci.lazes.shared.protocol.dto.HandUpdateMessage;
import com.muvrinovci.lazes.shared.protocol.dto.JoinRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.LeaveRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.LobbyStateMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayAnnouncedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayCardsMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayerDisconnectedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayerReadyMessage;
import com.muvrinovci.lazes.shared.protocol.dto.RoomJoinedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.SetAvatarMessage;
import com.muvrinovci.lazes.shared.protocol.dto.StartGameMessage;
import com.muvrinovci.lazes.shared.protocol.dto.TurnUpdateMessage;

/**
 * Serijalizacija i deserijalizacija poruka protokola.
 *
 * Poruke putuju kao JSON objekti, jedan po redu (newline-delimited) preko
 * TCP konekcije. Posto Gson ne zna sam da prepozna podtip, prilikom citanja se
 * prvo procita polje type, pa se preko registra odredi konkretna klasa.
 */
public final class JsonCodec {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final Map<String, Class<? extends Message>> REGISTRY = Map.ofEntries(
            // Klijent -> Server
            Map.entry(MessageType.CREATE_ROOM, CreateRoomMessage.class),
            Map.entry(MessageType.JOIN_ROOM, JoinRoomMessage.class),
            Map.entry(MessageType.PLAYER_READY, PlayerReadyMessage.class),
            Map.entry(MessageType.SET_AVATAR, SetAvatarMessage.class),
            Map.entry(MessageType.START_GAME, StartGameMessage.class),
            Map.entry(MessageType.PLAY_CARDS, PlayCardsMessage.class),
            Map.entry(MessageType.CALL_LIAR, CallLiarMessage.class),
            Map.entry(MessageType.DRAW_CARD, DrawCardMessage.class),
            Map.entry(MessageType.LEAVE_ROOM, LeaveRoomMessage.class),

            // Server -> Klijent
            Map.entry(MessageType.ROOM_JOINED, RoomJoinedMessage.class),
            Map.entry(MessageType.LOBBY_STATE, LobbyStateMessage.class),
            Map.entry(MessageType.GAME_START, GameStartMessage.class),
            Map.entry(MessageType.HAND_UPDATE, HandUpdateMessage.class),
            Map.entry(MessageType.TURN_UPDATE, TurnUpdateMessage.class),
            Map.entry(MessageType.PLAY_ANNOUNCED, PlayAnnouncedMessage.class),
            Map.entry(MessageType.CALL_RESULT, CallResultMessage.class),
            Map.entry(MessageType.CARD_DRAWN, CardDrawnMessage.class),
            Map.entry(MessageType.PLAYER_DISCONNECTED, PlayerDisconnectedMessage.class),
            Map.entry(MessageType.GAME_OVER, GameOverMessage.class),
            Map.entry(MessageType.ERROR, ErrorMessage.class));

    private JsonCodec() {
    }

    /** Pretvara poruku u jedan red JSON teksta, bez zavrsnog znaka za novi red. */
    public static String encode(Message message) {
        return GSON.toJson(message);
    }

    /**
     * Cita poruku iz JSON teksta.
     *
     * @throws ProtocolException ako tekst nije validan JSON objekat,
     *                           ako nema polje type ili ako je tip nepoznat
     */
    public static Message decode(String json) throws ProtocolException {
        JsonObject object;
        try {
            object = JsonParser.parseString(json).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException e) {
            throw new ProtocolException("Poruka nije validan JSON objekat", e);
        }

        if (!object.has("type") || !object.get("type").isJsonPrimitive()) {
            throw new ProtocolException("Poruka nema obavezno polje 'type'");
        }

        String type = object.get("type").getAsString();
        Class<? extends Message> target = REGISTRY.get(type);
        if (target == null) {
            throw new ProtocolException("Nepoznat tip poruke: " + type);
        }

        try {
            return GSON.fromJson(object, target);
        } catch (JsonSyntaxException e) {
            throw new ProtocolException("Poruka tipa '" + type + "' ima neispravan sadrzaj", e);
        }
    }
}
