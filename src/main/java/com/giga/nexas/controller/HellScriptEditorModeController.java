package com.giga.nexas.controller;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.HellEditorReferenceData;
import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.HellStageMetadataDraft;
import com.giga.nexas.controller.model.MainViewMode;
import com.giga.nexas.controller.model.WorkspaceState;
import com.giga.nexas.controller.support.HellEditorReferenceService;
import com.giga.nexas.controller.support.HellMapPreviewChooser;
import com.giga.nexas.controller.support.HellModPackService;
import com.giga.nexas.controller.support.HellScriptDataLoader;
import com.giga.nexas.controller.support.HellScriptEditorService;
import com.giga.nexas.controller.support.HellStageStatusFormatter;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.Node;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * MainView 内嵌 Hell Script Editor 的模式控制器。
 *
 * <p>这个控制器把主界面切换成 Hell 关卡编辑工作区。
 *
 * <p>控制范围包括：
 * 1. Hell 关卡编辑模式切换
 * 2. `root + mod` 覆盖会话下的关卡树加载
 * 3. 元数据、机体速查、地图预览和内嵌脚本编辑器的联动
 * 4. 关卡元数据保存、追加关卡和脚本回编入口
 */
@RequiredArgsConstructor
public class HellScriptEditorModeController implements ModeController {

    private static final String UPDATE4_PAC_FILE_NAME = "Update4.pac";
    private static final String DEFAULT_PAC_COMPRESS_MODE = "4";

    private final MainViewController view;
    private final WorkspaceState state;
    private final HellScriptDataLoader loader = new HellScriptDataLoader();
    private final HellScriptEditorService editorService = new HellScriptEditorService();
    private final HellEditorReferenceService referenceService = new HellEditorReferenceService();
    private final HellMapPreviewChooser mapPreviewChooser = new HellMapPreviewChooser(referenceService);
    private HellModPackService modPackService = new HellModPackService();

    /**
     * 地图预览控制器，负责预览显示和选择器调用。
     */
    private HellMapPreviewController mapPreviewController;

    /**
     * 机体速查控制器，负责速查表和敌机输入框联动。
     */
    private HellMekaLookupController mekaLookupController;

    /**
     * 元数据表单控制器，负责右侧面板显示、校验和追加关卡区域。
     */
    private HellStageDetailController stageDetailController;

    /**
     * 关卡树控制器，负责树构建、选中状态机和 suppress 逻辑。
     */
    private HellStageTreeController stageTreeController;

    /**
     * 内嵌脚本编辑器控制器，负责脚本生命周期和脏状态确认。
     */
    private HellEmbeddedScriptController embeddedScriptController;

    /**
     * 8 个敌机类型输入框，对应 `HellConfig[3..10]`。
     */
    private List<TextField> enemyTypeFields;

    /**
     * 8 个敌机编号旁边的机体名称标签。
     */
    private List<Label> enemyTypeNameLabels;

    /**
     * 8 个敌机附加参数输入框，对应 `HellConfig[11..18]`。
     */
    private List<TextField> enemyCountFields;

    /**
     * 当前会话下加载到的机体/地图引用快照。
     */
    private HellEditorReferenceData referenceData;

