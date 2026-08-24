package com.muvrinovci.lazes.server.game;

/** Faza u kojoj se partija trenutno nalazi. */
public enum GamePhase {

    /** Ceka se akcija igraca na potezu: bacanje karata ili vucenje. */
    TURN,

    /** Potez je odigran; ostali igraci imaju kratak prozor da prozovu laz. */
    CALL_WINDOW,

    /** Partija je zavrsena, pobednik je poznat. */
    FINISHED
}
