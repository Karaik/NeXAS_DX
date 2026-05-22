package com.giga.nexas.controller;

import com.giga.nexas.controller.model.MainViewMode;
import com.giga.nexas.controller.model.WorkspaceState;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import lombok.RequiredArgsConstructor;

/**
 * BSDX 资源编辑页的临时占位控制器。
 *
 * <p>正式编辑器需求未重新定稿前，该模式只显示占位提示，不再加载已删除的实验性资源树代码。
 */
@RequiredArgsConstructor
public class BsdxResourceEditorModeController implements ModeController {

    private final MainViewController view;
    private final WorkspaceState state;
    private StackPane placeholder;

    @Override
    public MainViewMode mode() {
        return MainViewMode.BSDX_RESOURCE_EDITOR;
    }

    @Override
    public Node content() {
        if (placeholder == null) {
            placeholder = new StackPane(new Label("BSDX resource editor is not enabled. Define the editing workflow first."));
            placeholder.setVisible(false);
            placeholder.setManaged(false);
            view.getModeContentPane().getChildren().add(placeholder);
        }
        return placeholder;
    }

    @Override
    public void activate() {
        state.getBsdxOverlaySession().set(null);
        view.getStatusLabel().setText("BSDX Resource Editor | Disabled pending workflow design.");
    }

    @Override
    public void deactivate() {
        // 占位页没有需要释放的编辑状态。
    }

    @Override
    public boolean canLeave(String reason) {
        return true;
    }
}
