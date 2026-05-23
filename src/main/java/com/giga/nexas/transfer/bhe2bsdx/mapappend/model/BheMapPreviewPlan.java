package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.nio.file.Path;

/**
 * 单张地图预览图的 Step1 计划与物料化结果。
 *
 * <p>命名在 plan 构建阶段确定；物料化阶段只能填充 sourcePath、outputPath、status 和 problem，
 * 不能重新推导源/目标文件名。</p>
 */
@Data
public class BheMapPreviewPlan {

    private String sourcePreviewFileName;
    private String targetPreviewFileName;
    private String bsdxPreviewFallbackFileName;
    private BheMapPreviewSourceKind sourceKind;
    private Path sourcePath;
    private Path outputPath;
    private BheMapPreviewStatus status = BheMapPreviewStatus.PENDING;
    private String problem;

    public boolean hasBsdxPreviewFallback() {
        return bsdxPreviewFallbackFileName != null && !bsdxPreviewFallbackFileName.isBlank();
    }
}
