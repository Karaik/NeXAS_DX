package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.nio.file.Path;

/**
 * BHE 地图追加执行结果。
 *
 * <p>当前结果只代表 Step1：完整计划已构建，preview 已按计划尝试物料化。</p>
 */
@Data
public class BheMapAppendResult {

    private Path outputRoot;
    private BheMapAppendPlan appendPlan;
    private BheMapAppendAudit audit = new BheMapAppendAudit();
    private BheMapAppendStatistics statistics = new BheMapAppendStatistics();
    private boolean previewMaterialized;
}
