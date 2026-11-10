package com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline;

import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.map.parser.MapDataParser;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityBlockingReason;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportBlockedMap;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportWrittenMap;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceMaterializationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapImportOutputPipelineTest {

    @TempDir
    Path outputRoot;

    @Test
    void completeStaticResourceRootImportsConvertibleMapsThroughPipeline() throws Exception {
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

        assertFalse(result.isPreviewMaterialized());
        assertFalse(result.isPreviewMaterializationEnabled());
        assertTrue(result.isOutputWritten());
        assertNotNull(result.getAppendPlan());
        assertNotNull(output);
        assertNotNull(resources);
        assertTrue(result.isResourcesMaterialized());
        assertEquals(142, result.getAppendPlan().size());
        assertEquals(142, output.getTotalPlanEntries());
        assertEquals(137, output.getWrittenMapCount());
        assertEquals(33, output.getDirectWrittenMapCount());
        assertEquals(104, output.getCompositionWrittenMapCount());
        assertEquals(104, output.getComposedSpmWrittenCount());
        assertEquals(137, output.getAppendedMapGroupCount());
        assertEquals(5, output.getBlockedMaps().size());
        assertEquals(0, output.getBlockedByMultipleSpriteMapListCount());
        assertEquals(0, output.getBlockedByUnsupportedScriptGroupCount());
        assertEquals(5, output.getBlockedByMissingSourceSpmCount());
        assertEquals(0, output.getBlockedByMissingFinalReferenceCount());
        assertEquals(5, output.compositionBlockingReasonCount(MapCompatibilityBlockingReason.MISSING_SOURCE_SPM));
        assertEquals(242, output.getWrittenFileCount());
        assertEquals(0, output.getExistingIdenticalFileCount());
        assertEquals(0, output.getOutputConflictCount());
        assertTrue(output.isOutputCompleted());
        assertTrue(output.isSpmWritten());
        assertTrue(output.isComposedSpmWritten());
        assertFalse(output.isPacked());
        assertEquals(97, resources.getMaterializedResourceCount());
        assertEquals(68, resources.getMaterializedForegroundCount());
        assertEquals(18, resources.getMaterializedResourceSlotCount());
        assertEquals(11, resources.getMaterializedSpriteMapCount());
        assertEquals(97, resources.getWrittenFileCount());
        assertEquals(0, resources.getExistingIdenticalFileCount());
        assertEquals(0, resources.getOutputConflictCount());
        assertEquals(0, resources.getMissingSourceResourceCount());
        assertFalse(resources.isComposedSpmWritten());
        assertFalse(resources.isPacked());

        assertTrue(Files.isRegularFile(output.getOutputMapGroupPath()));
        MapGroupGrp outputMapGroup = parseMapGroup(output.getOutputMapGroupPath(), request.getCharset());
        assertEquals(output.getBaselineMapGroupCount() + 137, outputMapGroup.getGroupList().size());

        MapDataParser mapDataParser = new MapDataParser();
        for (BheMapImportWrittenMap writtenMap : output.getWrittenMaps()) {
            assertTrue(Files.isRegularFile(writtenMap.getMapPath()));
            mapDataParser.parse(Files.readAllBytes(writtenMap.getMapPath()), writtenMap.getTargetMapFileName(), request.getCharset());
        }

        for (BheMapImportBlockedMap blockedMap : output.getBlockedMaps()) {
            assertFalse(Files.exists(outputRoot.resolve(blockedMap.getTargetMapFileName())));
        }

        assertEquals(137, countFilesByExtension(outputRoot, ".map"));
        assertEquals(
                resources.getMaterializedSpriteMapCount() + output.getComposedSpmWrittenCount(),
                countFilesByExtension(outputRoot, ".spm")
        );
        assertEquals(0, countFilesByExtension(outputRoot, ".bmp"));
        assertEquals(0, countFilesByExtension(outputRoot, ".pac"));
        assertFalse(Files.exists(Paths.get("tools/probe/bhe_map_spm_selector_probe.js")));
        assertFalse(Files.exists(Paths.get("tools/probe/bsdx_map_object_probe.js")));
    }

    @Test
    void completeStaticResourceRootImportsConvertibleMapsAndMaterializesPreviewWhenExplicitlyEnabled() throws Exception {
        Path completeResourceRoot = resolveCompleteStaticResourceRoot();
        assumeTrue(
                completeResourceRoot != null && Files.isDirectory(completeResourceRoot),
                "complete BHE static resource root must be provided by -Dbhe.staticResourceRoot or BHE_STATIC_RESOURCE_ROOT"
        );

        BheMapAppendRequest request = new BheMapAppendRequest();
        request.setBheStaticResourceRoot(completeResourceRoot);
        request.setCurrentTargetMapGroupPath(request.getBsdxMapGroupPath());
        request.setOutputRoot(outputRoot);
        request.setPreviewMaterializationEnabled(true);

        BheMapAppendResult result = new BheMapImportPipeline().importMapsIntoCurrentResourceTree(request);

        assertTrue(result.isPreviewMaterializationEnabled());
        assertTrue(result.isPreviewMaterialized());
        assertNotNull(result.getImportManifest());
        assertTrue(result.getImportManifest().isPreviewMaterializationEnabled());
        assertTrue(result.getImportManifest().isPreviewMaterialized());
        assertFalse(result.getImportManifest().isPreviewMaterializationTransactional());
        assertEquals(137, result.getOutputResult().getWrittenMapCount());
        assertEquals(97, result.getResourceMaterializationResult().getMaterializedResourceCount());
        assertTrue(countFilesByExtension(outputRoot, ".bmp") > 0);
        assertEquals(0, countFilesByExtension(outputRoot, ".pac"));
        assertFalse(Files.exists(Paths.get("tools/probe/bhe_map_spm_selector_probe.js")));
        assertFalse(Files.exists(Paths.get("tools/probe/bsdx_map_object_probe.js")));
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
