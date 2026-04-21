package com.giga.nexas.controller.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import lombok.Getter;

import java.nio.file.Path;

/**
 * 保存工作区全局状态的数据容器。
 */
@Getter
public class WorkspaceState {

    private final ObjectProperty<Path> inputDirectory = new SimpleObjectProperty<>();
    private final ObjectProperty<Path> outputDirectory = new SimpleObjectProperty<>();
    private final ObjectProperty<EngineType> engineType = new SimpleObjectProperty<>(EngineType.BSDX);
    private final ObjectProperty<String> charset = new SimpleObjectProperty<>(EngineType.BSDX.getDefaultCharset());
    private final ObjectProperty<MainViewMode> mainViewMode = new SimpleObjectProperty<>(MainViewMode.WORKSPACE);
    private final ObjectProperty<BsdxOverlayResourceSession> bsdxOverlaySession = new SimpleObjectProperty<>();
    private final ObservableList<WorkspaceCategory> categories = FXCollections.observableArrayList();

    public void clearCategories() {
        categories.clear();
    }
}

