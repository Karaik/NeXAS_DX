package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapOutputConflictType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapOutputPreflightResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapOutputWriteAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportWrittenMap;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapPendingOutputFile;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapSafeOutputWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class BheMapResourceMaterializer {

    private final BheMapOutputResourceReferenceResolver outputReferenceResolver =
            new BheMapOutputResourceReferenceResolver();
    private final BheMapSafeOutputWriter safeOutputWriter = new BheMapSafeOutputWriter();

    public BheMapResourceMaterializationResult materialize(
            BheMapAppendPlan plan,
            BheMapImportOutputResult outputResult,
            Path outputRoot,
            String charset
    ) {
        BheMapResourceMaterializationResult result = prepare(plan, outputResult, outputRoot, charset);
        try {
            BheMapOutputPreflightResult preflightResult = safeOutputWriter.preflight(result.getPendingOutputFiles());
            if (preflightResult.getConflictCount() > 0) {
                result.setOutputConflictCount(preflightResult.getConflictCount());
                result.getConflicts().addAll(preflightResult.getConflicts());
                result.getPendingOutputFiles().clear();
                return result;
            }
            safeOutputWriter.commit(preflightResult);
            applyCommit(result, preflightResult);
            result.getPendingOutputFiles().clear();
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("BHE map resource materialization failed", e);
        }
    }

    public BheMapResourceMaterializationResult prepare(
            BheMapAppendPlan plan,
            BheMapImportOutputResult outputResult,
            Path outputRoot,
            String charset
    ) {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }
        if (outputResult == null) {
            throw new IllegalArgumentException("outputResult must not be null");
        }
        if (outputRoot == null) {
            throw new IllegalArgumentException("outputRoot must not be null");
        }

        try {
            Path normalizedOutputRoot = outputRoot.toAbsolutePath().normalize();
            Files.createDirectories(normalizedOutputRoot);
            BheMapResourceMaterializationResult result = new BheMapResourceMaterializationResult();
            Map<String, BheMapEntryPlan> entriesByTargetMapFileName = entriesByTargetMapFileName(plan);
            Map<Path, BheMapPendingOutputFile> uniqueTargets = new HashMap<>();
            for (BheMapImportWrittenMap writtenMap : outputResult.getWrittenMaps()) {
                BheMapEntryPlan entry = entriesByTargetMapFileName.get(writtenMap.getTargetMapFileName());
                if (entry != null) {
                    materializeEntry(entry, writtenMap, normalizedOutputRoot, result, uniqueTargets);
                }
            }
            result.getPendingOutputFiles().addAll(uniqueTargets.values());
            return result;
        } catch (IOException e) {
            throw new IllegalStateException("BHE map resource materialization failed", e);
        }
    }

    private void materializeEntry(
            BheMapEntryPlan entry,
            BheMapImportWrittenMap writtenMap,
            Path outputRoot,
            BheMapResourceMaterializationResult result,
            Map<Path, BheMapPendingOutputFile> uniqueTargets
    ) throws IOException {
        if (entry.getResourceReferences() == null || entry.getResourceReferences().getReferences() == null) {
            return;
        }

        int spriteMapListReferenceCount = countReferences(entry, BheMapReferenceType.SPRITE_MAP_LIST);
        for (BheMapResourceReference reference : entry.getResourceReferences().getReferences()) {
            BheMapReferenceType outputType = outputReferenceType(reference, spriteMapListReferenceCount, writtenMap);
            if (outputType == null) {
                continue;
            }
            materializeReference(reference, outputType, outputRoot, result, uniqueTargets);
        }
    }

    private void materializeReference(
            BheMapResourceReference reference,
            BheMapReferenceType outputType,
            Path outputRoot,
            BheMapResourceMaterializationResult result,
            Map<Path, BheMapPendingOutputFile> uniqueTargets
    ) throws IOException {
        recordServicedReferenceCount(result, outputType);
        if (reference == null || reference.isMissing() || reference.getSourcePath() == null) {
            result.setMissingSourceResourceCount(result.getMissingSourceResourceCount() + 1);
            return;
        }
        if (!Files.isRegularFile(reference.getSourcePath())) {
            result.setMissingSourceResourceCount(result.getMissingSourceResourceCount() + 1);
            return;
        }

        Path targetPath = resolveTargetPath(outputRoot, reference);
        byte[] sourceBytes = Files.readAllBytes(reference.getSourcePath());
        BheMapPendingOutputFile existingPending = uniqueTargets.get(targetPath);
        if (existingPending != null) {
            if (!java.util.Arrays.equals(existingPending.getBytes(), sourceBytes)) {
                result.setOutputConflictCount(result.getOutputConflictCount() + 1);
                com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapOutputConflict conflict =
                        new com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapOutputConflict();
                conflict.setType(BheMapOutputConflictType.RESOURCE_FILE_CONFLICT);
                conflict.setPath(targetPath);
                conflict.setReason("resource references target the same output file with different content: " + targetPath);
                result.getConflicts().add(conflict);
            }
            result.setDuplicateTargetResourceCount(result.getDuplicateTargetResourceCount() + 1);
            return;
        }

        BheMapPendingOutputFile pendingFile = new BheMapPendingOutputFile();
        pendingFile.setPath(targetPath);
        pendingFile.setBytes(sourceBytes);
        pendingFile.setConflictType(BheMapOutputConflictType.RESOURCE_FILE_CONFLICT);
        pendingFile.setLogicalName(targetPath.getFileName().toString());
        uniqueTargets.put(targetPath, pendingFile);
        result.getWrittenResourceFileList().add(targetPath);
        result.setMaterializedResourceCount(result.getMaterializedResourceCount() + 1);
        recordTypeCount(result, outputType);
        if (isSpm(reference)) {
            result.setSpmWritten(true);
        }
    }

    private Path resolveTargetPath(Path outputRoot, BheMapResourceReference reference) {
        String mapInternalResourceText = isBlank(reference.getTargetText())
                ? reference.getTargetFileName()
                : reference.getTargetText();
        if (isBlank(mapInternalResourceText)) {
            throw new IllegalArgumentException("resource reference missing target text: " + reference.getSourceText());
        }

        Path targetPath = outputReferenceResolver.resolveOutputResourcePath(outputRoot, mapInternalResourceText);
        if (!isBlank(reference.getTargetFileName())) {
            String expectedFileName = outputReferenceResolver.toOutputResourceFileName(mapInternalResourceText);
            String planFileName = outputReferenceResolver.toOutputResourceFileName(reference.getTargetFileName());
            if (!expectedFileName.equals(planFileName)) {
                throw new IllegalArgumentException("resource targetText and targetFileName disagree: "
                        + mapInternalResourceText
                        + " / "
                        + reference.getTargetFileName());
            }
        }
        return targetPath;
    }

    private BheMapReferenceType outputReferenceType(
            BheMapResourceReference reference,
            int spriteMapListReferenceCount,
            BheMapImportWrittenMap writtenMap
    ) {
        if (reference == null || reference.getType() == null) {
            return null;
        }
        if (writtenMap != null && writtenMap.isComposition()
                && reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST) {
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
        List<BheMapResourceReference> references = entry.getResourceReferences().getReferences();
        for (BheMapResourceReference reference : references) {
            if (reference != null && reference.getType() == type) {
                count++;
            }
        }
        return count;
    }

    private Map<String, BheMapEntryPlan> entriesByTargetMapFileName(BheMapAppendPlan plan) {
        Map<String, BheMapEntryPlan> entries = new HashMap<>();
        if (plan.getEntries() == null) {
            return entries;
        }
        for (BheMapEntryPlan entry : plan.getEntries()) {
            if (entry != null && !isBlank(entry.getTargetMapFileName())) {
                entries.put(entry.getTargetMapFileName(), entry);
            }
        }
        return entries;
    }

    private void recordServicedReferenceCount(BheMapResourceMaterializationResult result, BheMapReferenceType type) {
        result.setServicedResourceReferenceCount(result.getServicedResourceReferenceCount() + 1);
        if (type == BheMapReferenceType.FOREGROUND) {
            result.setServicedForegroundReferenceCount(result.getServicedForegroundReferenceCount() + 1);
            return;
        }
        if (type == BheMapReferenceType.RESOURCE_SLOT) {
            result.setServicedResourceSlotReferenceCount(result.getServicedResourceSlotReferenceCount() + 1);
            return;
        }
        if (type == BheMapReferenceType.SPRITE_MAP) {
            result.setServicedSpriteMapReferenceCount(result.getServicedSpriteMapReferenceCount() + 1);
        }
    }

    public void applyCommit(BheMapResourceMaterializationResult result, BheMapOutputPreflightResult preflightResult) {
        if (result == null || preflightResult == null) {
            return;
        }
        Set<Path> resourcePaths = new HashSet<>();
        for (BheMapPendingOutputFile pendingOutputFile : result.getPendingOutputFiles()) {
            resourcePaths.add(pendingOutputFile.getPath().toAbsolutePath().normalize());
        }
        for (BheMapOutputWriteAudit audit : preflightResult.getWriteAudits()) {
            if (audit.getPath() == null || !resourcePaths.contains(audit.getPath().toAbsolutePath().normalize())) {
                continue;
            }
            if (audit.isWritten()) {
                result.setWrittenFileCount(result.getWrittenFileCount() + 1);
            }
            if (audit.isExistingIdentical()) {
                result.setExistingIdenticalFileCount(result.getExistingIdenticalFileCount() + 1);
            }
        }
    }

    private void recordTypeCount(BheMapResourceMaterializationResult result, BheMapReferenceType type) {
        if (type == BheMapReferenceType.FOREGROUND) {
            result.setMaterializedForegroundCount(result.getMaterializedForegroundCount() + 1);
            return;
        }
        if (type == BheMapReferenceType.RESOURCE_SLOT) {
            result.setMaterializedResourceSlotCount(result.getMaterializedResourceSlotCount() + 1);
            return;
        }
        if (type == BheMapReferenceType.SPRITE_MAP) {
            result.setMaterializedSpriteMapCount(result.getMaterializedSpriteMapCount() + 1);
        }
    }

    private boolean isSpm(BheMapResourceReference reference) {
        String target = isBlank(reference.getTargetFileName()) ? reference.getTargetText() : reference.getTargetFileName();
        return target != null && target.toLowerCase(java.util.Locale.ROOT).endsWith(".spm");
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
