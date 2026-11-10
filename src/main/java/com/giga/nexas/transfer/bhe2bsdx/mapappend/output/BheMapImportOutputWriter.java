package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.map.generator.MapDataGenerator;
import com.giga.nexas.dto.bsdx.map.parser.MapDataParser;
import com.giga.nexas.dto.bsdx.spm.generator.SpmGenerator;
import com.giga.nexas.dto.bsdx.spm.parser.SpmParser;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityBlockingReason;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityIssue;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MultipleSpriteMapListCompositionPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MultipleSpriteMapListCompositionPlanner;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MultipleSpriteMapListCompositionResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MultipleSpriteMapListMapDataConverter;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssue;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssueType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheToBsdxMapDataConverter;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapgroup.BheMapGroupEntryConverter;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceRewriteTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class BheMapImportOutputWriter {

    private static final String MAPGROUP_FILE_NAME = "MapGroup.grp";

    private final BheBinService bheBinService;
    private final BsdxBinService bsdxBinService;
    private final BheToBsdxMapDataConverter mapDataConverter;
    private final BheMapGroupEntryConverter mapGroupEntryConverter;
    private final MapDataGenerator mapDataGenerator;
    private final MapDataParser mapDataParser;
    private final SpmGenerator spmGenerator = new SpmGenerator();
    private final SpmParser spmParser = new SpmParser();
    private final MultipleSpriteMapListCompositionPlanner compositionPlanner =
            new MultipleSpriteMapListCompositionPlanner();
    private final MultipleSpriteMapListMapDataConverter compositionMapDataConverter =
            new MultipleSpriteMapListMapDataConverter();

    public BheMapImportOutputWriter() {
        this(
                new BheBinService(),
                new BsdxBinService(),
                new BheToBsdxMapDataConverter(),
                new BheMapGroupEntryConverter(),
                new MapDataGenerator(),
                new MapDataParser()
        );
    }

    public BheMapImportOutputWriter(
            BheBinService bheBinService,
            BsdxBinService bsdxBinService,
            BheToBsdxMapDataConverter mapDataConverter,
            BheMapGroupEntryConverter mapGroupEntryConverter,
            MapDataGenerator mapDataGenerator,
            MapDataParser mapDataParser
    ) {
        this.bheBinService = bheBinService == null ? new BheBinService() : bheBinService;
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
        this.mapDataConverter = mapDataConverter == null ? new BheToBsdxMapDataConverter() : mapDataConverter;
        this.mapGroupEntryConverter = mapGroupEntryConverter == null
                ? new BheMapGroupEntryConverter()
                : mapGroupEntryConverter;
        this.mapDataGenerator = mapDataGenerator == null ? new MapDataGenerator() : mapDataGenerator;
        this.mapDataParser = mapDataParser == null ? new MapDataParser() : mapDataParser;
    }

    public BheMapImportOutputResult write(
            BheMapAppendPlan plan,
            Path currentTargetMapGroupPath,
            Path outputRoot,
            String charset
    ) throws IOException {
        BheMapImportOutputResult result = prepare(plan, currentTargetMapGroupPath, outputRoot, charset);
        BheMapSafeOutputWriter safeOutputWriter = new BheMapSafeOutputWriter();
        BheMapOutputPreflightResult preflightResult = safeOutputWriter.preflight(result.getPendingOutputFiles());
        if (preflightResult.getConflictCount() > 0) {
            result.setOutputConflictCount(preflightResult.getConflictCount());
            result.getConflicts().addAll(preflightResult.getConflicts());
            result.getPendingOutputFiles().clear();
            return result;
        }
        safeOutputWriter.commit(preflightResult);
        applyCommit(result, preflightResult);
        verifyCommittedOutput(result, charset);
        result.getPendingOutputFiles().clear();
        return result;
    }

    public BheMapImportOutputResult prepare(
            BheMapAppendPlan plan,
            Path currentTargetMapGroupPath,
            Path outputRoot,
            String charset
    ) throws IOException {
        if (plan == null) {
            throw new IllegalArgumentException("plan must not be null");
        }
        if (currentTargetMapGroupPath == null) {
            throw new IllegalArgumentException("currentTargetMapGroupPath must not be null");
        }
        if (outputRoot == null) {
            throw new IllegalArgumentException("outputRoot must not be null");
        }

        String effectiveCharset = isBlank(charset) ? "windows-31j" : charset;
        Path normalizedOutputRoot = outputRoot.toAbsolutePath().normalize();
        Files.createDirectories(normalizedOutputRoot);
        Path normalizedCurrentMapGroupPath = currentTargetMapGroupPath.toAbsolutePath().normalize();
        byte[] baselineMapGroupBytes = Files.readAllBytes(normalizedCurrentMapGroupPath);

        BheMapImportOutputResult result = new BheMapImportOutputResult();
        result.setOutputRoot(normalizedOutputRoot);
        result.setTotalPlanEntries(plan.getEntries() == null ? 0 : plan.getEntries().size());

        MapGroupGrp outputMapGroup = readMapGroup(normalizedCurrentMapGroupPath, effectiveCharset);
        result.setBaselineMapGroupCount(outputMapGroup.getGroupList().size());

        List<BheMapEntryPlan> writtenEntries = new ArrayList<>();
        if (plan.getEntries() != null) {
            for (BheMapEntryPlan entry : plan.getEntries()) {
                MapConversionOutput conversion = convert(entry, effectiveCharset);
                if (!conversion.converted()) {
                    recordBlocked(result, entry, conversion);
                    continue;
                }
                result.setConvertedMapCount(result.getConvertedMapCount() + 1);
                Path targetMapPath = normalizedOutputRoot.resolve(entry.getTargetMapFileName()).normalize();
                byte[] mapBytes = generateMapDataBytes(conversion.mapData(), effectiveCharset);
                verifyMapDataBytes(mapBytes, entry.getTargetMapFileName(), effectiveCharset);
                result.getPendingOutputFiles().add(pendingFile(
                        targetMapPath,
                        mapBytes,
                        BheMapOutputConflictType.MAP_FILE_CONFLICT,
                        entry.getTargetMapFileName()
                ));
                Path composedSpmPath = null;
                if (conversion.composition()) {
                    composedSpmPath = normalizedOutputRoot.resolve(
                            conversion.compositionResult().getCompositionPlan().getTargetComposedSpriteMapFileName()
                    ).normalize();
                    byte[] spmBytes = generateSpmBytes(conversion.compositionResult().getComposedSpm(), effectiveCharset);
                    verifySpmBytes(
                            spmBytes,
                            conversion.compositionResult().getCompositionPlan().getTargetComposedSpriteMapFileName(),
                            effectiveCharset
                    );
                    result.getPendingOutputFiles().add(pendingFile(
                            composedSpmPath,
                            spmBytes,
                            BheMapOutputConflictType.COMPOSED_SPM_FILE_CONFLICT,
                            conversion.compositionResult().getCompositionPlan().getTargetComposedSpriteMapFileName()
                    ));
                    result.getWrittenComposedSpmFileList().add(composedSpmPath);
                    result.setComposedSpmWrittenCount(result.getComposedSpmWrittenCount() + 1);
                    result.setSpmWritten(true);
                    result.setComposedSpmWritten(true);
                }
                writtenEntries.add(entry);
                recordWritten(result, entry, targetMapPath, conversion, composedSpmPath);
            }
        }

        appendMapGroupEntries(outputMapGroup, writtenEntries, result);
        Path outputMapGroupPath = normalizedOutputRoot.resolve(MAPGROUP_FILE_NAME).normalize();
        byte[] mapGroupBytes = generateMapGroupBytes(outputMapGroup, effectiveCharset);
        MapGroupGrp verifiedMapGroup = parseMapGroupBytes(mapGroupBytes, MAPGROUP_FILE_NAME, effectiveCharset);
        BheMapPendingOutputFile mapGroupPendingFile = pendingFile(
                outputMapGroupPath,
                mapGroupBytes,
                BheMapOutputConflictType.MAPGROUP_FILE_CONFLICT,
                MAPGROUP_FILE_NAME
        );
        if (outputMapGroupPath.equals(normalizedCurrentMapGroupPath)) {
            mapGroupPendingFile.setWritePolicy(BheMapOutputWritePolicy.REPLACE_IF_EXISTING_MATCHES_EXPECTED);
            mapGroupPendingFile.setExpectedExistingBytes(baselineMapGroupBytes);
        }
        result.getPendingOutputFiles().add(mapGroupPendingFile);
        result.setOutputMapGroupPath(outputMapGroupPath);
        result.setOutputMapGroupCount(verifiedMapGroup.getGroupList().size());
        int expectedMapGroupCount = result.getBaselineMapGroupCount() + result.getWrittenMapCount();
        if (result.getOutputMapGroupCount() != expectedMapGroupCount) {
            throw new IllegalStateException("output MapGroup count mismatch: expected="
                    + expectedMapGroupCount
                    + ", actual="
                    + result.getOutputMapGroupCount());
        }
        return result;
    }

    private MapConversionOutput convert(BheMapEntryPlan entry, String charset) throws IOException {
        com.giga.nexas.dto.bhe.map.MapData sourceMapData =
                (com.giga.nexas.dto.bhe.map.MapData) bheBinService
                        .parse(entry.getSourceMapPath().toString(), charset)
                        .getData();
        BheMapResourceRewriteTable rewriteTable = BheMapResourceRewriteTable.fromEntryPlan(entry);
        BheMapDataConversionResult directConversion = mapDataConverter.convert(
                entry,
                sourceMapData,
                rewriteTable
        );
        if (directConversion.isConverted()) {
            return MapConversionOutput.direct(directConversion);
        }
        if (!hasIssue(directConversion, BheMapDataConversionIssueType.UNSUPPORTED_MULTIPLE_SPRITE_MAPS)) {
            return MapConversionOutput.blocked(directConversion);
        }

        MultipleSpriteMapListCompositionPlan compositionPlan =
                compositionPlanner.plan(entry, sourceMapData, charset);
        MultipleSpriteMapListCompositionResult compositionResult = compositionMapDataConverter.convert(
                entry,
                sourceMapData,
                rewriteTable,
                compositionPlan
        );
        if (compositionResult.isConverted()) {
            return MapConversionOutput.composition(directConversion, compositionResult);
        }
        return MapConversionOutput.blockedComposition(directConversion, compositionResult);
    }

    private void verifyMapData(Path targetMapPath, String targetMapFileName, String charset) throws IOException {
        mapDataParser.parse(Files.readAllBytes(targetMapPath), targetMapFileName, charset);
    }

    private void verifyMapDataBytes(byte[] bytes, String targetMapFileName, String charset) {
        mapDataParser.parse(bytes, targetMapFileName, charset);
    }

    private void verifySpm(Path targetSpmPath, String targetSpmFileName, String charset) throws IOException {
        spmParser.parse(Files.readAllBytes(targetSpmPath), targetSpmFileName, charset);
    }

    private void verifySpmBytes(byte[] bytes, String targetSpmFileName, String charset) {
        spmParser.parse(bytes, targetSpmFileName, charset);
    }

    private MapGroupGrp readMapGroup(Path mapGroupPath, String charset) throws IOException {
        return (MapGroupGrp) bsdxBinService.parse(mapGroupPath.toString(), charset).getData();
    }

    private byte[] generateMapDataBytes(com.giga.nexas.dto.bsdx.map.MapData mapData, String charset) throws IOException {
        Path stagingPath = Files.createTempFile("bhe-map-import-", ".map");
        try {
            mapDataGenerator.generate(stagingPath.toString(), mapData, charset);
            return Files.readAllBytes(stagingPath);
        } finally {
            Files.deleteIfExists(stagingPath);
        }
    }

    private byte[] generateSpmBytes(com.giga.nexas.dto.bsdx.spm.Spm spm, String charset) throws IOException {
        Path stagingPath = Files.createTempFile("bhe-map-import-", ".spm");
        try {
            spmGenerator.generate(stagingPath.toString(), spm, charset);
            return Files.readAllBytes(stagingPath);
        } finally {
            Files.deleteIfExists(stagingPath);
        }
    }

    private byte[] generateMapGroupBytes(MapGroupGrp mapGroup, String charset) throws IOException {
        mapGroup.setFileName("mapgroup");
        mapGroup.setExtensionName("grp");
        Path stagingPath = Files.createTempFile("bhe-map-import-", ".grp");
        try {
            bsdxBinService.generate(stagingPath.toString(), mapGroup, charset);
            return Files.readAllBytes(stagingPath);
        } finally {
            Files.deleteIfExists(stagingPath);
        }
    }

    private MapGroupGrp parseMapGroupBytes(byte[] bytes, String fileName, String charset) {
        Path stagingDir = null;
        Path stagingPath = null;
        try {
            stagingDir = Files.createTempDirectory("bhe-map-import-");
            stagingPath = stagingDir.resolve(fileName);
            Files.write(stagingPath, bytes);
            return (MapGroupGrp) bsdxBinService.parse(stagingPath.toString(), charset).getData();
        } catch (IOException e) {
            throw new IllegalStateException("output MapGroup verification failed: " + fileName, e);
        } finally {
            if (stagingPath != null) {
                try {
                    Files.deleteIfExists(stagingPath);
                } catch (IOException ignored) {
                }
            }
            if (stagingDir != null) {
                try {
                    Files.deleteIfExists(stagingDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    private BheMapPendingOutputFile pendingFile(
            Path path,
            byte[] bytes,
            BheMapOutputConflictType conflictType,
            String logicalName
    ) {
        BheMapPendingOutputFile pendingFile = new BheMapPendingOutputFile();
        pendingFile.setPath(path);
        pendingFile.setBytes(bytes);
        pendingFile.setConflictType(conflictType);
        pendingFile.setLogicalName(logicalName);
        return pendingFile;
    }

    public void applyCommit(BheMapImportOutputResult result, BheMapOutputPreflightResult preflightResult) {
        if (result == null || preflightResult == null) {
            return;
        }
        Set<Path> outputPaths = new HashSet<>();
        for (BheMapPendingOutputFile pendingOutputFile : result.getPendingOutputFiles()) {
            outputPaths.add(pendingOutputFile.getPath().toAbsolutePath().normalize());
        }
        for (BheMapOutputWriteAudit audit : preflightResult.getWriteAudits()) {
            if (audit.getPath() == null || !outputPaths.contains(audit.getPath().toAbsolutePath().normalize())) {
                continue;
            }
            if (audit.isWritten()) {
                result.setWrittenFileCount(result.getWrittenFileCount() + 1);
            }
            if (audit.isExistingIdentical()) {
                result.setExistingIdenticalFileCount(result.getExistingIdenticalFileCount() + 1);
            }
        }
        result.setOutputCompleted(true);
    }

    public void verifyCommittedOutput(BheMapImportOutputResult result, String charset) throws IOException {
        if (result == null || !result.isOutputCompleted()) {
            return;
        }
        for (BheMapImportWrittenMap writtenMap : result.getWrittenMaps()) {
            verifyMapData(writtenMap.getMapPath(), writtenMap.getTargetMapFileName(), charset);
            if (writtenMap.isComposition()) {
                verifySpm(writtenMap.getComposedSpmPath(), writtenMap.getComposedSpmFileName(), charset);
            }
        }
        MapGroupGrp verifiedMapGroup = readMapGroup(result.getOutputMapGroupPath(), charset);
        if (verifiedMapGroup.getGroupList().size() != result.getOutputMapGroupCount()) {
            throw new IllegalStateException("output MapGroup count mismatch after commit");
        }
    }

    private void appendMapGroupEntries(
            MapGroupGrp outputMapGroup,
            List<BheMapEntryPlan> writtenEntries,
            BheMapImportOutputResult result
    ) {
        for (BheMapEntryPlan entry : writtenEntries) {
            outputMapGroup.getGroupList().add(mapGroupEntryConverter.convert(entry).getTargetGroup());
            result.getAppendedGroupResourceNames().add(entry.getTargetGroupResourceName());
        }
        result.setAppendedMapGroupCount(writtenEntries.size());
    }

    private void recordWritten(
            BheMapImportOutputResult result,
            BheMapEntryPlan entry,
            Path targetMapPath,
            MapConversionOutput conversion,
            Path composedSpmPath
    ) {
        BheMapImportWrittenMap writtenMap = new BheMapImportWrittenMap();
        writtenMap.setSourceMapFileName(entry.getSourceMapFileName());
        writtenMap.setTargetMapFileName(entry.getTargetMapFileName());
        writtenMap.setTargetGroupResourceName(entry.getTargetGroupResourceName());
        writtenMap.setMapPath(targetMapPath);
        writtenMap.setComposition(conversion.composition());
        if (conversion.composition()) {
            writtenMap.setComposedSpmFileName(
                    conversion.compositionResult().getCompositionPlan().getTargetComposedSpriteMapFileName()
            );
            writtenMap.setComposedSpmPath(composedSpmPath);
            result.setCompositionWrittenMapCount(result.getCompositionWrittenMapCount() + 1);
        } else {
            result.setDirectWrittenMapCount(result.getDirectWrittenMapCount() + 1);
        }
        result.getWrittenMaps().add(writtenMap);
        result.getWrittenMapFileList().add(targetMapPath);
        result.setWrittenMapCount(result.getWrittenMapCount() + 1);
    }

    private void recordBlocked(
            BheMapImportOutputResult result,
            BheMapEntryPlan entry,
            MapConversionOutput conversion
    ) {
        if (conversion.compositionAttempted()) {
            recordCompositionBlocked(result, entry, conversion);
            return;
        }
        BheMapDataConversionResult directConversion = conversion.directConversion();
        boolean multipleSpriteMaps = false;
        boolean unsupportedScriptGroup = false;
        boolean missingFinalReference = false;
        Set<BheMapDataConversionIssueType> issueTypes = new LinkedHashSet<>();
        for (BheMapDataConversionIssue issue : directConversion.getBlockingIssues()) {
            result.recordIssueType(issue.getType());
            issueTypes.add(issue.getType());
            if (issue.getType() == BheMapDataConversionIssueType.UNSUPPORTED_MULTIPLE_SPRITE_MAPS) {
                multipleSpriteMaps = true;
            }
            if (issue.getType() == BheMapDataConversionIssueType.UNSUPPORTED_SCRIPT_GROUP_INDEX) {
                unsupportedScriptGroup = true;
            }
            if (issue.getType() == BheMapDataConversionIssueType.MISSING_FINAL_REFERENCE) {
                missingFinalReference = true;
            }
        }

        if (multipleSpriteMaps) {
            result.setBlockedByMultipleSpriteMapListCount(result.getBlockedByMultipleSpriteMapListCount() + 1);
        }
        if (unsupportedScriptGroup) {
            result.setBlockedByUnsupportedScriptGroupCount(result.getBlockedByUnsupportedScriptGroupCount() + 1);
        }
        if (missingFinalReference) {
            result.setBlockedByMissingFinalReferenceCount(result.getBlockedByMissingFinalReferenceCount() + 1);
        }
        boolean missingSourceSpm = hasMissingSourceSpm(entry);
        if (missingSourceSpm) {
            result.setBlockedByMissingSourceSpmCount(result.getBlockedByMissingSourceSpmCount() + 1);
        }

        BheMapImportBlockedMap blockedMap = new BheMapImportBlockedMap();
        blockedMap.setSourceMapFileName(entry.getSourceMapFileName());
        blockedMap.setTargetMapFileName(entry.getTargetMapFileName());
        blockedMap.setTargetGroupResourceName(entry.getTargetGroupResourceName());
        blockedMap.setMissingSourceSpm(missingSourceSpm);
        blockedMap.getIssueTypes().addAll(issueTypes);
        blockedMap.getBlockingIssues().addAll(directConversion.getBlockingIssues());
        result.getBlockedMaps().add(blockedMap);
    }

    private void recordCompositionBlocked(
            BheMapImportOutputResult result,
            BheMapEntryPlan entry,
            MapConversionOutput conversion
    ) {
        boolean missingSourceSpm = false;
        BheMapImportBlockedMap blockedMap = new BheMapImportBlockedMap();
        blockedMap.setSourceMapFileName(entry.getSourceMapFileName());
        blockedMap.setTargetMapFileName(entry.getTargetMapFileName());
        blockedMap.setTargetGroupResourceName(entry.getTargetGroupResourceName());
        for (MapCompatibilityIssue issue : conversion.compositionResult().getBlockingIssues()) {
            result.recordCompositionBlockingReason(issue.getReason());
            blockedMap.getCompositionBlockingReasons().add(issue.getReason());
            blockedMap.getCompositionBlockingIssues().add(issue);
            if (issue.getReason() == MapCompatibilityBlockingReason.MISSING_SOURCE_SPM) {
                missingSourceSpm = true;
            }
        }
        blockedMap.setMissingSourceSpm(missingSourceSpm);
        if (missingSourceSpm) {
            result.setBlockedByMissingSourceSpmCount(result.getBlockedByMissingSourceSpmCount() + 1);
        }
        result.getBlockedMaps().add(blockedMap);
    }

    private boolean hasMissingSourceSpm(BheMapEntryPlan entry) {
        if (entry == null || entry.getResourceReferences() == null) {
            return false;
        }
        for (BheMapResourceReference reference : entry.getResourceReferences().getReferences()) {
            if (reference == null || !reference.isMissing()) {
                continue;
            }
            if (reference.getType() == BheMapReferenceType.SPRITE_MAP
                    || reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean hasIssue(BheMapDataConversionResult result, BheMapDataConversionIssueType type) {
        if (result == null || result.getBlockingIssues() == null) {
            return false;
        }
        for (BheMapDataConversionIssue issue : result.getBlockingIssues()) {
            if (issue != null && issue.getType() == type) {
                return true;
            }
        }
        return false;
    }

    private record MapConversionOutput(
            BheMapDataConversionResult directConversion,
            MultipleSpriteMapListCompositionResult compositionResult
    ) {

        private static MapConversionOutput direct(BheMapDataConversionResult directConversion) {
            return new MapConversionOutput(directConversion, null);
        }

        private static MapConversionOutput blocked(BheMapDataConversionResult directConversion) {
            return new MapConversionOutput(directConversion, null);
        }

        private static MapConversionOutput composition(
                BheMapDataConversionResult directConversion,
                MultipleSpriteMapListCompositionResult compositionResult
        ) {
            return new MapConversionOutput(directConversion, compositionResult);
        }

        private static MapConversionOutput blockedComposition(
                BheMapDataConversionResult directConversion,
                MultipleSpriteMapListCompositionResult compositionResult
        ) {
            return new MapConversionOutput(directConversion, compositionResult);
        }

        private boolean converted() {
            return directConversion != null && directConversion.isConverted()
                    || compositionResult != null && compositionResult.isConverted();
        }

        private boolean composition() {
            return compositionResult != null && compositionResult.isConverted();
        }

        private boolean compositionAttempted() {
            return compositionResult != null;
        }

        private com.giga.nexas.dto.bsdx.map.MapData mapData() {
            return composition()
                    ? compositionResult.getConvertedMapData()
                    : directConversion.getConvertedMapData();
        }
    }
}
