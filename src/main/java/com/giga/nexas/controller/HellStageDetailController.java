package com.giga.nexas.controller;

import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.HellStageMetadataDraft;
import com.giga.nexas.controller.support.HellConfigLayout;
import com.giga.nexas.controller.support.HellStageStatusFormatter;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;

/**
 * Hell 关卡元数据表单控制器。
 *
 * <p>负责右侧元数据面板的显示、校验、追加关卡区域，
 * 以及与机体速查和地图预览控制器的联动。
 */
public class HellStageDetailController {

    private final TextField titleField;
    private final Label scriptLabel;
    private final Label layerLabel;
    private final TextField mapField;
    private final TextField hellLevelField;
    private final TextField shopField;
    private final TextField portraitField;
    private final TextField balloonField;
    private final TextArea descriptionArea;
    private final List<TextField> enemyTypeFields;
    private final List<TextField> enemyCountFields;

    private final Button openScriptButton;
    private final Button saveMetadataButton;
    private final Button appendStageButton;

    private final Label appendBlueprintLabel;
    private final TextField appendTitleField;
    private final Label appendScriptHintLabel;

    private final HellMekaLookupController mekaLookupController;
    private final HellMapPreviewController mapPreviewController;

    private final IntSupplier stageCountSupplier;
    private final BiConsumer<Integer, HellStageDescriptor> statusBarUpdater;

    public HellStageDetailController(
            TextField titleField,
            Label scriptLabel,
            Label layerLabel,
            TextField mapField,
            TextField hellLevelField,
            TextField shopField,
            TextField portraitField,
            TextField balloonField,
            TextArea descriptionArea,
            List<TextField> enemyTypeFields,
            List<TextField> enemyCountFields,
            Button openScriptButton,
            Button saveMetadataButton,
            Button appendStageButton,
            Label appendBlueprintLabel,
            TextField appendTitleField,
            Label appendScriptHintLabel,
            HellMekaLookupController mekaLookupController,
            HellMapPreviewController mapPreviewController,
            IntSupplier stageCountSupplier,
            BiConsumer<Integer, HellStageDescriptor> statusBarUpdater
    ) {
        this.titleField = titleField;
        this.scriptLabel = scriptLabel;
        this.layerLabel = layerLabel;
        this.mapField = mapField;
        this.hellLevelField = hellLevelField;
        this.shopField = shopField;
        this.portraitField = portraitField;
        this.balloonField = balloonField;
        this.descriptionArea = descriptionArea;
        this.enemyTypeFields = enemyTypeFields;
        this.enemyCountFields = enemyCountFields;
        this.openScriptButton = openScriptButton;
        this.saveMetadataButton = saveMetadataButton;
        this.appendStageButton = appendStageButton;
        this.appendBlueprintLabel = appendBlueprintLabel;
        this.appendTitleField = appendTitleField;
        this.appendScriptHintLabel = appendScriptHintLabel;
        this.mekaLookupController = mekaLookupController;
        this.mapPreviewController = mapPreviewController;
        this.stageCountSupplier = stageCountSupplier;
        this.statusBarUpdater = statusBarUpdater;
    }

    /**
     * 显示当前选中的关卡详情、机体联动和地图预览。
     */
    public void showStageDetails(HellStageDescriptor stage) {
        if (stage == null || stage.getIndex() < 0) {
            clearForm();
            return;
        }

        HellStageMetadataDraft draft = HellStageMetadataDraft.fromStage(stage);
        setMetadataEditorDisabled(false);
        titleField.setText(nullToEmpty(draft.getTitle()));
        scriptLabel.setText(stage.getScriptFileName() == null ? "-" : stage.getScriptFileName());
        layerLabel.setText(formatLayer(stage));
        mapField.setText(formatInteger(draft.getMapId()));
        hellLevelField.setText(formatInteger(draft.getHellLevel()));
        shopField.setText(formatInteger(draft.getShopId()));
        portraitField.setText(formatInteger(draft.getPortraitId()));
        balloonField.setText(formatInteger(draft.getBalloonStyleId()));
        descriptionArea.setText(nullToEmpty(draft.getDescription()));
        fillSlotFields(enemyTypeFields, draft.getEnemyTypes());
        fillSlotFields(enemyCountFields, draft.getEnemyCounts());
        mekaLookupController.refreshAllEnemyTypeNames();
        mapPreviewController.refreshMapSection(stage.getMapId());
        refreshAppendStageArea(stage);

        openScriptButton.setDisable(stage.getScriptFileName() == null);
        saveMetadataButton.setDisable(false);
        appendStageButton.setDisable(stage.getScriptFileName() == null);
        statusBarUpdater.accept(stageCountSupplier.getAsInt(), stage);
    }

