package com.giga.nexas.transfer.bhe2bsdx.mapappend.plan;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblem;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblemSeverity;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblemType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapDataSummary;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReferences;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapSourceEntry;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.naming.BheMapAppendNamingPolicy;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.CaseInsensitiveFileIndex;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * BHE map import plan 构建器。
 *
 * <p>本类消费 BHE MapGroup 目录，定位并解析 BHE .map，
 * 再把目标命名和 .map 内部资源引用写入同一份 BheMapAppendPlan。</p>
 */
public class BheMapAppendPlanBuilder {

    private final BheMapAppendNamingPolicy namingPolicy;
    private final BheBinService bheBinService;

    public BheMapAppendPlanBuilder() {
        this(new BheMapAppendNamingPolicy(), new BheBinService());
    }

    public BheMapAppendPlanBuilder(BheMapAppendNamingPolicy namingPolicy, BheBinService bheBinService) {
        this.namingPolicy = namingPolicy == null ? new BheMapAppendNamingPolicy() : namingPolicy;
        this.bheBinService = bheBinService == null ? new BheBinService() : bheBinService;
    }

    public BheMapAppendPlan build(
            BheMapCatalog catalog,
            Path bheMapDir,
            Path bheStaticResourceRoot,
            BsdxMapBaseline bsdxBaseline,
            String charset,
            BheMapAppendAudit audit
    ) {
        BheMapAppendPlan plan = new BheMapAppendPlan();
        if (catalog == null) {
            return plan;
        }

        plan.setSourceMapGroupTotalCount(catalog.getTotalGroupCount());
        plan.setSourceMigratableEntryCount(catalog.getEntries().size());
        CaseInsensitiveFileIndex mapFileIndex = CaseInsensitiveFileIndex.fromFlatDirectory(bheMapDir);
        CaseInsensitiveFileIndex staticResourceIndex = CaseInsensitiveFileIndex.fromTree(bheStaticResourceRoot);
        Set<String> seenTargetGroupResourceNames = new LinkedHashSet<>();
        for (BheMapSourceEntry sourceEntry : catalog.getEntries()) {
            BheMapEntryPlan entryPlan = buildOne(sourceEntry, mapFileIndex, staticResourceIndex, charset, audit);
            auditTargetCollision(entryPlan, bsdxBaseline, audit);
            auditSameRunTargetDuplicate(entryPlan, seenTargetGroupResourceNames, audit);
            plan.getEntries().add(entryPlan);
        }
        return plan;
    }

    private BheMapEntryPlan buildOne(
            BheMapSourceEntry sourceEntry,
            CaseInsensitiveFileIndex mapFileIndex,
            CaseInsensitiveFileIndex staticResourceIndex,
            String charset,
            BheMapAppendAudit audit
    ) {
        BheMapEntryPlan entryPlan = namingPolicy.buildEntryPlan(sourceEntry);
        entryPlan.setSourceGroupName(sourceEntry.getGroupName());
        entryPlan.setSourceGroupCodeName(sourceEntry.getGroupCodeName());
        entryPlan.setSourceMapGroupInt1(sourceEntry.getMapGroupInt1());
        entryPlan.setSourceMapGroupItemCount(sourceEntry.getItemCount());
        entryPlan.setSourceMapGroupPairArray1Count(sourceEntry.getPairArray1Count());
        entryPlan.setSourceMapGroupArray2Count(sourceEntry.getArray2Count());
        entryPlan.setSourceMapGroupArray3Count(sourceEntry.getArray3Count());
        entryPlan.setSourceMapGroupSnapshot(sourceEntry.getMapGroupSnapshot());

        Path sourceMapPath = mapFileIndex.resolveByName(entryPlan.getSourceMapFileName());
        entryPlan.setSourceMapPath(sourceMapPath);
        entryPlan.setSourceMapFound(sourceMapPath != null);
        if (sourceMapPath == null) {
            String message = "缺少 BHE .map: " + entryPlan.getSourceMapFileName();
            BheMapAppendProblem problem = problem(
                    BheMapAppendProblemType.MISSING_MAP_FILE,
                    BheMapAppendProblemSeverity.BLOCKING,
                    entryPlan,
                    null,
                    null,
                    message
            );
            addProblem(audit, entryPlan, problem);
            return entryPlan;
        }

        try {
            MapData mapData = (MapData) bheBinService.parse(sourceMapPath.toString(), charset).getData();
            entryPlan.setSourceMapParsed(true);
            entryPlan.setSourceMapDataSummary(buildMapDataSummary(mapData));
            entryPlan.setResourceReferences(collectResourceReferences(mapData, staticResourceIndex));
            for (String missing : entryPlan.getResourceReferences().getMissingResourceFiles()) {
                String message = entryPlan.getSourceMapFileName() + " 缺少内部引用资源: " + missing;
                BheMapAppendProblem problem = problem(
                        BheMapAppendProblemType.MISSING_INTERNAL_RESOURCE,
                        BheMapAppendProblemSeverity.NON_BLOCKING,
                        entryPlan,
                        null,
                        missing,
                        message
                );
                addProblem(audit, entryPlan, problem);
            }
        } catch (Exception e) {
            String message = "BHE .map 解析失败: " + entryPlan.getSourceMapFileName() + " -> " + e.getMessage();
            entryPlan.setSourceMapParseProblem(message);
            BheMapAppendProblem problem = problem(
                    BheMapAppendProblemType.MAP_PARSE_FAILURE,
                    BheMapAppendProblemSeverity.BLOCKING,
                    entryPlan,
                    null,
                    null,
                    message
            );
            addProblem(audit, entryPlan, problem);
        }
        return entryPlan;
    }

