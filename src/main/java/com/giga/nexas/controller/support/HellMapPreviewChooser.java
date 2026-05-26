package com.giga.nexas.controller.support;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.HellEditorReferenceData;
import com.giga.nexas.controller.model.MapLookupEntry;
import com.giga.nexas.controller.model.MapPreviewDescriptor;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Hell 编辑器的地图预览选择窗口。
 *
 * <p>这个类只负责展示 MapGroup 里的全部地图预览，并把用户双击的地图条目返回给调用方。
 * 它不修改关卡数据，也不写 HellConfig.dat。</p>
 */
public class HellMapPreviewChooser {

    private static final double TILE_WIDTH = 220.0;
    private static final double TILE_HEIGHT = 250.0;
    private static final double TILE_PREVIEW_HEIGHT = 150.0;
    private static final double IMAGE_FIT_WIDTH = 200.0;
    private static final double IMAGE_FIT_HEIGHT = 120.0;
    private static final int PREVIEW_LOADER_THREADS = 4;
    private static final String TILE_BASE_STYLE =
            "-fx-padding: 8; -fx-background-color: white; -fx-border-color: #d5d9e2; "
                    + "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;";
    private static final String TILE_HOVER_STYLE =
            "-fx-padding: 8; -fx-background-color: #f8fbff; -fx-border-color: #7fa7df; "
                    + "-fx-border-width: 1; -fx-border-radius: 6; -fx-background-radius: 6;";

    private final HellEditorReferenceService referenceService;

    public HellMapPreviewChooser(HellEditorReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    /**
     * 打开地图预览选择窗口。
     *
     * @param ownerNode 用于继承主窗口 owner，可为空
     * @param referenceData 当前覆盖会话解析出的地图引用数据
     * @param session 当前 BSDX 覆盖资源会话
     * @param charset 资源解析字符集
     * @return 用户双击选中的地图；取消时为空
     */
    public Optional<MapLookupEntry> choose(
            Node ownerNode,
            HellEditorReferenceData referenceData,
            BsdxOverlayResourceSession session,
            String charset
    ) {
        if (referenceData == null || referenceData.getMapEntries() == null || referenceData.getMapEntries().isEmpty()) {
            return Optional.empty();
        }
        if (session == null) {
            return Optional.empty();
        }

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Choose Map Preview");
        dialog.setHeaderText("Double-click a preview to use it for the current stage.");
        resolveOwner(ownerNode).ifPresent(dialog::initOwner);
        dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CANCEL);
        dialog.setResultConverter(buttonType -> null);

        AtomicBoolean closed = new AtomicBoolean(false);
        AtomicReference<MapLookupEntry> chosenEntry = new AtomicReference<>();
        Map<Integer, CompletableFuture<PreviewLoadResult>> previewCache = new ConcurrentHashMap<>();
        ExecutorService previewLoader = Executors.newFixedThreadPool(PREVIEW_LOADER_THREADS, runnable -> {
            Thread thread = new Thread(runnable, "hell-map-preview-loader");
            thread.setDaemon(true);
            return thread;
        });
        dialog.setOnHidden(event -> {
            closed.set(true);
            previewLoader.shutdownNow();
        });

        List<MapLookupEntry> previewEntries = referenceData.getMapEntries();
        if (previewEntries.isEmpty()) {
            return Optional.empty();
        }

        TilePane tilePane = new TilePane();
        tilePane.setPrefColumns(4);
        tilePane.setHgap(14.0);
        tilePane.setVgap(14.0);
        tilePane.setStyle("-fx-padding: 14; -fx-background-color: #eef1f6;");
        for (MapLookupEntry mapEntry : previewEntries) {
            tilePane.getChildren().add(buildTile(
                    dialog,
                    session,
                    charset,
                    mapEntry,
                    previewLoader,
                    closed,
                    previewCache,
                    chosenEntry
            ));
        }

        ScrollPane scrollPane = new ScrollPane(tilePane);
        scrollPane.setPrefWidth(980.0);
        scrollPane.setPrefHeight(680.0);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent;");
        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
        return Optional.ofNullable(chosenEntry.get());
    }

