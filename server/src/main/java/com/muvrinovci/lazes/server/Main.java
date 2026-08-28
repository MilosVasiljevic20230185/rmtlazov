package com.muvrinovci.lazes.server;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import com.muvrinovci.lazes.server.util.Log;
import com.muvrinovci.lazes.shared.GameRules;

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

    /**
     * Port se bira ovim redosledom: argument komandne linije, pa promenljiva
     * okruzenja  PORT, pa podrazumevani  GameRules#DEFAULT_POR}.
     * Promenljiva okruzenja je tu zbog hostovanja, gde port cesto zadaje platforma.
     */
    private static int parsePort(String[] args) {
        if (args.length > 0) {
            return parseOrDefault(args[0], "argument");
        }

        String fromEnv = System.getenv("PORT");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return parseOrDefault(fromEnv.trim(), "promenljiva okruzenja PORT");
        }

        return GameRules.DEFAULT_PORT;
    }

    private static int parseOrDefault(String value, String source) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Log.warn("Neispravan port '%s' (%s), koristi se podrazumevani %d",
                    value, source, GameRules.DEFAULT_PORT);
            return GameRules.DEFAULT_PORT;
        }
    }
}
