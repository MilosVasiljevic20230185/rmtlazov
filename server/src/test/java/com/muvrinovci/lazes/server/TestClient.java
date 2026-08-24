package com.muvrinovci.lazes.server;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import com.muvrinovci.lazes.shared.protocol.JsonCodec;
import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.ProtocolException;

/**
 * Minimalan klijent za integracione testove: salje poruke i ceka odgovor
 * odredjenog tipa, preskacuci poruke koje test u tom trenutku ne zanimaju.
 */
class TestClient implements Closeable {

    private static final int READ_TIMEOUT_MS = 10_000;

    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    TestClient(int port) throws IOException {
        socket = new Socket("localhost", port);
        socket.setSoTimeout(READ_TIMEOUT_MS);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    void send(Message message) {
        writer.println(JsonCodec.encode(message));
    }

    /** Cita poruke dok ne naidje na trazeni tip. */
    <T extends Message> T await(Class<T> type) throws IOException {
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                throw new IOException("Konekcija je zatvorena dok se cekala poruka " + type.getSimpleName());
            }
            try {
                Message message = JsonCodec.decode(line);
                if (type.isInstance(message)) {
                    return type.cast(message);
                }
            } catch (ProtocolException e) {
                throw new IOException("Server je poslao neispravnu poruku: " + line, e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
