package com.muvrinovci.lazes.shared.protocol;

/**
 * Zajednicka osnova svih poruka protokola.
 *
 * Polje type je jedino obavezno polje i sluzi da primalac zna
 * u koju konkretnu klasu treba da deserijalizuje ostatak JSON objekta.
 * 
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
