package com.giga.nexas.controller;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.EngineType;
import com.giga.nexas.controller.model.HellEditorReferenceData;
import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.HellStageMetadataDraft;
import com.giga.nexas.controller.model.MainViewMode;
import com.giga.nexas.controller.model.MapLookupEntry;
import com.giga.nexas.controller.model.MapPreviewDescriptor;
import com.giga.nexas.controller.model.MekaLookupEntry;
import com.giga.nexas.controller.model.WorkspaceState;
import com.giga.nexas.controller.support.HellConfigLayout;
import com.giga.nexas.controller.support.HellEditorReferenceService;
import com.giga.nexas.controller.support.HellModPackService;
import com.giga.nexas.controller.support.HellScriptDataLoader;
import com.giga.nexas.controller.support.HellScriptEditorService;
import com.giga.nexas.controller.support.HellStageStatusFormatter;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import lombok.RequiredArgsConstructor;

import java.awt.image.BufferedImage;
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
public class HellScriptEditorModeController {

    /**
     * 地图预览图在主界面里的固定显示高度。
     *
     * <p>当前预览图资源本身尺寸差异很大，
     * 主界面统一按这个高度等比放大显示，不再额外限制宽度。
     */
    private static final double MAP_PREVIEW_FIXED_HEIGHT = 280.0;
    private static final String UPDATE4_PAC_FILE_NAME = "Update4.pac";
    private static final String DEFAULT_PAC_COMPRESS_MODE = "4";

    private final MainViewController view;
    private final WorkspaceState state;
    private final HellScriptDataLoader loader = new HellScriptDataLoader();
    private final HellScriptEditorService editorService = new HellScriptEditorService();
    private final HellEditorReferenceService referenceService = new HellEditorReferenceService();
    private HellModPackService modPackService = new HellModPackService();

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
     * 当前正在编辑的敌机类型输入框。
     *
     * <p>机体速查表双击填值时，值写入这个输入框。
     */
    private TextField activeEnemyTypeField;

    /**
     * 主界面内嵌的脚本编辑器句柄。
     */
    private BinPseudoEditorController.EmbeddedEditorHandle embeddedEditorHandle;

    /**
     * 当前已经确认生效的树选中项。
     *
     * <p>树切换时如果用户点击 `Cancel`，控制器会把选中态恢复到这里。
     */
    private TreeItem<HellStageDescriptor> lastConfirmedStageSelection;

    /**
     * 临时屏蔽树选中回调。
     *
     * <p>恢复旧选中或程序化选中新项时，这个标记阻断回调再次进入，
     * 从而避免“回滚选中 -> 监听器再次触发 -> 再次弹框”的死循环和闪烁。
     */
    private boolean suppressStageSelectionListener;

    /**
     * 临时屏蔽输入目录回滚监听。
     */
    private boolean suppressInputDirectoryRollback;

    /**
     * 临时屏蔽引擎切换回滚监听。
     */
    private boolean suppressEngineRollback;

    /**
     * 测试可覆盖的离开当前脚本确认器。
     */
    private LeaveScriptConfirmationHandler leaveScriptConfirmationHandler = this::showLeaveScriptConfirmation;

    /**
     * 机体速查表的底层可变数据源。
     */
    private final ObservableList<MekaLookupEntry> mekaLookupItems = FXCollections.observableArrayList();

    /**
     * 机体速查过滤视图。
     */
    private final FilteredList<MekaLookupEntry> filteredMekaEntries = new FilteredList<>(mekaLookupItems, entry -> true);

    /**
     * 机体速查排序视图。
     */
    private final SortedList<MekaLookupEntry> sortedMekaEntries = new SortedList<>(filteredMekaEntries);

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

        configureMekaLookupTable();
        bindEnemyTypeFields();
        configureMapPreviewView();
        clearEmbeddedScriptEditor("Select a stage to edit pseudo code here.");
        showStageDetails(null);
        updateModeView();
        updateModeButton();

