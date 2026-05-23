package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

/**
 * BHE map append Step1 的验收统计。
 *
 * <p>统计只来自 plan 与 audit，不再额外扫描文件系统，避免测试结果和实际计划脱节。</p>
 */
@Data
public class BheMapAppendStatistics {

    private int bheMapGroupTotalCount;
    private int bheMapGroupMigratableCount;
    private int matchedMapFileCount;
    private int parsedMapFileCount;
    private int planEntryCount;
    private int previewCopiedCount;
    private int missingPreviewCount;
    private int blockingProblemCount;

    public static BheMapAppendStatistics from(BheMapAppendPlan plan, BheMapAppendAudit audit) {
        BheMapAppendStatistics statistics = new BheMapAppendStatistics();
        if (plan != null) {
            statistics.setBheMapGroupTotalCount(plan.getSourceMapGroupTotalCount());
            statistics.setBheMapGroupMigratableCount(plan.getSourceMigratableEntryCount());
            statistics.setPlanEntryCount(plan.size());
            statistics.setMatchedMapFileCount((int) plan.getEntries().stream()
                    .filter(BheMapEntryPlan::isSourceMapFound)
                    .count());
            statistics.setParsedMapFileCount((int) plan.getEntries().stream()
                    .filter(BheMapEntryPlan::isSourceMapParsed)
                    .count());
        }
        if (audit != null) {
            statistics.setPreviewCopiedCount(audit.getCopiedPreviewFiles().size());
            statistics.setMissingPreviewCount(audit.getMissingPreviewFiles().size());
            statistics.setBlockingProblemCount(audit.blockingProblemCount());
        }
        return statistics;
    }
}
