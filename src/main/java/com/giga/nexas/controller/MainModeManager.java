package com.giga.nexas.controller;

import com.giga.nexas.controller.model.MainViewMode;
import com.giga.nexas.controller.model.WorkspaceState;
import lombok.RequiredArgsConstructor;

import java.util.EnumMap;
import java.util.Map;

/**
 * 主页面模式切换管理器，集中处理工作区和内层编辑页的可见性。
 */
@RequiredArgsConstructor
public class MainModeManager {

    private static final String REASON_MODE_SWITCH = "mode-switch";

    private final MainViewController view;
    private final WorkspaceState state;
    private final ModeTreeController treeController;
    private final Map<MainViewMode, ModeController> controllers = new EnumMap<>(MainViewMode.class);
    private MainViewMode activeMode = MainViewMode.WORKSPACE;
    private boolean suppressModeUpdate;

    public void register(ModeController controller) {
        if (controller != null) {
            controllers.put(controller.mode(), controller);
        }
    }

    public void bind() {
        view.getWorkspaceModeButton().setOnAction(event -> requestMode(MainViewMode.WORKSPACE));
        view.getHellEditorModeButton().setOnAction(event -> requestMode(MainViewMode.HELL_SCRIPT_EDITOR));
        view.getBsdxResourceModeButton().setOnAction(event -> requestMode(MainViewMode.BSDX_RESOURCE_EDITOR));
        state.getMainViewMode().addListener((obs, oldMode, newMode) -> {
            if (!suppressModeUpdate) {
                switchMode(newMode == null ? MainViewMode.WORKSPACE : newMode);
            }
        });
        switchMode(state.getMainViewMode().get());
    }

    private void requestMode(MainViewMode requestedMode) {
        MainViewMode targetMode = requestedMode == null ? MainViewMode.WORKSPACE : requestedMode;
        if (!canLeaveActiveMode()) {
            updateModeButtons(activeMode);
            return;
        }
        state.getMainViewMode().set(targetMode);
        switchMode(targetMode);
    }

    private boolean canLeaveActiveMode() {
        ModeController controller = controllers.get(activeMode);
        return controller == null || controller.canLeave(REASON_MODE_SWITCH);
    }

    private void switchMode(MainViewMode requestedMode) {
        MainViewMode targetMode = requestedMode == null ? MainViewMode.WORKSPACE : requestedMode;
        if (targetMode == activeMode && view.getWorkspacePane().isVisible() == (targetMode == MainViewMode.WORKSPACE)) {
            updateModeButtons(targetMode);
            return;
        }

        ModeController oldController = controllers.get(activeMode);
        if (oldController != null) {
            oldController.deactivate();
        }

        activeMode = targetMode;
        view.getWorkspacePane().setVisible(targetMode == MainViewMode.WORKSPACE);
        view.getWorkspacePane().setManaged(targetMode == MainViewMode.WORKSPACE);
        controllers.values().forEach(controller -> {
            boolean visible = controller.mode() == targetMode;
            controller.content().setVisible(visible);
            controller.content().setManaged(visible);
        });

        ModeController newController = controllers.get(targetMode);
        if (newController != null) {
            newController.activate();
        } else if (targetMode == MainViewMode.WORKSPACE) {
            treeController.refreshViewState();
        }
        updateModeButtons(targetMode);
    }

    private void updateModeButtons(MainViewMode mode) {
        suppressModeUpdate = true;
        try {
            view.getWorkspaceModeButton().setSelected(mode == MainViewMode.WORKSPACE);
            view.getHellEditorModeButton().setSelected(mode == MainViewMode.HELL_SCRIPT_EDITOR);
            view.getBsdxResourceModeButton().setSelected(mode == MainViewMode.BSDX_RESOURCE_EDITOR);
        } finally {
            suppressModeUpdate = false;
        }
    }
}
