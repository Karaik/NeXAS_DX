package com.giga.nexas.controller;

import com.giga.nexas.controller.model.WorkspaceNodeKind;
import com.giga.nexas.controller.model.WorkspaceState;
import com.giga.nexas.controller.model.WorkspaceTreeNode;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeCell;
import lombok.RequiredArgsConstructor;

import java.nio.file.Path;
import java.util.Locale;

@RequiredArgsConstructor
public class TreeContextMenuController {

    private final MainViewController view;
    private final WorkspaceState state;

    public void setup() {
        view.getTree().setCellFactory(tree -> new TreeCell<>() {
            @Override
            protected void updateItem(WorkspaceTreeNode item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setContextMenu(null);
                    return;
                }

                setText(item.getLabel());
                setContextMenu(buildContextMenu(item));
            }
        });
    }

    private ContextMenu buildContextMenu(WorkspaceTreeNode node) {
        if (!supportsPseudoCode(node)) {
            return null;
        }

        MenuItem openPseudo = new MenuItem("Open as pseudo code");
        openPseudo.setOnAction(event -> openPseudoEditor(node.getFilePath()));
        return new ContextMenu(openPseudo);
    }

    private void openPseudoEditor(Path filePath) {
        String charset = state.getCharset().get() == null || state.getCharset().get().isBlank()
                ? state.getEngineType().get().getDefaultCharset()
                : state.getCharset().get();

        BinPseudoEditorController.openEditor(
                filePath,
                charset,
                view.getRoot().getScene().getWindow(),
                text -> view.getLogArea().appendText(text + System.lineSeparator())
        );
    }

    private boolean supportsPseudoCode(WorkspaceTreeNode node) {
        if (node == null || node.getKind() != WorkspaceNodeKind.FILE_BINARY || node.getFilePath() == null) {
            return false;
        }
        String fileName = node.getFilePath().getFileName().toString().toLowerCase(Locale.ROOT);
        return fileName.endsWith(".bin") && !"__global.bin".equals(fileName);
    }
}