    private Node buildTile(
            Dialog<Void> dialog,
            BsdxOverlayResourceSession session,
            String charset,
            MapLookupEntry mapEntry,
            ExecutorService previewLoader,
            AtomicBoolean closed,
            Map<Integer, CompletableFuture<PreviewLoadResult>> previewCache,
            AtomicReference<MapLookupEntry> chosenEntry
    ) {
        StackPane previewPane = new StackPane();
        previewPane.setMinHeight(TILE_PREVIEW_HEIGHT);
        previewPane.setPrefHeight(TILE_PREVIEW_HEIGHT);
        previewPane.setMaxHeight(TILE_PREVIEW_HEIGHT);
        previewPane.setMinWidth(TILE_WIDTH - 16.0);
        previewPane.setPrefWidth(TILE_WIDTH);
        previewPane.setStyle("-fx-background-color: #f5f6f8; -fx-border-color: #b7bcc7; -fx-border-radius: 4; -fx-background-radius: 4;");
        previewPane.getChildren().setAll(statusLabel("Loading preview..."));
        loadPreviewAsync(previewPane, session, charset, mapEntry, previewLoader, closed, previewCache);

        Label title = new Label(mapEntry.getMapId() + "  " + nullToEmpty(mapEntry.getGroupName()));
        title.setPrefWidth(TILE_WIDTH - 16.0);
        title.setMaxWidth(TILE_WIDTH - 16.0);
        title.setMinHeight(22.0);
        title.setPrefHeight(22.0);
        title.setTextOverrun(OverrunStyle.ELLIPSIS);
        title.setStyle("-fx-font-weight: bold;");

        Label resource = new Label(nullToEmpty(mapEntry.getGroupResourceName()));
        resource.setPrefWidth(TILE_WIDTH - 16.0);
        resource.setMaxWidth(TILE_WIDTH - 16.0);
        resource.setMinHeight(38.0);
        resource.setPrefHeight(38.0);
        resource.setWrapText(true);
        resource.setTextOverrun(OverrunStyle.ELLIPSIS);
        resource.setStyle("-fx-text-fill: #555555;");

        VBox tile = new VBox(6.0, previewPane, title, resource);
        tile.setMinWidth(TILE_WIDTH);
        tile.setPrefWidth(TILE_WIDTH);
        tile.setMaxWidth(TILE_WIDTH);
        tile.setMinHeight(TILE_HEIGHT);
        tile.setPrefHeight(TILE_HEIGHT);
        tile.setMaxHeight(TILE_HEIGHT);
        tile.setStyle(TILE_BASE_STYLE);
        tile.setOnMouseEntered(event -> tile.setStyle(TILE_HOVER_STYLE));
        tile.setOnMouseExited(event -> tile.setStyle(TILE_BASE_STYLE));
        tile.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (event.getClickCount() >= 2) {
                chosenEntry.set(mapEntry);
                dialog.close();
            }
            event.consume();
        });
        return tile;
    }

    /**
     * 预览图片解析和 ImageIO 读取都可能碰到大量地图资源，不能阻塞 JavaFX UI 线程。
     */
    private void loadPreviewAsync(
            StackPane previewPane,
            BsdxOverlayResourceSession session,
            String charset,
            MapLookupEntry mapEntry,
            ExecutorService previewLoader,
            AtomicBoolean closed,
            Map<Integer, CompletableFuture<PreviewLoadResult>> previewCache
    ) {
        previewCache
                .computeIfAbsent(
                        mapEntry.getMapId(),
                        ignored -> CompletableFuture.supplyAsync(
                                () -> loadPreviewResult(session, charset, mapEntry),
                                previewLoader
                        )
                )
                .whenComplete((node, error) -> Platform.runLater(() -> {
                    if (closed.get()) {
                        return;
                    }
                    if (error != null) {
                        previewPane.getChildren().setAll(statusLabel("Failed to load preview: " + error.getMessage()));
                        return;
                    }
                    previewPane.getChildren().setAll(buildPreviewNode(node));
                }));
    }

    private PreviewLoadResult loadPreviewResult(
            BsdxOverlayResourceSession session,
            String charset,
            MapLookupEntry mapEntry
    ) {
        try {
            MapPreviewDescriptor descriptor = referenceService.resolveMapPreview(session, charset, mapEntry);
            BufferedImage preview = descriptor == null
                    ? null
                    : referenceService.renderMapPreviewImage(session, charset, descriptor);
            if (preview != null) {
                return new PreviewLoadResult(preview, null);
            }
            return new PreviewLoadResult(null, descriptor == null ? "No preview image." : descriptor.getStatusText());
        } catch (Exception ex) {
            return new PreviewLoadResult(null, "Failed to load preview: " + ex.getMessage());
        }
    }

    private Node buildPreviewNode(PreviewLoadResult result) {
        if (result != null && result.image() != null) {
            ImageView imageView = new ImageView(toFxImage(result.image()));
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(IMAGE_FIT_WIDTH);
            imageView.setFitHeight(IMAGE_FIT_HEIGHT);
            imageView.setSmooth(true);
            return imageView;
        }
        return statusLabel(result == null ? "No preview image." : result.statusText());
    }

    private Label statusLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #555555; -fx-padding: 8;");
        return label;
    }

    private Optional<Window> resolveOwner(Node node) {
        if (node == null) {
            return Optional.empty();
        }
        Scene scene = node.getScene();
        if (scene == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(scene.getWindow());
    }

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

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private record PreviewLoadResult(BufferedImage image, String statusText) {
    }
}