    private BheMapResourceReferences collectResourceReferences(MapData mapData, CaseInsensitiveFileIndex staticResourceIndex) {
        BheMapResourceReferences references = new BheMapResourceReferences();
        if (mapData == null) {
            return references;
        }

        collectForeground(mapData, references, staticResourceIndex);
        collectResourceSlots(mapData, references, staticResourceIndex);
        collectSpriteMaps(mapData, references, staticResourceIndex);
        return references;
    }

    private BheMapDataSummary buildMapDataSummary(MapData mapData) {
        BheMapDataSummary summary = new BheMapDataSummary();
        if (mapData == null) {
            return summary;
        }

        summary.setMagic(mapData.getMagic());
        summary.setWidth(nullToZero(mapData.getWidth()));
        summary.setHeight(nullToZero(mapData.getHeight()));
        summary.setTileRecordCount(mapData.getTileRecords() == null ? 0 : mapData.getTileRecords().size());
        summary.setCameraRectCount(count(mapData.getCameraRectBlock() == null ? null : mapData.getCameraRectBlock().getCount()));
        summary.setRawPointGroup0Count(count(mapData.getRawPointGroup0() == null ? null : mapData.getRawPointGroup0().getCount()));
        summary.setRawPointGroup1Count(count(mapData.getRawPointGroup1() == null ? null : mapData.getRawPointGroup1().getCount()));
        summary.setRawRectGroup0Count(count(mapData.getRawRectGroup0() == null ? null : mapData.getRawRectGroup0().getCount()));
        summary.setRawRectGroup1Count(count(mapData.getRawRectGroup1() == null ? null : mapData.getRawRectGroup1().getCount()));
        summary.setRawRectGroup2Count(count(mapData.getRawRectGroup2() == null ? null : mapData.getRawRectGroup2().getCount()));
        summary.setResourceSlotBlockCount(mapData.getNamedResourceSlotBlocks() == null ? 0 : mapData.getNamedResourceSlotBlocks().size());
        summary.setScriptEntryGroupCount(mapData.getScriptEntryGroupBlocks() == null ? 0 : mapData.getScriptEntryGroupBlocks().size());
        summary.setTotalScriptEntryCount(totalScriptEntryCount(mapData));
        summary.setSpriteMapCount(mapData.getSpriteMapList() == null || mapData.getSpriteMapList().isEmpty()
                ? (normalizeReference(mapData.getSpriteMap()) == null ? 0 : 1)
                : mapData.getSpriteMapList().size());
        return summary;
    }

    private void collectForeground(
            MapData mapData,
            BheMapResourceReferences references,
            CaseInsensitiveFileIndex staticResourceIndex
    ) {
        String foreground = normalizeReference(mapData.getForegroundImage());
        references.setForegroundImage(foreground);
        BheMapResourceReference reference = buildResourceReference(
                BheMapReferenceType.FOREGROUND,
                foreground,
                staticResourceIndex
        );
        references.setForegroundReference(reference);
        addReference(reference, references);
    }

