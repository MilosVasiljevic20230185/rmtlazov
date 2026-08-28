package com.muvrinovci.lazes.client;

import java.io.IOException;
import java.io.UncheckedIOException;

import com.muvrinovci.lazes.client.controller.ScreenController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

/**
 * Prebacivanje izmedju ekrana i povezivanje trenutnog ekrana sa mreznim slojem.
 *
 * U svakom trenutku samo jedan kontroler slusa poruke sa servera; prilikom
 * promene ekrana slusalac se prebacuje na novi kontroler.
 */
public class ViewNavigator {

    private static final int WINDOW_WIDTH = 1280;
    private static final int WINDOW_HEIGHT = 800;

    private final Stage stage;
    private final NetworkClient network = new NetworkClient();
    private final GameSession session = new GameSession();

    public ViewNavigator(Stage stage) {
        this.stage = stage;
        network.setOnDisconnect(this::onConnectionLost);
    }

    public NetworkClient getNetwork() {
        return network;
    }

    public GameSession getSession() {
        return session;
    }

    public Stage getStage() {
        return stage;
    }

    public void showMainMenu() {
        show("/fxml/MainMenu.fxml");
    }

    public void showLobby() {
        show("/fxml/Lobby.fxml");
    }

    public void showTable() {
        show("/fxml/Table.fxml");
    }

    private void show(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            ScreenController controller = loader.getController();
            controller.init(this);
            network.setListener(controller::onMessage);

            Scene scene = stage.getScene();
            if (scene == null) {
                scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                stage.setScene(scene);
            } else {
                scene.setRoot(root);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Ne mogu da ucitam ekran " + fxmlPath, e);
        }
    }

    private void onConnectionLost() {
        network.disconnect();
        session.clearRoom();
        showError("Veza sa serverom je prekinuta.");
        showMainMenu();
    }

    public void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setHeaderText(null);
        alert.setTitle("Lazes");
        alert.initOwner(stage);
        alert.showAndWait();
    }
}
