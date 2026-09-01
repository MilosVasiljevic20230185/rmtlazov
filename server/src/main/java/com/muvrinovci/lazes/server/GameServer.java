package com.muvrinovci.lazes.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.muvrinovci.lazes.server.util.Log;

/**
 * Game server: slusa na portu i za svaku konekciju pokrece  ClientHandler.
 *
 * Izdvojeno iz  Main da bi server mogao da se pokrene i zaustavi
 */
public class GameServer {

    private final RoomManager roomManager = new RoomManager();

    private ServerSocket serverSocket;
    private ExecutorService connections;
    private Thread acceptThread;

    /**
     * Pokrece server i odmah se vraca, prihvatanje konekcija tece u posebnoj niti.
     *
     * port na kome server slusa,  0 znaci da ga bira operativni sistem
     */
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        connections = Executors.newCachedThreadPool(runnable -> {
            Thread thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });

        Log.info("Lazes server je pokrenut i slusa na portu %d", getPort());

        acceptThread = new Thread(this::acceptLoop, "lazes-accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void acceptLoop() {
        while (!serverSocket.isClosed()) {
            try {
                Socket socket = serverSocket.accept();
                connections.submit(new ClientHandler(socket, roomManager));
            } catch (IOException e) {
                if (!serverSocket.isClosed()) {
                    Log.error("Neuspesno prihvatanje konekcije: %s", e.getMessage());
                }
            }
        }
    }

    /** Port na kome server stvarno slusa. */
    public int getPort() {
        return serverSocket == null ? -1 : serverSocket.getLocalPort();
    }

    public void stop() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
            // Server se ionako gasi.
        }
        if (connections != null) {
            connections.shutdownNow();
        }
        roomManager.shutdown();
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
    }
}
