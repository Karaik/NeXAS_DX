package com.giga.nexas.controller;

import com.giga.nexas.controller.model.*;
import com.giga.nexas.service.engine.BinaryEngineAdapter;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TreeItem;
import lombok.RequiredArgsConstructor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * 负责执行操作按钮相关逻辑的控制器。
 */
@RequiredArgsConstructor
public class ActionButtonController {

    private final MainViewController view;
    private final WorkspaceState state;
    private final BranchGridController gridController;
    private final Function<EngineType, BinaryEngineAdapter> adapterFactory;

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "nexas-batch-worker");
        t.setDaemon(true);
        return t;
    });

    public void bind() {
        view.getActionButton().setOnAction(e -> runSelected());
        view.getProcessAllButton().setOnAction(e -> runAll());
        gridController.setActionHandler(this::handleCardRequest);
    }

    private void handleCardRequest(WorkspaceCategory category,
                                   BranchActionType actionType,
                                   List<Path> files) {
        if ((files == null || files.isEmpty()) && category == null) {
            logLater("Select a category or file to run.");
            return;
        }
        if (files != null && !files.isEmpty()) {
            runFileBatch(files, actionType);
            return;
        }
        runCategoryBatch(List.of(category),
                actionType == BranchActionType.PARSE,
                actionType == BranchActionType.GENERATE);
    }

    private void runSelected() {
        TreeItem<WorkspaceTreeNode> selected = view.getTree().getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) {
            logLater("Select a category or file to run.");
            return;
        }
        WorkspaceTreeNode node = selected.getValue();
        WorkspaceCategory category = node.getCategory();
        Path file = node.getFilePath();
        WorkspaceNodeKind kind = node.getKind();

        switch (kind) {
            case FILE_BINARY -> runSingleParse(category, file);
            case FILE_JSON -> runSingleGenerate(category, file);
            case GROUP_BINARY -> runCategoryBatch(List.of(category), true, false);
            case GROUP_JSON -> runCategoryBatch(List.of(category), false, true);
            case CATEGORY -> runCategoryBatch(List.of(category), true, true);
            case ROOT -> logLater("Select a category or file to run.");
        }
    }

    private void runAll() {
        List<WorkspaceCategory> categories = new ArrayList<>(state.getCategories());
        if (categories.isEmpty()) {
            logLater("Nothing to process. Load a directory first.");
            return;
        }
        int fileCount = categories.stream()
                .mapToInt(c -> c.getBinaryFiles().size() + c.getJsonFiles().size())
                .sum();
        String confirmMessage = fileCount + " files across " + categories.size() + " categories will be processed.\n"
                + "This may take some time for large directories.";
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, confirmMessage, ButtonType.OK, ButtonType.CANCEL);
        confirm.setTitle("Run All");
        confirm.setHeaderText("Run all files in the current input directory?");
        if (view.getRoot().getScene() != null) {
            confirm.initOwner(view.getRoot().getScene().getWindow());
        }
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        runCategoryBatch(categories, true, true);
    }

    private void runCategoryBatch(List<WorkspaceCategory> categories, boolean runParse, boolean runGenerate) {
        if (categories.isEmpty() || (!runParse && !runGenerate)) {
            return;
        }
        BinaryEngineAdapter adapter = createAdapter();
        String charset = resolveCharset();
        Path inputRoot = state.getInputDirectory().get();
        Path outputRoot = determineOutputRoot();

        int totalOps = 0;
        if (runParse) {
            totalOps += categories.stream().mapToInt(c -> c.getBinaryFiles().size()).sum();
        }
        if (runGenerate) {
            totalOps += categories.stream().mapToInt(c -> c.getJsonFiles().size()).sum();
        }

        if (totalOps == 0) {
            initializeProgress(0);
            logLater("No files matched the selected actions.");
            return;
        }

        final int totalTasks = totalOps;
        initializeProgress(totalTasks);
        AtomicInteger completed = new AtomicInteger();
        String label = buildLabel(runParse && runGenerate ? "Data batch" : (runParse ? "Parse" : "Generate"), categories.size());
        runAsync(label, true, () -> {
            BatchStats parseStats = new BatchStats();
            BatchStats generateStats = new BatchStats();

            for (WorkspaceCategory category : categories) {
                if (runParse && category.isCanParse()) {
                    parseCategory(adapter, category, inputRoot, outputRoot, charset, parseStats, completed, totalTasks);
                }
                if (runGenerate && category.isCanGenerate()) {
                    generateCategory(adapter, category, inputRoot, outputRoot, charset, generateStats, completed, totalTasks);
                }
            }

            if (runParse) {
                logLater(summaryText("Parse", parseStats));
                parseStats.failures().forEach(detail -> logLater(" - " + detail));
            }
            if (runGenerate) {
                logLater(summaryText("Generate", generateStats));
                generateStats.failures().forEach(detail -> logLater(" - " + detail));
            }
        });
    }

    private void runSingleParse(WorkspaceCategory category, Path file) {
        if (category == null || file == null) {
            logLater("Select a binary file to parse.");
            return;
        }
        runFileBatch(List.of(file), BranchActionType.PARSE);
    }

    private void runSingleGenerate(WorkspaceCategory category, Path file) {
        if (category == null || file == null) {
            logLater("Select a JSON file to generate.");
            return;
        }
        runFileBatch(List.of(file), BranchActionType.GENERATE);
    }

    private void runFileBatch(List<Path> files, BranchActionType type) {
        if (files == null || files.isEmpty()) {
            logLater("No files selected for " + (type == BranchActionType.PARSE ? "parse" : "generate") + ".");
            return;
        }
        BinaryEngineAdapter adapter = createAdapter();
        String charset = resolveCharset();
        Path inputRoot = state.getInputDirectory().get();
        Path outputRoot = determineOutputRoot();

        initializeProgress(files.size());
        AtomicInteger completed = new AtomicInteger();
        String label = type == BranchActionType.PARSE ? "Parse selected" : "Generate selected";
        runAsync(label, true, () -> {
            BatchStats stats = new BatchStats();
            for (Path file : files) {
                String name = file.getFileName().toString();
                try {
                    Path targetDir = resolveOutputDir(outputRoot, inputRoot, file);
                    if (type == BranchActionType.PARSE) {
                        adapter.parse(file, targetDir, charset);
                        logLater("Parsed " + name);
                    } else {
                        adapter.generate(file, targetDir, charset);
                        logLater("Generated binary from " + name);
                    }
                    stats.success();
                } catch (Exception ex) {
                    String message = (type == BranchActionType.PARSE ? "Parse failed for " : "Generation failed for ")
                            + name + ": " + ex.getMessage();
                    stats.failure(message);
                    logLater(message);
                } finally {
                    int done = completed.incrementAndGet();
                    updateProgress(done, files.size(), name);
                }
            }
            logLater(summaryText(type == BranchActionType.PARSE ? "Parse" : "Generate", stats));
            stats.failures().forEach(detail -> logLater(" - " + detail));
        });
    }

    private BinaryEngineAdapter createAdapter() {
        return adapterFactory.apply(state.getEngineType().get());
    }

    private String resolveCharset() {
        String charset = state.getCharset().get();
        return charset != null ? charset : state.getEngineType().get().getDefaultCharset();
    }

    private void parseCategory(BinaryEngineAdapter adapter,
                               WorkspaceCategory category,
                               Path inputRoot,
                               Path outputRoot,
                               String charset,
                               BatchStats stats,
                               AtomicInteger completed,
                               int totalTasks) {
        for (Path file : category.getBinaryFiles()) {
            String name = file.getFileName().toString();
            try {
                Path targetDir = resolveOutputDir(outputRoot, inputRoot, file);
                adapter.parse(file, targetDir, charset);
                stats.success();
                logLater("Parsed " + name);
            } catch (Exception ex) {
                stats.failure("Parse failed for " + name + ": " + ex.getMessage());
                logLater("Parse failed for " + name + ": " + ex.getMessage());
            } finally {
                int done = completed.incrementAndGet();
                updateProgress(done, totalTasks, name);
            }
        }
    }

    private void generateCategory(BinaryEngineAdapter adapter,
                                  WorkspaceCategory category,
                                  Path inputRoot,
                                  Path outputRoot,
                                  String charset,
                                  BatchStats stats,
                                  AtomicInteger completed,
                                  int totalTasks) {
        for (Path file : category.getJsonFiles()) {
            String name = file.getFileName().toString();
            try {
                Path targetDir = resolveOutputDir(outputRoot, inputRoot, file);
                adapter.generate(file, targetDir, charset);
                stats.success();
                logLater("Generated binary from " + name);
            } catch (Exception ex) {
                stats.failure("Generation failed for " + name + ": " + ex.getMessage());
                logLater("Generation failed for " + name + ": " + ex.getMessage());
            } finally {
                int done = completed.incrementAndGet();
                updateProgress(done, totalTasks, name);
            }
        }
    }

    private Path resolveOutputDir(Path outputRoot, Path inputRoot, Path sourceFile) throws Exception {
        if (outputRoot == null) {
            throw new IllegalStateException("Output directory is not configured.");
        }
        if (inputRoot != null) {
            try {
                Path relative = inputRoot.relativize(sourceFile).getParent();
                if (relative != null) {
                    Path target = outputRoot.resolve(relative);
                    Files.createDirectories(target);
                    return target;
                }
            } catch (IllegalArgumentException ignored) {
                // If the relative path cannot be determined, fall back to the output root directory
            }
        }
        Files.createDirectories(outputRoot);
        return outputRoot;
    }

    private Path determineOutputRoot() {
        Path output = state.getOutputDirectory().get();
        if (output != null) {
            return output;
        }
        Path input = state.getInputDirectory().get();
        if (input != null) {
            return input;
        }
        return Paths.get(".");
    }

    private void runAsync(String label, boolean hadWork, Runnable work) {
        setButtonsDisabled(true);
        updateStatus(label + " in progress...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                work.run();
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            setButtonsDisabled(false);
            updateStatus(label + " completed.");
            completeProgress(label + " completed.", hadWork);
        });
        task.setOnFailed(e -> {
            setButtonsDisabled(false);
            Throwable ex = task.getException();
            updateStatus(label + " failed: " + (ex != null ? ex.getMessage() : "unknown error"));
            if (ex != null) {
                logLater("Operation failed: " + ex.getMessage());
            }
            completeProgress(label + " failed.", hadWork);
        });
        executor.submit(task);
    }

    private void updateStatus(String message) {
        Platform.runLater(() -> view.getStatusLabel().setText(message));
    }

    private void setButtonsDisabled(boolean disabled) {
        Platform.runLater(() -> {
            view.getActionButton().setDisable(disabled);
            view.getProcessAllButton().setDisable(disabled);
        });
    }

    private void logLater(String text) {
        Platform.runLater(() -> view.getLogArea().appendText(text + System.lineSeparator()));
    }

    private void initializeProgress(int total) {
        Platform.runLater(() -> {
            view.getProgressBar().setProgress(0);
            view.getProgressLabel().setText(total <= 0 ? "ERROR" : String.format("0 / %d", total));
        });
    }

    private void updateProgress(int completed, int total, String currentName) {
        Platform.runLater(() -> {
            if (total <= 0) {
                view.getProgressBar().setProgress(0);
            } else {
                double progress = Math.min(1.0, Math.max(0.0, completed / (double) total));
                view.getProgressBar().setProgress(progress);
                view.getProgressLabel().setText(String.format("%d / %d - %s", completed, total, currentName));
            }
        });
    }

    private void completeProgress(String message, boolean hadWork) {
        Platform.runLater(() -> {
            view.getProgressBar().setProgress(hadWork ? 1.0 : 0.0);
            view.getProgressLabel().setText(message);
        });
    }

    private String summaryText(String title, BatchStats stats) {
        return title + " summary: success " + stats.success + ", failed " + stats.failed;
    }

    private String buildLabel(String action, int categoryCount) {
        return action + " (" + categoryCount + " categories)";
    }

    private RuntimeException rethrow(Exception ex) {
        return ex instanceof RuntimeException ? (RuntimeException) ex : new RuntimeException(ex);
    }

    private static class BatchStats {
        private int success = 0;
        private int failed = 0;
        private final List<String> failureDetails = new ArrayList<>();

        void success() {
            success++;
        }

        void failure(String detail) {
            failed++;
            failureDetails.add(detail);
        }

        List<String> failures() {
            return failureDetails;
        }
    }
}
