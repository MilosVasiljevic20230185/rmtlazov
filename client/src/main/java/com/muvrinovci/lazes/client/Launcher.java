package com.muvrinovci.lazes.client;

/**
 * Pokretac klijenta iz izvrsnog jar-a.
 *
 * <p>Kada je glavna klasa naslednik {@code Application}, JVM zahteva JavaFX
 * module na module-path-u i inace odbija da se pokrene. Pokretanje kroz ovu
 * klasu zaobilazi tu proveru, pa {@code java -jar lazes-client.jar} radi i kada
 * su JavaFX biblioteke samo na classpath-u.</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        MainApp.main(args);
    }
}
