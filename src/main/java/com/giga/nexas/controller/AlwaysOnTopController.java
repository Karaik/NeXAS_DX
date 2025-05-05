package com.giga.nexas.controller;

import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AlwaysOnTopController {

    private final MainViewController view;

    public void bind() {
        view.getAlwaysOnTop().selectedProperty().addListener((obs, oldVal, newVal) -> {
            Stage stage = (Stage) view.getRoot().getScene().getWindow();
            if (stage != null) stage.setAlwaysOnTop(newVal);
        });
    }
}
