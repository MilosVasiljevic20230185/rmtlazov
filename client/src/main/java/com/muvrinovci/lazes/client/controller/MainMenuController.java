package com.muvrinovci.lazes.client.controller;

import java.io.IOException;

import com.muvrinovci.lazes.client.DeviceId;
import com.muvrinovci.lazes.client.ViewNavigator;
import com.muvrinovci.lazes.shared.GameRules;
import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;
import com.muvrinovci.lazes.shared.protocol.dto.CreateRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.ErrorMessage;
import com.muvrinovci.lazes.shared.protocol.dto.GameSnapshotMessage;
import com.muvrinovci.lazes.shared.protocol.dto.JoinRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.LobbyStateMessage;
import com.muvrinovci.lazes.shared.protocol.dto.ReconnectMessage;
import com.muvrinovci.lazes.shared.protocol.dto.RoomJoinedMessage;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

/** Glavni meni: unos imena, adrese servera i kreiranje ili pridruzivanje sobi. */
public class MainMenuController implements ScreenController {

    @FXML private TextField nameField;
    @FXML private TextField hostField;
    @FXML private TextField portField;
    @FXML private TextField codeField;
    @FXML private Button createButton;
    @FXML private Button joinButton;
    @FXML private Button resumeButton;
    @FXML private Label statusLabel;

    private ViewNavigator navigator;

    /** true dok cekamo odgovor na pokusaj povratka u prekinutu partiju. */
    private boolean resuming;

    @Override
    public void init(ViewNavigator navigator) {
        this.navigator = navigator;

        String previousName = navigator.getSession().getPlayerName();
        if (previousName != null) {
            nameField.setText(previousName);
        }
        codeField.textProperty().addListener((observable, oldValue, newValue) -> {
            String upper = newValue.toUpperCase();
            if (!upper.equals(newValue)) {
                codeField.setText(upper);
            }
        });
    }

    @FXML
    private void handleCreateRoom() {
        if (!connect()) {
            return;
        }
        navigator.getNetwork().send(new CreateRoomMessage(nameField.getText().trim(), DeviceId.get()));
        setBusy(true);
    }

    @FXML
    private void handleJoinRoom() {
        String code = codeField.getText().trim();
        if (code.isEmpty()) {
            showStatus("Unesite kod sobe.");
            return;
        }
        if (!connect()) {
            return;
        }
        navigator.getNetwork().send(new JoinRoomMessage(code, nameField.getText().trim(), DeviceId.get()));
        setBusy(true);
    }

    /**
     * Povratak na mesto koje se cuva posle prekida veze.
     *
     * Kod sobe se ne unosi - server prepoznaje uredjaj po otisku i sam zna gde
     * je igrac bio. Povratak radi iskljucivo sa uredjaja sa koga je i ispao.
     */
    @FXML
    private void handleResume() {
        if (!connect()) {
            return;
        }
        resuming = true;
        setBusy(true);
        navigator.getNetwork().send(new ReconnectMessage(DeviceId.get(), nameField.getText().trim()));
    }

    /** Povezuje se na server; vraca {@code false} ako unos nije ispravan ili server ne odgovara. */
    private boolean connect() {
        hideStatus();

        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showStatus("Unesite svoje ime.");
            return false;
        }

        String host = hostField.getText().trim();
        if (host.isEmpty()) {
            showStatus("Unesite adresu servera.");
            return false;
        }

        int port;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException e) {
            showStatus("Port mora biti broj (podrazumevano " + GameRules.DEFAULT_PORT + ").");
            return false;
        }

        navigator.getSession().setPlayerName(name);

        try {
            navigator.getNetwork().connect(host, port);
            return true;
        } catch (IOException e) {
            showStatus("Nije moguće povezati se na " + host + ":" + port + ".");
            return false;
        }
    }

    @Override
    public void onMessage(Message message) {
        switch (message.getType()) {
            case MessageType.ROOM_JOINED -> {
                RoomJoinedMessage joined = (RoomJoinedMessage) message;
                navigator.getSession().setPlayerId(joined.getPlayerId());
                navigator.getSession().setRoomCode(joined.getRoomCode());
                navigator.getSession().setHost(joined.isHost());

                // Pri povratku tek naredna poruka kaze da li se ide na sto ili u lobby.
                if (!resuming) {
                    navigator.showLobby();
                }
            }
            case MessageType.GAME_SNAPSHOT -> {
                navigator.getSession().setPendingSnapshot((GameSnapshotMessage) message);
                resuming = false;
                navigator.showTable();
            }
            case MessageType.LOBBY_STATE -> {
                // Partija se zavrsila dok nas nije bilo; vracamo se u cekaonicu.
                if (resuming) {
                    resuming = false;
                    navigator.getSession().setPendingLobbyState((LobbyStateMessage) message);
                    navigator.showLobby();
                }
            }
            case MessageType.ERROR -> {
                resuming = false;
                setBusy(false);
                navigator.getNetwork().disconnect();
                showStatus(((ErrorMessage) message).getMessage());
            }
            default -> {
                // Ostale poruke u glavnom meniju nisu od znacaja.
            }
        }
    }

    private void setBusy(boolean busy) {
        createButton.setDisable(busy);
        joinButton.setDisable(busy);
        resumeButton.setDisable(busy);
    }

    private void showStatus(String text) {
        statusLabel.setText(text);
        statusLabel.setVisible(true);
        statusLabel.setManaged(true);
    }

    private void hideStatus() {
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);
    }
}
