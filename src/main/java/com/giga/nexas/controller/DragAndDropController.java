package com.giga.nexas.controller;

import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import lombok.RequiredArgsConstructor;

import java.io.File;

@RequiredArgsConstructor
public class DragAndDropController {

    private final MainViewController view;

    public void setup() {
        view.getRoot().setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        view.getRoot().setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasFiles()) {
                File file = db.getFiles().get(0);
                view.getInputField().setText(file.getAbsolutePath());
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });
    }
}
