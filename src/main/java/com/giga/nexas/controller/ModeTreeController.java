package com.giga.nexas.controller;

import com.giga.nexas.controller.model.*;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用于构建工作区树形视图的控制器。
 */
@RequiredArgsConstructor
public class ModeTreeController {

    private final MainViewController view;
    private final WorkspaceState state;
    private final BranchGridController gridController;
    private final Map<String, TreeItem<WorkspaceTreeNode>> categoryItems = new HashMap<>();
    private static final int STATUS_SEGMENT_LIMIT = 3;

    public void setup() {
        TreeView<WorkspaceTreeNode> tree = view.getTree();
        tree.setShowRoot(true);
        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            if (newItem != null) {
                WorkspaceTreeNode node = newItem.getValue();
                if (node != null && node.getCategory() != null) {
                    gridController.highlightCategory(node.getCategory().getId());
                }
            }
        });
        tree.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                TreeItem<WorkspaceTreeNode> selected = tree.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    WorkspaceNodeKind kind = selected.getValue().getKind();
                    if (kind == WorkspaceNodeKind.FILE_BINARY || kind == WorkspaceNodeKind.FILE_JSON) {
                        view.getActionButton().fire();
                    }
                }
            }
        });

        state.getCategories().addListener((ListChangeListener<WorkspaceCategory>) change -> rebuildTree());
        state.getInputDirectory().addListener((obs, oldPath, newPath) -> rebuildTree());
        state.getEngineType().addListener((obs, oldEngine, newEngine) -> updateStatus());

        rebuildTree();
        updateStatus();
    }

    public void selectCategory(String categoryId) {
        TreeItem<WorkspaceTreeNode> item = categoryItems.get(categoryId);
        if (item != null) {
            view.getTree().getSelectionModel().select(item);
        }
    }

    /**
     * 供其他模式在退出时恢复主工作区状态栏使用。
     */
    public void refreshViewState() {
        updateSummary();
        updateStatus();
    }

    private void rebuildTree() {
        Platform.runLater(() -> {
            TreeItem<WorkspaceTreeNode> rootItem = buildRoot();
            categoryItems.clear();
            for (WorkspaceCategory category : state.getCategories()) {
                TreeItem<WorkspaceTreeNode> catItem = buildCategoryItem(category);
                categoryItems.put(category.getId(), catItem);
                rootItem.getChildren().add(catItem);
            }
            rootItem.setExpanded(true);
            view.getTree().setRoot(rootItem);
            updateSummary();
            updateStatus();
        });
    }

    private TreeItem<WorkspaceTreeNode> buildRoot() {
        Path dir = state.getInputDirectory().get();
        String label = dir == null ? "No directory" : dir.toString();
        return new TreeItem<>(new WorkspaceTreeNode(label, null, dir, WorkspaceNodeKind.ROOT));
    }

    private TreeItem<WorkspaceTreeNode> buildCategoryItem(WorkspaceCategory category) {
        TreeItem<WorkspaceTreeNode> categoryItem = new TreeItem<>(new WorkspaceTreeNode(
                category.getTitle(), category, null, WorkspaceNodeKind.CATEGORY));
        categoryItem.setExpanded(true);

        categoryItem.getChildren().add(buildGroupNode(
                "Binary (" + category.getBinaryFiles().size() + ")",
                category,
                WorkspaceNodeKind.GROUP_BINARY,
                WorkspaceNodeKind.FILE_BINARY,
                category.getBinaryFiles()));

        categoryItem.getChildren().add(buildGroupNode(
                "JSON (" + category.getJsonFiles().size() + ")",
                category,
                WorkspaceNodeKind.GROUP_JSON,
                WorkspaceNodeKind.FILE_JSON,
                category.getJsonFiles()));

        return categoryItem;
    }

    private TreeItem<WorkspaceTreeNode> buildGroupNode(String label,
                                                       WorkspaceCategory category,
                                                       WorkspaceNodeKind groupKind,
                                                       WorkspaceNodeKind fileKind,
                                                       List<Path> files) {
        TreeItem<WorkspaceTreeNode> groupItem = new TreeItem<>(new WorkspaceTreeNode(label, category, null, groupKind));
        for (Path file : files) {
            groupItem.getChildren().add(new TreeItem<>(new WorkspaceTreeNode(
                    file.getFileName().toString(), category, file, fileKind)));
        }
        return groupItem;
    }

    private void updateSummary() {
        int categoryCount = state.getCategories().size();
        long fileCount = state.getCategories().stream()
                .mapToLong(cat -> (long) cat.getBinaryFiles().size() + cat.getJsonFiles().size())
                .sum();
        view.getTreeSummaryLabel().setText(
                categoryCount == 0
                        ? "Select a directory to begin."
                        : "Loaded " + categoryCount + " categories, " + fileCount + " entries.");
    }

    private void updateStatus() {
        EngineType engine = state.getEngineType().get();
        int categoryCount = state.getCategories().size();
        String summary;
        if (categoryCount == 0) {
            summary = "waiting for scan";
        } else {
            List<String> segments = state.getCategories().stream()
                    .map(this::formatCategoryInfo)
                    .collect(Collectors.toList());
            String displayed = segments.stream()
                    .limit(STATUS_SEGMENT_LIMIT)
                    .collect(Collectors.joining(", "));
            if (segments.size() > STATUS_SEGMENT_LIMIT) {
                displayed += ", +" + (segments.size() - STATUS_SEGMENT_LIMIT) + " more";
            }
            summary = categoryCount + " categories | " + displayed;
        }
        String status = "Data files | " + engine.getDisplayName() + " | " + summary;
        view.getStatusLabel().setText(status);
    }

    private String formatCategoryInfo(WorkspaceCategory category) {
        return category.getExtension().toUpperCase(Locale.ROOT)
                + " (" + category.getBinaryFiles().size()
                + "/" + category.getJsonFiles().size() + ")";
    }
}

