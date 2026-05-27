package com.giga.nexas.controller;

import com.giga.nexas.controller.model.HellEditorReferenceData;
import com.giga.nexas.controller.model.MekaLookupEntry;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Label;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.List;
import java.util.function.Supplier;

/**
 * 机体速查表的独立控制器。
 *
 * 负责：
 * 1. 构建速查表列和数据绑定
 * 2. 过滤逻辑
 * 3. 双击填值到当前焦点的 enemyType 字段
 * 4. enemyType 文本变化后实时刷新旁边的名称标签
 * 5. 焦点追踪（activeEnemyTypeField）
 */
public class HellMekaLookupController {

    private final TableView<MekaLookupEntry> mekaLookupTable;
    private final TextField mekaLookupFilterField;
    private final Label mekaLookupHintLabel;
    private final List<TextField> enemyTypeFields;
    private final List<Label> enemyTypeNameLabels;
    private final Supplier<HellEditorReferenceData> referenceDataSupplier;

    private final ObservableList<MekaLookupEntry> mekaLookupItems = FXCollections.observableArrayList();
    private final FilteredList<MekaLookupEntry> filteredMekaEntries = new FilteredList<>(mekaLookupItems, entry -> true);
    private final SortedList<MekaLookupEntry> sortedMekaEntries = new SortedList<>(filteredMekaEntries);

    /**
     * 当前正在编辑的敌机类型输入框。
     */
    private TextField activeEnemyTypeField;

    public HellMekaLookupController(
            TableView<MekaLookupEntry> mekaLookupTable,
            TextField mekaLookupFilterField,
            Label mekaLookupHintLabel,
            List<TextField> enemyTypeFields,
            List<Label> enemyTypeNameLabels,
            Supplier<HellEditorReferenceData> referenceDataSupplier
    ) {
        this.mekaLookupTable = mekaLookupTable;
        this.mekaLookupFilterField = mekaLookupFilterField;
        this.mekaLookupHintLabel = mekaLookupHintLabel;
        this.enemyTypeFields = enemyTypeFields;
        this.enemyTypeNameLabels = enemyTypeNameLabels;
        this.referenceDataSupplier = referenceDataSupplier;
    }

    /**
     * 初始化速查表和敌机输入框联动。
     */
    public void setup() {
        configureMekaLookupTable();
        bindEnemyTypeFields();
    }

    /**
     * 用新的机体引用数据刷新速查表。
     */
    public void setMekaEntries(List<MekaLookupEntry> entries) {
        mekaLookupItems.setAll(entries);
        refreshMekaLookupFilter();
    }

    /**
     * 清空速查表数据。
     */
    public void clearMekaEntries() {
        mekaLookupItems.clear();
    }

    /**
     * 刷新全部敌机名称标签。
     */
    public void refreshAllEnemyTypeNames() {
        for (int i = 0; i < enemyTypeFields.size(); i++) {
            refreshEnemyTypeName(i);
        }
    }

    /**
     * 清空全部敌机名称标签。
     */
    public void clearEnemyTypeNames() {
        for (Label label : enemyTypeNameLabels) {
            label.setText("-");
        }
    }

    /**
     * 刷新过滤。
     */
    public void refreshMekaLookupFilter() {
        String filter = mekaLookupFilterField.getText();
        String keyword = filter == null ? "" : filter.trim().toLowerCase();
        filteredMekaEntries.setPredicate(entry -> keyword.isEmpty() || entry.getSearchText().contains(keyword));
    }

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

        mekaLookupTable.getColumns().setAll(idColumn, nameColumn, codeColumn, fileColumn);
        mekaLookupTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        mekaLookupTable.setPlaceholder(new Label("No meka reference data loaded."));
        mekaLookupTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        sortedMekaEntries.comparatorProperty().bind(mekaLookupTable.comparatorProperty());
        mekaLookupTable.setItems(sortedMekaEntries);
        mekaLookupTable.setOnMouseClicked(event -> {
            if (event.getClickCount() >= 2) {
                fillFocusedEnemyTypeFromLookup();
            }
        });
    }

    private void bindEnemyTypeFields() {
        for (int i = 0; i < enemyTypeFields.size(); i++) {
            final int slotIndex = i;
            TextField field = enemyTypeFields.get(i);
            field.textProperty().addListener((obs, oldValue, newValue) -> refreshEnemyTypeName(slotIndex));
            field.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    activeEnemyTypeField = field;
                    mekaLookupHintLabel.setText("Focused enemy slot " + slotIndex + ". Double-click a meka row to fill this slot.");
                    syncLookupSelectionToEnemyTypeField(field);
                }
            });
        }
        mekaLookupHintLabel.setText("Focus an enemy type field, then double-click a meka row to fill it.");
    }

    private void fillFocusedEnemyTypeFromLookup() {
        MekaLookupEntry selectedEntry = mekaLookupTable.getSelectionModel().getSelectedItem();
        if (selectedEntry == null || activeEnemyTypeField == null || activeEnemyTypeField.isDisabled()) {
            return;
        }
        activeEnemyTypeField.setText(Integer.toString(selectedEntry.getMekaIndex()));
        activeEnemyTypeField.requestFocus();
        activeEnemyTypeField.positionCaret(activeEnemyTypeField.getText().length());
    }

    private void refreshEnemyTypeName(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= enemyTypeFields.size()) {
            return;
        }
        TextField field = enemyTypeFields.get(slotIndex);
        Label label = enemyTypeNameLabels.get(slotIndex);
        Integer mekaIndex = parseOptionalInteger(field.getText());
        HellEditorReferenceData refData = referenceDataSupplier.get();
        MekaLookupEntry entry = mekaIndex == null || refData == null ? null : refData.getMekaByIndex().get(mekaIndex);
        label.setText(entry == null ? "-" : formatMekaEntryInline(entry));
        if (field == activeEnemyTypeField) {
            syncLookupSelectionToEnemyTypeField(field);
        }
    }

    private void syncLookupSelectionToEnemyTypeField(TextField field) {
        Integer mekaIndex = parseOptionalInteger(field.getText());
        if (mekaIndex == null) {
            mekaLookupTable.getSelectionModel().clearSelection();
            return;
        }

        for (int i = 0; i < sortedMekaEntries.size(); i++) {
            MekaLookupEntry entry = sortedMekaEntries.get(i);
            if (entry.getMekaIndex() == mekaIndex) {
                mekaLookupTable.getSelectionModel().select(i);
                mekaLookupTable.scrollTo(i);
                return;
            }
        }
        mekaLookupTable.getSelectionModel().clearSelection();
    }

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
}