    private void collectResourceSlots(
            MapData mapData,
            BheMapResourceReferences references,
            CaseInsensitiveFileIndex staticResourceIndex
    ) {
        if (mapData.getNamedResourceSlotBlocks() == null) {
            return;
        }
        for (MapData.NamedResourceSlotBlock slot : mapData.getNamedResourceSlotBlocks()) {
            String slotText = normalizeReference(slot == null ? null : slot.getSlotText());
            if (slotText == null) {
                continue;
            }
            references.getResourceSlotFiles().add(slotText);
            BheMapResourceReference reference = buildResourceReference(
                    BheMapReferenceType.RESOURCE_SLOT,
                    slotText,
                    staticResourceIndex
            );
            applyResourceSlotFields(reference, slot);
            addReference(reference, references);
        }
    }

    private void collectSpriteMaps(
            MapData mapData,
            BheMapResourceReferences references,
            CaseInsensitiveFileIndex staticResourceIndex
    ) {
        if (mapData.getSpriteMapList() != null && !mapData.getSpriteMapList().isEmpty()) {
            for (String spriteMap : mapData.getSpriteMapList()) {
                String normalized = normalizeReference(spriteMap);
                if (normalized == null) {
                    continue;
                }
                references.getSpriteMapFiles().add(normalized);
                addReference(buildResourceReference(
                        BheMapReferenceType.SPRITE_MAP_LIST,
                        normalized,
                        staticResourceIndex
                ), references);
            }
            return;
        }

        String spriteMap = normalizeReference(mapData.getSpriteMap());
        if (spriteMap != null) {
            references.getSpriteMapFiles().add(spriteMap);
            addReference(buildResourceReference(
                    BheMapReferenceType.SPRITE_MAP,
                    spriteMap,
                    staticResourceIndex
            ), references);
        }
    }

    private BheMapResourceReference buildResourceReference(
            BheMapReferenceType type,
            String reference,
            CaseInsensitiveFileIndex staticResourceIndex
    ) {
        if (reference == null) {
            return null;
        }
        BheMapResourceReference resourceReference = new BheMapResourceReference();
        resourceReference.setType(type);
        resourceReference.setSourceText(reference);
        String sourceFileName = sourceFileName(reference);
        resourceReference.setSourceFileName(sourceFileName);
        resourceReference.setTargetFileName(namingPolicy.toTargetResourceFileName(sourceFileName));
        resourceReference.setTargetText(toTargetReferenceText(reference, resourceReference.getTargetFileName()));
        Path sourcePath = staticResourceIndex.resolveByName(reference);
        resourceReference.setSourcePath(sourcePath);
        if (sourcePath != null) {
            resourceReference.setStatus(BheMapReferenceStatus.FOUND_DEFERRED);
        } else {
            resourceReference.setStatus(BheMapReferenceStatus.MISSING_NON_BLOCKING);
            resourceReference.setProblem("缺少内部引用资源: " + reference);
        }
        return resourceReference;
    }

    private void applyResourceSlotFields(BheMapResourceReference reference, MapData.NamedResourceSlotBlock slot) {
        if (reference == null || slot == null) {
            return;
        }
        reference.setResourceSlotNum(slot.getSlotNum());
        reference.setResourceSlotParam0(slot.getParam0());
        reference.setResourceSlotParam1(slot.getParam1());
        reference.setResourceSlotParam2(slot.getParam2());
        reference.setResourceSlotParam3(slot.getParam3());
        reference.setResourceSlotParam4(slot.getParam4());
    }

    private void addReference(BheMapResourceReference reference, BheMapResourceReferences references) {
        if (reference == null) {
            return;
        }
        references.getReferences().add(reference);
        if (reference.isFound()) {
            references.getDeferredResourceFiles().add(reference.getSourceText());
        }
        if (reference.isMissing()) {
            references.getMissingResourceFiles().add(reference.getSourceText());
        }
    }

    private void auditTargetCollision(
            BheMapEntryPlan entryPlan,
            BsdxMapBaseline bsdxBaseline,
            BheMapAppendAudit audit
    ) {
        if (entryPlan == null || bsdxBaseline == null) {
            return;
        }
        if (containsIgnoreCase(bsdxBaseline.getExistingGroupResourceNames(), entryPlan.getTargetGroupResourceName())) {
            String message = "BHE 地图目标资源名撞 BSDX MapGroup: "
                    + entryPlan.getSourceGroupResourceName()
                    + " -> "
                    + entryPlan.getTargetGroupResourceName();
            BheMapAppendProblem problem = problem(
                    BheMapAppendProblemType.TARGET_NAME_COLLISION,
                    BheMapAppendProblemSeverity.BLOCKING,
                    entryPlan,
                    entryPlan.getTargetGroupResourceName(),
                    null,
                    message
            );
            addProblem(audit, entryPlan, problem);
        }
    }

