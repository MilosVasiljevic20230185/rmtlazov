package com.muvrinovci.lazes.shared.protocol;


public final class MessageType {

    // Klijent -> Server 
    public static final String CREATE_ROOM = "create_room";
    public static final String JOIN_ROOM = "join_room";
    public static final String PLAYER_READY = "player_ready";
    public static final String SET_AVATAR = "set_avatar";
    public static final String START_GAME = "start_game";
    public static final String PLAY_CARDS = "play_cards";
    public static final String CALL_LIAR = "call_liar";
    public static final String DRAW_CARD = "draw_card";
    public static final String LEAVE_ROOM = "leave_room";
    public static final String RECONNECT = "reconnect";

    //  Server -> Klijent 
    public static final String ROOM_JOINED = "room_joined";
    public static final String LOBBY_STATE = "lobby_state";
    public static final String GAME_START = "game_start";
    public static final String HAND_UPDATE = "hand_update";
    public static final String TURN_UPDATE = "turn_update";
    public static final String PLAY_ANNOUNCED = "play_announced";
    public static final String CALL_RESULT = "call_result";
    public static final String CARD_DRAWN = "card_drawn";
    public static final String PLAYER_DISCONNECTED = "player_disconnected";
    public static final String PLAYER_RECONNECTED = "player_reconnected";
    public static final String GAME_SNAPSHOT = "game_snapshot";
    public static final String GAME_OVER = "game_over";
    public static final String ERROR = "error";

    private MessageType() {
    }
}
