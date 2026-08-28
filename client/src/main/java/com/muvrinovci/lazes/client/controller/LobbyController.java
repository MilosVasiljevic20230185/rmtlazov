package com.muvrinovci.lazes.client.controller;

import java.util.List;

import com.muvrinovci.lazes.client.ViewNavigator;
import com.muvrinovci.lazes.client.view.Avatars;
import com.muvrinovci.lazes.shared.GameRules;
import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;
import com.muvrinovci.lazes.shared.protocol.dto.ErrorMessage;
import com.muvrinovci.lazes.shared.protocol.dto.LeaveRoomMessage;
import com.muvrinovci.lazes.shared.protocol.dto.LobbyStateMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayerReadyMessage;
import com.muvrinovci.lazes.shared.protocol.dto.SetAvatarMessage;
import com.muvrinovci.lazes.shared.protocol.dto.StartGameMessage;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/** Cekaonica: lista igraca sa statusom spremnosti, izbor boje i pokretanje partije. */
public class LobbyController implements ScreenController {

    @FXML private Label roomCodeLabel;
    @FXML private Label playersHeading;
    @FXML private Label statusLabel;
    @FXML private VBox playerList;
    @FXML private HBox avatarBox;
    @FXML private Button copyButton;
    @FXML private Button readyButton;
    @FXML private Button startButton;
    @FXML private Button leaveButton;

    private ViewNavigator navigator;
    private boolean ready;
    private String myAvatar;

    @Override
    public void init(ViewNavigator navigator) {
        this.navigator = navigator;

        roomCodeLabel.setText(navigator.getSession().getRoomCode());
        startButton.setVisible(navigator.getSession().isHost());
        startButton.setManaged(navigator.getSession().isHost());
        statusLabel.setText("Potrebna su najmanje " + GameRules.MIN_PLAYERS
                + " igrača, a najviše " + GameRules.MAX_PLAYERS + ".");
    }

    // Akcije korisnika

    @FXML
    private void handleCopyCode() {
        ClipboardContent content = new ClipboardContent();
        content.putString(navigator.getSession().getRoomCode());
        Clipboard.getSystemClipboard().setContent(content);

    }

    @FXML
    private void handleToggleReady() {
        ready = !ready;
        navigator.getNetwork().send(new PlayerReadyMessage(ready));
        readyButton.setText(ready ? "Nisam spreman" : "Spreman sam");
    }

    @FXML
    private void handleStart() {
        navigator.getNetwork().send(new StartGameMessage());
    }

    @FXML
    private void handleLeave() {
        navigator.getNetwork().send(new LeaveRoomMessage());
        navigator.getNetwork().disconnect();
        navigator.getSession().clearRoom();
        navigator.showMainMenu();
    }

    // Poruke sa servera

    @Override
    public void onMessage(Message message) {
        switch (message.getType()) {
            case MessageType.LOBBY_STATE -> renderLobby((LobbyStateMessage) message);
            case MessageType.GAME_START -> navigator.showTable();
            case MessageType.ERROR -> navigator.showError(((ErrorMessage) message).getMessage());
            default -> {
                // Ostale poruke stizu tek kada partija pocne.
            }
        }
    }

    private void renderLobby(LobbyStateMessage state) {
        List<LobbyStateMessage.LobbyPlayer> players = state.getPlayers();

        playersHeading.setText("Igrači za stolom (" + players.size() + "/" + GameRules.MAX_PLAYERS + ")");
        playerList.getChildren().clear();

        for (LobbyStateMessage.LobbyPlayer player : players) {
            playerList.getChildren().add(buildPlayerRow(player, state.getHostId()));

            if (navigator.getSession().isMe(player.getId())) {
                ready = player.isReady();
                myAvatar = player.getAvatar();
                readyButton.setText(ready ? "Nisam spreman" : "Spreman sam");
            }
        }

        // Host se mogao promeniti ako je prethodni napustio sobu.
        boolean amHost = navigator.getSession().isMe(state.getHostId());
        navigator.getSession().setHost(amHost);
        startButton.setVisible(amHost);
        startButton.setManaged(amHost);
        startButton.setDisable(!state.isCanStart());

        renderAvatars(players);

        if (players.size() < GameRules.MIN_PLAYERS) {
            statusLabel.setText("Čeka se još igrača...");
        } else if (!state.isCanStart()) {
            statusLabel.setText("Nisu svi igrači spremni.");
        } else {
            statusLabel.setText(amHost
                    ? "Svi su spremni — možeš pokrenuti partiju."
                    : "Svi su spremni — čeka se da host pokrene partiju.");
        }
    }

    private HBox buildPlayerRow(LobbyStateMessage.LobbyPlayer player, String hostId) {
        Label dot = new Label();
        dot.getStyleClass().add("avatar-dot");
        dot.setStyle("-fx-background-color: " + Avatars.colorOf(player.getAvatar()) + ";");

        String name = player.getName();
        if (player.getId().equals(hostId)) {
            name += "  ·  host";
        }
        if (navigator.getSession().isMe(player.getId())) {
            name += "  (ti)";
        }

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("seat-name");

        Label status = new Label(player.isReady() ? "SPREMAN" : "čeka");
        status.getStyleClass().add(player.isReady() ? "badge-ready" : "badge-waiting");

        HBox row = new HBox(12, dot, nameLabel, new Region(), status);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("seat");
        HBox.setHgrow(row.getChildren().get(2), javafx.scene.layout.Priority.ALWAYS);

        return row;
    }

    private void renderAvatars(List<LobbyStateMessage.LobbyPlayer> players) {
        avatarBox.getChildren().clear();

        for (String avatar : Avatars.ALL) {
            boolean takenByOther = players.stream()
                    .anyMatch(player -> avatar.equals(player.getAvatar())
                            && !navigator.getSession().isMe(player.getId()));

            Label swatch = new Label();
            swatch.getStyleClass().add("avatar-swatch");
            swatch.setStyle("-fx-background-color: " + Avatars.colorOf(avatar) + ";");
            swatch.setTooltip(new javafx.scene.control.Tooltip(Avatars.nameOf(avatar)));

            if (avatar.equals(myAvatar)) {
                swatch.getStyleClass().add("avatar-swatch-selected");
            }
            if (takenByOther) {
                swatch.getStyleClass().add("avatar-swatch-taken");
            } else {
                swatch.setOnMouseClicked(event -> navigator.getNetwork().send(new SetAvatarMessage(avatar)));
            }

            avatarBox.getChildren().add(swatch);
        }
    }
}
