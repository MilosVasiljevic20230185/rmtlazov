package com.muvrinovci.lazes.shared.protocol;

/** Signalizira da primljeni tekst nije ispravna poruka protokola. */
public class ProtocolException extends Exception {

    public ProtocolException(String message) {
        super(message);
    }

    public ProtocolException(String message, Throwable cause) {
        super(message, cause);
    }
}
