package com.muvrinovci.lazes.client;

import javafx.application.Application;
import javafx.stage.Stage;


public class MainApp extends Application {

    private ViewNavigator navigator;

    @Override
    public void start(Stage stage) {
        stage.setTitle("Lazes");
        stage.setMinWidth(1024);
        stage.setMinHeight(700);

        navigator = new ViewNavigator(stage);
        navigator.showMainMenu();

        stage.show();
    }

    @Override
    public void stop() {
        if (navigator != null) {
            navigator.getNetwork().disconnect();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
