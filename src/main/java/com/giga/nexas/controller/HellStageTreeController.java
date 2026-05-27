package com.giga.nexas.controller;

import com.giga.nexas.controller.model.HellStageDescriptor;
import javafx.application.Platform;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Hell 关卡树控制器。
 *
 * <p>负责关卡树的构建、选中状态机和 suppress 逻辑。
 * 选中变化时通过回调通知外部协调器刷新元数据和脚本编辑器。
 */
public class HellStageTreeController {

    private final TreeView<HellStageDescriptor> stageTree;
    private final BiConsumer<HellStageDescriptor, Boolean> selectionAppliedCallback;
    private final Function<HellStageDescriptor, Boolean> canSwitchChecker;

    private TreeItem<HellStageDescriptor> lastConfirmedStageSelection;
    private boolean suppressStageSelectionListener;

    /**
     * @param stageTree                关卡树视图
     * @param selectionAppliedCallback 选中确认后的回调 (stage, forceScriptReload)
     * @param canSwitchChecker         切换前的确认检查，返回 true 允许切换
     */
    public HellStageTreeController(
            TreeView<HellStageDescriptor> stageTree,
            BiConsumer<HellStageDescriptor, Boolean> selectionAppliedCallback,
            Function<HellStageDescriptor, Boolean> canSwitchChecker
    ) {
        this.stageTree = stageTree;
        this.selectionAppliedCallback = selectionAppliedCallback;
        this.canSwitchChecker = canSwitchChecker;
    }

    /**
     * 绑定树选中监听器。
     */
    public void setup() {
        stageTree.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            handleStageTreeSelectionChanged(oldItem, newItem);
        });
    }

    /**
     * 把 Hell 关卡列表装进树视图。
     */
    public void populateStageTree(List<HellStageDescriptor> stages, Integer preferredIndex) {
        TreeItem<HellStageDescriptor> root = new TreeItem<>(HellStageDescriptor.builder()
                .index(-1)
                .title("Hell stages")
                .build());
        root.setExpanded(true);

        for (HellStageDescriptor stage : stages) {
            root.getChildren().add(new TreeItem<>(stage));
        }

        stageTree.setShowRoot(false);
        stageTree.setRoot(root);
        if (!root.getChildren().isEmpty()) {
            int selectedChildIndex = preferredIndex == null
                    ? 0
                    : Math.max(0, Math.min(preferredIndex, root.getChildren().size() - 1));
            Platform.runLater(() -> applyConfirmedSelection(root.getChildren().get(selectedChildIndex), true));
        } else {
            lastConfirmedStageSelection = null;
            selectionAppliedCallback.accept(null, false);
        }
    }

    /**
     * 把某个树选中项作为真正生效的新上下文。
     */
    public void applyConfirmedSelection(TreeItem<HellStageDescriptor> item, boolean forceScriptReload) {
        suppressStageSelectionListener = true;
        try {
            stageTree.getSelectionModel().select(item);
        } finally {
            suppressStageSelectionListener = false;
        }
        lastConfirmedStageSelection = item;

        HellStageDescriptor stage = item == null ? null : item.getValue();
        selectionAppliedCallback.accept(stage, forceScriptReload);
    }

    /**
     * 获取当前选中的关卡描述。
     */
    public HellStageDescriptor getSelectedStage() {
        TreeItem<HellStageDescriptor> selected = stageTree.getSelectionModel().getSelectedItem();
        return selected == null ? null : selected.getValue();
    }

    /**
     * 获取当前选中的树节点。
     */
    public TreeItem<HellStageDescriptor> getSelectedItem() {
        return stageTree.getSelectionModel().getSelectedItem();
    }

    /**
     * 计算当前关卡树里已加载的条目数量。
     */
    public int getStageCount() {
        TreeItem<HellStageDescriptor> root = stageTree.getRoot();
        return root == null ? 0 : root.getChildren().size();
    }

    /**
     * 清空树视图。
     */
    public void clear() {
        lastConfirmedStageSelection = null;
        stageTree.setRoot(new TreeItem<>());
    }

    private void handleStageTreeSelectionChanged(
            TreeItem<HellStageDescriptor> oldItem,
            TreeItem<HellStageDescriptor> newItem
    ) {
        if (suppressStageSelectionListener) {
            return;
        }
        HellStageDescriptor nextStage = newItem == null ? null : newItem.getValue();
        if (!canSwitchChecker.apply(nextStage)) {
            restoreConfirmedStageSelection(oldItem);
            return;
        }
        applyConfirmedSelection(newItem, false);
    }

    private void restoreConfirmedStageSelection(TreeItem<HellStageDescriptor> fallbackItem) {
        TreeItem<HellStageDescriptor> target = lastConfirmedStageSelection != null ? lastConfirmedStageSelection : fallbackItem;
        suppressStageSelectionListener = true;
        try {
            stageTree.getSelectionModel().select(target);
        } finally {
            suppressStageSelectionListener = false;
        }
    }
}
