package com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BheMapCatalogLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BsdxMapBaselineLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendStatistics;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapOutputPreflightResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapOutputConflictException;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputWriter;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapPendingOutputFile;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapSafeOutputWriter;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.plan.BheMapAppendPlanBuilder;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.preview.BheMapPreviewMaterializer;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceMaterializationResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceMaterializer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * BHE map import into current BSDX resource tree 的入口编排。
 *
 * <p>当前入口构建可复用的 BHE map import plan，并按 plan 物料化 preview。
 * 后续写入当前目标资源树时继续消费同一份 plan，不能重新推导地图清单或命名。</p>
 */
public class BheMapImportPipeline {

    private final BheMapCatalogLoader bheMapCatalogLoader;
    private final BsdxMapBaselineLoader bsdxMapBaselineLoader;
    private final BheMapAppendPlanBuilder planBuilder;
    private final BheMapPreviewMaterializer previewMaterializer;
    private final BheMapImportOutputWriter outputWriter;
    private final BheMapResourceMaterializer resourceMaterializer;
    private final BheMapSafeOutputWriter safeOutputWriter;
    private final BheMapImportManifestBuilder manifestBuilder = new BheMapImportManifestBuilder();

    public BheMapImportPipeline() {
        this(
                new BheMapCatalogLoader(),
                new BsdxMapBaselineLoader(),
                new BheMapAppendPlanBuilder(),
                new BheMapPreviewMaterializer(),
                new BheMapImportOutputWriter(),
                new BheMapResourceMaterializer(),
                new BheMapSafeOutputWriter()
        );
    }

    public BheMapImportPipeline(
            BheMapCatalogLoader bheMapCatalogLoader,
            BsdxMapBaselineLoader bsdxMapBaselineLoader,
            BheMapAppendPlanBuilder planBuilder,
            BheMapPreviewMaterializer previewMaterializer
    ) {
        this(
                bheMapCatalogLoader,
                bsdxMapBaselineLoader,
                planBuilder,
                previewMaterializer,
                new BheMapImportOutputWriter(),
                new BheMapResourceMaterializer(),
                new BheMapSafeOutputWriter()
        );
    }

    public BheMapImportPipeline(
            BheMapCatalogLoader bheMapCatalogLoader,
            BsdxMapBaselineLoader bsdxMapBaselineLoader,
            BheMapAppendPlanBuilder planBuilder,
            BheMapPreviewMaterializer previewMaterializer,
            BheMapImportOutputWriter outputWriter
    ) {
        this(
                bheMapCatalogLoader,
                bsdxMapBaselineLoader,
                planBuilder,
                previewMaterializer,
                outputWriter,
                new BheMapResourceMaterializer(),
                new BheMapSafeOutputWriter()
        );
    }

    public BheMapImportPipeline(
            BheMapCatalogLoader bheMapCatalogLoader,
            BsdxMapBaselineLoader bsdxMapBaselineLoader,
            BheMapAppendPlanBuilder planBuilder,
            BheMapPreviewMaterializer previewMaterializer,
            BheMapImportOutputWriter outputWriter,
            BheMapResourceMaterializer resourceMaterializer
    ) {
        this(
                bheMapCatalogLoader,
                bsdxMapBaselineLoader,
                planBuilder,
                previewMaterializer,
                outputWriter,
                resourceMaterializer,
                new BheMapSafeOutputWriter()
        );
    }

    public BheMapImportPipeline(
            BheMapCatalogLoader bheMapCatalogLoader,
            BsdxMapBaselineLoader bsdxMapBaselineLoader,
            BheMapAppendPlanBuilder planBuilder,
            BheMapPreviewMaterializer previewMaterializer,
            BheMapImportOutputWriter outputWriter,
            BheMapResourceMaterializer resourceMaterializer,
            BheMapSafeOutputWriter safeOutputWriter
    ) {
        this.bheMapCatalogLoader = bheMapCatalogLoader;
        this.bsdxMapBaselineLoader = bsdxMapBaselineLoader;
        this.planBuilder = planBuilder;
        this.previewMaterializer = previewMaterializer;
        this.outputWriter = outputWriter == null ? new BheMapImportOutputWriter() : outputWriter;
        this.resourceMaterializer = resourceMaterializer == null
                ? new BheMapResourceMaterializer()
                : resourceMaterializer;
        this.safeOutputWriter = safeOutputWriter == null ? new BheMapSafeOutputWriter() : safeOutputWriter;
    }

    public BheMapAppendResult buildImportPlan(BheMapAppendRequest request) {
        return buildImportPlan(request, request == null ? null : request.resolveBsdxMapGroupPath(), true);
    }

