package com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportBlockedManifestEntry;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportManifest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportManifestEntry;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportBlockedMap;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportWrittenMap;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapOutputResourceReferenceResolver;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceMaterializationResult;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class BheMapImportManifestBuilder {

    private final BheMapOutputResourceReferenceResolver outputReferenceResolver =
            new BheMapOutputResourceReferenceResolver();

    public BheMapImportManifest build(BheMapAppendResult result) {
        BheMapImportManifest manifest = new BheMapImportManifest();
        if (result == null) {
            return manifest;
        }

        BheMapAppendPlan plan = result.getAppendPlan();
        BheMapImportOutputResult output = result.getOutputResult();
        BheMapResourceMaterializationResult resources = result.getResourceMaterializationResult();

        manifest.setOutputRoot(result.getOutputRoot());
        if (plan != null) {
            manifest.setTotalPlanEntries(plan.size());
        }
        applyOutput(manifest, output, result.isOutputWritten());
        applyResources(manifest, resources, result.isResourcesMaterialized());
        applyEntries(manifest, plan, output, result.isOutputWritten());
        manifest.setResourcesMaterialized(result.isResourcesMaterialized());
        manifest.setPreviewMaterialized(result.isPreviewMaterialized());
        manifest.setPreviewMaterializationEnabled(result.isPreviewMaterializationEnabled());
        manifest.setPreviewMaterializationTransactional(false);
        manifest.setDynamicValidation(false);
        manifest.setProbeRestored(false);
        return manifest;
    }

    private void applyOutput(
            BheMapImportManifest manifest,
            BheMapImportOutputResult output,
            boolean outputWritten
    ) {
        if (output == null) {
            return;
        }
        manifest.setOutputMapGroupPath(output.getOutputMapGroupPath());
        manifest.setOutputConflictCount(output.getOutputConflictCount());
        manifest.setExistingIdenticalFileCount(output.getExistingIdenticalFileCount());
        manifest.setOutputCompleted(output.isOutputCompleted() && outputWritten);
        manifest.setPack(output.isPacked());
        if (outputWritten && output.isOutputCompleted()) {
            manifest.setWrittenMapCount(output.getWrittenMapCount());
            manifest.setDirectWrittenMapCount(output.getDirectWrittenMapCount());
            manifest.setCompositionWrittenMapCount(output.getCompositionWrittenMapCount());
            manifest.setComposedSpmWrittenCount(output.getComposedSpmWrittenCount());
            manifest.setAppendedMapGroupCount(output.getAppendedMapGroupCount());
            manifest.setComposedSpmWritten(output.isComposedSpmWritten());
        }
        manifest.setBlockedMapCount(output.getBlockedMaps().size());
        manifest.setBlockedByMultipleSpriteMapListCount(output.getBlockedByMultipleSpriteMapListCount());
        manifest.setBlockedByUnsupportedScriptGroupCount(output.getBlockedByUnsupportedScriptGroupCount());
        manifest.setBlockedByMissingSourceSpmCount(output.getBlockedByMissingSourceSpmCount());
        manifest.setBlockedByMissingFinalReferenceCount(output.getBlockedByMissingFinalReferenceCount());
    }

    private void applyResources(
            BheMapImportManifest manifest,
            BheMapResourceMaterializationResult resources,
            boolean resourcesMaterialized
    ) {
        if (resources == null) {
            return;
        }
        manifest.setOutputConflictCount(manifest.getOutputConflictCount() + resources.getOutputConflictCount());
        manifest.setExistingIdenticalFileCount(
                manifest.getExistingIdenticalFileCount() + resources.getExistingIdenticalFileCount()
        );
        manifest.setComposedSpmWritten(manifest.isComposedSpmWritten() || resources.isComposedSpmWritten());
        manifest.setPack(manifest.isPack() || resources.isPacked());
        if (!resourcesMaterialized) {
            return;
        }
        manifest.setMaterializedResourceCount(resources.getMaterializedResourceCount());
        manifest.setForegroundResourceCount(resources.getMaterializedForegroundCount());
        manifest.setResourceSlotResourceCount(resources.getMaterializedResourceSlotCount());
        manifest.setSpriteMapResourceCount(resources.getMaterializedSpriteMapCount());
        manifest.setInternalResourceReferenceCount(resources.getServicedResourceReferenceCount());
        manifest.setDuplicateTargetReferenceCount(resources.getDuplicateTargetResourceCount());
    }

    private void applyEntries(
            BheMapImportManifest manifest,
            BheMapAppendPlan plan,
            BheMapImportOutputResult output,
            boolean outputWritten
    ) {
        if (output == null) {
            return;
        }
        Map<String, BheMapEntryPlan> entriesByTargetMapFileName = entriesByTargetMapFileName(plan);
        if (outputWritten && output.isOutputCompleted()) {
            for (BheMapImportWrittenMap writtenMap : output.getWrittenMaps()) {
                BheMapImportManifestEntry entry = toWrittenManifestEntry(
                        writtenMap,
                        entriesByTargetMapFileName.get(writtenMap.getTargetMapFileName())
                );
                manifest.getWrittenEntries().add(entry);
            }
        }
        for (BheMapImportBlockedMap blockedMap : output.getBlockedMaps()) {
            manifest.getBlockedEntries().add(toBlockedManifestEntry(blockedMap));
        }
    }

    private BheMapImportManifestEntry toWrittenManifestEntry(
            BheMapImportWrittenMap writtenMap,
            BheMapEntryPlan planEntry
    ) {
        BheMapImportManifestEntry entry = new BheMapImportManifestEntry();
        entry.setSourceMapFileName(writtenMap.getSourceMapFileName());
        entry.setTargetMapFileName(writtenMap.getTargetMapFileName());
        entry.setTargetGroupResourceName(writtenMap.getTargetGroupResourceName());
        entry.setOutputMapPath(writtenMap.getMapPath());
        if (planEntry != null) {
            applyResourceReferences(entry, planEntry);
        }
        return entry;
    }

    private void applyResourceReferences(BheMapImportManifestEntry entry, BheMapEntryPlan planEntry) {
        if (planEntry.getResourceReferences() == null || planEntry.getResourceReferences().getReferences() == null) {
            return;
        }
        int spriteMapListReferenceCount = countReferences(planEntry, BheMapReferenceType.SPRITE_MAP_LIST);
        Set<String> resourceFileNames = new LinkedHashSet<>();
        for (BheMapResourceReference reference : planEntry.getResourceReferences().getReferences()) {
            BheMapReferenceType outputType = outputReferenceType(reference, spriteMapListReferenceCount);
            if (outputType == null || reference == null || reference.isMissing()) {
                continue;
            }
            String targetText = isBlank(reference.getTargetText())
                    ? reference.getTargetFileName()
                    : reference.getTargetText();
            if (isBlank(targetText)) {
                continue;
            }
            resourceFileNames.add(outputReferenceResolver.toOutputResourceFileName(targetText));
            if (outputType == BheMapReferenceType.FOREGROUND) {
                entry.setForegroundReferenceCount(entry.getForegroundReferenceCount() + 1);
            } else if (outputType == BheMapReferenceType.RESOURCE_SLOT) {
                entry.setResourceSlotReferenceCount(entry.getResourceSlotReferenceCount() + 1);
            } else if (outputType == BheMapReferenceType.SPRITE_MAP) {
                entry.setSpriteMapReferenceCount(entry.getSpriteMapReferenceCount() + 1);
            }
        }
        entry.getWrittenResourceTargetFileNames().addAll(resourceFileNames);
    }

    private BheMapImportBlockedManifestEntry toBlockedManifestEntry(BheMapImportBlockedMap blockedMap) {
        BheMapImportBlockedManifestEntry entry = new BheMapImportBlockedManifestEntry();
        entry.setSourceMapFileName(blockedMap.getSourceMapFileName());
        entry.setTargetMapFileName(blockedMap.getTargetMapFileName());
        entry.setTargetGroupResourceName(blockedMap.getTargetGroupResourceName());
        entry.setMissingSourceSpm(blockedMap.isMissingSourceSpm());
        entry.setNotWritten(true);
        entry.getIssueTypes().addAll(blockedMap.getIssueTypes());
        entry.getBlockingIssues().addAll(blockedMap.getBlockingIssues());
        entry.getCompositionBlockingReasons().addAll(blockedMap.getCompositionBlockingReasons());
        entry.getCompositionBlockingIssues().addAll(blockedMap.getCompositionBlockingIssues());
        return entry;
    }

    private Map<String, BheMapEntryPlan> entriesByTargetMapFileName(BheMapAppendPlan plan) {
        Map<String, BheMapEntryPlan> entries = new LinkedHashMap<>();
        if (plan == null || plan.getEntries() == null) {
            return entries;
        }
        for (BheMapEntryPlan entry : plan.getEntries()) {
            if (entry != null && !isBlank(entry.getTargetMapFileName())) {
                entries.put(entry.getTargetMapFileName(), entry);
            }
        }
        return entries;
    }

    private BheMapReferenceType outputReferenceType(BheMapResourceReference reference, int spriteMapListReferenceCount) {
        if (reference == null || reference.getType() == null) {
            return null;
        }
        if (reference.getType() == BheMapReferenceType.FOREGROUND
                || reference.getType() == BheMapReferenceType.RESOURCE_SLOT
                || reference.getType() == BheMapReferenceType.SPRITE_MAP) {
            return reference.getType();
        }
        if (reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST && spriteMapListReferenceCount == 1) {
            return BheMapReferenceType.SPRITE_MAP;
        }
        return null;
    }

    private int countReferences(BheMapEntryPlan entry, BheMapReferenceType type) {
        int count = 0;
        if (entry == null || entry.getResourceReferences() == null) {
            return count;
        }
        for (BheMapResourceReference reference : entry.getResourceReferences().getReferences()) {
            if (reference != null && reference.getType() == type) {
                count++;
            }
        }
        return count;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
