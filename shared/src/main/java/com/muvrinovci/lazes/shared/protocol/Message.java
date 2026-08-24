package com.muvrinovci.lazes.shared.protocol;

/**
 * Zajednicka osnova svih poruka protokola.
 *
 * <p>Polje {@code type} je jedino obavezno polje i sluzi da primalac zna
 * u koju konkretnu klasu treba da deserijalizuje ostatak JSON objekta.
 * Vidi {@link JsonCodec}.</p>
 */
public abstract class Message {

    private String type;

    protected Message(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
