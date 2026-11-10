package com.giga.nexas.controller;

import com.giga.nexas.controller.model.BranchActionType;
import com.giga.nexas.controller.model.WorkspaceCategory;
import com.giga.nexas.controller.model.WorkspaceState;
import com.giga.nexas.controller.support.BranchActionHandler;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.beans.binding.Bindings;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import lombok.Setter;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.HashMap;

/**
 * 用于渲染分类卡片网格的控制器。
 */
public class BranchGridController {

    private final MainViewController view;
    private final WorkspaceState state;
    private final Map<String, VBox> cardById = new HashMap<>();

    @Setter
    private BranchActionHandler actionHandler;

    @Setter
    private ModeTreeController treeController;

    public BranchGridController(MainViewController view, WorkspaceState state) {
        this.view = view;
        this.state = state;
    }

    public void setup() {
        state.getCategories().addListener((ListChangeListener<WorkspaceCategory>) change -> rebuildCards());
        rebuildCards();
    }

    public void highlightCategory(String categoryId) {
        cardById.forEach((id, node) -> {
            boolean active = Objects.equals(id, categoryId);
            node.setStyle(active ? highlightedStyle() : defaultStyle());
        });
    }

    public void selectCategoryFromCard(String categoryId, ModeTreeController treeController) {
        VBox card = cardById.get(categoryId);
        if (card != null) {
            treeController.selectCategory(categoryId);
            highlightCategory(categoryId);
        }
    }

    private void rebuildCards() {
        Platform.runLater(() -> {
            TilePane grid = view.getBranchGrid();
            grid.getChildren().clear();
            cardById.clear();
            for (WorkspaceCategory category : state.getCategories()) {
                VBox card = buildDataCard(category);
                cardById.put(category.getId(), card);
                grid.getChildren().add(card);
            }
            highlightCategory(null);
        });
    }

    private VBox buildDataCard(WorkspaceCategory category) {
        VBox box = createCard(category);
        Label title = new Label(category.getTitle());
        title.getStyleClass().add("branch-card-title");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);

        String statsText = "Binary: " + category.getBinaryFiles().size()
                + " | JSON: " + category.getJsonFiles().size();
        Label stats = new Label(statsText);
        stats.setWrapText(true);
        stats.setMaxWidth(Double.MAX_VALUE);

        ListView<Path> binaryList = createFileList(category.getBinaryFiles());
        ListView<Path> jsonList = createFileList(category.getJsonFiles());
        VBox.setVgrow(binaryList, Priority.ALWAYS);
        VBox.setVgrow(jsonList, Priority.ALWAYS);

        Button parseAll = new Button("Parse all");
        parseAll.setDisable(!category.isCanParse() || category.getBinaryFiles().isEmpty());
        parseAll.setOnAction(e -> triggerAction(category, BranchActionType.PARSE, null));
        parseAll.setTooltip(new Tooltip("Convert all binary files to JSON"));

        Button parseSelected = new Button("Parse selected");
        parseSelected.disableProperty().bind(Bindings.isEmpty(binaryList.getSelectionModel().getSelectedItems()));
        parseSelected.setOnAction(e -> {
            List<Path> targets = new ArrayList<>(binaryList.getSelectionModel().getSelectedItems());
            triggerAction(category, BranchActionType.PARSE, targets);
        });
        HBox parseButtons = new HBox(8, parseAll, parseSelected);
        parseButtons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button generateAll = new Button("Generate all");
        generateAll.setDisable(!category.isCanGenerate() || category.getJsonFiles().isEmpty());
        generateAll.setOnAction(e -> triggerAction(category, BranchActionType.GENERATE, null));
        generateAll.setTooltip(new Tooltip("Generate binaries for all JSON files"));

        Button generateSelected = new Button("Generate selected");
        generateSelected.disableProperty().bind(Bindings.isEmpty(jsonList.getSelectionModel().getSelectedItems()));
        generateSelected.setOnAction(e -> {
            List<Path> targets = new ArrayList<>(jsonList.getSelectionModel().getSelectedItems());
            triggerAction(category, BranchActionType.GENERATE, targets);
        });
        HBox generateButtons = new HBox(8, generateAll, generateSelected);
        generateButtons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        configureListInteractions(binaryList, category, BranchActionType.PARSE);
        configureListInteractions(jsonList, category, BranchActionType.GENERATE);

        VBox binarySection = new VBox(4,
                new Label("Binary files"),
                binaryList,
                parseButtons);
        VBox jsonSection = new VBox(4,
                new Label("JSON files"),
                jsonList,
                generateButtons);

        box.getChildren().addAll(title, stats, binarySection, jsonSection);
        return box;
    }

    private ListView<Path> createFileList(List<Path> files) {
        ListView<Path> list = new ListView<>();
        list.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        list.getItems().addAll(files);
        list.setPrefHeight(120);
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Path item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getFileName().toString());
            }
        });
        list.setPlaceholder(new Label("No files"));
        return list;
    }

    private void configureListInteractions(ListView<Path> list,
                                           WorkspaceCategory category,
                                           BranchActionType type) {
        list.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Path selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    triggerAction(category, type, List.of(selected));
                }
            }
        });
        ContextMenu menu = new ContextMenu();
        MenuItem openItem = new MenuItem(type == BranchActionType.PARSE ? "Parse selected" : "Generate selected");
        openItem.setOnAction(e -> {
            List<Path> targets = new ArrayList<>(list.getSelectionModel().getSelectedItems());
            if (!targets.isEmpty()) {
                triggerAction(category, type, targets);
            }
        });
        menu.getItems().add(openItem);
        list.setContextMenu(menu);
    }

    private VBox createCard(WorkspaceCategory category) {
        VBox box = new VBox(8);
        box.setFillWidth(true);
        box.setMinWidth(280);
        box.setPrefWidth(320);
        box.setStyle(defaultStyle());
        box.setOnMouseClicked(e -> triggerSelection(category));
        return box;
    }

    private void triggerSelection(WorkspaceCategory category) {
        if (treeController != null) {
            selectCategoryFromCard(category.getId(), treeController);
        }
    }

    private void triggerAction(WorkspaceCategory category, BranchActionType type, List<Path> files) {
        if (actionHandler != null) {
            List<Path> payload = files == null ? null : Collections.unmodifiableList(new ArrayList<>(files));
            actionHandler.handle(category, type, payload);
        }
    }

    private String defaultStyle() {
        return "-fx-padding:12; -fx-spacing:8; -fx-border-color:#8aa2c1; -fx-border-radius:8; "
                + "-fx-background-radius:8; -fx-background-color:#f4f7fb;";
    }

    private String highlightedStyle() {
        return "-fx-padding:12; -fx-spacing:8; -fx-border-color:#346bd6; -fx-border-radius:8; "
                + "-fx-background-radius:8; -fx-background-color:#e4eeff;";
    }
}
