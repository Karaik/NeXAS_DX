package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 单张 BHE 地图进入 BSDX 追加流程前的完整 Step1 计划。
 *
 * <p>这里同时保留源 MapGroup 信息、源 .map 解析状态、目标命名和 .map 内部资源引用。
 * 后续阶段只能消费这些字段，不能重新扫描 MapGroup 或另起一套命名规则。</p>
 */
@Data
public class BheMapEntryPlan {

    private int sourceMapIndex;
    private String sourceGroupName;
    private String sourceGroupCodeName;
    private String sourceGroupResourceName;
    private int sourceMapGroupInt1;
    private int sourceMapGroupItemCount;
    private int sourceMapGroupPairArray1Count;
    private int sourceMapGroupArray2Count;
    private int sourceMapGroupArray3Count;
    private BheMapGroupEntrySnapshot sourceMapGroupSnapshot = new BheMapGroupEntrySnapshot();
    private String targetGroupResourceName;
    private String sourceMapFileName;
    private Path sourceMapPath;
    private boolean sourceMapFound;
    private boolean sourceMapParsed;
    private String sourceMapParseProblem;
    private BheMapDataSummary sourceMapDataSummary = new BheMapDataSummary();
    private String targetMapFileName;
    private String sourcePreviewFileName;
    private String targetPreviewFileName;
    private String bsdxPreviewFallbackFileName;
    private BheMapPreviewPlan previewPlan = new BheMapPreviewPlan();
    private BheMapResourceReferences resourceReferences = new BheMapResourceReferences();
    private List<String> blockingProblems = new ArrayList<>();
    private List<BheMapAppendProblem> problems = new ArrayList<>();

    public boolean hasBsdxPreviewFallback() {
        return bsdxPreviewFallbackFileName != null && !bsdxPreviewFallbackFileName.isBlank();
    }

    public void addBlockingProblem(String message) {
        if (message != null && !message.isBlank()) {
            blockingProblems.add(message);
        }
    }

    public void addProblem(BheMapAppendProblem problem) {
        if (problem == null) {
            return;
        }
        problems.add(problem);
        if (problem.getSeverity() == BheMapAppendProblemSeverity.BLOCKING) {
            addBlockingProblem(problem.getMessage());
        }
    }
}
