package com.giga.nexas.transfer.bhe2bsdx.mapappend.preview;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblem;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblemSeverity;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblemType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapPreviewPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapPreviewSourceKind;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapPreviewStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.CaseInsensitiveFileIndex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 按 Step1 plan 物料化地图预览图。
 *
 * <p>本 step 只消费 BheMapEntryPlan 中已经确定的源/目标预览图名；
 * 不允许重新扫描 MapGroup，也不允许自己拼接 groupResourceName。</p>
 */
public class MaterializeMapPreviewAssetsStep {

    public void materialize(
            BheMapAppendPlan plan,
            Path bheStaticResourceRoot,
            List<Path> bsdxPreviewFallbackRoots,
            Path outputRoot,
            BheMapAppendAudit audit
    ) {
        if (plan == null || outputRoot == null) {
            return;
        }

        try {
            Files.createDirectories(outputRoot);
            CaseInsensitiveFileIndex bhePreviewIndex = CaseInsensitiveFileIndex.fromTree(bheStaticResourceRoot);
            List<CaseInsensitiveFileIndex> fallbackIndexes = buildFallbackIndexes(bsdxPreviewFallbackRoots);
            for (BheMapEntryPlan entryPlan : plan.getEntries()) {
                materializeOne(entryPlan, bhePreviewIndex, fallbackIndexes, outputRoot, audit);
            }
        } catch (IOException e) {
            throw new IllegalStateException("按计划复制 BHE 地图预览图失败", e);
        }
    }

    private void materializeOne(
            BheMapEntryPlan entryPlan,
            CaseInsensitiveFileIndex bhePreviewIndex,
            List<CaseInsensitiveFileIndex> fallbackIndexes,
            Path outputRoot,
            BheMapAppendAudit audit
    ) throws IOException {
        if (entryPlan == null) {
            return;
        }
        BheMapPreviewPlan previewPlan = entryPlan.getPreviewPlan();
        if (previewPlan == null) {
            return;
        }

        Path source = bhePreviewIndex.resolveByName(previewPlan.getSourcePreviewFileName());
        BheMapPreviewSourceKind sourceKind = BheMapPreviewSourceKind.BHE;
        if (source == null && previewPlan.hasBsdxPreviewFallback()) {
            source = resolveFallbackPreview(fallbackIndexes, previewPlan.getBsdxPreviewFallbackFileName());
            sourceKind = BheMapPreviewSourceKind.BSDX_FALLBACK;
        }

        if (source == null) {
            String message = "缺少地图预览图: "
                    + previewPlan.getSourcePreviewFileName()
                    + missingFallbackSuffix(previewPlan);
            previewPlan.setStatus(BheMapPreviewStatus.MISSING_NON_BLOCKING);
            previewPlan.setProblem(message);
            BheMapAppendProblem problem = problem(entryPlan, message);
            entryPlan.addProblem(problem);
            if (audit != null) {
                audit.addMissingPreview(problem);
            }
            return;
        }

        Path output = outputRoot.resolve(previewPlan.getTargetPreviewFileName());
        Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        previewPlan.setSourceKind(sourceKind);
        previewPlan.setSourcePath(source);
        previewPlan.setOutputPath(output);
        previewPlan.setStatus(BheMapPreviewStatus.COPIED);
        if (audit != null) {
            audit.addCopiedPreview(output);
            audit.addNote("按计划复制地图预览图: "
                    + sourceKind
                    + " "
                    + source.getFileName()
                    + " -> "
                    + output.getFileName());
        }
    }

    private List<CaseInsensitiveFileIndex> buildFallbackIndexes(List<Path> fallbackRoots) {
        if (fallbackRoots == null || fallbackRoots.isEmpty()) {
            return List.of();
        }
        return fallbackRoots.stream()
                .map(CaseInsensitiveFileIndex::fromTree)
                .toList();
    }

    private Path resolveFallbackPreview(List<CaseInsensitiveFileIndex> fallbackIndexes, String fileName) {
        if (fallbackIndexes == null || fallbackIndexes.isEmpty()) {
            return null;
        }
        for (CaseInsensitiveFileIndex index : fallbackIndexes) {
            Path source = index.resolveByName(fileName);
            if (source != null) {
                return source;
            }
        }
        return null;
    }

    private String missingFallbackSuffix(BheMapPreviewPlan previewPlan) {
        if (previewPlan == null || !previewPlan.hasBsdxPreviewFallback()) {
            return "";
        }
        return "，BSDX fallback 也缺失: " + previewPlan.getBsdxPreviewFallbackFileName();
    }

    private BheMapAppendProblem problem(BheMapEntryPlan entryPlan, String message) {
        BheMapAppendProblem problem = new BheMapAppendProblem();
        problem.setType(BheMapAppendProblemType.MISSING_PREVIEW);
        problem.setSeverity(BheMapAppendProblemSeverity.NON_BLOCKING);
        problem.setMessage(message);
        if (entryPlan != null) {
            problem.setSourceGroupResourceName(entryPlan.getSourceGroupResourceName());
            problem.setSourceMapFileName(entryPlan.getSourceMapFileName());
            if (entryPlan.getPreviewPlan() != null) {
                problem.setTargetName(entryPlan.getPreviewPlan().getTargetPreviewFileName());
                problem.setReferenceText(entryPlan.getPreviewPlan().getSourcePreviewFileName());
            }
        }
        return problem;
    }
}
