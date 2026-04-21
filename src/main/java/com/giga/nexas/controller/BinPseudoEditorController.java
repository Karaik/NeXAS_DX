package com.giga.nexas.controller;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.consts.OperandDocEntry;
import com.giga.nexas.dto.bsdx.bin.consts.OperandDocRegistry;
import com.giga.nexas.dto.bsdx.bin.pseudo.recognizer.BsdxBinRecognizer;
import com.giga.nexas.dto.bsdx.bin.pseudo.renderer.BsdxBinRenderer;
import com.giga.nexas.service.BsdxBinService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.skin.TextAreaSkin;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class BinPseudoEditorController {

    private static final Map<Path, Stage> OPEN_WINDOWS = new HashMap<>();
    private static final String DEFAULT_CHARSET = "windows-31j";

    @FXML private BorderPane root;
    @FXML private Label fileLabel;
    @FXML private TextArea pseudoArea;
    @FXML private Label statusLabel;
    @FXML private RadioButton asmModeButton;
    @FXML private RadioButton pseudoModeButton;
    @FXML private Button reloadButton;
    @FXML private Button compileButton;

    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BsdxBinRenderer renderer = new BsdxBinRenderer();
    private final BsdxBinRecognizer recognizer = new BsdxBinRecognizer();
    private final Tooltip operandTooltip = new Tooltip();

    private Path binPath;
    private String fallbackCharset = DEFAULT_CHARSET;
    private Consumer<String> logger;
    private Bin currentBin;
    private Stage stage;
    private boolean suppressDirtyTracking;
    private boolean suppressModeEvents;
    private boolean dirty;
    private EditorMode currentMode = EditorMode.ASM;
    private String currentLosslessPseudoText = "";

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

    @FXML
    public void initialize() {
        ToggleGroup modeGroup = new ToggleGroup();
        asmModeButton.setToggleGroup(modeGroup);
        pseudoModeButton.setToggleGroup(modeGroup);
        asmModeButton.setSelected(true);
        asmModeButton.setOnAction(event -> switchMode(EditorMode.ASM));
        pseudoModeButton.setOnAction(event -> switchMode(EditorMode.PSEUDO));
        reloadButton.setOnAction(event -> reloadFromDisk());
        compileButton.setOnAction(event -> compileBack());
        pseudoArea.addEventHandler(MouseEvent.MOUSE_MOVED, this::handlePseudoHover);
        pseudoArea.addEventHandler(MouseEvent.MOUSE_EXITED, event -> hideOperandTooltip());
        pseudoArea.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> hideOperandTooltip());
        pseudoArea.textProperty().addListener((obs, oldText, newText) -> {
            if (!suppressDirtyTracking) {
                setDirty(true);
            }
        });
    }

    private void bindStage(Stage stage) {
        this.stage = stage;
        stage.xProperty().addListener((obs, oldValue, newValue) -> hideOperandTooltip());
        stage.yProperty().addListener((obs, oldValue, newValue) -> hideOperandTooltip());
        stage.widthProperty().addListener((obs, oldValue, newValue) -> hideOperandTooltip());
        stage.heightProperty().addListener((obs, oldValue, newValue) -> hideOperandTooltip());
        stage.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                hideOperandTooltip();
            }
        });
        stage.iconifiedProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue) {
                hideOperandTooltip();
            }
        });
    }

    private void initializeDocument(Path binPath, String charset, Consumer<String> logger) {
        this.binPath = binPath;
        this.fallbackCharset = (charset == null || charset.isBlank()) ? DEFAULT_CHARSET : charset;
        this.logger = logger == null ? text -> { } : logger;
        fileLabel.setText(binPath.toString());
        reloadFromDisk();
    }

    private void reloadFromDisk() {
        if (binPath == null) {
            return;
        }
        try {
            ResponseDTO<?> dto = bsdxBinService.parse(binPath.toString(), fallbackCharset);
            Bin parsed = (Bin) dto.getData();
            parsed.setExtensionName("bin");
            currentBin = parsed;
            refreshEditorText();
            setDirty(false);
            setStatus("Loaded " + currentMode.label + " view for " + binPath.getFileName());
            logger.accept("Opened pseudo code editor for " + binPath.getFileName());
        } catch (Exception ex) {
            suppressDirtyTracking = false;
            showError("Failed to load BIN pseudo code", ex);
        }
    }

    private void compileBack() {
        if (binPath == null || currentBin == null) {
            return;
        }
        try {
            Bin compiled = currentMode == EditorMode.ASM
                    ? recognizer.compileStrict(currentBin, pseudoArea.getText())
                    : recognizer.compile(currentBin, rebuildLosslessPseudoForCompile());
            compiled.setExtensionName("bin");
            compiled.setCharset(resolveCharset());
            backupCurrentBinary();
            bsdxBinService.generate(binPath.toString(), compiled, resolveCharset());
            currentBin = compiled;
            reloadFromDisk();
            setStatus("Compiled back to " + binPath.getFileName());
            logger.accept("Compiled pseudo code back into " + binPath.getFileName());
        } catch (Exception ex) {
            showError("Failed to compile pseudo code back into BIN", ex);
        }
    }

    private void backupCurrentBinary() throws IOException {
        Path backupPath = binPath.resolveSibling(binPath.getFileName().toString() + ".bak");
        Files.copy(binPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private String resolveCharset() {
        if (currentBin != null && currentBin.getCharset() != null && !currentBin.getCharset().isBlank()) {
            return currentBin.getCharset();
        }
        return fallbackCharset;
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
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
        setStatus(targetMode == EditorMode.ASM
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
        restoreModeSelection();
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
        if (currentLosslessPseudoText == null || currentLosslessPseudoText.isEmpty()) {
            return pseudoArea.getText();
        }

        String[] originalLosslessLines = splitLines(currentLosslessPseudoText);
        String[] originalVisibleLines = splitLines(stripPseudoMetadata(currentLosslessPseudoText));
        String[] currentVisibleLines = splitLines(pseudoArea.getText());

        if (currentVisibleLines.length == originalVisibleLines.length) {
            StringBuilder out = new StringBuilder();
            for (int i = 0; i < currentVisibleLines.length; i++) {
                String current = currentVisibleLines[i];
                String originalVisible = originalVisibleLines[i];
                String line = current.equals(originalVisible) ? originalLosslessLines[i] : current;
                out.append(line).append("\r\n");
            }
            return out.toString();
        }

        int prefix = 0;
        while (prefix < currentVisibleLines.length
                && prefix < originalVisibleLines.length
                && currentVisibleLines[prefix].equals(originalVisibleLines[prefix])) {
            prefix++;
        }

        int suffix = 0;
        while (suffix < (currentVisibleLines.length - prefix)
                && suffix < (originalVisibleLines.length - prefix)
                && currentVisibleLines[currentVisibleLines.length - 1 - suffix]
                .equals(originalVisibleLines[originalVisibleLines.length - 1 - suffix])) {
            suffix++;
        }

        StringBuilder out = new StringBuilder();
        for (int i = 0; i < currentVisibleLines.length; i++) {
            String line;
            if (i < prefix) {
                line = originalLosslessLines[i];
            } else if (i >= currentVisibleLines.length - suffix) {
                int originalIndex = originalLosslessLines.length - (currentVisibleLines.length - i);
                line = originalLosslessLines[originalIndex];
            } else {
                line = currentVisibleLines[i];
            }
            out.append(line).append("\r\n");
        }
        return out.toString();
    }

    private String stripPseudoMetadata(String text) {
        StringBuilder out = new StringBuilder();
        for (String line : splitLines(text)) {
            int marker = line.indexOf("// IRHASH=");
            out.append(marker >= 0 ? line.substring(0, marker).stripTrailing() : line);
            out.append("\r\n");
        }
        return out.toString();
    }

    private String[] splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    private void setDirty(boolean dirty) {
        this.dirty = dirty;
        if (stage != null) {
            String title = "BIN Pseudo Code [" + currentMode.label + "] - " + (binPath == null ? "" : binPath.getFileName());
            if (dirty) {
                title += " *";
            }
            stage.setTitle(title);
        }
    }

    private void showError(String header, Exception ex) {
        setStatus(header + ": " + ex.getMessage());
        logger.accept(header + ": " + ex.getMessage());
        Alert alert = new Alert(Alert.AlertType.ERROR);
        if (stage != null) {
            alert.initOwner(stage);
        }
        alert.setTitle("BIN pseudo editor");
        alert.setHeaderText(header);
        alert.setContentText(ex.getMessage());
        alert.showAndWait();
    }

    private void handlePseudoHover(MouseEvent event) {
        if (currentMode != EditorMode.PSEUDO || currentBin == null) {
            hideOperandTooltip();
            return;
        }

        if (pseudoArea.getSkin() == null) {
            hideOperandTooltip();
            return;
        }
        TextAreaSkin skin = (TextAreaSkin) pseudoArea.getSkin();
        int charIndex = skin.getIndex(event.getX(), event.getY()).getCharIndex();
        String token = extractTokenAt(pseudoArea.getText(), charIndex);
        if (token == null) {
            hideOperandTooltip();
            return;
        }

        OperandDocEntry entry = OperandDocRegistry.findByName(token);
        if (entry == null) {
            hideOperandTooltip();
            return;
        }

        operandTooltip.setText(entry.toTooltipText());
        if (!operandTooltip.isShowing()) {
            var screen = pseudoArea.localToScreen(event.getX() + 14, event.getY() + 18);
            if (screen != null) {
                operandTooltip.show(pseudoArea, screen.getX(), screen.getY());
            }
            return;
        }
        var screen = pseudoArea.localToScreen(event.getX() + 14, event.getY() + 18);
        if (screen != null) {
            operandTooltip.setAnchorX(screen.getX());
            operandTooltip.setAnchorY(screen.getY());
        }
    }

    private void hideOperandTooltip() {
        operandTooltip.hide();
    }

    private String extractTokenAt(String text, int charIndex) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        int index = Math.max(0, Math.min(charIndex, text.length() - 1));
        if (!isTokenChar(text.charAt(index)) && index > 0 && isTokenChar(text.charAt(index - 1))) {
            index--;
        }
        if (!isTokenChar(text.charAt(index))) {
            return null;
        }
        int start = index;
        int end = index + 1;
        while (start > 0 && isTokenChar(text.charAt(start - 1))) {
            start--;
        }
        while (end < text.length() && isTokenChar(text.charAt(end))) {
            end++;
        }
        String token = text.substring(start, end);
        return Character.isLetter(token.charAt(0)) || token.charAt(0) == '_' ? token : null;
    }

    private boolean isTokenChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
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
