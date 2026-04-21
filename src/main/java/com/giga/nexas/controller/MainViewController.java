package com.giga.nexas.controller;

import com.giga.nexas.controller.model.EngineType;
import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.MekaLookupEntry;
import com.giga.nexas.controller.model.WorkspaceState;
import com.giga.nexas.controller.model.WorkspaceTreeNode;
import com.giga.nexas.controller.support.DirectoryScanner;
import com.giga.nexas.service.engine.BinaryEngineFactory;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import lombok.Getter;

@Getter
public class MainViewController {

    @FXML private BorderPane root;
    @FXML private MenuBar menuBar;
    @FXML private TreeView<WorkspaceTreeNode> tree;
    @FXML private Button inputBrowse;
    @FXML private Button outputBrowse;
    @FXML private TextField inputField;
    @FXML private TextField outputField;
    @FXML private TextArea logArea;
    @FXML private Button reloadButton;
    @FXML private Button actionButton;
    @FXML private Button processAllButton;
    @FXML private Button scriptEditorModeButton;
    @FXML private CheckBox alwaysOnTop;
    @FXML private CheckBox lockOutPutPath;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private ComboBox<EngineType> engineSelector;
    @FXML private Label statusLabel;
    @FXML private Label treeSummaryLabel;
    @FXML private TilePane branchGrid;
    @FXML private ScrollPane branchScroll;
    @FXML private SplitPane workspacePane;
    @FXML private BorderPane hellScriptEditorPane;
    @FXML private StackPane modeContentPane;
    @FXML private TreeView<HellStageDescriptor> hellStageTree;
    @FXML private Label hellAppendBlueprintLabel;
    @FXML private TextField hellAppendTitleField;
    @FXML private Label hellAppendScriptHintLabel;
    @FXML private Button appendHellStageButton;
    @FXML private Button packageHellModButton;
    @FXML private Label hellSessionRootLabel;
    @FXML private Label hellSessionModLabel;
    @FXML private Label hellEditorStatusLabel;
    @FXML private TextField hellStageTitleField;
    @FXML private Label hellStageScriptLabel;
    @FXML private Label hellStageLayerLabel;
    @FXML private TextField hellStageMapField;
    @FXML private Label hellMapNameLabel;
    @FXML private Label hellMapResourceLabel;
    @FXML private ImageView hellMapPreviewImageView;
    @FXML private Label hellMapPreviewStatusLabel;
    @FXML private TextField hellStageHellLevelField;
    @FXML private TextField hellStageShopField;
    @FXML private TextField hellStagePortraitField;
    @FXML private TextField hellStageBalloonField;
    @FXML private TextArea hellStageDescriptionArea;
    @FXML private TextField hellEnemyType0Field;
    @FXML private TextField hellEnemyType1Field;
    @FXML private TextField hellEnemyType2Field;
    @FXML private TextField hellEnemyType3Field;
    @FXML private TextField hellEnemyType4Field;
    @FXML private TextField hellEnemyType5Field;
    @FXML private TextField hellEnemyType6Field;
    @FXML private TextField hellEnemyType7Field;
    @FXML private Label hellEnemyType0NameLabel;
    @FXML private Label hellEnemyType1NameLabel;
    @FXML private Label hellEnemyType2NameLabel;
    @FXML private Label hellEnemyType3NameLabel;
    @FXML private Label hellEnemyType4NameLabel;
    @FXML private Label hellEnemyType5NameLabel;
    @FXML private Label hellEnemyType6NameLabel;
    @FXML private Label hellEnemyType7NameLabel;
    @FXML private TextField hellEnemyCount0Field;
    @FXML private TextField hellEnemyCount1Field;
    @FXML private TextField hellEnemyCount2Field;
    @FXML private TextField hellEnemyCount3Field;
    @FXML private TextField hellEnemyCount4Field;
    @FXML private TextField hellEnemyCount5Field;
    @FXML private TextField hellEnemyCount6Field;
    @FXML private TextField hellEnemyCount7Field;
    @FXML private Button saveHellMetadataButton;
    @FXML private TextField hellMekaLookupFilterField;
    @FXML private Label hellMekaLookupHintLabel;
    @FXML private TableView<MekaLookupEntry> hellMekaLookupTable;
    @FXML private Button openSelectedHellScriptButton;
    @FXML private StackPane hellScriptEditorHost;
    @FXML private Label hellScriptEditorPlaceholderLabel;

    private final WorkspaceState workspaceState = new WorkspaceState();
    private HellScriptEditorModeController hellScriptEditorModeController;

    @FXML
    public void initialize() {
        WorkspaceState state = workspaceState;
        progressBar.setProgress(0);

        ToggleModeController modeController = new ToggleModeController(this, state);
        modeController.bind();

        BranchGridController gridController = new BranchGridController(this, state);
        gridController.setup();

        ModeTreeController treeController = new ModeTreeController(this, state, gridController);
        treeController.setup();

        TreeContextMenuController treeContextMenuController = new TreeContextMenuController(this, state);
        treeContextMenuController.setup();

        FilePickerController pickerController = new FilePickerController(this, state, new DirectoryScanner());
        pickerController.setup();

        DragAndDropController dragAndDropController = new DragAndDropController(this, pickerController);
        dragAndDropController.setup();

//        SettingsMenuController settingsMenuController = new SettingsMenuController(this, state);
//        settingsMenuController.setup();

        LogContextMenuController logContextMenuController = new LogContextMenuController(this);
        logContextMenuController.setup();

        AlwaysOnTopController alwaysOnTopController = new AlwaysOnTopController(this);
        alwaysOnTopController.bind();

        ActionButtonController actionController = new ActionButtonController(this, state, gridController, BinaryEngineFactory::create);
        actionController.bind();

        hellScriptEditorModeController = new HellScriptEditorModeController(this, state);
        hellScriptEditorModeController.setup();
    }
}