        view.getScriptEditorModeButton().setOnAction(event -> toggleMode());
        view.getOpenSelectedHellScriptButton().setOnAction(event -> reloadCurrentSelectedStageScript());
        view.getSaveHellMetadataButton().setOnAction(event -> saveSelectedStageMetadata());
        view.getAppendHellStageButton().setOnAction(event -> appendSelectedStage());
        view.getPackageHellModButton().setOnAction(event -> packageModDirectory());
        view.getHellStageTree().getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> {
            handleStageTreeSelectionChanged(oldItem, newItem);
        });
        view.getHellMekaLookupFilterField().textProperty().addListener((obs, oldValue, newValue) -> refreshMekaLookupFilter());

        state.getMainViewMode().addListener((obs, oldMode, newMode) -> {
            updateModeView();
            updateModeButton();
            if (newMode == MainViewMode.HELL_SCRIPT_EDITOR) {
                reloadEditorData();
            } else {
                restoreWorkspaceStatusBar();
            }
        });

        state.getInputDirectory().addListener((obs, oldPath, newPath) -> {
            if (suppressInputDirectoryRollback) {
                return;
            }
            if (state.getMainViewMode().get() == MainViewMode.HELL_SCRIPT_EDITOR) {
                if (!confirmCanLeaveCurrentScript("reload the BSDX resource directory")) {
                    restoreInputDirectory(oldPath);
                    return;
                }
                reloadEditorData();
            } else {
                updateModeButton();
            }
        });

        state.getEngineType().addListener((obs, oldEngine, newEngine) -> {
            if (suppressEngineRollback) {
                return;
            }
            if (state.getMainViewMode().get() == MainViewMode.HELL_SCRIPT_EDITOR && newEngine != EngineType.BSDX) {
                if (!confirmCanLeaveCurrentScript("leave Hell Script Editor because the engine changes")) {
                    restoreEngine(oldEngine);
                    return;
                }
                state.getMainViewMode().set(MainViewMode.WORKSPACE);
            }
            updateModeButton();
        });
    }

    /**
     * 在工作区模式与 Hell 编辑模式之间切换。
     */
    private void toggleMode() {
        if (state.getMainViewMode().get() == MainViewMode.HELL_SCRIPT_EDITOR) {
            if (!confirmCanLeaveCurrentScript("return to the workspace")) {
                return;
            }
            state.getMainViewMode().set(MainViewMode.WORKSPACE);
            return;
        }

        if (state.getEngineType().get() != EngineType.BSDX) {
            showInfo("Hell Script Editor", "Hell Script Editor only works in BSDX mode.");
            return;
        }

        Path inputDirectory = state.getInputDirectory().get();
        if (inputDirectory == null || !Files.isDirectory(inputDirectory)) {
            showInfo("Hell Script Editor", "Select a BSDX resource directory first.");
            return;
        }

        state.getMainViewMode().set(MainViewMode.HELL_SCRIPT_EDITOR);
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
            mekaLookupItems.setAll(referenceData.getMekaEntries());
            refreshMekaLookupFilter();

            List<HellStageDescriptor> stages = loader.loadStages(session, state.getCharset().get());
            populateStageTree(stages, preferredIndex);
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
     * 把 Hell 关卡列表装进树视图。
     */
    private void populateStageTree(List<HellStageDescriptor> stages, Integer preferredIndex) {
        TreeItem<HellStageDescriptor> root = new TreeItem<>(HellStageDescriptor.builder()
                .index(-1)
                .title("Hell stages")
                .build());
        root.setExpanded(true);

        for (HellStageDescriptor stage : stages) {
            root.getChildren().add(new TreeItem<>(stage));
        }

        view.getHellStageTree().setShowRoot(false);
        view.getHellStageTree().setRoot(root);
        if (!root.getChildren().isEmpty()) {
            int selectedChildIndex = preferredIndex == null
                    ? 0
                    : Math.max(0, Math.min(preferredIndex, root.getChildren().size() - 1));
            Platform.runLater(() -> applyConfirmedStageSelection(root.getChildren().get(selectedChildIndex), true));
        } else {
            lastConfirmedStageSelection = null;
            showStageDetails(null);
        }
    }

    /**
     * 显示当前选中的关卡详情、机体联动和地图预览。
     */
    private void showStageDetails(HellStageDescriptor stage) {
        if (stage == null || stage.getIndex() < 0) {
            view.getHellStageTitleField().setText("");
            view.getHellStageScriptLabel().setText("-");
            view.getHellStageLayerLabel().setText("-");
            view.getHellStageMapField().setText("");
            view.getHellMapNameLabel().setText("-");
            view.getHellMapResourceLabel().setText("-");
            view.getHellStageHellLevelField().setText("");
            view.getHellStageShopField().setText("");
            view.getHellStagePortraitField().setText("");
            view.getHellStageBalloonField().setText("");
            view.getHellStageDescriptionArea().setText("");
            fillSlotFields(enemyTypeFields, List.of());
            fillSlotFields(enemyCountFields, List.of());
            clearEnemyTypeNames();
            clearMapPreview("Select a stage to inspect the current map.");
            clearAppendStageArea();
            view.getOpenSelectedHellScriptButton().setDisable(true);
            view.getSaveHellMetadataButton().setDisable(true);
            view.getAppendHellStageButton().setDisable(true);
            setMetadataEditorDisabled(true);
            updateEditorStatusBar(currentStageCount(), null);
            return;
        }

        HellStageMetadataDraft draft = HellStageMetadataDraft.fromStage(stage);
        setMetadataEditorDisabled(false);
        view.getHellStageTitleField().setText(nullToEmpty(draft.getTitle()));
        view.getHellStageScriptLabel().setText(stage.getScriptFileName() == null ? "-" : stage.getScriptFileName());
        view.getHellStageLayerLabel().setText(formatLayer(stage));
        view.getHellStageMapField().setText(formatInteger(draft.getMapId()));
        view.getHellStageHellLevelField().setText(formatInteger(draft.getHellLevel()));
        view.getHellStageShopField().setText(formatInteger(draft.getShopId()));
        view.getHellStagePortraitField().setText(formatInteger(draft.getPortraitId()));
        view.getHellStageBalloonField().setText(formatInteger(draft.getBalloonStyleId()));
        view.getHellStageDescriptionArea().setText(nullToEmpty(draft.getDescription()));
        fillSlotFields(enemyTypeFields, draft.getEnemyTypes());
        fillSlotFields(enemyCountFields, draft.getEnemyCounts());
        refreshAllEnemyTypeNames();
        refreshMapSection(stage.getMapId());
        refreshAppendStageArea(stage);

        view.getOpenSelectedHellScriptButton().setDisable(stage.getScriptFileName() == null);
        view.getSaveHellMetadataButton().setDisable(false);
        view.getAppendHellStageButton().setDisable(stage.getScriptFileName() == null);
        updateEditorStatusBar(currentStageCount(), stage);
    }

    /**
     * 处理关卡树选中变化。
     *
     * <p>只有用户确认离开当前脚本后，新的树选中才真正生效。
     * 取消时会恢复到上一次确认过的选中项。
     */
    private void handleStageTreeSelectionChanged(
            TreeItem<HellStageDescriptor> oldItem,
            TreeItem<HellStageDescriptor> newItem
    ) {
        if (suppressStageSelectionListener) {
            return;
        }
        if (!confirmCanSwitchToStage(newItem == null ? null : newItem.getValue())) {
            restoreConfirmedStageSelection(oldItem);
            return;
        }
        applyConfirmedStageSelection(newItem, false);
    }

    /**
     * 把某个树选中项作为真正生效的新上下文。
     *
     * <p>这里会同步三件事：
     * 1. 稳定写回树选中
     * 2. 刷新右侧元数据
     * 3. 自动切换右侧脚本编辑器
     */
    private void applyConfirmedStageSelection(TreeItem<HellStageDescriptor> item, boolean forceScriptReload) {
        suppressStageSelectionListener = true;
        try {
            view.getHellStageTree().getSelectionModel().select(item);
        } finally {
            suppressStageSelectionListener = false;
        }
        lastConfirmedStageSelection = item;

        HellStageDescriptor stage = item == null ? null : item.getValue();
        showStageDetails(stage);
        syncEmbeddedScriptToStage(stage, forceScriptReload);
    }

    /**
     * 取消切换时恢复到上一次确认过的树选中。
     *
     * <p>恢复动作通过 `suppressStageSelectionListener` 包裹，
     * 所以不会触发第二次确认框，也不会出现树选中闪烁死循环。
     */
    private void restoreConfirmedStageSelection(TreeItem<HellStageDescriptor> fallbackItem) {
        TreeItem<HellStageDescriptor> target = lastConfirmedStageSelection != null ? lastConfirmedStageSelection : fallbackItem;
        suppressStageSelectionListener = true;
        try {
            view.getHellStageTree().getSelectionModel().select(target);
        } finally {
            suppressStageSelectionListener = false;
        }
    }

    /**
     * 把右侧内嵌脚本编辑器同步到指定关卡。
     *
     * <p>树选中只读取当前命中的脚本，
     * 不会为了“浏览”先把脚本复制进 `mod`。
     * 真正回编时，编辑器再按需申请可写的 `mod` 路径。
     */
    private void syncEmbeddedScriptToStage(HellStageDescriptor stage, boolean forceReload) {
        if (stage == null || stage.getScriptFileName() == null || stage.getScriptFileName().isBlank()) {
            clearEmbeddedScriptEditor("Selected stage has no script file.");
            return;
        }

        if (embeddedEditorHandle != null
                && stage.getScriptFileName().equalsIgnoreCase(embeddedEditorHandle.controller().getDocumentLogicalName())) {
            if (forceReload) {
                embeddedEditorHandle.controller().discardUnsavedChangesByReload();
                view.getHellEditorStatusLabel().setText("Reloaded " + stage.getScriptFileName() + " from the current overlay layer.");
            }
            return;
        }

        try {
            embeddedEditorHandle = BinPseudoEditorController.createEmbeddedEditor(
                    stage.getScriptFileName(),
                    () -> resolveScriptReadPath(stage.getScriptFileName()),
                    () -> resolveScriptCompileOutputPath(stage.getScriptFileName()),
                    state.getCharset().get(),
                    text -> view.getLogArea().appendText(text + System.lineSeparator()),
                    this::refreshAfterEmbeddedCompile
            );
            view.getHellScriptEditorHost().getChildren().setAll(embeddedEditorHandle.root());
            view.getHellEditorStatusLabel().setText("Loaded " + stage.getScriptFileName() + " into the embedded script editor.");
        } catch (Exception ex) {
            clearEmbeddedScriptEditor("Failed to load current stage script.");
            showInfo("Hell Script Editor", "Failed to load stage script: " + ex.getMessage());
        }
    }

    /**
     * 把当前主界面的脚本编辑区切换为选中关卡的脚本。
     */
    private void reloadCurrentSelectedStageScript() {
        TreeItem<HellStageDescriptor> selected = view.getHellStageTree().getSelectionModel().getSelectedItem();
        if (!confirmCanLeaveCurrentScript("reload the current script from disk")) {
            return;
        }
        applyConfirmedStageSelection(selected, true);
    }

    /**
     * 把当前右侧元数据草稿写回 `mod/HellConfig.dat`。
     */
    private void saveSelectedStageMetadata() {
        TreeItem<HellStageDescriptor> selected = view.getHellStageTree().getSelectionModel().getSelectedItem();
        HellStageDescriptor stage = selected == null ? null : selected.getValue();
        if (stage == null || stage.getIndex() < 0) {
            return;
        }

        BsdxOverlayResourceSession session = state.getBsdxOverlaySession().get();
        if (session == null) {
            showInfo("Hell Script Editor", "No active BSDX resource session.");
            return;
        }

        try {
            HellStageMetadataDraft draft = buildMetadataDraft(stage);
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
        TreeItem<HellStageDescriptor> selected = view.getHellStageTree().getSelectionModel().getSelectedItem();
        HellStageDescriptor stage = selected == null ? null : selected.getValue();
        if (stage == null || stage.getIndex() < 0) {
            return;
        }

        BsdxOverlayResourceSession session = state.getBsdxOverlaySession().get();
        if (session == null) {
            showInfo("Hell Script Editor", "No active BSDX resource session.");
            return;
        }

        try {
            String targetTitle = view.getHellAppendTitleField().getText();
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
     * 构建机体速查表，并把双击填值逻辑接上。
     */
    private void configureMekaLookupTable() {
        TableColumn<MekaLookupEntry, Number> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getMekaIndex()));
        idColumn.setPrefWidth(70);

        TableColumn<MekaLookupEntry, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getDisplayTitle()));
        nameColumn.setPrefWidth(180);

        TableColumn<MekaLookupEntry, String> codeColumn = new TableColumn<>("Code");
        codeColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getMekaCodeName()));
        codeColumn.setPrefWidth(140);

        TableColumn<MekaLookupEntry, String> fileColumn = new TableColumn<>(".mek");
        fileColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getMekFileName()));
        fileColumn.setPrefWidth(160);

        view.getHellMekaLookupTable().getColumns().setAll(idColumn, nameColumn, codeColumn, fileColumn);
        view.getHellMekaLookupTable().setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        view.getHellMekaLookupTable().setPlaceholder(new Label("No meka reference data loaded."));
        view.getHellMekaLookupTable().getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        sortedMekaEntries.comparatorProperty().bind(view.getHellMekaLookupTable().comparatorProperty());
        view.getHellMekaLookupTable().setItems(sortedMekaEntries);
        view.getHellMekaLookupTable().setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                fillFocusedEnemyTypeFromLookup();
            }
        });
    }

    /**
     * 把敌机类型输入框和速查表联动起来。
     */
    private void bindEnemyTypeFields() {
        for (int i = 0; i < enemyTypeFields.size(); i++) {
            final int slotIndex = i;
            TextField field = enemyTypeFields.get(i);
            field.textProperty().addListener((obs, oldValue, newValue) -> refreshEnemyTypeName(slotIndex));
            field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    activeEnemyTypeField = field;
                    updateMekaLookupHint("Focused enemy slot " + slotIndex + ". Double-click a meka row to fill this slot.");
                    syncLookupSelectionToEnemyTypeField(field);
                }
            });
        }
        updateMekaLookupHint("Focus an enemy type field, then double-click a meka row to fill it.");
    }

    /**
     * 根据搜索框刷新机体速查过滤。
     */
    private void refreshMekaLookupFilter() {
        String filter = view.getHellMekaLookupFilterField().getText();
        String keyword = filter == null ? "" : filter.trim().toLowerCase();
        filteredMekaEntries.setPredicate(entry -> keyword.isEmpty() || entry.getSearchText().contains(keyword));
    }

    /**
     * 用速查表当前选中机体回写到正在编辑的敌机类型输入框。
     */
    private void fillFocusedEnemyTypeFromLookup() {
        MekaLookupEntry selectedEntry = view.getHellMekaLookupTable().getSelectionModel().getSelectedItem();
        if (selectedEntry == null || activeEnemyTypeField == null || activeEnemyTypeField.isDisabled()) {
            return;
        }
        activeEnemyTypeField.setText(Integer.toString(selectedEntry.getMekaIndex()));
        activeEnemyTypeField.requestFocus();
        activeEnemyTypeField.positionCaret(activeEnemyTypeField.getText().length());
    }

    /**
     * 当前敌机编号改变后，立即把旁边的机体名称刷新出来。
     */
    private void refreshEnemyTypeName(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= enemyTypeFields.size()) {
            return;
        }
        TextField field = enemyTypeFields.get(slotIndex);
        Label label = enemyTypeNameLabels.get(slotIndex);
        Integer mekaIndex = parseOptionalInteger(field.getText());
        MekaLookupEntry entry = mekaIndex == null || referenceData == null ? null : referenceData.getMekaByIndex().get(mekaIndex);
        label.setText(entry == null ? "-" : formatMekaEntryInline(entry));
        if (field == activeEnemyTypeField) {
            syncLookupSelectionToEnemyTypeField(field);
        }
    }

    /**
     * 刷新全部敌机名称标签。
     */
    private void refreshAllEnemyTypeNames() {
        for (int i = 0; i < enemyTypeFields.size(); i++) {
            refreshEnemyTypeName(i);
        }
    }

    /**
     * 清空全部敌机名称标签。
     */
    private void clearEnemyTypeNames() {
        for (Label label : enemyTypeNameLabels) {
            label.setText("-");
        }
    }

    /**
     * 根据当前敌机输入框里的值，同步速查表选中态。
     */
    private void syncLookupSelectionToEnemyTypeField(TextField field) {
        Integer mekaIndex = parseOptionalInteger(field.getText());
        if (mekaIndex == null) {
            view.getHellMekaLookupTable().getSelectionModel().clearSelection();
            return;
        }

        for (int i = 0; i < sortedMekaEntries.size(); i++) {
            MekaLookupEntry entry = sortedMekaEntries.get(i);
            if (entry.getMekaIndex() == mekaIndex) {
                view.getHellMekaLookupTable().getSelectionModel().select(i);
                view.getHellMekaLookupTable().scrollTo(i);
                return;
            }
        }
        view.getHellMekaLookupTable().getSelectionModel().clearSelection();
    }

    /**
     * 刷新地图信息和预览区。
     */
    private void refreshMapSection(Integer mapId) {
        MapLookupEntry mapEntry = mapId == null || referenceData == null ? null : referenceData.getMapById().get(mapId);
        view.getHellMapNameLabel().setText(mapEntry == null ? "-" : mapEntry.getGroupName());
        view.getHellMapResourceLabel().setText(mapEntry == null ? "-" : mapEntry.getGroupResourceName());

        if (mapEntry == null) {
            clearMapPreview("Map index not found in MapGroup.grp.");
            return;
        }

        try {
            BsdxOverlayResourceSession session = state.getBsdxOverlaySession().get();
            MapPreviewDescriptor descriptor = referenceService.resolveMapPreview(session, state.getCharset().get(), mapEntry);
            BufferedImage preview = descriptor == null
                    ? null
                    : referenceService.renderMapPreviewImage(session, state.getCharset().get(), descriptor);
            if (descriptor == null || preview == null) {
                clearMapPreview(descriptor == null ? "Map preview unavailable." : descriptor.getStatusText());
                return;
            }

            view.getHellMapPreviewImageView().setImage(toFxImage(preview));
            view.getHellMapPreviewImageView().setManaged(true);
            view.getHellMapPreviewImageView().setVisible(true);
            view.getHellMapPreviewStatusLabel().setManaged(false);
            view.getHellMapPreviewStatusLabel().setVisible(false);
            view.getHellMapPreviewStatusLabel().setText(descriptor.getStatusText());
        } catch (Exception ex) {
            clearMapPreview("Failed to load map preview: " + ex.getMessage());
        }
    }

    /**
     * 清空地图预览，只保留状态文本。
     */
    private void clearMapPreview(String statusText) {
        view.getHellMapPreviewImageView().setImage(null);
        view.getHellMapPreviewImageView().setManaged(false);
        view.getHellMapPreviewImageView().setVisible(false);
        view.getHellMapPreviewStatusLabel().setManaged(true);
        view.getHellMapPreviewStatusLabel().setVisible(true);
        view.getHellMapPreviewStatusLabel().setText(statusText);
    }

    /**
     * 读取当前脚本文件在覆盖会话里的命中路径。
     */
    private Path resolveScriptReadPath(String scriptFileName) {
        BsdxOverlayResourceSession session = state.getBsdxOverlaySession().get();
        if (session == null) {
            throw new IllegalStateException("No active BSDX overlay session.");
        }
        return session.resolveReadPath(scriptFileName)
                .orElseThrow(() -> new IllegalStateException("Missing script file: " + scriptFileName));
    }

    /**
     * 在真正回编时为脚本申请可写的 `mod` 路径。
     */
    private Path resolveScriptCompileOutputPath(String scriptFileName) {
        BsdxOverlayResourceSession session = state.getBsdxOverlaySession().get();
        if (session == null) {
            throw new IllegalStateException("No active BSDX overlay session.");
        }
        try {
            Path writablePath = session.prepareWritablePath(scriptFileName);
            BsdxOverlayResourceSession refreshed = session.refresh();
            state.getBsdxOverlaySession().set(refreshed);
            return writablePath;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to prepare writable script path for " + scriptFileName, ex);
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
            TreeItem<HellStageDescriptor> selected = view.getHellStageTree().getSelectionModel().getSelectedItem();
            Integer preferredIndex = selected == null || selected.getValue() == null ? null : selected.getValue().getIndex();
            reloadEditorData(preferredIndex);
        });
    }

    /**
     * 判断切换到指定关卡前是否允许离开当前脚本。
     */
    private boolean confirmCanSwitchToStage(HellStageDescriptor nextStage) {
        if (nextStage == null || nextStage.getScriptFileName() == null || nextStage.getScriptFileName().isBlank()) {
            return confirmCanLeaveCurrentScript("switch to a stage without script content");
        }
        if (embeddedEditorHandle == null) {
            return true;
        }
        String currentScriptFileName = embeddedEditorHandle.controller().getDocumentLogicalName();
        if (currentScriptFileName != null && currentScriptFileName.equalsIgnoreCase(nextStage.getScriptFileName())) {
            return true;
        }
        return confirmCanLeaveCurrentScript("switch to " + nextStage.getScriptFileName());
    }

    /**
     * 在离开当前脚本前统一处理脏状态确认。
     */
    private boolean confirmCanLeaveCurrentScript(String actionDescription) {
        if (embeddedEditorHandle == null || !embeddedEditorHandle.controller().hasUnsavedChanges()) {
            return true;
        }

        LeaveScriptDecision decision = leaveScriptConfirmationHandler.confirm(
                embeddedEditorHandle.controller().getDocumentLogicalName(),
                actionDescription
        );
        if (decision == LeaveScriptDecision.COMPILE) {
            return embeddedEditorHandle.controller().compileCurrentDocument();
        }
        if (decision == LeaveScriptDecision.DISCARD) {
            return true;
        }
        return false;
    }

    /**
     * 统一弹出离开当前脚本确认框。
     */
    private LeaveScriptDecision showLeaveScriptConfirmation(String scriptFileName, String actionDescription) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (view.getRoot().getScene() != null) {
            alert.initOwner(view.getRoot().getScene().getWindow());
        }

        ButtonType compileButton = new ButtonType("Compile Back");
        ButtonType discardButton = new ButtonType("Discard");
        ButtonType cancelButton = ButtonType.CANCEL;

        alert.getButtonTypes().setAll(compileButton, discardButton, cancelButton);
        alert.setTitle("Unsaved script changes");
        alert.setHeaderText("Current script has unsaved pseudo changes.");
        alert.setContentText("Script: " + scriptFileName + "\nNext action: " + actionDescription);

        ButtonType chosen = alert.showAndWait().orElse(cancelButton);
        if (chosen == compileButton) {
            return LeaveScriptDecision.COMPILE;
        }
        if (chosen == discardButton) {
            return LeaveScriptDecision.DISCARD;
        }
        return LeaveScriptDecision.CANCEL;
    }

    /**
     * 回滚输入目录变更。
     */
    private void restoreInputDirectory(Path oldPath) {
        suppressInputDirectoryRollback = true;
        try {
            state.getInputDirectory().set(oldPath);
        } finally {
            suppressInputDirectoryRollback = false;
        }
    }

    /**
     * 回滚引擎切换。
     */
    private void restoreEngine(EngineType oldEngine) {
        suppressEngineRollback = true;
        try {
            state.getEngineType().set(oldEngine);
        } finally {
            suppressEngineRollback = false;
        }
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
        if (!confirmCanLeaveCurrentScript("package the current mod directory")) {
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
     * 统一地图预览图的显示约束。
     *
     * <p>当前逻辑只固定高度并保持等比缩放，
     * 这样不同宽高比的 `T_map*.bmp` 都会按一致高度放大显示。
     */
    private void configureMapPreviewView() {
        view.getHellMapPreviewImageView().setPreserveRatio(true);
        view.getHellMapPreviewImageView().setFitHeight(MAP_PREVIEW_FIXED_HEIGHT);
        view.getHellMapPreviewImageView().setFitWidth(0);
        view.getHellMapPreviewImageView().setSmooth(true);
    }

    /**
     * 刷新追加关卡区，让它围绕当前蓝本工作。
     */
    private void refreshAppendStageArea(HellStageDescriptor stage) {
        String blueprintText = String.format("%03d %s", stage.getIndex(), stage.getTitle());
        view.getHellAppendBlueprintLabel().setText(blueprintText);
        view.getHellAppendTitleField().setDisable(false);
        view.getHellAppendTitleField().setText(nullToEmpty(stage.getTitle()) + " Copy");
        view.getHellAppendScriptHintLabel().setText(
                "A new mod script file will be generated automatically from blueprint "
                        + nullToEmpty(stage.getScriptFileName()) + "."
        );
    }

    /**
     * 清空追加关卡区。
     */
    private void clearAppendStageArea() {
        view.getHellAppendBlueprintLabel().setText("Select a stage first.");
        view.getHellAppendTitleField().setText("");
        view.getHellAppendTitleField().setDisable(true);
        view.getHellAppendScriptHintLabel().setText("A new mod script file name will be generated automatically.");
    }

    /**
     * 把主界面的脚本编辑区恢复成占位状态。
     */
    private void clearEmbeddedScriptEditor(String message) {
        embeddedEditorHandle = null;
        view.getHellScriptEditorPlaceholderLabel().setText(message);
        view.getHellScriptEditorHost().getChildren().setAll(view.getHellScriptEditorPlaceholderLabel());
    }

    /**
     * 把 `BufferedImage` 转成 JavaFX `Image`。
     *
     * <p>像素数据通过 `PixelWriter` 逐点写入。
     */
    private Image toFxImage(BufferedImage image) {
        WritableImage fxImage = new WritableImage(image.getWidth(), image.getHeight());
        PixelWriter writer = fxImage.getPixelWriter();
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                writer.setArgb(x, y, image.getRGB(x, y));
            }
        }
        return fxImage;
    }

    /**
     * 切换可见面板。
     */
    private void updateModeView() {
        boolean hellMode = state.getMainViewMode().get() == MainViewMode.HELL_SCRIPT_EDITOR;
        view.getWorkspacePane().setVisible(!hellMode);
        view.getWorkspacePane().setManaged(!hellMode);
        view.getHellScriptEditorPane().setVisible(hellMode);
        view.getHellScriptEditorPane().setManaged(hellMode);
    }

    /**
     * 更新主界面上的模式切换按钮状态。
     */
    private void updateModeButton() {
        boolean bsdx = state.getEngineType().get() == EngineType.BSDX;
        boolean hasDirectory = state.getInputDirectory().get() != null;
        boolean hellMode = state.getMainViewMode().get() == MainViewMode.HELL_SCRIPT_EDITOR;

        view.getScriptEditorModeButton().setDisable(!bsdx || !hasDirectory);
        view.getScriptEditorModeButton().setText(hellMode ? "Back to Workspace" : "Script Editor");
    }

    /**
     * 清空编辑模式状态。
     */
    private void clearEditorState(String status) {
        referenceData = null;
        mekaLookupItems.clear();
        view.getHellSessionRootLabel().setText("Root: -");
        view.getHellSessionModLabel().setText("Mod: -");
        view.getHellEditorStatusLabel().setText(status);
        view.getHellStageTree().setRoot(new TreeItem<>());
        clearEmbeddedScriptEditor("Select a stage to edit pseudo code here.");
        showStageDetails(null);
        updateEditorStatusBar(0, null);
    }

    /**
     * 从右侧表单构建可写回的元数据草稿。
     */
    private HellStageMetadataDraft buildMetadataDraft(HellStageDescriptor stage) {
        return HellStageMetadataDraft.builder()
                .index(stage.getIndex())
                .title(view.getHellStageTitleField().getText())
                .description(view.getHellStageDescriptionArea().getText())
                .hellLevel(parseRequiredInteger(view.getHellStageHellLevelField(), "Hell level"))
                .mapId(parseRequiredInteger(view.getHellStageMapField(), "Map"))
                .shopId(parseRequiredInteger(view.getHellStageShopField(), "Shop"))
                .portraitId(parseRequiredInteger(view.getHellStagePortraitField(), "Portrait"))
                .balloonStyleId(parseRequiredInteger(view.getHellStageBalloonField(), "Balloon"))
                .enemyTypes(parseSlotValues(enemyTypeFields, "Enemy type"))
                .enemyCounts(parseSlotValues(enemyCountFields, "Enemy param"))
                .build();
    }

    /**
     * 统一控制元数据表单可编辑状态。
     */
    private void setMetadataEditorDisabled(boolean disabled) {
        view.getHellStageTitleField().setDisable(disabled);
        view.getHellStageMapField().setDisable(disabled);
        view.getHellStageHellLevelField().setDisable(disabled);
        view.getHellStageShopField().setDisable(disabled);
        view.getHellStagePortraitField().setDisable(disabled);
        view.getHellStageBalloonField().setDisable(disabled);
        view.getHellStageDescriptionArea().setDisable(disabled);
        for (TextField field : enemyTypeFields) {
            field.setDisable(disabled);
        }
        for (TextField field : enemyCountFields) {
            field.setDisable(disabled);
        }
    }

    /**
     * 把固定槽位的值回填到表单。
     */
    private void fillSlotFields(List<TextField> fields, List<Integer> values) {
        for (int i = 0; i < fields.size(); i++) {
            Integer value = values != null && i < values.size() ? values.get(i) : null;
            fields.get(i).setText(formatInteger(value));
        }
    }

    /**
     * 解析必须填写的整数输入框。
     */
    private Integer parseRequiredInteger(TextField field, String fieldName) {
        Integer value = parseOptionalInteger(field.getText());
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must be an integer.");
        }
        return value;
    }

    /**
     * 解析一个可选整数文本。
     */
    private Integer parseOptionalInteger(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 解析固定 8 槽的敌机类型/附加参数输入。
     */
    private List<Integer> parseSlotValues(List<TextField> fields, String fieldName) {
        if (fields.size() != HellConfigLayout.ENEMY_SLOT_COUNT) {
            throw new IllegalStateException("Unexpected slot field count: " + fields.size());
        }
        return fields.stream()
                .map(field -> parseRequiredInteger(field, fieldName + " slot"))
                .toList();
    }

    /**
     * 把整数渲染成表单文本。
     */
    private String formatInteger(Integer value) {
        return value == null ? "" : Integer.toString(value);
    }

    /**
     * 规避表单出现字面 `null`。
     */
    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    /**
     * 把机体条目压成适合紧邻输入框显示的一行。
     */
    private String formatMekaEntryInline(MekaLookupEntry entry) {
        if (entry == null) {
            return "-";
        }
        String display = entry.getDisplayTitle();
        String code = entry.getMekaCodeName();
        if (code == null || code.isBlank()) {
            return display;
        }
        return display + " [" + code + "]";
    }

    /**
     * 更新速查表提示语。
     */
    private void updateMekaLookupHint(String text) {
        view.getHellMekaLookupHintLabel().setText(text);
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
     * 计算当前关卡树里已加载的条目数量。
     */
    private int currentStageCount() {
        TreeItem<HellStageDescriptor> root = view.getHellStageTree().getRoot();
        return root == null ? 0 : root.getChildren().size();
    }

    /**
     * 从 Hell 编辑模式返回工作区时，恢复原本状态栏。
     */
    private void restoreWorkspaceStatusBar() {
        Object controller = view.getTree().getProperties().get("modeTreeController");
        if (controller instanceof ModeTreeController modeTreeController) {
            modeTreeController.refreshViewState();
        }
    }

    /**
     * 组合展示当前配置层与脚本层来源。
     */
    private String formatLayer(HellStageDescriptor stage) {
        return HellStageStatusFormatter.formatLayerSummary(stage);
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
     * 暴露当前内嵌编辑器句柄，供测试验证自动切换和脏状态提示。
     */
    public BinPseudoEditorController.EmbeddedEditorHandle getEmbeddedEditorHandle() {
        return embeddedEditorHandle;
    }

    /**
     * 测试用覆盖离开脚本确认器。
     */
    public void setLeaveScriptConfirmationHandlerForTest(LeaveScriptConfirmationHandler handler) {
        this.leaveScriptConfirmationHandler = handler == null ? this::showLeaveScriptConfirmation : handler;
    }

    /**
     * 测试用覆盖打包服务。
     */
    public void setModPackServiceForTest(HellModPackService modPackService) {
        this.modPackService = modPackService == null ? new HellModPackService() : modPackService;
    }

    public enum LeaveScriptDecision {
        COMPILE,
        DISCARD,
        CANCEL
    }

    @FunctionalInterface
    public interface LeaveScriptConfirmationHandler {
        LeaveScriptDecision confirm(String scriptFileName, String actionDescription);
    }
}
