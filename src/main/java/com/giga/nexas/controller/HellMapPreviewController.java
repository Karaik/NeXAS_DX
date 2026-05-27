package com.giga.nexas.controller;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.HellEditorReferenceData;
import com.giga.nexas.controller.model.MapLookupEntry;
import com.giga.nexas.controller.model.MapPreviewDescriptor;
import com.giga.nexas.controller.support.HellEditorReferenceService;
import com.giga.nexas.controller.support.HellMapPreviewChooser;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.stage.Window;

import java.awt.image.BufferedImage;
import java.util.function.Supplier;

/**
 * 地图预览显示与选择器调用的独立控制器。
 *
 * 负责：
 * 1. 根据 mapId 刷新地图名称、资源名和预览图
 * 2. 双击预览区域打开地图选择器
 * 3. 选择器返回后回填 mapId 并刷新预览
 *
 * 不负责：
 * - 资源解析规则（由 HellEditorReferenceService 决定）
 * - 选择器内部布局（由 HellMapPreviewChooser 决定）
 * - 保存操作（由调用方决定）
 */
public class HellMapPreviewController {

    private static final double MAP_PREVIEW_FIXED_HEIGHT = 280.0;

    private final ImageView mapPreviewImageView;
    private final Label mapPreviewStatusLabel;
    private final TextField stageMapField;
    private final Label mapNameLabel;
    private final Label mapResourceLabel;
    private final Label editorStatusLabel;
    private final HellEditorReferenceService referenceService;
    private final HellMapPreviewChooser mapPreviewChooser;
    private final Supplier<HellEditorReferenceData> referenceDataSupplier;
    private final Supplier<BsdxOverlayResourceSession> sessionSupplier;
    private final Supplier<String> charsetSupplier;

    public HellMapPreviewController(
            ImageView mapPreviewImageView,
            Label mapPreviewStatusLabel,
            TextField stageMapField,
            Label mapNameLabel,
            Label mapResourceLabel,
            Label editorStatusLabel,
            HellEditorReferenceService referenceService,
            HellMapPreviewChooser mapPreviewChooser,
            Supplier<HellEditorReferenceData> referenceDataSupplier,
            Supplier<BsdxOverlayResourceSession> sessionSupplier,
            Supplier<String> charsetSupplier
    ) {
        this.mapPreviewImageView = mapPreviewImageView;
        this.mapPreviewStatusLabel = mapPreviewStatusLabel;
        this.stageMapField = stageMapField;
        this.mapNameLabel = mapNameLabel;
        this.mapResourceLabel = mapResourceLabel;
        this.editorStatusLabel = editorStatusLabel;
        this.referenceService = referenceService;
        this.mapPreviewChooser = mapPreviewChooser;
        this.referenceDataSupplier = referenceDataSupplier;
        this.sessionSupplier = sessionSupplier;
        this.charsetSupplier = charsetSupplier;
    }

    /**
     * 初始化地图预览区域的显示约束和交互。
     */
    public void setup() {
        mapPreviewImageView.setPreserveRatio(true);
        mapPreviewImageView.setFitHeight(MAP_PREVIEW_FIXED_HEIGHT);
        mapPreviewImageView.setFitWidth(0);
        mapPreviewImageView.setSmooth(true);
        mapPreviewImageView.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) {
                openMapPreviewChooser();
                event.consume();
            }
        });
        mapPreviewStatusLabel.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() >= 2) {
                openMapPreviewChooser();
                event.consume();
            }
        });
    }

    /**
     * 根据 mapId 刷新地图信息和预览区。
     */
    public void refreshMapSection(Integer mapId) {
        HellEditorReferenceData refData = referenceDataSupplier.get();
        MapLookupEntry mapEntry = mapId == null || refData == null ? null : refData.getMapById().get(mapId);
        mapNameLabel.setText(mapEntry == null ? "-" : mapEntry.getGroupName());
        mapResourceLabel.setText(mapEntry == null ? "-" : mapEntry.getGroupResourceName());

        if (mapEntry == null) {
            clearMapPreview("Map index not found in MapGroup.grp.");
            return;
        }

        try {
            BsdxOverlayResourceSession session = sessionSupplier.get();
            MapPreviewDescriptor descriptor = referenceService.resolveMapPreview(session, charsetSupplier.get(), mapEntry);
            BufferedImage preview = descriptor == null
                    ? null
                    : referenceService.renderMapPreviewImage(session, charsetSupplier.get(), descriptor);
            if (descriptor == null || preview == null) {
                clearMapPreview(descriptor == null ? "Map preview unavailable." : descriptor.getStatusText());
                return;
            }

            mapPreviewImageView.setImage(toFxImage(preview));
            mapPreviewImageView.setManaged(true);
            mapPreviewImageView.setVisible(true);
            mapPreviewStatusLabel.setManaged(false);
            mapPreviewStatusLabel.setVisible(false);
            mapPreviewStatusLabel.setText(descriptor.getStatusText());
        } catch (Exception ex) {
            clearMapPreview("Failed to load map preview: " + ex.getMessage());
        }
    }

    /**
     * 清空地图预览，只保留状态文本。
     */
    public void clearMapPreview(String statusText) {
        mapPreviewImageView.setImage(null);
        mapPreviewImageView.setManaged(false);
        mapPreviewImageView.setVisible(false);
        mapPreviewStatusLabel.setManaged(true);
        mapPreviewStatusLabel.setVisible(true);
        mapPreviewStatusLabel.setText(statusText);
    }

    /**
     * 打开全地图预览选择器，并把选中的地图写回当前关卡表单。
     */
    private void openMapPreviewChooser() {
        if (stageMapField.isDisabled()) {
            showInfo("Map Preview", "Select a stage first.");
            return;
        }
        BsdxOverlayResourceSession session = sessionSupplier.get();
        if (session == null) {
            showInfo("Map Preview", "No active BSDX resource session.");
            return;
        }
        HellEditorReferenceData refData = referenceDataSupplier.get();
        if (refData == null || refData.getMapEntries() == null || refData.getMapEntries().isEmpty()) {
            showInfo("Map Preview", "No map reference data loaded.");
            return;
        }

        mapPreviewChooser.choose(mapPreviewImageView, refData, session, charsetSupplier.get())
                .ifPresent(this::applySelectedMap);
    }

    /**
     * 应用预览窗口选择的地图。
     *
     * 只更新右侧表单和预览，不直接保存文件；真正写回仍然由 Save Metadata 完成。
     */
    private void applySelectedMap(MapLookupEntry mapEntry) {
        if (mapEntry == null) {
            return;
        }
        stageMapField.setText(Integer.toString(mapEntry.getMapId()));
        refreshMapSection(mapEntry.getMapId());
        editorStatusLabel.setText(
                "Selected map " + mapEntry.getMapId() + " (" + nullToEmpty(mapEntry.getGroupResourceName())
                        + "). Click Save Metadata to persist."
        );
    }

    /**
     * 把 BufferedImage 转成 JavaFX Image。
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

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        Window owner = mapPreviewImageView.getScene() == null ? null : mapPreviewImageView.getScene().getWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.showAndWait();
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }
}
