package com.muvrinovci.lazes.server;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import com.muvrinovci.lazes.server.util.Log;
import com.muvrinovci.lazes.shared.GameRules;

/**
 * Ulazna tacka game servera.
 *
 * <p>Pokretanje: {@code mvn -pl server exec:java} ili
 * {@code java -jar lazes-server.jar [port]}.</p>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws InterruptedException {
        int port = parsePort(args);
        GameServer server = new GameServer();

        try {
            server.start(port);
        } catch (IOException e) {
            Log.error("Server ne moze da slusa na portu %d: %s", port, e.getMessage());
            System.exit(1);
            return;
        }

        Log.info("Ceka se povezivanje igraca (%d-%d po sobi)...",
                GameRules.MIN_PLAYERS, GameRules.MAX_PLAYERS);

        CountDownLatch running = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Log.info("Gasenje servera...");
            server.stop();
            running.countDown();
        }));

        running.await();
    }

    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return GameRules.DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            Log.warn("Neispravan port '%s', koristi se podrazumevani %d", args[0], GameRules.DEFAULT_PORT);
            return GameRules.DEFAULT_PORT;
        }
    }
}
