package com.muvrinovci.lazes.shared.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.muvrinovci.lazes.shared.protocol.dto.CallLiarMessage;
import com.muvrinovci.lazes.shared.protocol.dto.CallResultMessage;
import com.muvrinovci.lazes.shared.protocol.dto.JoinRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayCardsMessage;

class JsonCodecTest {

    @Test
    @DisplayName("Poruka sa poljima prezivi enkodiranje i dekodiranje")
    void roundTripWithFields() throws ProtocolException {
        PlayCardsMessage original = new PlayCardsMessage(List.of("7H1", "7S2"), 7);

        Message decoded = JsonCodec.decode(JsonCodec.encode(original));

        PlayCardsMessage result = assertInstanceOf(PlayCardsMessage.class, decoded);
        assertEquals(MessageType.PLAY_CARDS, result.getType());
        assertEquals(List.of("7H1", "7S2"), result.getCardIds());
        assertEquals(7, result.getDeclaredValue());
    }

    @Test
    @DisplayName("Poruka bez polja zadrzava svoj tip")
    void roundTripWithoutFields() throws ProtocolException {
        Message decoded = JsonCodec.decode(JsonCodec.encode(new CallLiarMessage()));

        assertInstanceOf(CallLiarMessage.class, decoded);
        assertEquals(MessageType.CALL_LIAR, decoded.getType());
    }

    @Test
    @DisplayName("Ugnjezdene liste se ispravno prenose")
    void nestedCollections() throws ProtocolException {
        CallResultMessage original = new CallResultMessage(
                "uuid-3", "Zika", "uuid-2", "Mika", 7, true,
                List.of("7H1", "2C2"), "uuid-2", 2, "uuid-3");

        CallResultMessage result =
                assertInstanceOf(CallResultMessage.class, JsonCodec.decode(JsonCodec.encode(original)));

        assertTrue(result.isWasLying());
        assertEquals(List.of("7H1", "2C2"), result.getRevealedCards());
        assertEquals("uuid-2", result.getCardsCollectedBy());
        assertEquals(2, result.getCollectedCount());
    }

    @Test
    @DisplayName("Enkodirana poruka je jedan red, bez znaka za novi red")
    void encodedMessageIsSingleLine() {
        String json = JsonCodec.encode(new JoinRoomMessage("XK7Q2P", "Pera"));

        assertFalse(json.contains("\n"));
        assertTrue(json.contains("\"type\":\"join_room\""));
        assertTrue(json.contains("\"roomCode\":\"XK7Q2P\""));
    }

    @Test
    @DisplayName("Neispravan ili nepoznat sadrzaj podize ProtocolException")
    void malformedInput() {
        assertThrows(ProtocolException.class, () -> JsonCodec.decode("ovo nije json"));
        assertThrows(ProtocolException.class, () -> JsonCodec.decode("{}"));
        assertThrows(ProtocolException.class, () -> JsonCodec.decode("{\"type\":\"nepostojeci\"}"));
        assertThrows(ProtocolException.class, () -> JsonCodec.decode("[1,2,3]"));
    }
}
