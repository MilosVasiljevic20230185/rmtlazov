package com.muvrinovci.lazes.client.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.muvrinovci.lazes.client.ViewNavigator;
import com.muvrinovci.lazes.client.view.Avatars;
import com.muvrinovci.lazes.client.view.CardView;
import com.muvrinovci.lazes.shared.GameRules;
import com.muvrinovci.lazes.shared.model.Card;
import com.muvrinovci.lazes.shared.model.Rank;
import com.muvrinovci.lazes.shared.protocol.Message;
import com.muvrinovci.lazes.shared.protocol.MessageType;
import com.muvrinovci.lazes.shared.protocol.dto.CallLiarMessage;
import com.muvrinovci.lazes.shared.protocol.dto.CallResultMessage;
import com.muvrinovci.lazes.shared.protocol.dto.CardDrawnMessage;
import com.muvrinovci.lazes.shared.protocol.dto.DrawCardMessage;
import com.muvrinovci.lazes.shared.protocol.dto.ErrorMessage;
import com.muvrinovci.lazes.shared.protocol.dto.GameOverMessage;
import com.muvrinovci.lazes.shared.protocol.dto.HandUpdateMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayAnnouncedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayCardsMessage;
import com.muvrinovci.lazes.shared.protocol.dto.PlayerDisconnectedMessage;
import com.muvrinovci.lazes.shared.protocol.dto.TurnUpdateMessage;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Sto za igru: ruka igraca, centar stola, akcije i tok partije.
 *
 * <p>Kontroler ne odlucuje ni o cemu sto se tice pravila - samo prikazuje stanje
 * koje je server poslao i salje akcije nazad. Dugmad se omogucavaju/onemogucavaju
 * prema fazi poteza, ali konacnu rec ima server.</p>
 */
public class TableController implements ScreenController {

    /** Najvise koliko karata sa centra se iscrtava jedna preko druge. */
    private static final int MAX_CENTER_CARDS_SHOWN = 8;

    /** Koliko sekundi otkrivene karte ostaju na stolu nakon prozivanja. */
    private static final double REVEAL_SECONDS = 4;

    private static final int MAX_LOG_ENTRIES = 40;

    @FXML private Label roomLabel;
    @FXML private Label timerLabel;
    @FXML private ProgressBar timerBar;
    @FXML private HBox opponentsBox;
    @FXML private Label tableValueLabel;
    @FXML private StackPane centerPileBox;
    @FXML private Label centerCountLabel;
    @FXML private StackPane drawPileBox;
    @FXML private Label drawCountLabel;
    @FXML private HBox revealBox;
    @FXML private VBox logBox;
    @FXML private ScrollPane logScroll;
    @FXML private FlowPane handPane;
    @FXML private Label selectionLabel;
    @FXML private ComboBox<Integer> valueCombo;
    @FXML private Button playButton;
    @FXML private Button drawButton;
    @FXML private Button callButton;
    @FXML private StackPane overlayPane;
    @FXML private VBox overlayContent;

    private ViewNavigator navigator;
    private final List<CardView> handCards = new ArrayList<>();

    /** Imena igraca po identifikatoru, popunjena iz {@code turn_update} poruka. */
    private final Map<String, String> namesById = new HashMap<>();

    private String currentPlayerId;
    private String announcerId;
    private int tableValue;
    private int drawPileCount;
    private boolean callWindowActive;
    private boolean gameOver;

    private Timeline timer;
    private double timerTotal;
    private double timerRemaining;
    private String timerPrefix = "";

    // ------------------------------------------------------------------
    // Priprema ekrana
    // ------------------------------------------------------------------

    @Override
    public void init(ViewNavigator navigator) {
        this.navigator = navigator;

        roomLabel.setText("Soba " + navigator.getSession().getRoomCode());
        setUpValueCombo();
        showStartCountdown();
        addLog("Partija počinje.", true);
    }

    private void setUpValueCombo() {
        List<Integer> values = new ArrayList<>();
        for (int value = Rank.MIN_VALUE; value <= Rank.MAX_VALUE; value++) {
            values.add(value);
        }
        valueCombo.setItems(FXCollections.observableArrayList(values));
        valueCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : Rank.fromValue(value).label();
            }

