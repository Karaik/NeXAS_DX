package com.giga.nexas.controller;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.HellStageDescriptor;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Hell 内嵌脚本编辑器生命周期控制器。
 *
 * <p>负责脚本编辑器的创建、切换、脏状态确认和清理。
 */
public class HellEmbeddedScriptController {

    private final StackPane editorHost;
    private final Label placeholderLabel;
    private final Label editorStatusLabel;
    private final Consumer<String> logAppender;
    private final Supplier<BsdxOverlayResourceSession> sessionSupplier;
    private final Consumer<BsdxOverlayResourceSession> sessionUpdater;
    private final Supplier<String> charsetSupplier;
    private final Runnable afterCompileCallback;
    private final Supplier<Window> ownerWindowSupplier;
    private final BiConsumer<String, String> infoShower;

    private BinPseudoEditorController.EmbeddedEditorHandle embeddedEditorHandle;
    private LeaveScriptConfirmationHandler leaveScriptConfirmationHandler = this::showLeaveScriptConfirmation;

    public HellEmbeddedScriptController(
            StackPane editorHost,
            Label placeholderLabel,
            Label editorStatusLabel,
            Consumer<String> logAppender,
            Supplier<BsdxOverlayResourceSession> sessionSupplier,
            Consumer<BsdxOverlayResourceSession> sessionUpdater,
            Supplier<String> charsetSupplier,
            Runnable afterCompileCallback,
            Supplier<Window> ownerWindowSupplier,
            BiConsumer<String, String> infoShower
    ) {
        this.editorHost = editorHost;
        this.placeholderLabel = placeholderLabel;
        this.editorStatusLabel = editorStatusLabel;
        this.logAppender = logAppender;
        this.sessionSupplier = sessionSupplier;
        this.sessionUpdater = sessionUpdater;
        this.charsetSupplier = charsetSupplier;
        this.afterCompileCallback = afterCompileCallback;
        this.ownerWindowSupplier = ownerWindowSupplier;
        this.infoShower = infoShower;
    }

    /**
     * 把内嵌脚本编辑器同步到指定关卡。
     */
    public void syncToStage(HellStageDescriptor stage, boolean forceReload) {
        if (stage == null || stage.getScriptFileName() == null || stage.getScriptFileName().isBlank()) {
            clear("Selected stage has no script file.");
            return;
        }

        if (embeddedEditorHandle != null
                && stage.getScriptFileName().equalsIgnoreCase(embeddedEditorHandle.controller().getDocumentLogicalName())) {
            if (forceReload) {
                embeddedEditorHandle.controller().discardUnsavedChangesByReload();
                editorStatusLabel.setText("Reloaded " + stage.getScriptFileName() + " from the current overlay layer.");
            }
            return;
        }

        try {
            embeddedEditorHandle = BinPseudoEditorController.createEmbeddedEditor(
                    stage.getScriptFileName(),
                    () -> resolveScriptReadPath(stage.getScriptFileName()),
                    () -> resolveScriptCompileOutputPath(stage.getScriptFileName()),
                    charsetSupplier.get(),
                    logAppender,
                    afterCompileCallback
            );
            editorHost.getChildren().setAll(embeddedEditorHandle.root());
            editorStatusLabel.setText("Loaded " + stage.getScriptFileName() + " into the embedded script editor.");
        } catch (Exception ex) {
            clear("Failed to load current stage script.");
            infoShower.accept("Hell Script Editor", "Failed to load stage script: " + ex.getMessage());
        }
    }

    /**
     * 把脚本编辑区恢复成占位状态。
     */
    public void clear(String message) {
        embeddedEditorHandle = null;
        placeholderLabel.setText(message);
        editorHost.getChildren().setAll(placeholderLabel);
    }

    /**
     * 判断切换到指定关卡前是否允许离开当前脚本。
     */
    public boolean canSwitchToStage(HellStageDescriptor nextStage) {
        if (nextStage == null || nextStage.getScriptFileName() == null || nextStage.getScriptFileName().isBlank()) {
            return canLeaveCurrentScript("switch to a stage without script content");
        }
        if (embeddedEditorHandle == null) {
            return true;
        }
        String currentScriptFileName = embeddedEditorHandle.controller().getDocumentLogicalName();
        if (currentScriptFileName != null && currentScriptFileName.equalsIgnoreCase(nextStage.getScriptFileName())) {
            return true;
        }
        return canLeaveCurrentScript("switch to " + nextStage.getScriptFileName());
    }

    /**
     * 在离开当前脚本前统一处理脏状态确认。
     */
    public boolean canLeaveCurrentScript(String actionDescription) {
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

    private Path resolveScriptReadPath(String scriptFileName) {
        BsdxOverlayResourceSession session = sessionSupplier.get();
        if (session == null) {
            throw new IllegalStateException("No active BSDX overlay session.");
        }
        return session.resolveReadPath(scriptFileName)
                .orElseThrow(() -> new IllegalStateException("Missing script file: " + scriptFileName));
    }

    private Path resolveScriptCompileOutputPath(String scriptFileName) {
        BsdxOverlayResourceSession session = sessionSupplier.get();
        if (session == null) {
            throw new IllegalStateException("No active BSDX overlay session.");
        }
        try {
            Path writablePath = session.prepareWritablePath(scriptFileName);
            BsdxOverlayResourceSession refreshed = session.refresh();
            sessionUpdater.accept(refreshed);
            return writablePath;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to prepare writable script path for " + scriptFileName, ex);
        }
    }

    private LeaveScriptDecision showLeaveScriptConfirmation(String scriptFileName, String actionDescription) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        Window owner = ownerWindowSupplier.get();
        if (owner != null) {
            alert.initOwner(owner);
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
