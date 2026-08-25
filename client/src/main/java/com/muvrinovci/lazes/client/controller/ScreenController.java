package com.muvrinovci.lazes.client.controller;

import com.muvrinovci.lazes.client.ViewNavigator;
import com.muvrinovci.lazes.shared.protocol.Message;

/** Zajednicki ugovor svih ekrana: prijem navigatora i poruka sa servera. */
public interface ScreenController {

    /** Poziva se odmah nakon ucitavanja FXML-a. */
    void init(ViewNavigator navigator);

    /** Poruka pristigla sa servera, uvek u JavaFX niti. */
    void onMessage(Message message);
}