            @Override
            public Integer fromString(String text) {
                return Rank.fromLabel(text).value();
            }
        });
        valueCombo.getSelectionModel().select(Integer.valueOf(Rank.MAX_VALUE));
        valueCombo.valueProperty().addListener((observable, oldValue, newValue) -> updateControls());
    }

    /** Odbrojavanje 3 - 2 - 1 dok server deli karte. */
    private void showStartCountdown() {
        Label counter = new Label(String.valueOf(GameRules.START_COUNTDOWN_SECONDS));
        counter.getStyleClass().add("countdown");
        Label caption = new Label("Karte se dele...");
        caption.getStyleClass().add("subtitle");
        showOverlay(counter, caption);

        Timeline countdown = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            int remaining = Integer.parseInt(counter.getText()) - 1;
            counter.setText(remaining > 0 ? String.valueOf(remaining) : "Start!");
        }));
        countdown.setCycleCount(GameRules.START_COUNTDOWN_SECONDS);
        countdown.play();
    }

    // ------------------------------------------------------------------
    // Akcije korisnika
    // ------------------------------------------------------------------

    @FXML
    private void handlePlay() {
        List<String> selected = selectedCardIds();
        if (selected.isEmpty()) {
            return;
        }
        navigator.getNetwork().send(new PlayCardsMessage(selected, valueCombo.getValue()));
    }

    @FXML
    private void handleDraw() {
        navigator.getNetwork().send(new DrawCardMessage());
    }

    @FXML
    private void handleCallLiar() {
        callButton.setDisable(true);
        navigator.getNetwork().send(new CallLiarMessage());
    }

    private List<String> selectedCardIds() {
        return handCards.stream()
                .filter(CardView::isSelected)
                .map(view -> view.getCard().id())
                .toList();
    }

    // ------------------------------------------------------------------
    // Poruke sa servera
    // ------------------------------------------------------------------

    @Override
    public void onMessage(Message message) {
        switch (message.getType()) {
            case MessageType.HAND_UPDATE -> renderHand((HandUpdateMessage) message);
            case MessageType.TURN_UPDATE -> renderTurn((TurnUpdateMessage) message);
            case MessageType.PLAY_ANNOUNCED -> onPlayAnnounced((PlayAnnouncedMessage) message);
            case MessageType.CALL_RESULT -> onCallResult((CallResultMessage) message);
            case MessageType.CARD_DRAWN -> onCardDrawn((CardDrawnMessage) message);
            case MessageType.PLAYER_DISCONNECTED -> onPlayerLeft((PlayerDisconnectedMessage) message);
            case MessageType.GAME_OVER -> onGameOver((GameOverMessage) message);
            case MessageType.ERROR -> onError((ErrorMessage) message);
            default -> {
                // Ostale poruke sto ne prikazuje.
            }
        }
    }

    private void renderHand(HandUpdateMessage message) {
        handCards.clear();
        handPane.getChildren().clear();

        // Ruka se prikazuje sortirana, pa iste vrednosti stoje jedna uz drugu.
        List<Card> cards = message.getCards().stream()
                .map(Card::fromId)
                .sorted(Card.BY_VALUE)
                .toList();

        for (Card card : cards) {
            CardView view = new CardView(card);
            view.makeSelectable();
            view.setOnMouseClicked(event -> {
                view.toggleSelected();
                updateControls();
            });
            handCards.add(view);
            handPane.getChildren().add(view);
        }

        updateControls();
    }

    private void renderTurn(TurnUpdateMessage message) {
        hideOverlay();

        currentPlayerId = message.getCurrentPlayerId();
        tableValue = message.getTableValue();
        drawPileCount = message.getDrawPileCount();
        callWindowActive = false;
        announcerId = null;

        renderOpponents(message.getPlayers());
        renderCenter(message.getCenterCount());
        renderDrawPile(message.getDrawPileCount());
        renderTableValue();

        boolean myTurn = navigator.getSession().isMe(currentPlayerId);
        startTimer(message.getTurnSeconds(), myTurn ? "Tvoj potez" : "Na potezu: " + nameOf(currentPlayerId));

        updateControls();
    }

    private void onPlayAnnounced(PlayAnnouncedMessage message) {
        announcerId = message.getPlayerId();
        callWindowActive = true;
        revealBox.getChildren().clear();

        String declared = Rank.fromValue(message.getDeclaredValue()).label();
        addLog(message.getPlayerName() + " igra " + message.getDeclaredCount()
                + " × " + declared + ". Prozvati?", true);

        startTimer(message.getCallWindowMs() / 1000.0,
                navigator.getSession().isMe(announcerId)
                        ? "Čeka se da li će te prozvati"
                        : "Možeš prozvati laž!");

        updateControls();
    }

    private void onCallResult(CallResultMessage message) {
        callWindowActive = false;
        announcerId = null;
        stopTimer();

        String declared = Rank.fromValue(message.getDeclaredValue()).label();
        String verdict = message.isWasLying() ? "LAGAO JE" : "GOVORIO JE ISTINU";
        addLog(message.getCallerName() + " proziva " + message.getAccusedName()
                + " — " + verdict + " (najavio " + declared + ").", true);
        addLog(nameOf(message.getCardsCollectedBy()) + " uzima "
                + message.getCollectedCount() + " karata sa stola.", false);

        showRevealedCards(message.getRevealedCards());
        updateControls();
    }

    private void onCardDrawn(CardDrawnMessage message) {
        addLog(message.getPlayerName() + " vuče kartu"
                + (message.isAutomatic() ? " (isteklo vreme)." : "."), false);
    }

    private void onPlayerLeft(PlayerDisconnectedMessage message) {
        addLog(message.getPlayerName() + " je napustio partiju.", true);
    }

    private void onError(ErrorMessage message) {
        addLog("Greška: " + message.getMessage(), false);
        navigator.showError(message.getMessage());
    }

    private void onGameOver(GameOverMessage message) {
        gameOver = true;
        stopTimer();
        addLog("Partiju je pobedio " + message.getWinnerName() + "!", true);

        boolean iWon = navigator.getSession().isMe(message.getWinnerId());

        Label title = new Label(iWon ? "POBEDA!" : "Kraj partije");
        title.getStyleClass().add("title");

        Label subtitle = new Label(iWon
                ? "Rešio si se svih karata."
                : "Pobedio je " + message.getWinnerName() + ".");
        subtitle.getStyleClass().add("subtitle");

        VBox ranking = new VBox(8);
        ranking.setAlignment(Pos.CENTER);
        for (GameOverMessage.RankingEntry entry : message.getFinalRanking()) {
            Label row = new Label(entry.getRank() + ".  " + entry.getName()
                    + "  —  " + entry.getCardsLeft() + " karata");
            row.getStyleClass().add(entry.getRank() == 1 ? "log-entry-highlight" : "label");
            ranking.getChildren().add(row);
        }

        Button backButton = new Button("Nazad na meni");
        backButton.getStyleClass().add("button-primary");
        backButton.setOnAction(event -> {
            navigator.getNetwork().disconnect();
            navigator.getSession().clearRoom();
            navigator.showMainMenu();
        });

        showOverlay(title, subtitle, new Separator(), ranking, backButton);
        updateControls();
    }

    // ------------------------------------------------------------------
    // Iscrtavanje stola
    // ------------------------------------------------------------------

    private void renderOpponents(List<TurnUpdateMessage.PlayerInfo> players) {
        opponentsBox.getChildren().clear();

        for (TurnUpdateMessage.PlayerInfo player : players) {
            namesById.put(player.getId(), player.getName());

            if (navigator.getSession().isMe(player.getId())) {
                continue;
            }

            Label dot = new Label();
            dot.getStyleClass().add("avatar-dot");
            dot.setStyle("-fx-background-color: " + Avatars.colorOf(player.getAvatar()) + ";");

            Label name = new Label(player.getName());
            name.getStyleClass().add("seat-name");

            Label count = new Label(player.getCardCount() + " karata u ruci");
            count.getStyleClass().add("seat-cards");

            HBox header = new HBox(8, dot, name);
            header.setAlignment(Pos.CENTER);

            VBox seat = new VBox(4, header, count, miniHand(player.getCardCount()));
            seat.setAlignment(Pos.CENTER);
            seat.getStyleClass().add("seat");
            if (player.getId().equals(currentPlayerId)) {
                seat.getStyleClass().add("seat-active");
            }

            opponentsBox.getChildren().add(seat);
        }
    }

    /** Sicusan prikaz protivnickih karata - uvek okrenutih nadole. */
    private HBox miniHand(int cardCount) {
        HBox mini = new HBox(-14);
        mini.setAlignment(Pos.CENTER);
        for (int i = 0; i < Math.min(cardCount, 10); i++) {
            CardView back = CardView.faceDown();
            back.setScaleX(0.32);
            back.setScaleY(0.32);
            mini.getChildren().add(back);
        }
        mini.setMinHeight(38);
        return mini;
    }

    private void renderCenter(int centerCount) {
        centerPileBox.getChildren().clear();
        centerCountLabel.setText("Centar: " + centerCount + (centerCount == 1 ? " karta" : " karata"));

        for (int i = 0; i < Math.min(centerCount, MAX_CENTER_CARDS_SHOWN); i++) {
            CardView back = CardView.faceDown();
            back.setTranslateX(i * 5 - 12);
            back.setTranslateY(i * 3 - 8);
            back.setRotate((i % 2 == 0 ? 1 : -1) * (2 + i));
            centerPileBox.getChildren().add(back);
        }
    }

    private void renderDrawPile(int count) {
        drawPileBox.getChildren().clear();
        drawCountLabel.setText("Špil: " + count);

        if (count > 0) {
            drawPileBox.getChildren().add(CardView.faceDown());
        }
    }

    private void renderTableValue() {
        if (tableValue <= 0) {
            tableValueLabel.setText("Nova runda — biraš vrednost");
            valueCombo.setDisable(false);
        } else {
            String label = Rank.fromValue(tableValue).label();
            tableValueLabel.setText("Vrednost runde:  " + label);
            valueCombo.getSelectionModel().select(Integer.valueOf(tableValue));
            valueCombo.setDisable(true);
        }
    }

    /** Otkrivene karte ostaju na stolu nekoliko sekundi nakon prozivanja. */
    private void showRevealedCards(List<String> cardIds) {
        revealBox.getChildren().clear();

        Label caption = new Label("Otkriveno:");
        caption.getStyleClass().add("hint");
        revealBox.getChildren().add(caption);

        for (String cardId : cardIds) {
            CardView view = new CardView(Card.fromId(cardId));
            view.setScaleX(0.7);
            view.setScaleY(0.7);
            revealBox.getChildren().add(view);
        }

        PauseTransition pause = new PauseTransition(Duration.seconds(REVEAL_SECONDS));
        pause.setOnFinished(event -> revealBox.getChildren().clear());
        pause.play();
    }

    // ------------------------------------------------------------------
    // Dugmad i tajmer
    // ------------------------------------------------------------------

    private void updateControls() {
        int selected = selectedCardIds().size();
        selectionLabel.setText("Izabrano: " + selected);

        boolean myTurn = navigator.getSession().isMe(currentPlayerId);
        boolean canAct = myTurn && !callWindowActive && !gameOver;

        playButton.setDisable(!canAct
                || selected < GameRules.MIN_CARDS_PER_PLAY
                || selected > GameRules.MAX_CARDS_PER_PLAY
                || valueCombo.getValue() == null);
        drawButton.setDisable(!canAct || drawPileCount == 0);
        callButton.setDisable(gameOver || !callWindowActive
                || navigator.getSession().isMe(announcerId));
    }

    private void startTimer(double totalSeconds, String prefix) {
        stopTimer();

        timerTotal = totalSeconds;
        timerRemaining = totalSeconds;
        timerPrefix = prefix;
        updateTimerUi();

        timer = new Timeline(new KeyFrame(Duration.millis(100), event -> {
            timerRemaining = Math.max(0, timerRemaining - 0.1);
            updateTimerUi();
            if (timerRemaining <= 0) {
                stopTimer();
                onTimerElapsed();
            }
        }));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }

    private void onTimerElapsed() {
        if (callWindowActive) {
            // Prozor za prozivanje je istekao; server ce poslati novo stanje stola.
            callWindowActive = false;
            updateControls();
        }
    }

    private void updateTimerUi() {
        timerLabel.setText(timerPrefix + "  ·  " + Math.max(0, (int) Math.ceil(timerRemaining)) + "s");
        timerBar.setProgress(timerTotal <= 0 ? 0 : timerRemaining / timerTotal);

        boolean urgent = timerRemaining <= 5;
        timerBar.getStyleClass().remove("urgent");
        if (urgent) {
            timerBar.getStyleClass().add("urgent");
        }
    }

    private void stopTimer() {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }

    // ------------------------------------------------------------------
    // Log i preklop
    // ------------------------------------------------------------------

    private void addLog(String text, boolean highlight) {
        Label entry = new Label(text);
        entry.getStyleClass().add("log-entry");
        if (highlight) {
            entry.getStyleClass().add("log-entry-highlight");
        }
        entry.setWrapText(true);

        logBox.getChildren().add(entry);
        if (logBox.getChildren().size() > MAX_LOG_ENTRIES) {
            logBox.getChildren().remove(0);
        }
        logScroll.setVvalue(1.0);
    }

    private void showOverlay(javafx.scene.Node... content) {
        overlayContent.getChildren().setAll(content);
        overlayPane.setVisible(true);
        overlayPane.setManaged(true);
    }

    private void hideOverlay() {
        if (!gameOver) {
            overlayPane.setVisible(false);
            overlayPane.setManaged(false);
        }
    }

    private String nameOf(String playerId) {
        if (navigator.getSession().isMe(playerId)) {
            return "Ti";
        }
        return namesById.getOrDefault(playerId, "igrač");
    }
}
