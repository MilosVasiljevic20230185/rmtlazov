package com.muvrinovci.lazes.client.view;

import java.util.List;
import java.util.Map;

/** Boje mesta za stolom, iste vrednosti koristi i server. */
public final class Avatars {

    public static final List<String> ALL = List.of("blue", "red", "green", "gold");

    private static final Map<String, String> COLORS = Map.of(
            "blue", "#4a7fb5",
            "red", "#b5504a",
            "green", "#5c9e5f",
            "gold", "#d4a24c");

    private static final Map<String, String> NAMES = Map.of(
            "blue", "Plava",
            "red", "Crvena",
            "green", "Zelena",
            "gold", "Zlatna");

    private Avatars() {
    }

    public static String colorOf(String avatar) {
        return COLORS.getOrDefault(avatar, "#6b6357");
    }

    public static String nameOf(String avatar) {
        return NAMES.getOrDefault(avatar, "Bez boje");
    }
}
