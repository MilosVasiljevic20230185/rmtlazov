package com.muvrinovci.lazes.client.view;

import com.muvrinovci.lazes.shared.model.Card;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/**
 * Karta nacrtana u kodu 
 */
public class CardView extends StackPane {

    public static final double WIDTH = 74;
    public static final double HEIGHT = 106;

    private static final double SELECT_LIFT = 18;

    private static final double CORNER_RADIUS = 7;

    private final Card card;
    private boolean selected;

    /** Karta okrenuta licem nagore. */
    public CardView(Card card) {
        this(card, true);
    }

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

        Label center = new Label(symbol);
        center.getStyleClass().addAll("card-suit", colorClass);

        Label topLeft = cornerIndex(card, symbol, colorClass);

        Label bottomRight = cornerIndex(card, symbol, colorClass);
        bottomRight.setRotate(180);

        BorderPane face = new BorderPane();
        face.setPadding(new Insets(4, 6, 4, 6));
        face.setTop(topLeft);
        face.setCenter(center);
        face.setBottom(bottomRight);
        BorderPane.setAlignment(bottomRight, Pos.BOTTOM_RIGHT);

        Rectangle clip = new Rectangle(WIDTH, HEIGHT);
        clip.setArcWidth(CORNER_RADIUS * 2);
        clip.setArcHeight(CORNER_RADIUS * 2);
        face.setClip(clip);

        return face;
    }

    private Label cornerIndex(Card card, String symbol, String colorClass) {
        Label pip = new Label(symbol);
        pip.getStyleClass().addAll("card-corner-pip", colorClass);

        Label index = new Label(card.rank().label(), pip);
        index.getStyleClass().addAll("card-rank", colorClass);
        index.setContentDisplay(ContentDisplay.BOTTOM);
        index.setGraphicTextGap(0);

        return index;
    }

    public Card getCard() {
        return card;
    }


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
