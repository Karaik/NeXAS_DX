package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.map.parser.MapDataParser;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityBlockingReason;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportBlockedMap;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportWrittenMap;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline.BheMapImportPipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapResourceMaterializerTest {

    @TempDir
    Path outputRoot;

    @Test
    void completeStaticResourceRootMaterializesOnlyResourcesUsedByWrittenMaps() throws Exception {
        Path completeResourceRoot = resolveCompleteStaticResourceRoot();
        assumeTrue(
                completeResourceRoot != null && Files.isDirectory(completeResourceRoot),
                "complete BHE static resource root must be provided by -Dbhe.staticResourceRoot or BHE_STATIC_RESOURCE_ROOT"
        );

        BheMapAppendRequest request = new BheMapAppendRequest();
        request.setBheStaticResourceRoot(completeResourceRoot);
        request.setCurrentTargetMapGroupPath(request.getBsdxMapGroupPath());
        request.setOutputRoot(outputRoot);

        BheMapAppendResult result = new BheMapImportPipeline().importMapsIntoCurrentResourceTree(request);
        BheMapImportOutputResult output = result.getOutputResult();
        BheMapResourceMaterializationResult resources = result.getResourceMaterializationResult();

        assertEquals(142, result.getAppendPlan().size());
        assertEquals(137, output.getWrittenMapCount());
        assertEquals(33, output.getDirectWrittenMapCount());
        assertEquals(104, output.getCompositionWrittenMapCount());
        assertEquals(104, output.getComposedSpmWrittenCount());
        assertEquals(137, output.getAppendedMapGroupCount());
        assertEquals(5, output.getBlockedMaps().size());
        assertEquals(5, output.compositionBlockingReasonCount(MapCompatibilityBlockingReason.MISSING_SOURCE_SPM));
        assertEquals(97, resources.getMaterializedResourceCount());
        assertEquals(68, resources.getMaterializedForegroundCount());
        assertEquals(18, resources.getMaterializedResourceSlotCount());
        assertEquals(11, resources.getMaterializedSpriteMapCount());
        assertEquals(97, resources.getWrittenFileCount());
        assertEquals(0, resources.getExistingIdenticalFileCount());
        assertEquals(0, resources.getOutputConflictCount());
        assertEquals(137, resources.getDuplicateTargetResourceCount());
        assertEquals(0, resources.getSkippedBlockedResourceCount());
        assertEquals(0, resources.getMissingSourceResourceCount());
        assertFalse(resources.isComposedSpmWritten());
        assertFalse(resources.isPreviewWritten());
        assertFalse(resources.isPacked());
        assertTrue(resources.isSpmWritten());
        assertEquals(
                resources.getMaterializedForegroundCount()
                        + resources.getMaterializedResourceSlotCount()
                        + resources.getMaterializedSpriteMapCount(),
                resources.getMaterializedResourceCount()
        );

        Set<String> writtenMapNames = output.getWrittenMaps().stream()
                .map(BheMapImportWrittenMap::getTargetMapFileName)
                .collect(Collectors.toSet());
        Set<String> materializedFileNames = resources.getWrittenResourceFileList().stream()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toSet());
        Set<String> expectedWrittenResourceNames = new HashSet<>();
        Set<String> blockedOnlyResourceNames = new HashSet<>();
        for (BheMapEntryPlan entry : result.getAppendPlan().getEntries()) {
            boolean written = writtenMapNames.contains(entry.getTargetMapFileName());
            for (BheMapResourceReference reference : entry.getResourceReferences().getReferences()) {
                if (!isOutputReferenceForEntry(entry, reference) || reference.isMissing()) {
                    continue;
                }
                if (written && isOutputReferenceForWrittenMap(writtenMapFor(output, entry), reference)) {
                    expectedWrittenResourceNames.add(reference.getTargetFileName());
                }
            }
        }
        for (BheMapEntryPlan entry : result.getAppendPlan().getEntries()) {
            if (writtenMapNames.contains(entry.getTargetMapFileName())) {
                continue;
            }
            for (BheMapResourceReference reference : entry.getResourceReferences().getReferences()) {
                if (isOutputReferenceForEntry(entry, reference) && !reference.isMissing()) {
                    blockedOnlyResourceNames.add(reference.getTargetFileName());
                }
            }
        }
        blockedOnlyResourceNames.removeAll(expectedWrittenResourceNames);

        assertEquals(expectedWrittenResourceNames.size(), resources.getMaterializedResourceCount());
        assertEquals(expectedWrittenResourceNames, materializedFileNames);
        assertTrue(blockedOnlyResourceNames.stream().noneMatch(materializedFileNames::contains));

        for (Path writtenResource : resources.getWrittenResourceFileList()) {
            assertTrue(Files.isRegularFile(writtenResource));
        }
        for (BheMapImportBlockedMap blockedMap : output.getBlockedMaps()) {
            assertFalse(Files.exists(outputRoot.resolve(blockedMap.getTargetMapFileName())));
        }

        assertTrue(Files.isRegularFile(output.getOutputMapGroupPath()));
        MapGroupGrp outputMapGroup = parseMapGroup(output.getOutputMapGroupPath(), request.getCharset());
        assertEquals(output.getBaselineMapGroupCount() + 137, outputMapGroup.getGroupList().size());
        MapDataParser mapDataParser = new MapDataParser();
        for (BheMapImportWrittenMap writtenMap : output.getWrittenMaps()) {
            assertTrue(Files.isRegularFile(writtenMap.getMapPath()));
            mapDataParser.parse(Files.readAllBytes(writtenMap.getMapPath()), writtenMap.getTargetMapFileName(), request.getCharset());
        }

        assertEquals(0, output.getBlockedByMultipleSpriteMapListCount());
        assertEquals(0, output.getBlockedByUnsupportedScriptGroupCount());
        assertEquals(5, output.getBlockedByMissingSourceSpmCount());
        assertEquals(0, output.getBlockedByMissingFinalReferenceCount());
        assertEquals(
                resources.getMaterializedSpriteMapCount() + output.getComposedSpmWrittenCount(),
                countFilesByExtension(outputRoot, ".spm")
        );
        assertEquals(0, countFilesByExtension(outputRoot, ".pac"));
        assertFalse(Files.exists(Paths.get("tools/probe/bhe_map_spm_selector_probe.js")));
        assertFalse(Files.exists(Paths.get("tools/probe/bsdx_map_object_probe.js")));
    }

    private boolean isOutputReferenceForEntry(BheMapEntryPlan entry, BheMapResourceReference reference) {
        if (reference == null || reference.getType() == null) {
            return false;
        }
        if (reference.getType() == BheMapReferenceType.FOREGROUND
                || reference.getType() == BheMapReferenceType.RESOURCE_SLOT
                || reference.getType() == BheMapReferenceType.SPRITE_MAP) {
            return true;
        }
        return reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST
                && entry.getResourceReferences().getReferences().stream()
                .filter(candidate -> candidate.getType() == BheMapReferenceType.SPRITE_MAP_LIST)
                .count() == 1;
    }

    private boolean isOutputReferenceForWrittenMap(
            BheMapImportWrittenMap writtenMap,
            BheMapResourceReference reference
    ) {
        if (writtenMap != null && writtenMap.isComposition()
                && reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST) {
            return false;
        }
        return true;
    }

    private BheMapImportWrittenMap writtenMapFor(BheMapImportOutputResult output, BheMapEntryPlan entry) {
        for (BheMapImportWrittenMap writtenMap : output.getWrittenMaps()) {
            if (writtenMap.getTargetMapFileName().equals(entry.getTargetMapFileName())) {
                return writtenMap;
            }
        }
        return null;
    }

    private MapGroupGrp parseMapGroup(Path mapGroupPath, String charset) throws Exception {
        return (MapGroupGrp) new BsdxBinService().parse(mapGroupPath.toString(), charset).getData();
    }

    private long countFilesByExtension(Path root, String extension) throws Exception {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension))
                    .count();
        }
    }

    private Path resolveCompleteStaticResourceRoot() {
        String propertyValue = System.getProperty("bhe.staticResourceRoot");
        if (!isBlank(propertyValue)) {
            return Paths.get(propertyValue).toAbsolutePath().normalize();
        }
        String environmentValue = System.getenv("BHE_STATIC_RESOURCE_ROOT");
        if (!isBlank(environmentValue)) {
            return Paths.get(environmentValue).toAbsolutePath().normalize();
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