    /**
     * 初始化 Hell 编辑模式的所有交互。
     */
    public void setup() {
        enemyTypeFields = List.of(
                view.getHellEnemyType0Field(),
                view.getHellEnemyType1Field(),
                view.getHellEnemyType2Field(),
                view.getHellEnemyType3Field(),
                view.getHellEnemyType4Field(),
                view.getHellEnemyType5Field(),
                view.getHellEnemyType6Field(),
                view.getHellEnemyType7Field()
        );
        enemyTypeNameLabels = List.of(
                view.getHellEnemyType0NameLabel(),
                view.getHellEnemyType1NameLabel(),
                view.getHellEnemyType2NameLabel(),
                view.getHellEnemyType3NameLabel(),
                view.getHellEnemyType4NameLabel(),
                view.getHellEnemyType5NameLabel(),
                view.getHellEnemyType6NameLabel(),
                view.getHellEnemyType7NameLabel()
        );
        enemyCountFields = List.of(
                view.getHellEnemyCount0Field(),
                view.getHellEnemyCount1Field(),
                view.getHellEnemyCount2Field(),
                view.getHellEnemyCount3Field(),
                view.getHellEnemyCount4Field(),
                view.getHellEnemyCount5Field(),
                view.getHellEnemyCount6Field(),
                view.getHellEnemyCount7Field()
        );

        mekaLookupController = new HellMekaLookupController(
                view.getHellMekaLookupTable(),
                view.getHellMekaLookupFilterField(),
                view.getHellMekaLookupHintLabel(),
                enemyTypeFields,
                enemyTypeNameLabels,
                () -> referenceData
        );
        mekaLookupController.setup();
        mapPreviewController = new HellMapPreviewController(
                view.getHellMapPreviewImageView(),
                view.getHellMapPreviewStatusLabel(),
                view.getHellStageMapField(),
                view.getHellMapNameLabel(),
                view.getHellMapResourceLabel(),
                view.getHellEditorStatusLabel(),
                referenceService,
                mapPreviewChooser,
                () -> referenceData,
                () -> state.getBsdxOverlaySession().get(),
                () -> state.getCharset().get()
        );
        mapPreviewController.setup();
        stageDetailController = new HellStageDetailController(
                view.getHellStageTitleField(),
                view.getHellStageScriptLabel(),
                view.getHellStageLayerLabel(),
                view.getHellStageMapField(),
                view.getHellStageHellLevelField(),
                view.getHellStageShopField(),
                view.getHellStagePortraitField(),
                view.getHellStageBalloonField(),
                view.getHellStageDescriptionArea(),
                enemyTypeFields,
                enemyCountFields,
                view.getOpenSelectedHellScriptButton(),
                view.getSaveHellMetadataButton(),
                view.getAppendHellStageButton(),
                view.getHellAppendBlueprintLabel(),
                view.getHellAppendTitleField(),
                view.getHellAppendScriptHintLabel(),
                mekaLookupController,
                mapPreviewController,
                () -> stageTreeController.getStageCount(),
                this::updateEditorStatusBar
        );
        stageTreeController = new HellStageTreeController(
                view.getHellStageTree(),
                this::onStageSelectionApplied,
                this::confirmCanSwitchToStage
        );
        stageTreeController.setup();
        embeddedScriptController = new HellEmbeddedScriptController(
                view.getHellScriptEditorHost(),
                view.getHellScriptEditorPlaceholderLabel(),
                view.getHellEditorStatusLabel(),
                text -> view.getLogArea().appendText(text + System.lineSeparator()),
                () -> state.getBsdxOverlaySession().get(),
                session -> state.getBsdxOverlaySession().set(session),
                () -> state.getCharset().get(),
                this::refreshAfterEmbeddedCompile,
                () -> view.getRoot().getScene() == null ? null : view.getRoot().getScene().getWindow(),
                this::showInfo
        );
        embeddedScriptController.clear("Select a stage to edit pseudo code here.");
        stageDetailController.showStageDetails(null);

        view.getOpenSelectedHellScriptButton().setOnAction(event -> reloadCurrentSelectedStageScript());
        view.getSaveHellMetadataButton().setOnAction(event -> saveSelectedStageMetadata());
        view.getAppendHellStageButton().setOnAction(event -> appendSelectedStage());
        view.getPackageHellModButton().setOnAction(event -> packageModDirectory());
        view.getHellMekaLookupFilterField().textProperty().addListener((obs, oldValue, newValue) -> mekaLookupController.refreshMekaLookupFilter());

    }

    @Override
    public MainViewMode mode() {
        return MainViewMode.HELL_SCRIPT_EDITOR;
    }

    @Override
    public Node content() {
        return view.getHellScriptEditorPane();
    }

    @Override
    public void activate() {
        reloadEditorData();
    }

    @Override
    public void deactivate() {
        // MainModeManager restores workspace status through ModeTreeController.
    }

    @Override
    public boolean canLeave(String reason) {
        return embeddedScriptController.canLeaveCurrentScript(reason);
    }

