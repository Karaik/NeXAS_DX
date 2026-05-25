package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceMaterializationResult;
import lombok.Data;

import java.nio.file.Path;

/**
 * BHE 地图追加执行结果。
 *
 * <p>当前结果代表 import plan 已构建，preview 已按计划尝试物料化。</p>
 */
@Data
public class BheMapAppendResult {

    private Path outputRoot;
    private BheMapAppendPlan appendPlan;
    private BheMapAppendAudit audit = new BheMapAppendAudit();
    private BheMapAppendStatistics statistics = new BheMapAppendStatistics();
    private BheMapImportOutputResult outputResult;
    private BheMapResourceMaterializationResult resourceMaterializationResult;
    private BheMapImportManifest importManifest;
    private boolean previewMaterialized;
    private boolean previewMaterializationEnabled;
    private boolean outputWritten;
    private boolean resourcesMaterialized;
}
