package com.giga.nexas.controller;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.consts.OperandDocEntry;
import com.giga.nexas.dto.bsdx.bin.consts.OperandDocRegistry;
import com.giga.nexas.dto.bsdx.bin.pseudo.recognizer.BsdxBinRecognizer;
import com.giga.nexas.dto.bsdx.bin.pseudo.renderer.BsdxBinRenderer;
import com.giga.nexas.service.BsdxBinService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BinPseudoEditorController {

    private static final Map<Path, Stage> OPEN_WINDOWS = new HashMap<>();
    private static final String DEFAULT_CHARSET = "windows-31j";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    @FXML private BorderPane root;
    @FXML private Label fileLabel;
    @FXML private TextArea pseudoArea;
    @FXML private Label statusLabel;
    @FXML private RadioButton asmModeButton;
    @FXML private RadioButton pseudoModeButton;
    @FXML private Button reloadButton;
    @FXML private Button compileButton;
    @FXML private TextField searchField;
    @FXML private Button searchPreviousButton;
    @FXML private Button searchNextButton;
    @FXML private Label searchStatusLabel;

    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BsdxBinRenderer renderer = new BsdxBinRenderer();
    private final BsdxBinRecognizer recognizer = new BsdxBinRecognizer();
    private final Popup operandPopup = new Popup();
    private final Label operandPopupLabel = new Label();

    private Path binPath;
    private String documentLogicalName;
    private Supplier<Path> readPathSupplier;
    private Supplier<Path> compileOutputPathSupplier;
    private Runnable onSuccessfulCompile = () -> { };
    private String fallbackCharset = DEFAULT_CHARSET;
    private Consumer<String> logger;
    private Bin currentBin;
    private Stage stage;
    private boolean suppressDirtyTracking;
    private boolean suppressModeEvents;
    private boolean suppressSearchEvents;
    private boolean dirty;
    private EditorMode currentMode = EditorMode.PSEUDO;
    private String currentLosslessPseudoText = "";
    private String statusBaseText = "";
    private final Map<EditorMode, SearchState> searchStates = new HashMap<>();

    /**
     * 主界面内嵌脚本编辑器的加载结果。
     *
     * <p>这个结果对象同时携带编辑器视图和控制器。
     */
    public record EmbeddedEditorHandle(
            BorderPane root,
            BinPseudoEditorController controller
    ) {
    }

    public static void openEditor(
            Path binPath,
            String charset,
            Window owner,
            Consumer<String> logger
    ) {
        if (binPath == null) {
            return;
        }
        Path normalized = binPath.toAbsolutePath().normalize();
        Stage existing = OPEN_WINDOWS.get(normalized);
        if (existing != null) {
            existing.toFront();
            existing.requestFocus();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(BinPseudoEditorController.class.getResource("/fxml/BinPseudoEditorView.fxml"));
            Scene scene = new Scene(loader.load(), 1080, 780);
            BinPseudoEditorController controller = loader.getController();
            Stage stage = new Stage();
            if (owner != null) {
                stage.initOwner(owner);
            }
            controller.bindStage(stage);
            controller.initializeDocument(normalized, charset, logger);
            stage.setScene(scene);
            stage.show();
            OPEN_WINDOWS.put(normalized, stage);
            stage.setOnHidden(event -> OPEN_WINDOWS.remove(normalized));
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to open BIN pseudo editor for " + normalized, ex);
        }
    }

    /**
     * 把 BIN 编辑器以主界面内嵌组件形式加载出来。
     *
     * <p>这个入口返回内嵌视图和控制器。
     * 读盘、回编、tooltip、模式切换逻辑与编辑器本体一致。
     */
    public static EmbeddedEditorHandle createEmbeddedEditor(
            Path binPath,
            String charset,
            Consumer<String> logger
    ) {
        if (binPath == null) {
            throw new IllegalArgumentException("BIN path must not be null.");
        }
        return createEmbeddedEditor(
                binPath.getFileName().toString(),
                () -> binPath.toAbsolutePath().normalize(),
                () -> binPath.toAbsolutePath().normalize(),
                charset,
                logger,
                () -> { }
        );
    }

    /**
     * 把 BIN 编辑器以主界面内嵌组件形式加载出来，并允许读路径与回编输出路径分离。
     *
     * <p>这条入口用于 Hell 编辑器。
     * 关卡切换时只读取当前命中的脚本，
     * 真正发生回编时才向 `mod` 申请可写路径。
     */
    public static EmbeddedEditorHandle createEmbeddedEditor(
            String logicalFileName,
            Supplier<Path> readPathSupplier,
            Supplier<Path> compileOutputPathSupplier,
            String charset,
            Consumer<String> logger,
            Runnable onSuccessfulCompile
    ) {
        if (logicalFileName == null || logicalFileName.isBlank()) {
            throw new IllegalArgumentException("BIN logical file name must not be blank.");
        }
        if (readPathSupplier == null || compileOutputPathSupplier == null) {
            throw new IllegalArgumentException("BIN read path supplier must not be null.");
        }
        try {
            FXMLLoader loader = new FXMLLoader(BinPseudoEditorController.class.getResource("/fxml/BinPseudoEditorView.fxml"));
            BorderPane root = loader.load();
            BinPseudoEditorController controller = loader.getController();
            controller.initializeDocument(
                    logicalFileName,
                    readPathSupplier,
                    compileOutputPathSupplier,
                    charset,
                    logger,
                    onSuccessfulCompile
            );
            return new EmbeddedEditorHandle(root, controller);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load embedded BIN editor for " + logicalFileName, ex);
        }
    }

    @FXML
    public void initialize() {
        configureOperandPopup();
        ToggleGroup modeGroup = new ToggleGroup();
        asmModeButton.setToggleGroup(modeGroup);
        pseudoModeButton.setToggleGroup(modeGroup);
        pseudoModeButton.setSelected(true);
        asmModeButton.setOnAction(event -> switchMode(EditorMode.ASM));
        pseudoModeButton.setOnAction(event -> switchMode(EditorMode.PSEUDO));
        reloadButton.setOnAction(event -> reloadFromDisk());
        compileButton.setOnAction(event -> compileBack());
        searchPreviousButton.setOnAction(event -> findPrevious());
        searchNextButton.setOnAction(event -> findNext());
        searchField.setOnAction(event -> findNext());
        searchField.textProperty().addListener((obs, oldText, newText) -> updateSearchText(newText));
        pseudoArea.selectedTextProperty().addListener((obs, oldText, newText) -> scheduleOperandTooltipUpdate());
        pseudoArea.caretPositionProperty().addListener((obs, oldValue, newValue) -> scheduleOperandTooltipUpdate());
        pseudoArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!suppressDirtyTracking) {
                setDirty(true);
            }
            hideOperandPopup();
            refreshSearchStatus();
        });
        refreshSearchControls();
    }

    private void bindStage(Stage stage) {
        this.stage = stage;
        stage.xProperty().addListener((obs, oldValue, newValue) -> hideOperandPopup());
        stage.yProperty().addListener((obs, oldValue, newValue) -> hideOperandPopup());
        stage.widthProperty().addListener((obs, oldValue, newValue) -> hideOperandPopup());
        stage.heightProperty().addListener((obs, oldValue, newValue) -> hideOperandPopup());
        stage.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                hideOperandPopup();
            }
        });
        stage.iconifiedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                hideOperandPopup();
            }
        });
    }

    private void initializeDocument(Path binPath, String charset, Consumer<String> logger) {
        this.documentLogicalName = binPath.getFileName().toString();
        this.readPathSupplier = () -> binPath;
        this.compileOutputPathSupplier = () -> binPath;
        this.fallbackCharset = (charset == null || charset.isBlank()) ? DEFAULT_CHARSET : charset;
        this.logger = logger == null ? text -> { } : logger;
        refreshFileLabel();
        reloadFromDisk();
    }

    /**
     * 初始化一个“读路径”和“写路径”分离的文档。
     */
    private void initializeDocument(
            String logicalFileName,
            Supplier<Path> readPathSupplier,
            Supplier<Path> compileOutputPathSupplier,
            String charset,
            Consumer<String> logger,
            Runnable onSuccessfulCompile
    ) {
        this.documentLogicalName = logicalFileName;
        this.readPathSupplier = readPathSupplier;
        this.compileOutputPathSupplier = compileOutputPathSupplier;
        this.onSuccessfulCompile = onSuccessfulCompile == null ? () -> { } : onSuccessfulCompile;
        this.fallbackCharset = (charset == null || charset.isBlank()) ? DEFAULT_CHARSET : charset;
        this.logger = logger == null ? text -> { } : logger;
        refreshFileLabel();
        reloadFromDisk();
    }

    private void reloadFromDisk() {
        if (readPathSupplier == null) {
            return;
        }
        try {
            Path readPath = resolveReadPath();
            ResponseDTO<?> dto = bsdxBinService.parse(readPath.toString(), fallbackCharset);
            Bin parsed = (Bin) dto.getData();
            parsed.setExtensionName("bin");
            binPath = readPath;
            currentBin = parsed;
            refreshEditorText();
            setDirty(false);
            updateEditorHintStatus("Loaded " + currentMode.label + " view for " + binPath.getFileName());
            logger.accept("Opened pseudo code editor for " + binPath.getFileName());
        } catch (Exception ex) {
            suppressDirtyTracking = false;
            showError("Failed to load BIN pseudo code", ex);
        }
    }

    private void compileBack() {
        compileCurrentDocument();
    }

    /**
     * 执行当前文档的回编。
     *
     * <p>返回值用于外层离开确认逻辑：
     * 成功时继续切换，
     * 失败时停留在当前脚本。
     */
    public boolean compileCurrentDocument() {
        if (currentBin == null) {
            return false;
        }
        Path outputPath;
        try {
            outputPath = resolveCompileOutputPath();
        } catch (Exception ex) {
            showError("Failed to prepare writable BIN target", new IllegalStateException(ex));
            return false;
        }
        try {
            Bin compiled = currentMode == EditorMode.ASM
                    ? recognizer.compileStrict(currentBin, pseudoArea.getText())
                    : recognizer.compile(currentBin, rebuildLosslessPseudoForCompile());
            compiled.setExtensionName("bin");
            compiled.setCharset(resolveCharset());
            bsdxBinService.generate(outputPath.toString(), compiled, resolveCharset());
            onSuccessfulCompile.run();
            binPath = outputPath.toAbsolutePath().normalize();
            currentBin = compiled;
            reloadFromDisk();
            setStatus("Compiled back to " + binPath.getFileName());
            logger.accept("Compiled pseudo code back into " + binPath.getFileName());
            return true;
        } catch (Exception ex) {
            showError("Failed to compile pseudo code back into BIN", ex);
            return false;
        }
    }

    /**
     * 丢弃当前未保存的文本改动，并从当前读层重新加载文档。
     */
    public void discardUnsavedChangesByReload() {
        reloadFromDisk();
    }

    /**
     * 当前编辑器是否存在未回编的改动。
     */
    public boolean hasUnsavedChanges() {
        return dirty;
    }

    /**
     * 当前文档的逻辑文件名。
     *
     * <p>这个名字稳定对应 `HellXXX.bin`，
     * 不随当前读层从 ROOT 切到 MOD 而变化。
     */
    public String getDocumentLogicalName() {
        return documentLogicalName;
    }

    /**
     * 当前已经解析进编辑器的物理读路径。
     */
    public Path getLoadedPath() {
        return binPath;
    }

    private Path resolveReadPath() {
        Path resolved = readPathSupplier == null ? binPath : readPathSupplier.get();
        if (resolved == null) {
            throw new IllegalStateException("BIN read path is unavailable.");
        }
        return resolved.toAbsolutePath().normalize();
    }

    private Path resolveCompileOutputPath() {
        Path resolved = compileOutputPathSupplier == null ? binPath : compileOutputPathSupplier.get();
        if (resolved == null) {
            throw new IllegalStateException("BIN compile output path is unavailable.");
        }
        return resolved.toAbsolutePath().normalize();
    }

    private String resolveCharset() {
        if (currentBin != null && currentBin.getCharset() != null && !currentBin.getCharset().isBlank()) {
            return currentBin.getCharset();
        }
        return fallbackCharset;
    }

    private void setStatus(String text) {
        statusBaseText = text == null ? "" : text;
        refreshStatusLabel();
    }

    private void updateEditorHintStatus(String baseText) {
        if (currentMode != EditorMode.PSEUDO) {
            setStatus(baseText);
            return;
        }

        int documentedCount = countDocumentedFunctionTokens(pseudoArea.getText());
        if (documentedCount > 0) {
            setStatus(baseText + " Select a function name to view docs. " + documentedCount + " documented function names found in this file.");
            return;
        }
        setStatus(baseText + " Select a function name to view docs.");
    }

    private void switchMode(EditorMode targetMode) {
        if (suppressModeEvents || targetMode == currentMode) {
            return;
        }
        if (dirty && currentMode == EditorMode.ASM && targetMode != EditorMode.ASM) {
            boolean discard = confirmDiscardAsmEdits();
            if (!discard) {
                restoreModeSelection();
                return;
            }
        }
        currentMode = targetMode;
        refreshEditorText();
        setDirty(false);
        restoreSearchControls();
        updateEditorHintStatus(targetMode == EditorMode.ASM
                ? "ASM mode: strict reversible editing enabled."
                : "Pseudo mode: lossless pseudo enabled; unchanged lines replay exact IR.");
    }

    private void refreshEditorText() {
        if (currentBin == null) {
            return;
        }
        suppressDirtyTracking = true;
        if (currentMode == EditorMode.ASM) {
            pseudoArea.setText(renderer.renderStrictPseudo(currentBin));
            currentLosslessPseudoText = "";
        } else {
            currentLosslessPseudoText = renderer.renderPseudo(currentBin);
            pseudoArea.setText(stripPseudoMetadata(currentLosslessPseudoText));
        }
        pseudoArea.positionCaret(0);
        pseudoArea.setEditable(true);
        compileButton.setDisable(false);
        suppressDirtyTracking = false;
        hideOperandPopup();
        restoreModeSelection();
        restoreSearchControls();
    }

    private void updateSearchText(String text) {
        if (suppressSearchEvents) {
            return;
        }
        SearchState state = currentSearchState();
        state.query = text == null ? "" : text;
        state.lastMatchStart = -1;
        state.lastMatchEnd = -1;
        refreshSearchStatus();
    }

    private void restoreSearchControls() {
        suppressSearchEvents = true;
        searchField.setText(currentSearchState().query);
        suppressSearchEvents = false;
        refreshSearchStatus();
    }

    private void refreshSearchControls() {
        restoreSearchControls();
    }

    private SearchState currentSearchState() {
        return searchStates.computeIfAbsent(currentMode, ignored -> new SearchState());
    }

    private void findNext() {
        findInCurrentView(true);
    }

    private void findPrevious() {
        findInCurrentView(false);
    }

    private void findInCurrentView(boolean forward) {
        SearchState state = currentSearchState();
        String query = normalizeSearchQuery(searchField.getText());
        state.query = query;
        if (query.isEmpty()) {
            state.lastMatchStart = -1;
            state.lastMatchEnd = -1;
            refreshSearchStatus();
            return;
        }

        String text = pseudoArea.getText();
        if (text == null || text.isEmpty()) {
            state.lastMatchStart = -1;
            state.lastMatchEnd = -1;
            refreshSearchStatus();
            return;
        }

        int matchStart = forward ? findNextIndex(text, query, state) : findPreviousIndex(text, query, state);
        if (matchStart < 0) {
            state.lastMatchStart = -1;
            state.lastMatchEnd = -1;
            refreshSearchStatus();
            return;
        }

        state.lastMatchStart = matchStart;
        state.lastMatchEnd = matchStart + query.length();
        pseudoArea.requestFocus();
        pseudoArea.selectRange(state.lastMatchStart, state.lastMatchEnd);
        refreshSearchStatus();
    }

    private int findNextIndex(String text, String query, SearchState state) {
        int start = state.lastMatchEnd >= 0 ? state.lastMatchEnd : pseudoArea.getCaretPosition();
        if (start < 0 || start > text.length()) {
            start = 0;
        }
        int found = indexOfIgnoreCase(text, query, start);
        return found >= 0 ? found : indexOfIgnoreCase(text, query, 0);
    }

    private int findPreviousIndex(String text, String query, SearchState state) {
        int start = state.lastMatchStart >= 0 ? state.lastMatchStart - 1 : pseudoArea.getCaretPosition() - 1;
        if (start >= text.length()) {
            start = text.length() - 1;
        }
        int found = lastIndexOfIgnoreCase(text, query, start);
        return found >= 0 ? found : lastIndexOfIgnoreCase(text, query, text.length() - 1);
    }

    private int indexOfIgnoreCase(String text, String query, int fromIndex) {
        int max = text.length() - query.length();
        for (int i = Math.max(0, fromIndex); i <= max; i++) {
            if (text.regionMatches(true, i, query, 0, query.length())) {
                return i;
            }
        }
        return -1;
    }

    private int lastIndexOfIgnoreCase(String text, String query, int fromIndex) {
        int max = text.length() - query.length();
        for (int i = Math.min(fromIndex, max); i >= 0; i--) {
            if (text.regionMatches(true, i, query, 0, query.length())) {
                return i;
            }
        }
        return -1;
    }

    private void refreshSearchStatus() {
        if (searchStatusLabel == null) {
            return;
        }
        SearchState state = currentSearchState();
        String query = normalizeSearchQuery(state.query);
        if (query.isEmpty()) {
            searchStatusLabel.setText("");
            return;
        }
        int count = countMatchesIgnoreCase(pseudoArea.getText(), query);
        if (count <= 0) {
            searchStatusLabel.setText("0 matches");
            return;
        }
        int current = state.lastMatchStart >= 0 ? countMatchesBefore(pseudoArea.getText(), query, state.lastMatchStart) + 1 : 0;
        searchStatusLabel.setText(current <= 0 ? count + " matches" : current + " / " + count);
    }

    private int countMatchesIgnoreCase(String text, String query) {
        if (text == null || text.isEmpty() || query.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while (index <= text.length() - query.length()) {
            int found = indexOfIgnoreCase(text, query, index);
            if (found < 0) {
                break;
            }
            count++;
            index = found + Math.max(1, query.length());
        }
        return count;
    }

    private int countMatchesBefore(String text, String query, int beforeIndex) {
        if (text == null || query.isEmpty() || beforeIndex <= 0) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while (index < beforeIndex) {
            int found = indexOfIgnoreCase(text, query, index);
            if (found < 0 || found >= beforeIndex) {
                break;
            }
            count++;
            index = found + Math.max(1, query.length());
        }
        return count;
    }

    private String normalizeSearchQuery(String query) {
        return query == null ? "" : query;
    }

    private void restoreModeSelection() {
        suppressModeEvents = true;
        asmModeButton.setSelected(currentMode == EditorMode.ASM);
        pseudoModeButton.setSelected(currentMode == EditorMode.PSEUDO);
        suppressModeEvents = false;
    }

    private boolean confirmDiscardAsmEdits() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        if (stage != null) {
            alert.initOwner(stage);
        }
        alert.setTitle("Switch mode");
        alert.setHeaderText("Discard unsaved ASM edits?");
        alert.setContentText("Pseudo mode reloads from the current BIN model. Unsaved ASM text changes will be lost.");
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private String rebuildLosslessPseudoForCompile() {
        return buildPseudoSourceForCompile(pseudoArea.getText(), currentLosslessPseudoText);
    }

    /**
     * 构建 pseudo compile 入口最终喂给 recognizer 的文本。
     *
     * <p>当前策略分两种：
     * 1. 文本未改动时，直接回放完整的 lossless pseudo 原文，保持精确回编
     * 2. 文本有改动时，只提交当前可见 pseudo 文本，避免“部分行走 IR 回放、部分行走语义编译”时丢指令
     */
    static String buildPseudoSourceForCompile(String currentVisibleText, String originalLosslessText) {
        if (originalLosslessText == null || originalLosslessText.isEmpty()) {
            return currentVisibleText == null ? "" : currentVisibleText;
        }

        String normalizedCurrent = normalizePseudoSource(currentVisibleText);
        String normalizedOriginalVisible = normalizePseudoSource(stripPseudoMetadataStatic(originalLosslessText));
        if (normalizedCurrent.equals(normalizedOriginalVisible)) {
            return originalLosslessText;
        }
        return mergeVisibleAndLosslessText(currentVisibleText, originalLosslessText);
    }

    static String mergeVisibleAndLosslessText(String currentVisibleText, String originalLosslessText) {
        String[] currentVisibleLines = splitLinesStatic(currentVisibleText);
        String[] originalLosslessLines = splitLinesStatic(originalLosslessText);
        String[] originalVisibleLines = splitLinesStatic(stripPseudoMetadataStatic(originalLosslessText));
        int[] preservedLineMapping = mapPreservedLines(currentVisibleLines, originalVisibleLines);

        StringBuilder merged = new StringBuilder();
        int currentIndex = 0;
        int originalIndex = 0;
        while (currentIndex < currentVisibleLines.length) {
            int matchedOriginalIndex = preservedLineMapping[currentIndex];
            if (matchedOriginalIndex >= 0 && matchedOriginalIndex == originalIndex) {
                merged.append(originalLosslessLines[originalIndex]).append("\r\n");
                currentIndex++;
                originalIndex++;
                continue;
            }

            int blockCurrentStart = currentIndex;
            int blockOriginalStart = originalIndex;
            while (currentIndex < currentVisibleLines.length) {
                int nextMatchedOriginal = preservedLineMapping[currentIndex];
                if (nextMatchedOriginal >= 0 && nextMatchedOriginal >= originalIndex) {
                    break;
                }
                currentIndex++;
            }
            int blockCurrentEnd = currentIndex;
            int blockOriginalEnd = blockCurrentEnd < currentVisibleLines.length
                    ? preservedLineMapping[blockCurrentEnd]
                    : originalVisibleLines.length;
            if (blockOriginalEnd < blockOriginalStart) {
                blockOriginalEnd = blockOriginalStart;
            }

            int pairedCount = Math.min(blockCurrentEnd - blockCurrentStart, blockOriginalEnd - blockOriginalStart);
            for (int i = 0; i < pairedCount; i++) {
                merged.append(mergeVisibleLineWithOriginalMetadata(
                        currentVisibleLines[blockCurrentStart + i],
                        originalLosslessLines[blockOriginalStart + i]
                )).append("\r\n");
            }
            for (int i = blockCurrentStart + pairedCount; i < blockCurrentEnd; i++) {
                merged.append(currentVisibleLines[i]).append("\r\n");
            }
            originalIndex = blockOriginalEnd;
        }
        return merged.toString();
    }

    static String mergeVisibleLineWithOriginalMetadata(String currentVisibleLine, String originalLosslessLine) {
        if (originalLosslessLine == null) {
            return currentVisibleLine == null ? "" : currentVisibleLine;
        }
        int marker = originalLosslessLine.indexOf("// IRHASH=");
        if (marker < 0) {
            return currentVisibleLine == null ? "" : currentVisibleLine;
        }
        String visible = currentVisibleLine == null ? "" : currentVisibleLine.stripTrailing();
        String suffix = originalLosslessLine.substring(marker);
        if (visible.isEmpty()) {
            return suffix;
        }
        return visible + " " + suffix;
    }

    static int[] mapPreservedLines(String[] currentVisibleLines, String[] originalVisibleLines) {
        int currentCount = currentVisibleLines == null ? 0 : currentVisibleLines.length;
        int originalCount = originalVisibleLines == null ? 0 : originalVisibleLines.length;
        int[][] lcs = new int[currentCount + 1][originalCount + 1];
        for (int i = currentCount - 1; i >= 0; i--) {
            for (int j = originalCount - 1; j >= 0; j--) {
                if (currentVisibleLines[i].equals(originalVisibleLines[j])) {
                    lcs[i][j] = lcs[i + 1][j + 1] + 1;
                } else {
                    lcs[i][j] = Math.max(lcs[i + 1][j], lcs[i][j + 1]);
                }
            }
        }

        int[] mapping = new int[currentCount];
        java.util.Arrays.fill(mapping, -1);
        int i = 0;
        int j = 0;
        while (i < currentCount && j < originalCount) {
            if (currentVisibleLines[i].equals(originalVisibleLines[j])
                    && lcs[i][j] == lcs[i + 1][j + 1] + 1) {
                mapping[i] = j;
                i++;
                j++;
                continue;
            }
            if (lcs[i + 1][j] >= lcs[i][j + 1]) {
                i++;
            } else {
                j++;
            }
        }
        return mapping;
    }

    private String stripPseudoMetadata(String text) {
        return stripPseudoMetadataStatic(text);
    }

    /**
     * 去掉编辑器可见 pseudo 文本里不展示的 IR 注释尾巴。
     */
    static String stripPseudoMetadataStatic(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : splitLinesStatic(text)) {
            int marker = line.indexOf("// IRHASH=");
            out.append(marker >= 0 ? line.substring(0, marker).stripTrailing() : line);
            out.append("\r\n");
        }
        return out.toString();
    }

    private String[] splitLines(String text) {
        return splitLinesStatic(text);
    }

    /**
     * 统一 pseudo 文本的换行切分规则。
     */
    static String[] splitLinesStatic(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        if (lines.length > 0 && lines[lines.length - 1].isEmpty()) {
            String[] trimmed = new String[lines.length - 1];
            System.arraycopy(lines, 0, trimmed, 0, trimmed.length);
            return trimmed;
        }
        return lines;
    }

    /**
     * 统一 compile 入口使用的文本归一化规则。
     */
    static String normalizePseudoSource(String text) {
        return String.join("\n", splitLinesStatic(text));
    }

    private void setDirty(boolean dirty) {
        this.dirty = dirty;
        refreshFileLabel();
        refreshStatusLabel();
        if (stage != null) {
            String title = "BIN Pseudo Code [" + currentMode.label + "] - " + (documentLogicalName == null ? "" : documentLogicalName);
            if (dirty) {
                title += " *";
            }
            stage.setTitle(title);
        }
    }

    /**
     * 刷新文件标签。
     *
     * <p>内嵌模式下没有窗口标题栏，
     * 所以未保存状态直接显示在 `fileLabel` 上，避免用户看不见脏状态。
     */
    private void refreshFileLabel() {
        String logicalName = documentLogicalName != null && !documentLogicalName.isBlank()
                ? documentLogicalName
                : (binPath == null ? "-" : binPath.getFileName().toString());
        String layerTag = "";
        if (binPath != null && binPath.getParent() != null) {
            layerTag = "mod".equalsIgnoreCase(binPath.getParent().getFileName().toString()) ? " [MOD]" : " [ROOT]";
        }
        fileLabel.setText(logicalName + layerTag + (dirty ? " *" : ""));
    }

    /**
     * 刷新状态栏文本。
     *
     * <p>状态栏保留原始提示，同时在脏状态下附加未保存标记。
     */
    private void refreshStatusLabel() {
        if (dirty) {
            String logicalName = documentLogicalName == null ? "current script" : documentLogicalName;
            statusLabel.setText((statusBaseText == null || statusBaseText.isBlank() ? "" : statusBaseText + " ") + "Unsaved changes in " + logicalName + ".");
            return;
        }
        statusLabel.setText(statusBaseText);
    }

    private void showError(String header, Exception ex) {
        setStatus(header + ": " + ex.getMessage());
        logger.accept(header + ": " + ex.getMessage());
        Alert alert = new Alert(Alert.AlertType.ERROR);
        Window owner = resolveOwnerWindow();
        if (owner != null) {
            alert.initOwner(owner);
        }
        alert.setTitle("BIN pseudo editor");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }

    private void scheduleOperandTooltipUpdate() {
        Platform.runLater(this::updateOperandTooltipForSelection);
    }

    private void updateOperandTooltipForSelection() {
        if (currentMode != EditorMode.PSEUDO || currentBin == null || !(pseudoArea.getSkin() instanceof TextAreaSkin skin)) {
            hideOperandPopup();
            return;
        }

        String selectedText = pseudoArea.getSelectedText();
        if (selectedText == null || selectedText.isBlank() || selectedText.indexOf('\n') >= 0 || selectedText.indexOf('\r') >= 0) {
            hideOperandPopup();
            return;
        }

        OperandDocEntry entry = resolveOperandDocEntryFromSelection(selectedText);
        if (entry == null) {
            hideOperandPopup();
            return;
        }

        Bounds screenBounds = resolveSelectionAnchorScreenBounds(skin);
        if (screenBounds == null) {
            hideOperandPopup();
            return;
        }

        double anchorX = screenBounds.getMinX();
        double anchorY = screenBounds.getMaxY() + 6;
        showOperandPopup(entry.toTooltipText(), anchorX, anchorY);
    }

    private void configureOperandPopup() {
        operandPopup.setAutoFix(true);
        operandPopup.setAutoHide(false);
        operandPopup.setHideOnEscape(true);

        operandPopupLabel.setWrapText(true);
        operandPopupLabel.setMaxWidth(420);
        operandPopupLabel.setStyle(
                "-fx-background-color: rgba(20,20,20,0.96);"
                        + "-fx-text-fill: white;"
                        + "-fx-padding: 10 12 10 12;"
                        + "-fx-background-radius: 8;"
                        + "-fx-border-color: rgba(255,255,255,0.12);"
                        + "-fx-border-radius: 8;"
                        + "-fx-font-size: 12px;"
        );

        StackPane container = new StackPane(operandPopupLabel);
        container.setStyle("-fx-background-color: transparent;");
        operandPopup.getContent().setAll(container);
    }

    private void showOperandPopup(String text, double anchorX, double anchorY) {
        operandPopupLabel.setText(text);
        if (operandPopup.isShowing()) {
            operandPopup.hide();
        }
        if (stage != null) {
            operandPopup.show(stage, anchorX, anchorY);
            return;
        }
        Window window = pseudoArea.getScene() == null ? null : pseudoArea.getScene().getWindow();
        if (window != null) {
            operandPopup.show(window, anchorX, anchorY);
        }
    }

    private void hideOperandPopup() {
        operandPopup.hide();
    }

    /**
     * 统一解析当前弹窗模式或内嵌模式下可用的 owner window。
     */
    private Window resolveOwnerWindow() {
        if (stage != null) {
            return stage;
        }
        return root == null || root.getScene() == null ? null : root.getScene().getWindow();
    }

    private OperandDocEntry resolveOperandDocEntryFromSelection(String selectedText) {
        String normalized = selectedText == null ? "" : selectedText.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        OperandDocEntry direct = OperandDocRegistry.findByName(normalized);
        if (direct != null) {
            return direct;
        }

        if (normalized.startsWith("call ")) {
            normalized = normalized.substring("call ".length()).trim();
        } else if (normalized.startsWith("syscall ")) {
            normalized = normalized.substring("syscall ".length()).trim();
        }

        int paren = normalized.indexOf('(');
        if (paren >= 0) {
            normalized = normalized.substring(0, paren).trim();
        }

        Matcher matcher = TOKEN_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        return OperandDocRegistry.findByName(matcher.group());
    }

    private Bounds resolveSelectionAnchorScreenBounds(TextAreaSkin skin) {
        Bounds selectionBounds = resolveSelectedTextScreenBounds(skin);
        if (selectionBounds != null) {
            return selectionBounds;
        }
        Bounds caretBounds = resolveCaretScreenBounds(skin);
        if (caretBounds != null) {
            return caretBounds;
        }
        return null;
    }

    private Bounds resolveSelectedTextScreenBounds(TextAreaSkin skin) {
        var selection = pseudoArea.getSelection();
        if (selection == null || selection.getLength() <= 0) {
            return null;
        }

        int anchorIndex = Math.max(selection.getEnd() - 1, selection.getStart());
        Rectangle2D charBounds = skin.getCharacterBounds(anchorIndex);
        if (charBounds == null) {
            return null;
        }
        return pseudoArea.localToScreen(new BoundingBox(
                charBounds.getMinX(),
                charBounds.getMinY(),
                Math.max(1, charBounds.getWidth()),
                Math.max(1, charBounds.getHeight())
        ));
    }

    private Bounds resolveCaretScreenBounds(TextAreaSkin skin) {
        Bounds caretBounds = skin.getCaretBounds();
        if (caretBounds == null) {
            return null;
        }
        return pseudoArea.localToScreen(caretBounds);
    }

    private int countDocumentedFunctionTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        Set<String> documented = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            if (OperandDocRegistry.findByName(token) != null) {
                documented.add(token);
            }
        }
        return documented.size();
    }

    private static class SearchState {
        private String query = "";
        private int lastMatchStart = -1;
        private int lastMatchEnd = -1;
    }

    private enum EditorMode {
        ASM("ASM"),
        PSEUDO("Pseudo");

        private final String label;

        EditorMode(String label) {
            this.label = label;
        }
    }
}
