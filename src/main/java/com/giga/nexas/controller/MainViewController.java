package com.giga.nexas.controller;

import com.giga.nexas.controller.model.EngineType;
import com.giga.nexas.controller.model.WorkspaceState;
import com.giga.nexas.controller.model.WorkspaceTreeNode;
import com.giga.nexas.controller.support.DirectoryScanner;
import com.giga.nexas.service.engine.BinaryEngineFactory;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
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
    @FXML private CheckBox alwaysOnTop;
    @FXML private CheckBox lockOutPutPath;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private ComboBox<EngineType> engineSelector;
    @FXML private Label statusLabel;
    @FXML private Label treeSummaryLabel;
    @FXML private TilePane branchGrid;
    @FXML private ScrollPane branchScroll;

    private final WorkspaceState workspaceState = new WorkspaceState();

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
    }
}
