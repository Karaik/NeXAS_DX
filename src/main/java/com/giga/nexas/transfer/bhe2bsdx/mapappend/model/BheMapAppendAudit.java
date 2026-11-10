package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * BHE 地图追加审计结果。
 *
 * <p>审计对象只记录明确问题和实际产物，不做业务决策。
 * preview 缺失和内部资源缺失都不是 import plan construction 阻塞问题；源 .map 缺失、解析失败、目标撞名才算阻塞。</p>
 */
@Data
public class BheMapAppendAudit {

    private List<Path> copiedPreviewFiles = new ArrayList<>();
    private List<String> missingPreviewFiles = new ArrayList<>();
    private List<String> missingMapFiles = new ArrayList<>();
    private List<String> mapParseFailures = new ArrayList<>();
    private List<String> missingResourceFiles = new ArrayList<>();
    private List<String> targetNameCollisions = new ArrayList<>();
    private List<String> notes = new ArrayList<>();
    private List<BheMapAppendProblem> problems = new ArrayList<>();

    public void addCopiedPreview(Path path) {
        copiedPreviewFiles.add(path);
    }

    public void addMissingPreview(BheMapAppendProblem problem) {
        addProblem(problem);
        missingPreviewFiles.add(problem.getMessage());
    }

    public void addMissingMapFile(BheMapAppendProblem problem) {
        addProblem(problem);
        missingMapFiles.add(problem.getMessage());
    }

    public void addMapParseFailure(BheMapAppendProblem problem) {
        addProblem(problem);
        mapParseFailures.add(problem.getMessage());
    }

    public void addMissingResourceFile(BheMapAppendProblem problem) {
        addProblem(problem);
        missingResourceFiles.add(problem.getMessage());
    }

    public void addTargetNameCollision(BheMapAppendProblem problem) {
        addProblem(problem);
        targetNameCollisions.add(problem.getMessage());
    }

    public void addNote(String message) {
        notes.add(message);
    }

    public boolean hasBlockingProblems() {
        return blockingProblemCount() > 0;
    }

    public int blockingProblemCount() {
        return (int) problems.stream()
                .filter(problem -> problem.getSeverity() == BheMapAppendProblemSeverity.BLOCKING)
                .count();
    }

    public String blockingSummary() {
        List<String> lines = new ArrayList<>();
        lines.addAll(missingMapFiles);
        lines.addAll(mapParseFailures);
        lines.addAll(targetNameCollisions);
        return String.join("; ", lines);
    }

    private void addProblem(BheMapAppendProblem problem) {
        if (problem != null) {
            problems.add(problem);
        }
    }
}