    /**
     * 从右侧表单构建可写回的元数据草稿。
     */
    public HellStageMetadataDraft buildMetadataDraft(HellStageDescriptor stage) {
        return HellStageMetadataDraft.builder()
                .index(stage.getIndex())
                .title(titleField.getText())
                .description(descriptionArea.getText())
                .hellLevel(parseRequiredInteger(hellLevelField, "Hell level"))
                .mapId(parseRequiredInteger(mapField, "Map"))
                .shopId(parseRequiredInteger(shopField, "Shop"))
                .portraitId(parseRequiredInteger(portraitField, "Portrait"))
                .balloonStyleId(parseRequiredInteger(balloonField, "Balloon"))
                .enemyTypes(parseSlotValues(enemyTypeFields, "Enemy type"))
                .enemyCounts(parseSlotValues(enemyCountFields, "Enemy param"))
                .build();
    }

    /**
     * 获取追加关卡标题输入。
     */
    public String getAppendTitleText() {
        return appendTitleField.getText();
    }

    /**
     * 统一控制元数据表单可编辑状态。
     */
    public void setMetadataEditorDisabled(boolean disabled) {
        titleField.setDisable(disabled);
        mapField.setDisable(disabled);
        hellLevelField.setDisable(disabled);
        shopField.setDisable(disabled);
        portraitField.setDisable(disabled);
        balloonField.setDisable(disabled);
        descriptionArea.setDisable(disabled);
        for (TextField field : enemyTypeFields) {
            field.setDisable(disabled);
        }
        for (TextField field : enemyCountFields) {
            field.setDisable(disabled);
        }
    }

    private void clearForm() {
        titleField.setText("");
        scriptLabel.setText("-");
        layerLabel.setText("-");
        mapField.setText("");
        hellLevelField.setText("");
        shopField.setText("");
        portraitField.setText("");
        balloonField.setText("");
        descriptionArea.setText("");
        fillSlotFields(enemyTypeFields, List.of());
        fillSlotFields(enemyCountFields, List.of());
        mekaLookupController.clearEnemyTypeNames();
        mekaLookupController.clearActiveEnemySlotHighlight();
        mapPreviewController.resetMapDisplay("Select a stage to inspect the current map.");
        clearAppendStageArea();
        openScriptButton.setDisable(true);
        saveMetadataButton.setDisable(true);
        appendStageButton.setDisable(true);
        setMetadataEditorDisabled(true);
        statusBarUpdater.accept(stageCountSupplier.getAsInt(), null);
    }

    private void refreshAppendStageArea(HellStageDescriptor stage) {
        String blueprintText = String.format("%03d %s", stage.getIndex(), stage.getTitle());
        appendBlueprintLabel.setText(blueprintText);
        appendTitleField.setDisable(false);
        appendTitleField.setText(nullToEmpty(stage.getTitle()) + " Copy");
        appendScriptHintLabel.setText(
                "A new mod script file will be generated automatically from blueprint "
                        + nullToEmpty(stage.getScriptFileName()) + "."
        );
    }

    private void clearAppendStageArea() {
        appendBlueprintLabel.setText("Select a stage first.");
        appendTitleField.setText("");
        appendTitleField.setDisable(true);
        appendScriptHintLabel.setText("A new mod script file name will be generated automatically.");
    }

    private void fillSlotFields(List<TextField> fields, List<Integer> values) {
        for (int i = 0; i < fields.size(); i++) {
            Integer value = values != null && i < values.size() ? values.get(i) : null;
            fields.get(i).setText(formatInteger(value));
        }
    }

    private Integer parseRequiredInteger(TextField field, String fieldName) {
        Integer value = parseOptionalInteger(field.getText());
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must be an integer.");
        }
        return value;
    }

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

    private List<Integer> parseSlotValues(List<TextField> fields, String fieldName) {
        if (fields.size() != HellConfigLayout.ENEMY_SLOT_COUNT) {
            throw new IllegalStateException("Unexpected slot field count: " + fields.size());
        }
        return fields.stream()
                .map(field -> parseRequiredInteger(field, fieldName + " slot"))
                .toList();
    }

    private String formatInteger(Integer value) {
        return value == null ? "" : Integer.toString(value);
    }

    private String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private String formatLayer(HellStageDescriptor stage) {
        return HellStageStatusFormatter.formatLayerSummary(stage);
    }
}
