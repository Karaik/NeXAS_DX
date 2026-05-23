package com.giga.nexas.transfer.bhe2bsdx.mapappend;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BheMapCatalogLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BsdxMapBaselineLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendStatistics;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.plan.BheMapAppendPlanBuilder;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.preview.MaterializeMapPreviewAssetsStep;

/**
 * BHE 地图追加 Step1 总入口。
 *
 * <p>Step1 的主体是构建完整 BheMapAppendPlan；
 * preview 复制只是按 plan 物料化，不代表地图追加完成。</p>
 */
public class BheMapAppendPipeline {

    private final BheMapCatalogLoader bheMapCatalogLoader;
    private final BsdxMapBaselineLoader bsdxMapBaselineLoader;
    private final BheMapAppendPlanBuilder planBuilder;
    private final MaterializeMapPreviewAssetsStep materializeMapPreviewAssetsStep;

    public BheMapAppendPipeline() {
        this(
                new BheMapCatalogLoader(),
                new BsdxMapBaselineLoader(),
                new BheMapAppendPlanBuilder(),
                new MaterializeMapPreviewAssetsStep()
        );
    }

    public BheMapAppendPipeline(
            BheMapCatalogLoader bheMapCatalogLoader,
            BsdxMapBaselineLoader bsdxMapBaselineLoader,
            BheMapAppendPlanBuilder planBuilder,
            MaterializeMapPreviewAssetsStep materializeMapPreviewAssetsStep
    ) {
        this.bheMapCatalogLoader = bheMapCatalogLoader;
        this.bsdxMapBaselineLoader = bsdxMapBaselineLoader;
        this.planBuilder = planBuilder;
        this.materializeMapPreviewAssetsStep = materializeMapPreviewAssetsStep;
    }

    public BheMapAppendResult executeStep1(BheMapAppendRequest request) {
        BheMapAppendResult result = new BheMapAppendResult();
        if (request == null) {
            return result;
        }

        BheMapAppendAudit audit = new BheMapAppendAudit();
        BheMapCatalog sourceCatalog = bheMapCatalogLoader.load(request.resolveBheMapGroupPath(), request.getCharset());
        BsdxMapBaseline bsdxBaseline = bsdxMapBaselineLoader.load(request.resolveBsdxMapGroupPath(), request.getCharset());
        BheMapAppendPlan plan = planBuilder.build(
                sourceCatalog,
                request.resolveBheMapDir(),
                request.resolveBheStaticResourceRoot(),
                bsdxBaseline,
                request.getCharset(),
                audit
        );

        materializeMapPreviewAssetsStep.materialize(
                plan,
                request.resolveBheStaticResourceRoot(),
                request.resolveBsdxPreviewFallbackRoots(),
                request.resolveOutputRoot(),
                audit
        );

        result.setOutputRoot(request.resolveOutputRoot());
        result.setAppendPlan(plan);
        result.setAudit(audit);
        result.setPreviewMaterialized(true);
        result.setStatistics(BheMapAppendStatistics.from(plan, audit));
        return result;
    }
}
