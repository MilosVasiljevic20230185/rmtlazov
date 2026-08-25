package com.muvrinovci.lazes.client.view;

import com.muvrinovci.lazes.shared.model.Card;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

/**
 * Karta nacrtana u kodu - bez slikovnih fajlova.
 *
 * <p>Lice karte prikazuje oznaku ranga u dva ugla i veliki Unicode simbol boje
 * u sredini; nalicje je jednobojna pozadina sa zlatnim okvirom.</p>
 */
public class CardView extends StackPane {

    public static final double WIDTH = 74;
    public static final double HEIGHT = 106;

    /** Za koliko se piksela izabrana karta podigne iznad ostalih. */
    private static final double SELECT_LIFT = 18;

    private final Card card;
    private boolean selected;

    /** Karta okrenuta licem nagore. */
    public CardView(Card card) {
        this(card, true);
    }

    /** Karta ciji je sadrzaj sakriven ({@code card} sme biti {@code null}). */
    public static CardView faceDown() {
        return new CardView(null, false);
    }

    private CardView(Card card, boolean faceUp) {
        this.card = card;

        setPrefSize(WIDTH, HEIGHT);
        setMinSize(WIDTH, HEIGHT);
        setMaxSize(WIDTH, HEIGHT);

        if (faceUp && card != null) {
            getStyleClass().add("card");
            getChildren().add(buildFace(card));
        } else {
            getStyleClass().add("card-back");
        }
    }

    private BorderPane buildFace(Card card) {
        String colorClass = card.suit().isRed() ? "card-red" : "card-black";
        String symbol = String.valueOf(card.suit().symbol());

        Label topLeft = new Label(card.rank().label() + "\n" + symbol);
        topLeft.getStyleClass().addAll("card-rank", colorClass);

        Label center = new Label(symbol);
        center.getStyleClass().addAll("card-suit", colorClass);

        Label bottomRight = new Label(symbol + "\n" + card.rank().label());
        bottomRight.getStyleClass().addAll("card-rank", colorClass);
        bottomRight.setRotate(180);

        BorderPane face = new BorderPane();
        face.setPadding(new Insets(5, 7, 5, 7));
        face.setTop(topLeft);
        face.setCenter(center);
        face.setBottom(bottomRight);
        BorderPane.setAlignment(bottomRight, Pos.BOTTOM_RIGHT);

        return face;
    }

    public Card getCard() {
        return card;
    }

    /** Oznacava kartu kao kliktabilnu (koristi se samo za karte u sopstvenoj ruci). */
    public void makeSelectable() {
        getStyleClass().add("card-selectable");
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if (this.selected == selected) {
            return;
        }
        this.selected = selected;

        if (selected) {
            getStyleClass().add("card-selected");
            setTranslateY(-SELECT_LIFT);
        } else {
            getStyleClass().remove("card-selected");
            setTranslateY(0);
        }
    }

    public void toggleSelected() {
        setSelected(!selected);
    }
}
