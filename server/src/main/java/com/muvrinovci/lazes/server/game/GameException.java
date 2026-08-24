package com.muvrinovci.lazes.server.game;

/**
 * Akcija igraca je odbijena jer krsi pravila igre.
 * {@code code} odgovara konstantama iz
 * {@link com.muvrinovci.lazes.shared.protocol.ErrorCode} i salje se klijentu.
 */
public class GameException extends RuntimeException {

    private final String code;

    public GameException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
