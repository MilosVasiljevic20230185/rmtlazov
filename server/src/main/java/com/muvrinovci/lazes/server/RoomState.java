package com.muvrinovci.lazes.server;

/** Stanje sobe. */
public enum RoomState {

    /** Igraci se okupljaju i oznacavaju spremnost. */
    LOBBY,

    /** Partija je u toku. */
    IN_GAME,

    /** Partija je zavrsena, pobednik je poznat. */
    FINISHED
}
