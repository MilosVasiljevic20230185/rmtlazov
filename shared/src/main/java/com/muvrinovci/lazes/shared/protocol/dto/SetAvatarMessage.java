package com.muvrinovci.lazes.shared.protocol.dto;

import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;

/** Klijent -> Server: izbor boje/avatara mesta za stolom. */
public class SetAvatarMessage extends Message {

    private String avatar;

    public SetAvatarMessage() {
        super(MessageType.SET_AVATAR);
    }

    public SetAvatarMessage(String avatar) {
        this();
        this.avatar = avatar;
    }

    public String getAvatar() {
        return avatar;
    }
}