    /**
     * 树选中确认后的回调入口。
     */
    private void onStageSelectionApplied(HellStageDescriptor stage, Boolean forceScriptReload) {
        stageDetailController.showStageDetails(stage);
        embeddedScriptController.syncToStage(stage, forceScriptReload);
    }

    /**
     * 刷新 Hell 编辑模式数据，并默认回到第一条关卡。
     */
    private void reloadEditorData() {
        reloadEditorData(null);
    }

    /**
     * 刷新 Hell 编辑模式数据，并尽量保留指定索引的选中态。
     */
    private void reloadEditorData(Integer preferredIndex) {
        Path inputDirectory = state.getInputDirectory().get();
        if (inputDirectory == null || !Files.isDirectory(inputDirectory)) {
            clearEditorState("No BSDX resource directory selected.");
            return;
        }

        try {
            BsdxOverlayResourceSession session = BsdxOverlayResourceSession.open(inputDirectory);
            state.getBsdxOverlaySession().set(session);
            referenceData = loadReferenceDataSafely(session);
            mekaLookupController.setMekaEntries(referenceData.getMekaEntries());

            List<HellStageDescriptor> stages = loader.loadStages(session, state.getCharset().get());
            stageTreeController.populateStageTree(stages, preferredIndex);
            view.getHellSessionRootLabel().setText("Root: " + session.getRootDirectory());
            view.getHellSessionModLabel().setText("Mod: " + session.getModDirectory());
            view.getHellEditorStatusLabel().setText("Loaded " + stages.size() + " Hell stage entries.");
            updateEditorStatusBar(stages.size(), null);
        } catch (Exception ex) {
            clearEditorState("Failed to load Hell resources: " + ex.getMessage());
        }
    }

    /**
     * 加载机体与地图引用表。
     *
     * <p>引用数据缺失时，关卡树正常显示，速查和预览区域显示空结果。
     */
    private HellEditorReferenceData loadReferenceDataSafely(BsdxOverlayResourceSession session) {
        try {
            return referenceService.loadReferenceData(session, state.getCharset().get());
        } catch (Exception ex) {
            view.getHellMekaLookupHintLabel().setText("Reference data unavailable: " + ex.getMessage());
            return HellEditorReferenceData.builder()
                    .mekaEntries(List.of())
                    .mekaByIndex(java.util.Map.of())
                    .mapEntries(List.of())
                    .mapById(java.util.Map.of())
                    .build();
        }
    }

    /**
     * 把当前主界面的脚本编辑区切换为选中关卡的脚本。
     */
    private void reloadCurrentSelectedStageScript() {
        TreeItem<HellStageDescriptor> selected = stageTreeController.getSelectedItem();
        if (!embeddedScriptController.canLeaveCurrentScript("reload the current script from disk")) {
            return;
        }
        stageTreeController.applyConfirmedSelection(selected, true);
    }

    /**
     * 把当前右侧元数据草稿写回 `mod/HellConfig.dat`。
     */
    private void saveSelectedStageMetadata() {
        HellStageDescriptor stage = stageTreeController.getSelectedStage();
        if (stage == null || stage.getIndex() < 0) {
            return;
        }

        BsdxOverlayResourceSession session = state.getBsdxOverlaySession().get();
        if (session == null) {
            showInfo("Hell Script Editor", "No active BSDX resource session.");
            return;
        }

        try {
            HellStageMetadataDraft draft = stageDetailController.buildMetadataDraft(stage);
            BsdxOverlayResourceSession refreshed = editorService.saveMetadata(session, state.getCharset().get(), draft);
            state.getBsdxOverlaySession().set(refreshed);
            reloadEditorData(stage.getIndex());
            view.getHellEditorStatusLabel().setText("Saved metadata to mod/HellConfig.dat for stage " + stage.getIndex() + ".");
        } catch (Exception ex) {
            showInfo("Hell Script Editor", "Failed to save stage metadata: " + ex.getMessage());
        }
    }

