package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.ErrorCode;
import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/**
 * Server -> Klijent: greska pri obradi akcije.
 * Kodovi su definisani u ErrorCode.
 */
public class ErrorMessage extends Message {

    private String code;
    private String message;

    public ErrorMessage() {
        super(MessageType.ERROR);
    }

    public ErrorMessage(String code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