    private void auditSameRunTargetDuplicate(
            BheMapEntryPlan entryPlan,
            Set<String> seenTargetGroupResourceNames,
            BheMapAppendAudit audit
    ) {
        if (entryPlan == null || seenTargetGroupResourceNames == null) {
            return;
        }

        String targetGroupResourceName = entryPlan.getTargetGroupResourceName();
        String normalizedTarget = normalize(targetGroupResourceName);
        if (normalizedTarget.isBlank()) {
            return;
        }

        // 同一轮目标名重复时，后续追加无法唯一定位 MapGroup 目标条目，必须作为阻塞问题记录。
        if (!seenTargetGroupResourceNames.add(normalizedTarget)) {
            String message = "BHE 地图目标资源名在 import plan 内重复: "
                    + entryPlan.getSourceGroupResourceName()
                    + " -> "
                    + targetGroupResourceName;
            BheMapAppendProblem problem = problem(
                    BheMapAppendProblemType.TARGET_NAME_COLLISION,
                    BheMapAppendProblemSeverity.BLOCKING,
                    entryPlan,
                    targetGroupResourceName,
                    null,
                    message
            );
            addProblem(audit, entryPlan, problem);
        }
    }

    private boolean containsIgnoreCase(Set<String> values, String target) {
        if (values == null || target == null) {
            return false;
        }
        String normalizedTarget = normalize(target);
        for (String value : values) {
            if (normalize(value).equals(normalizedTarget)) {
                return true;
            }
        }
        return false;
    }

    private int totalScriptEntryCount(MapData mapData) {
        if (mapData == null || mapData.getScriptEntryGroupBlocks() == null) {
            return 0;
        }
        int total = 0;
        for (MapData.ScriptEntryGroupBlock block : mapData.getScriptEntryGroupBlocks()) {
            if (block == null || block.getEntries() == null) {
                continue;
            }
            total += block.getEntries().size();
        }
        return total;
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int count(Integer value) {
        return value == null ? 0 : value;
    }

    private String normalizeReference(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String sourceFileName(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        return Path.of(reference.trim().replace('\\', '/')).getFileName().toString();
    }

    private String toTargetReferenceText(String sourceReference, String targetFileName) {
        if (sourceReference == null || sourceReference.isBlank() || targetFileName == null || targetFileName.isBlank()) {
            return targetFileName;
        }
        String normalized = sourceReference.trim().replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        if (lastSlash < 0) {
            return targetFileName;
        }
        return normalized.substring(0, lastSlash + 1) + targetFileName;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void addProblem(BheMapAppendAudit audit, BheMapEntryPlan entryPlan, BheMapAppendProblem problem) {
        if (entryPlan != null) {
            entryPlan.addProblem(problem);
        }
        if (audit == null || problem == null) {
            return;
        }
        if (problem.getType() == BheMapAppendProblemType.MISSING_MAP_FILE) {
            audit.addMissingMapFile(problem);
            return;
        }
        if (problem.getType() == BheMapAppendProblemType.MAP_PARSE_FAILURE) {
            audit.addMapParseFailure(problem);
            return;
        }
        if (problem.getType() == BheMapAppendProblemType.MISSING_INTERNAL_RESOURCE) {
            audit.addMissingResourceFile(problem);
            return;
        }
        if (problem.getType() == BheMapAppendProblemType.TARGET_NAME_COLLISION) {
            audit.addTargetNameCollision(problem);
        }
    }

    private BheMapAppendProblem problem(
            BheMapAppendProblemType type,
            BheMapAppendProblemSeverity severity,
            BheMapEntryPlan entryPlan,
            String targetName,
            String referenceText,
            String message
    ) {
        BheMapAppendProblem problem = new BheMapAppendProblem();
        problem.setType(type);
        problem.setSeverity(severity);
        if (entryPlan != null) {
            problem.setSourceGroupResourceName(entryPlan.getSourceGroupResourceName());
            problem.setSourceMapFileName(entryPlan.getSourceMapFileName());
        }
        problem.setTargetName(targetName);
        problem.setReferenceText(referenceText);
        problem.setMessage(message);
        return problem;
    }
}
