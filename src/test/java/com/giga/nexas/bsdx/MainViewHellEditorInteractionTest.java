package com.giga.nexas.bsdx;

import com.giga.nexas.controller.MainViewController;
import com.giga.nexas.controller.HellEmbeddedScriptController;
import com.giga.nexas.controller.HellScriptEditorModeController;
import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.MainViewMode;
import com.giga.nexas.controller.model.WorkspaceState;
import com.giga.nexas.controller.support.HellModPackService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TreeItem;
import javafx.stage.Stage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hell 编辑器主界面的 JavaFX 交互回归测试。
 *
 * <p>这组测试直接驱动 MainView 上的 Hell 编辑器工作流，
 * 覆盖模式切换、关卡详情联动、地图预览、机体速查联动、内嵌脚本编辑区加载，
 * 以及返回工作区的切换路径。
 */
class MainViewHellEditorInteractionTest {

    private static final Path GAME_BIN_DIR = Paths.get("src/main/resources/game/bsdx/bin");
    private static final Path GAME_DAT_DIR = Paths.get("src/main/resources/game/bsdx/dat");
    private static final Path GAME_GRP_DIR = Paths.get("src/main/resources/game/bsdx/grp");

    /**
     * 初始化 JavaFX toolkit。
     */
    @BeforeAll
    static void bootstrapJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // toolkit already started
        }
    }

    /**
     * 验证 Hell 编辑器主工作流在主界面内完整跑通。
     */
    @Test
    void hellEditorModeSupportsSelectionPreviewLookupEmbeddedEditorAndBackNavigation() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR) || !Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path root = Files.createTempDirectory("mainview-hell-editor-ui");
        try {
            prepareFlatRoot(root);

            AtomicReference<MainViewController> controllerRef = new AtomicReference<>();
            AtomicReference<Stage> stageRef = new AtomicReference<>();
            onFxAndWait(() -> {
                FXMLLoader loader = new FXMLLoader(MainViewHellEditorInteractionTest.class.getResource("/fxml/MainView.fxml"));
                Parent rootNode = loader.load();
                MainViewController controller = loader.getController();
                Stage stage = new Stage();
                stage.setScene(new Scene(rootNode, 1600, 980));
                controllerRef.set(controller);
                stageRef.set(stage);
            });

            MainViewController controller = controllerRef.get();
            Assertions.assertNotNull(controller);

            onFxAndWait(() -> {
                WorkspaceState state = controller.getWorkspaceState();
                state.getEngineType().set(com.giga.nexas.controller.model.EngineType.BSDX);
                state.getInputDirectory().set(root);
            });

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());

            onFxAndWait(() -> {
                Assertions.assertEquals(MainViewMode.HELL_SCRIPT_EDITOR, controller.getWorkspaceState().getMainViewMode().get());
                Assertions.assertTrue(controller.getHellScriptEditorPane().isVisible());
                Assertions.assertFalse(controller.getWorkspacePane().isVisible());
                Assertions.assertFalse(controller.getHellStageTree().getRoot().getChildren().isEmpty());
            });

            onFxAndWait(() -> {
                TreeItem<com.giga.nexas.controller.model.HellStageDescriptor> stageItem =
                        controller.getHellStageTree().getRoot().getChildren().get(14);
                controller.getHellStageTree().getSelectionModel().select(stageItem);
            });

            onFxAndWait(() -> {
                Assertions.assertEquals("45", controller.getHellStageMapField().getText());
                Assertions.assertTrue(controller.getHellEnemyType0NameLabel().getText().contains("Noi"));
                Assertions.assertEquals("mapNPC_L03_01", controller.getHellMapResourceLabel().getText());
                Assertions.assertTrue(controller.getHellMapPreviewImageView().isVisible());
                Assertions.assertNotNull(controller.getHellMapPreviewImageView().getImage());
                Assertions.assertEquals(280.0, controller.getHellMapPreviewImageView().getFitHeight());
                Assertions.assertEquals(0.0, controller.getHellMapPreviewImageView().getFitWidth());
                Assertions.assertFalse(controller.getHellMapPreviewStatusLabel().isVisible());
            });

            onFxAndWait(() -> {
                controller.getHellEnemyType0Field().requestFocus();
                controller.getHellMekaLookupFilterField().setText("Chris");
            });

            onFxAndWait(() -> {
                Assertions.assertFalse(controller.getHellMekaLookupTable().getItems().isEmpty());
                controller.getHellMekaLookupTable().getSelectionModel().select(0);
            });

            onFxAndWait(() -> {
                controller.getHellEnemyType0Field().setText(Integer.toString(controller.getHellMekaLookupTable().getItems().get(0).getMekaIndex()));
                Assertions.assertTrue(controller.getHellEnemyType0NameLabel().getText().contains("Chris"));
            });

            onFxAndWait(() -> controller.getHellAppendTitleField().setText("UI Append Test"));
            onFxAndWait(() -> controller.getAppendHellStageButton().fire());

            onFxAndWait(() -> {
                Assertions.assertEquals(101, controller.getHellStageTree().getRoot().getChildren().size());
                Object selected = controller.getHellStageTree().getSelectionModel().getSelectedItem().getValue();
                Assertions.assertTrue(String.valueOf(selected).contains("UI Append Test"));
            });

            onFxAndWait(() -> controller.getOpenSelectedHellScriptButton().fire());

            onFxAndWait(() -> {
                Assertions.assertEquals(1, controller.getHellScriptEditorHost().getChildren().size());
                Assertions.assertNotSame(controller.getHellScriptEditorPlaceholderLabel(), controller.getHellScriptEditorHost().getChildren().get(0));
            });

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());

            onFxAndWait(() -> {
                Assertions.assertEquals(MainViewMode.WORKSPACE, controller.getWorkspaceState().getMainViewMode().get());
                Assertions.assertTrue(controller.getWorkspacePane().isVisible());
                Assertions.assertFalse(controller.getHellScriptEditorPane().isVisible());
            });

            onFxAndWait(() -> stageRef.get().close());
        } finally {
            deleteRecursively(root);
        }
    }

    /**
     * 验证主界面里的元数据保存、来源状态刷新和重新进入编辑模式后的覆盖读取。
     */
    @Test
    void hellEditorModeSaveFlowShowsModSourceAndPersistsAfterReload() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR) || !Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path root = Files.createTempDirectory("mainview-hell-editor-save");
        try {
            prepareFlatRoot(root);
            UiFixture fixture = createMainView(root);
            MainViewController controller = fixture.controller();

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            TreeItem<HellStageDescriptor> hell100Item = findStageItemByScriptPrefix(controller, "Hell100");
            onFxAndWait(() -> controller.getHellStageTree().getSelectionModel().select(hell100Item));

            onFxAndWait(() -> {
                controller.getHellStageTitleField().setText("UI Save Test");
                controller.getHellStageMapField().setText("46");
                controller.getSaveHellMetadataButton().fire();
            });

            onFxAndWait(() -> {
                Assertions.assertEquals("UI Save Test", controller.getHellStageTitleField().getText());
                Assertions.assertEquals("46", controller.getHellStageMapField().getText());
                Assertions.assertTrue(controller.getHellStageLayerLabel().getText().contains("config=MOD"));
                Assertions.assertTrue(controller.getStatusLabel().getText().contains("config=MOD"));
                Assertions.assertTrue(controller.getTreeSummaryLabel().getText().contains("config=MOD"));
            });

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            onFxAndWait(() -> controller.getHellStageTree().getSelectionModel().select(controller.getHellStageTree().getRoot().getChildren().get(14)));

            onFxAndWait(() -> {
                Assertions.assertEquals("UI Save Test", controller.getHellStageTitleField().getText());
                Assertions.assertEquals("46", controller.getHellStageMapField().getText());
                Assertions.assertTrue(controller.getHellStageLayerLabel().getText().contains("config=MOD"));
            });

        } finally {
            deleteRecursively(root);
        }
    }

    /**
     * 验证地图预览缺图时会显示文本状态，不会留下一个空白预览区。
     */
    @Test
    void hellEditorModeShowsMapPreviewFallbackWhenPreviewPngIsMissing() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR) || !Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path root = Files.createTempDirectory("mainview-hell-editor-mappreview-fallback");
        try {
            prepareFlatRoot(root);
            Files.deleteIfExists(root.resolve("T_mapNPC_L03_01.bmp"));

            UiFixture fixture = createMainView(root);
            MainViewController controller = fixture.controller();

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            onFxAndWait(() -> controller.getHellStageTree().getSelectionModel().select(controller.getHellStageTree().getRoot().getChildren().get(14)));

            onFxAndWait(() -> {
                Assertions.assertFalse(controller.getHellMapPreviewImageView().isVisible());
                Assertions.assertTrue(controller.getHellMapPreviewStatusLabel().isVisible());
                Assertions.assertTrue(controller.getHellMapPreviewStatusLabel().getText().contains("Preview image not found"));
            });

        } finally {
            deleteRecursively(root);
        }
    }

    /**
     * 验证保存后关闭并重新创建一份新的 MainView，Hell 编辑器仍然读取 `mod` 覆盖版本。
     */
    @Test
    void hellEditorModeKeepsModOverrideAfterGuiReopen() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR) || !Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path root = Files.createTempDirectory("mainview-hell-editor-reopen");
        try {
            prepareFlatRoot(root);

            UiFixture firstFixture = createMainView(root);
            MainViewController first = firstFixture.controller();

            onFxAndWait(() -> first.getScriptEditorModeButton().fire());
            onFxAndWait(() -> first.getHellStageTree().getSelectionModel().select(first.getHellStageTree().getRoot().getChildren().get(14)));
            onFxAndWait(() -> {
                first.getHellStageTitleField().setText("GUI Reopen Check");
                first.getSaveHellMetadataButton().fire();
            });
            onFxAndWait(() -> firstFixture.stage().close());

            UiFixture secondFixture = createMainView(root);
            MainViewController second = secondFixture.controller();

            onFxAndWait(() -> second.getScriptEditorModeButton().fire());
            onFxAndWait(() -> second.getHellStageTree().getSelectionModel().select(second.getHellStageTree().getRoot().getChildren().get(14)));

            onFxAndWait(() -> {
                Assertions.assertEquals("GUI Reopen Check", second.getHellStageTitleField().getText());
                Assertions.assertTrue(second.getHellStageLayerLabel().getText().contains("config=MOD"));
                Assertions.assertTrue(second.getStatusLabel().getText().contains("config=MOD"));
            });

            onFxAndWait(() -> secondFixture.stage().close());
        } finally {
            deleteRecursively(root);
        }
    }

    /**
     * 验证主界面内编辑日文元数据后，保存链仍然按 `windows-31j` 写回 `mod/HellConfig.dat`。
     *
     * <p>这条测试覆盖 GUI 输入、保存按钮、落盘 bytes 和重开后回显四个环节。
     */
    @Test
    void hellEditorModeSavesJapaneseMetadataUsingWindows31j() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR) || !Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path root = Files.createTempDirectory("mainview-hell-editor-japanese-save");
        try {
            prepareFlatRoot(root);
            UiFixture fixture = createMainView(root);
            MainViewController controller = fixture.controller();

            String japaneseTitle = "テスト関門名";
            String japaneseDescription = "これは日本語の説明です。";

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            onFxAndWait(() -> controller.getHellStageTree().getSelectionModel().select(controller.getHellStageTree().getRoot().getChildren().get(14)));

            onFxAndWait(() -> {
                Assertions.assertEquals("windows-31j", controller.getWorkspaceState().getCharset().get());
                controller.getHellStageTitleField().setText(japaneseTitle);
                controller.getHellStageDescriptionArea().setText(japaneseDescription);
                controller.getSaveHellMetadataButton().fire();
            });

            Path modConfig = root.resolve("mod").resolve("HellConfig.dat");
            byte[] modBytes = Files.readAllBytes(modConfig);

            onFxAndWait(() -> {
                Assertions.assertEquals(japaneseTitle, controller.getHellStageTitleField().getText());
                Assertions.assertEquals(japaneseDescription, controller.getHellStageDescriptionArea().getText());
                Assertions.assertTrue(controller.getHellStageLayerLabel().getText().contains("config=MOD"));
            });

            Assertions.assertTrue(containsBytes(modBytes, japaneseTitle.getBytes(Charset.forName("windows-31j"))));
            Assertions.assertTrue(containsBytes(modBytes, japaneseDescription.getBytes(Charset.forName("windows-31j"))));
            Assertions.assertFalse(containsBytes(modBytes, japaneseTitle.getBytes(StandardCharsets.UTF_8)));
            Assertions.assertFalse(containsBytes(modBytes, japaneseDescription.getBytes(StandardCharsets.UTF_8)));

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            onFxAndWait(() -> controller.getHellStageTree().getSelectionModel().select(controller.getHellStageTree().getRoot().getChildren().get(14)));

            onFxAndWait(() -> {
                Assertions.assertEquals(japaneseTitle, controller.getHellStageTitleField().getText());
                Assertions.assertEquals(japaneseDescription, controller.getHellStageDescriptionArea().getText());
            });

            onFxAndWait(() -> fixture.stage().close());
        } finally {
            deleteRecursively(root);
        }
    }

    /**
     * 构建符合 Hell 编辑器假设的平铺资源目录。
     */
    @Test
    void hellEditorModeAutoLoadsSelectedScriptWithoutEagerModCopy() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR) || !Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path root = Files.createTempDirectory("mainview-hell-editor-auto-load");
        try {
            prepareFlatRoot(root, "Hell100");
            UiFixture fixture = createMainView(root);
            MainViewController controller = fixture.controller();

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            onFxAndWait(() -> controller.getHellStageTree().getSelectionModel().select(controller.getHellStageTree().getRoot().getChildren().get(14)));

            onFxAndWait(() -> {
                HellStageDescriptor selectedStage = controller.getHellStageTree().getSelectionModel().getSelectedItem().getValue();
                Assertions.assertEquals(
                        selectedStage.getScriptFileName(),
                        controller.getHellScriptEditorModeController().getEmbeddedScriptController().getEmbeddedEditorHandle().controller().getDocumentLogicalName()
                );
                Assertions.assertEquals(1, controller.getHellScriptEditorHost().getChildren().size());
            });

            HellStageDescriptor selectedStage = controller.getHellStageTree().getSelectionModel().getSelectedItem().getValue();
            Assertions.assertFalse(Files.exists(root.resolve("mod").resolve(selectedStage.getScriptFileName())));

            onFxAndWait(() -> fixture.stage().close());
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void hellEditorModeRestoresTreeSelectionWhenDirtyScriptSwitchIsCancelled() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR) || !Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path root = Files.createTempDirectory("mainview-hell-editor-selection-cancel");
        try {
            prepareFlatRoot(root, "Hell100", "Hell101");
            UiFixture fixture = createMainView(root);
            MainViewController controller = fixture.controller();

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            TreeItem<HellStageDescriptor> hell100Item = findStageItemByScriptPrefix(controller, "Hell100");
            TreeItem<HellStageDescriptor> hell101Item = findStageItemByScriptPrefix(controller, "Hell101");
            onFxAndWait(() -> controller.getHellStageTree().getSelectionModel().select(hell100Item));

            HellScriptEditorModeController hellModeController = controller.getHellScriptEditorModeController();
            onFxAndWait(() -> {
                javafx.scene.control.TextArea pseudoArea = readPrivateField(
                        hellModeController.getEmbeddedScriptController().getEmbeddedEditorHandle().controller(),
                        "pseudoArea",
                        javafx.scene.control.TextArea.class
                );
                pseudoArea.appendText("\r\nmarker 999");
                hellModeController.getEmbeddedScriptController().setLeaveScriptConfirmationHandlerForTest((script, action) -> HellEmbeddedScriptController.LeaveScriptDecision.CANCEL);
                controller.getHellStageTree().getSelectionModel().select(hell101Item);
            });

            onFxAndWait(() -> {
                HellStageDescriptor selectedStage = controller.getHellStageTree().getSelectionModel().getSelectedItem().getValue();
                Assertions.assertEquals(hell100Item.getValue().getIndex(), selectedStage.getIndex());
                Assertions.assertEquals(
                        selectedStage.getScriptFileName(),
                        hellModeController.getEmbeddedScriptController().getEmbeddedEditorHandle().controller().getDocumentLogicalName()
                );
            });

            onFxAndWait(() -> {
                hellModeController.getEmbeddedScriptController().setLeaveScriptConfirmationHandlerForTest((script, action) -> HellEmbeddedScriptController.LeaveScriptDecision.DISCARD);
                controller.getHellStageTree().getSelectionModel().select(hell101Item);
            });

            onFxAndWait(() -> {
                HellStageDescriptor selectedStage = controller.getHellStageTree().getSelectionModel().getSelectedItem().getValue();
                Assertions.assertEquals(hell101Item.getValue().getIndex(), selectedStage.getIndex());
                Assertions.assertEquals(
                        selectedStage.getScriptFileName(),
                        hellModeController.getEmbeddedScriptController().getEmbeddedEditorHandle().controller().getDocumentLogicalName()
                );
            });

            onFxAndWait(() -> fixture.stage().close());
        } finally {
            deleteRecursively(root);
        }
    }

    @Test
    void hellEditorModeUsesUnifiedDirtyConfirmationForLeaveActions() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR) || !Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path rootA = Files.createTempDirectory("mainview-hell-editor-leave-a");
        Path rootB = Files.createTempDirectory("mainview-hell-editor-leave-b");
        try {
            prepareFlatRoot(rootA, "Hell100");
            prepareFlatRoot(rootB, "Hell100");
            UiFixture fixture = createMainView(rootA);
            MainViewController controller = fixture.controller();

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            TreeItem<HellStageDescriptor> hell100Item = findStageItemByScriptPrefix(controller, "Hell100");
            onFxAndWait(() -> controller.getHellStageTree().getSelectionModel().select(hell100Item));

            HellScriptEditorModeController hellModeController = controller.getHellScriptEditorModeController();
            onFxAndWait(() -> {
                javafx.scene.control.TextArea pseudoArea = readPrivateField(
                        hellModeController.getEmbeddedScriptController().getEmbeddedEditorHandle().controller(),
                        "pseudoArea",
                        javafx.scene.control.TextArea.class
                );
                pseudoArea.appendText("\r\nmarker 998");
                hellModeController.getEmbeddedScriptController().setLeaveScriptConfirmationHandlerForTest((script, action) -> HellEmbeddedScriptController.LeaveScriptDecision.CANCEL);
            });

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            onFxAndWait(() -> Assertions.assertEquals(MainViewMode.HELL_SCRIPT_EDITOR, controller.getWorkspaceState().getMainViewMode().get()));

            onFxAndWait(() -> controller.getWorkspaceState().getInputDirectory().set(rootB));
            onFxAndWait(() -> Assertions.assertEquals(rootA, controller.getWorkspaceState().getInputDirectory().get()));

            onFxAndWait(() -> controller.getWorkspaceState().getEngineType().set(com.giga.nexas.controller.model.EngineType.BHE));
            onFxAndWait(() -> Assertions.assertEquals(com.giga.nexas.controller.model.EngineType.BSDX, controller.getWorkspaceState().getEngineType().get()));

            onFxAndWait(() -> {
                hellModeController.getEmbeddedScriptController().setLeaveScriptConfirmationHandlerForTest((script, action) -> HellEmbeddedScriptController.LeaveScriptDecision.DISCARD);
                controller.getScriptEditorModeButton().fire();
            });
            onFxAndWait(() -> Assertions.assertEquals(MainViewMode.WORKSPACE, controller.getWorkspaceState().getMainViewMode().get()));

        } finally {
            deleteRecursively(rootA);
            deleteRecursively(rootB);
        }
    }

    @Test
    void hellEditorModePackagesModDirectoryAfterDirtyScriptIsHandled() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR) || !Files.isDirectory(GAME_GRP_DIR)) {
            return;
        }

        Path root = Files.createTempDirectory("mainview-hell-editor-pack");
        try {
            prepareFlatRoot(root, "Hell100");
            UiFixture fixture = createMainView(root);
            MainViewController controller = fixture.controller();

            onFxAndWait(() -> controller.getScriptEditorModeButton().fire());
            TreeItem<HellStageDescriptor> hell100Item = findStageItemByScriptPrefix(controller, "Hell100");
            onFxAndWait(() -> controller.getHellStageTree().getSelectionModel().select(hell100Item));

            HellScriptEditorModeController hellModeController = controller.getHellScriptEditorModeController();
            HellModPackService packService = new HellModPackService();
            packService.setPackRunnerForTest((folderPath, compressMode) -> {
                Path sourceFolder = Path.of(folderPath);
                Path pacNew = sourceFolder.resolveSibling(sourceFolder.getFileName().toString() + ".pacNew");
                Files.writeString(pacNew, "packed-" + compressMode);
                return "ok";
            });
            hellModeController.setModPackServiceForTest(packService);

            onFxAndWait(() -> {
                javafx.scene.control.TextArea pseudoArea = readPrivateField(
                        hellModeController.getEmbeddedScriptController().getEmbeddedEditorHandle().controller(),
                        "pseudoArea",
                        javafx.scene.control.TextArea.class
                );
                pseudoArea.appendText("\r\nmarker 997");
                hellModeController.getEmbeddedScriptController().setLeaveScriptConfirmationHandlerForTest((script, action) -> HellEmbeddedScriptController.LeaveScriptDecision.CANCEL);
                controller.getPackageHellModButton().fire();
            });

            Assertions.assertFalse(Files.exists(root.resolve("mod").resolve("pac").resolve("Update4.pac")));

            onFxAndWait(() -> {
                hellModeController.getEmbeddedScriptController().setLeaveScriptConfirmationHandlerForTest((script, action) -> HellEmbeddedScriptController.LeaveScriptDecision.DISCARD);
                controller.getPackageHellModButton().fire();
            });

            Assertions.assertTrue(Files.exists(root.resolve("mod").resolve("pac").resolve("Update4.pac")));

        } finally {
            deleteRecursively(root);
        }
    }

    private void prepareFlatRoot(Path root) throws Exception {
        prepareFlatRoot(root, "Hell100");
    }

    private void prepareFlatRoot(Path root, String... scriptPrefixes) throws Exception {
        copyRequired(root, GAME_BIN_DIR.resolve("Hell.bin"));
        copyRequired(root, GAME_BIN_DIR.resolve("__GLOBAL.bin"));
        copyRequired(root, GAME_DAT_DIR.resolve("HellConfig.dat"));
        copyRequired(root, GAME_GRP_DIR.resolve("MekaGroup.grp"));
        copyRequired(root, GAME_GRP_DIR.resolve("MapGroup.grp"));
        for (String scriptPrefix : scriptPrefixes) {
            copyBinByPrefix(root, scriptPrefix);
        }
        Path previewImage = root.resolve("T_mapNPC_L03_01.bmp");
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "bmp", previewImage.toFile());
    }

    /**
     * 创建 MainView 的 JavaFX 测试实例，并把输入目录指向指定平铺资源目录。
     */
    private UiFixture createMainView(Path root) throws Exception {
        AtomicReference<MainViewController> controllerRef = new AtomicReference<>();
        AtomicReference<Stage> stageRef = new AtomicReference<>();
        onFxAndWait(() -> {
            FXMLLoader loader = new FXMLLoader(MainViewHellEditorInteractionTest.class.getResource("/fxml/MainView.fxml"));
            Parent rootNode = loader.load();
            MainViewController controller = loader.getController();
            Stage stage = new Stage();
            stage.setScene(new Scene(rootNode, 1600, 980));
            controllerRef.set(controller);
            stageRef.set(stage);
        });

        MainViewController controller = controllerRef.get();
        Assertions.assertNotNull(controller);
        onFxAndWait(() -> {
            WorkspaceState state = controller.getWorkspaceState();
            state.getEngineType().set(com.giga.nexas.controller.model.EngineType.BSDX);
            state.getInputDirectory().set(root);
        });
        return new UiFixture(controller, stageRef.get());
    }

    /**
     * 复制一个真实资源文件到测试目录。
     */
    private void copyRequired(Path targetRoot, Path source) throws Exception {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Missing source resource: " + source);
        }
        Files.copy(source, targetRoot.resolve(source.getFileName().toString()));
    }

    /**
     * 通过前缀复制一个 Hell 子脚本。
     */
    private void copyBinByPrefix(Path targetRoot, String prefix) throws Exception {
        try (var stream = Files.list(GAME_BIN_DIR)) {
            Path source = stream
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Missing test bin with prefix: " + prefix));
            copyRequired(targetRoot, source);
        }
    }

    /**
     * 在当前 Hell 关卡树里按脚本文件名前缀定位一个真实节点。
     */
    private TreeItem<HellStageDescriptor> findStageItemByScriptPrefix(MainViewController controller, String scriptPrefix) {
        return controller.getHellStageTree().getRoot().getChildren().stream()
                .filter(item -> item.getValue() != null
                        && item.getValue().getScriptFileName() != null
                        && item.getValue().getScriptFileName().startsWith(scriptPrefix))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Missing stage item for script prefix: " + scriptPrefix));
    }

    /**
     * 在 JavaFX 线程执行并等待完成。
     */
    private void onFxAndWait(FxRunnable runnable) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        Assertions.assertTrue(latch.await(20, TimeUnit.SECONDS), "Timed out while waiting for JavaFX task");
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
    }

    /**
     * 清理测试目录。
     */
    private void deleteRecursively(Path root) throws Exception {
        Files.walk(root)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> path.toFile().delete());
    }

    /**
     * 判断原始字节数组里是否出现了指定子序列。
     */
    private boolean containsBytes(byte[] source, byte[] target) {
        if (source == null || target == null || target.length == 0 || target.length > source.length) {
            return false;
        }
        for (int start = 0; start <= source.length - target.length; start++) {
            boolean matched = true;
            for (int offset = 0; offset < target.length; offset++) {
                if (source[start + offset] != target[offset]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    /**
     * 通过反射读取控制器私有字段。
     */
    private <T> T readPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    @FunctionalInterface
    private interface FxRunnable {
        void run() throws Exception;
    }

    private record UiFixture(
            MainViewController controller,
            Stage stage
    ) {
    }
}
