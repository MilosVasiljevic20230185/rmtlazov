package com.muvrinovci.lazes.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import com.muvrinovci.lazes.shared.protocol.JsonCodec;
import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.ProtocolException;

import javafx.application.Platform;

/**
 * Veza klijenta sa serverom.
 *
 * Citanje tece u zasebnoj niti, ali se svaka primljena poruka prosledjuje
 * kroz runLater, tako se stanje interfejsa menja iskljucivo
 * iz JavaFX niti.
 */
public class NetworkClient {

    private static final int CONNECT_TIMEOUT_MS = 5000;

    private Socket socket;
    private PrintWriter writer;
    private Thread readerThread;

    private volatile Consumer<Message> listener;
    private volatile Runnable onDisconnect;
    private volatile boolean closing;

    /** Poruke primljene sa servera prosledjuju se ovom slusaocu, u JavaFX niti. */
    public void setListener(Consumer<Message> listener) {
        this.listener = listener;
    }

    /** Poziva se kada veza pukne ili je server zatvori. */
    public void setOnDisconnect(Runnable onDisconnect) {
        this.onDisconnect = onDisconnect;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void connect(String host, int port) throws IOException {
        disconnect();
        closing = false;

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);

        writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        readerThread = new Thread(() -> readLoop(reader), "lazes-network");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop(BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                try {
                    Message message = JsonCodec.decode(line);
                    Platform.runLater(() -> {
                        Consumer<Message> current = listener;
                        if (current != null) {
                            current.accept(message);
                        }
                    });
                } catch (ProtocolException e) {
                    System.err.println("Neispravna poruka sa servera: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            // kada se veza zatvara sa nase strane.
        } finally {
            if (!closing) {
                Platform.runLater(() -> {
                    Runnable handler = onDisconnect;
                    if (handler != null) {
                        handler.run();
                    }
                });
            }
        }
    }

    public void send(Message message) {
        if (writer != null) {
            writer.println(JsonCodec.encode(message));
        }
    }

    public void disconnect() {
        closing = true;
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // Veza je vec zatvorena.
            }
            socket = null;
        }
        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }
        writer = null;
    }
}
