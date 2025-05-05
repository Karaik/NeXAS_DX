package com.giga.nexas.controller;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class LogContextMenuController {

    private final MainViewController view;

    public void setup() {
        MenuItem clear = new MenuItem("清空日志");
        clear.setOnAction(e -> view.getLogArea().clear());
        view.getLogArea().setContextMenu(new ContextMenu(clear));
    }
}