    private BheMapAppendResult buildImportPlan(
            BheMapAppendRequest request,
            Path targetMapGroupPath,
            boolean previewMaterializationEnabled
    ) {
        BheMapAppendResult result = new BheMapAppendResult();
        if (request == null) {
            return result;
        }

        BheMapAppendAudit audit = new BheMapAppendAudit();
        BheMapCatalog sourceCatalog = bheMapCatalogLoader.load(request.resolveBheMapGroupPath(), request.getCharset());
        BsdxMapBaseline bsdxBaseline = bsdxMapBaselineLoader.load(targetMapGroupPath, request.getCharset());
        BheMapAppendPlan plan = planBuilder.build(
                sourceCatalog,
                request.resolveBheMapDir(),
                request.resolveBheStaticResourceRoot(),
                bsdxBaseline,
                request.getCharset(),
                audit
        );

        if (previewMaterializationEnabled) {
            previewMaterializer.materialize(
                    plan,
                    request.resolveBheStaticResourceRoot(),
                    request.resolveBsdxPreviewFallbackRoots(),
                    request.resolveOutputRoot(),
                    audit
            );
        }

        result.setOutputRoot(request.resolveOutputRoot());
        result.setAppendPlan(plan);
        result.setAudit(audit);
        result.setPreviewMaterializationEnabled(previewMaterializationEnabled);
        result.setPreviewMaterialized(previewMaterializationEnabled);
        result.setStatistics(BheMapAppendStatistics.from(plan, audit));
        return result;
    }

    public BheMapAppendResult importMapsIntoCurrentResourceTree(BheMapAppendRequest request) {
        if (request == null) {
            return new BheMapAppendResult();
        }

        Path currentTargetMapGroupPath = request.resolveCurrentTargetMapGroupPath();
        BheMapAppendResult result = buildImportPlan(
                request,
                currentTargetMapGroupPath,
                request.isPreviewMaterializationEnabled()
        );
        if (result.getAppendPlan() == null) {
            return result;
        }

        try {
            BheMapImportOutputResult outputResult = outputWriter.prepare(
                    result.getAppendPlan(),
                    currentTargetMapGroupPath,
                    request.resolveOutputRoot(),
                    request.getCharset()
            );
            result.setOutputResult(outputResult);
            result.setOutputRoot(outputResult.getOutputRoot());

            BheMapOutputPreflightResult outputPreflightResult =
                    safeOutputWriter.preflight(outputResult.getPendingOutputFiles());
            if (outputPreflightResult.getConflictCount() > 0) {
                outputResult.setOutputConflictCount(outputPreflightResult.getConflictCount());
                outputResult.getConflicts().addAll(outputPreflightResult.getConflicts());
                outputResult.getPendingOutputFiles().clear();
                result.setOutputWritten(false);
                result.setResourcesMaterialized(false);
                return attachManifest(result);
            }

            BheMapResourceMaterializationResult resourceResult = resourceMaterializer.prepare(
                    result.getAppendPlan(),
                    outputResult,
                    outputResult.getOutputRoot(),
                    request.getCharset()
            );
            result.setResourceMaterializationResult(resourceResult);

            List<BheMapPendingOutputFile> pendingFiles = new ArrayList<>();
            pendingFiles.addAll(outputResult.getPendingOutputFiles());
            pendingFiles.addAll(resourceResult.getPendingOutputFiles());
            BheMapOutputPreflightResult preflightResult = safeOutputWriter.preflight(pendingFiles);
            if (preflightResult.getConflictCount() > 0) {
                outputResult.setOutputConflictCount(preflightResult.getConflictCount());
                outputResult.getConflicts().addAll(preflightResult.getConflicts());
                outputResult.getPendingOutputFiles().clear();
                resourceResult.getPendingOutputFiles().clear();
                result.setOutputWritten(false);
                result.setResourcesMaterialized(false);
                return attachManifest(result);
            }

            safeOutputWriter.commit(preflightResult);
            outputWriter.applyCommit(outputResult, preflightResult);
            resourceMaterializer.applyCommit(resourceResult, preflightResult);
            outputWriter.verifyCommittedOutput(outputResult, request.getCharset());
            outputResult.getPendingOutputFiles().clear();
            resourceResult.getPendingOutputFiles().clear();
            result.setOutputWritten(true);
            result.setResourcesMaterialized(true);
            return attachManifest(result);
        } catch (BheMapOutputConflictException e) {
            BheMapImportOutputResult outputResult = result.getOutputResult();
            if (outputResult == null) {
                outputResult = new BheMapImportOutputResult();
                outputResult.setOutputRoot(request.resolveOutputRoot());
                outputResult.setTotalPlanEntries(result.getAppendPlan().size());
                result.setOutputResult(outputResult);
            }
            outputResult.setOutputConflictCount(outputResult.getOutputConflictCount() + 1);
            outputResult.getConflicts().add(e.getConflict());
            result.setOutputWritten(false);
            return attachManifest(result);
        } catch (IOException e) {
            throw new IllegalStateException("BHE map import output failed", e);
        }
    }

    private BheMapAppendResult attachManifest(BheMapAppendResult result) {
        result.setImportManifest(manifestBuilder.build(result));
        return result;
    }
}