    /**
     * 以当前选中的关卡作为蓝本，执行“追加关卡”主流程。
     */
    private void appendSelectedStage() {
        HellStageDescriptor stage = stageTreeController.getSelectedStage();
        if (stage == null || stage.getIndex() < 0) {
            return;
        }

        BsdxOverlayResourceSession session = state.getBsdxOverlaySession().get();
        if (session == null) {
            showInfo("Hell Script Editor", "No active BSDX resource session.");
            return;
        }

        try {
            String targetTitle = stageDetailController.getAppendTitleText();
            HellScriptEditorService.DuplicateStageResult result = editorService.duplicateStage(
                    session,
                    state.getCharset().get(),
                    stage,
                    targetTitle
            );
            state.getBsdxOverlaySession().set(result.session());
            reloadEditorData(result.stageIndex());
            view.getHellEditorStatusLabel().setText(
                    "Appended a new stage from blueprint " + stage.getIndex() + " into " + result.scriptFileName() + "."
            );
        } catch (Exception ex) {
            showInfo("Hell Script Editor", "Failed to append stage: " + ex.getMessage());
        }
    }

    /**
     * 内嵌脚本编辑器完成回编后的刷新入口。
     *
     * <p>这里刷新覆盖会话与关卡来源层显示，
     * 但不会重新创建相同脚本的编辑器实例。
     */
    private void refreshAfterEmbeddedCompile() {
        Platform.runLater(() -> {
            HellStageDescriptor selected = stageTreeController.getSelectedStage();
            Integer preferredIndex = selected == null ? null : selected.getIndex();
            reloadEditorData(preferredIndex);
        });
    }

    /**
     * 判断切换到指定关卡前是否允许离开当前脚本。
     */
    private boolean confirmCanSwitchToStage(HellStageDescriptor nextStage) {
        return embeddedScriptController.canSwitchToStage(nextStage);
    }

    /**
     * 打包当前 `mod` 目录为 `Update4.pac`。
     *
     * <p>打包前先确认当前脚本没有未回编的脏状态。
     * 只要当前脚本仍然脏，就不会把伪代码直接打包进去。
     */
    private void packageModDirectory() {
        BsdxOverlayResourceSession session = state.getBsdxOverlaySession().get();
        if (session == null) {
            showInfo("Hell Script Editor", "No active BSDX resource session.");
            return;
        }
        if (!embeddedScriptController.canLeaveCurrentScript("package the current mod directory")) {
            return;
        }

        try {
            Path outputPac = modPackService.packModDirectory(
                    session.getModDirectory(),
                    UPDATE4_PAC_FILE_NAME,
                    DEFAULT_PAC_COMPRESS_MODE
            );
            view.getHellEditorStatusLabel().setText("Packed mod directory into " + outputPac + ".");
        } catch (Exception ex) {
            showInfo("Hell Script Editor", "Failed to pack mod directory: " + ex.getMessage());
        }
    }

    /**
     * 清空编辑模式状态。
     */
    private void clearEditorState(String status) {
        referenceData = null;
        mekaLookupController.clearMekaEntries();
        view.getHellSessionRootLabel().setText("Root: -");
        view.getHellSessionModLabel().setText("Mod: -");
        view.getHellEditorStatusLabel().setText(status);
        stageTreeController.clear();
        embeddedScriptController.clear("Select a stage to edit pseudo code here.");
        stageDetailController.showStageDetails(null);
        updateEditorStatusBar(0, null);
    }

    /**
     * 同步 Hell 编辑模式顶部与底部状态栏。
     */
    private void updateEditorStatusBar(int stageCount, HellStageDescriptor selectedStage) {
        String summary = HellStageStatusFormatter.formatEditorSummary(stageCount, selectedStage);
        view.getTreeSummaryLabel().setText(summary);
        view.getStatusLabel().setText(summary);
    }

    /**
     * 统一的信息提示框。
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        if (view.getRoot().getScene() != null) {
            alert.initOwner(view.getRoot().getScene().getWindow());
        }
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    /**
     * 暴露内嵌脚本编辑器控制器，供测试验证自动切换和脏状态提示。
     */
    public HellEmbeddedScriptController getEmbeddedScriptController() {
        return embeddedScriptController;
    }

    /**
     * 测试用覆盖打包服务。
     */
    public void setModPackServiceForTest(HellModPackService modPackService) {
        this.modPackService = modPackService == null ? new HellModPackService() : modPackService;
    }
}
