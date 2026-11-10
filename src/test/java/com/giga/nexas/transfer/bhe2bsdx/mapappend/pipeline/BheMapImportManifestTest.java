package com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportBlockedManifestEntry;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportManifest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportManifestEntry;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceMaterializationResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapImportManifestTest {

    @TempDir
    Path outputRoot;

    @Test
    void completeStaticResourceRootBuildsInMemoryImportManifest() {
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
        BheMapImportManifest manifest = result.getImportManifest();

        assertNotNull(output);
        assertNotNull(resources);
        assertNotNull(manifest);
        assertEquals(result.getAppendPlan().size(), manifest.getTotalPlanEntries());
        assertEquals(output.getWrittenMapCount(), manifest.getWrittenMapCount());
        assertEquals(output.getAppendedMapGroupCount(), manifest.getAppendedMapGroupCount());
        assertEquals(resources.getMaterializedResourceCount(), manifest.getMaterializedResourceCount());
        assertEquals(resources.getMaterializedForegroundCount(), manifest.getForegroundResourceCount());
        assertEquals(resources.getMaterializedResourceSlotCount(), manifest.getResourceSlotResourceCount());
        assertEquals(resources.getMaterializedSpriteMapCount(), manifest.getSpriteMapResourceCount());
        assertEquals(resources.getServicedResourceReferenceCount(), manifest.getInternalResourceReferenceCount());
        assertEquals(resources.getDuplicateTargetResourceCount(), manifest.getDuplicateTargetReferenceCount());
        assertEquals(output.getBlockedMaps().size(), manifest.getBlockedMapCount());
        assertEquals(output.getBlockedByMultipleSpriteMapListCount(), manifest.getBlockedByMultipleSpriteMapListCount());
        assertEquals(output.getBlockedByUnsupportedScriptGroupCount(), manifest.getBlockedByUnsupportedScriptGroupCount());
        assertEquals(output.getBlockedByMissingSourceSpmCount(), manifest.getBlockedByMissingSourceSpmCount());
        assertEquals(output.getBlockedByMissingFinalReferenceCount(), manifest.getBlockedByMissingFinalReferenceCount());
        assertEquals(output.getOutputConflictCount() + resources.getOutputConflictCount(), manifest.getOutputConflictCount());
        assertEquals(
                output.getExistingIdenticalFileCount() + resources.getExistingIdenticalFileCount(),
                manifest.getExistingIdenticalFileCount()
        );
        assertEquals(output.isOutputCompleted(), manifest.isOutputCompleted());
        assertEquals(result.isResourcesMaterialized(), manifest.isResourcesMaterialized());
        assertEquals(output.getOutputRoot(), manifest.getOutputRoot());
        assertEquals(output.getOutputMapGroupPath(), manifest.getOutputMapGroupPath());

        assertEquals(142, manifest.getTotalPlanEntries());
        assertEquals(137, manifest.getWrittenMapCount());
        assertEquals(33, manifest.getDirectWrittenMapCount());
        assertEquals(104, manifest.getCompositionWrittenMapCount());
        assertEquals(104, manifest.getComposedSpmWrittenCount());
        assertEquals(137, manifest.getAppendedMapGroupCount());
        assertEquals(97, manifest.getMaterializedResourceCount());
        assertEquals(68, manifest.getForegroundResourceCount());
        assertEquals(18, manifest.getResourceSlotResourceCount());
        assertEquals(11, manifest.getSpriteMapResourceCount());
        assertEquals(234, manifest.getInternalResourceReferenceCount());
        assertEquals(137, manifest.getDuplicateTargetReferenceCount());
        assertEquals(5, manifest.getBlockedMapCount());
        assertEquals(0, manifest.getBlockedByMultipleSpriteMapListCount());
        assertEquals(0, manifest.getBlockedByUnsupportedScriptGroupCount());
        assertEquals(5, manifest.getBlockedByMissingSourceSpmCount());
        assertEquals(0, manifest.getBlockedByMissingFinalReferenceCount());
        assertEquals(0, manifest.getOutputConflictCount());
        assertEquals(0, manifest.getExistingIdenticalFileCount());
        assertTrue(manifest.isOutputCompleted());
        assertTrue(manifest.isResourcesMaterialized());
        assertFalse(manifest.isPack());
        assertTrue(manifest.isComposedSpmWritten());
        assertFalse(manifest.isDynamicValidation());
        assertFalse(manifest.isProbeRestored());
        assertFalse(manifest.isPreviewMaterialized());
        assertFalse(manifest.isPreviewMaterializationEnabled());
        assertEquals(
                BheMapImportManifest.TRANSACTIONAL_OUTPUT_SCOPE_MAP_FILES_AND_MAPGROUP_AND_RESOURCES,
                manifest.getTransactionalOutputScope()
        );
        assertFalse(manifest.isPreviewMaterializationTransactional());

        assertEquals(137, manifest.getWrittenEntries().size());
        assertEquals(5, manifest.getBlockedEntries().size());
        assertWrittenManifestEntries(manifest);
        assertBlockedManifestEntries(manifest);
        assertFalse(Files.exists(outputRoot.resolve("BheMapImportManifest.json")));
        assertFalse(Files.exists(Paths.get("tools/probe/bhe_map_spm_selector_probe.js")));
        assertFalse(Files.exists(Paths.get("tools/probe/bsdx_map_object_probe.js")));
    }

    private void assertWrittenManifestEntries(BheMapImportManifest manifest) {
        int foregroundReferences = 0;
        int resourceSlotReferences = 0;
        int spriteMapReferences = 0;
        for (BheMapImportManifestEntry entry : manifest.getWrittenEntries()) {
            assertNotNull(entry.getSourceMapFileName());
            assertNotNull(entry.getTargetMapFileName());
            assertNotNull(entry.getTargetGroupResourceName());
            assertTrue(Files.isRegularFile(entry.getOutputMapPath()));
            for (String resourceTargetFileName : entry.getWrittenResourceTargetFileNames()) {
                assertTrue(Files.isRegularFile(outputRoot.resolve(resourceTargetFileName)));
            }
            foregroundReferences += entry.getForegroundReferenceCount();
            resourceSlotReferences += entry.getResourceSlotReferenceCount();
            spriteMapReferences += entry.getSpriteMapReferenceCount();
        }
        assertEquals(137, foregroundReferences);
        assertEquals(72, resourceSlotReferences);
        assertEquals(25, spriteMapReferences);
        assertEquals(manifest.getInternalResourceReferenceCount(),
                foregroundReferences + resourceSlotReferences + spriteMapReferences);
    }

    private void assertBlockedManifestEntries(BheMapImportManifest manifest) {
        int missingSourceSpm = 0;
        for (BheMapImportBlockedManifestEntry entry : manifest.getBlockedEntries()) {
            assertNotNull(entry.getSourceMapFileName());
            assertNotNull(entry.getTargetMapFileName());
            assertNotNull(entry.getTargetGroupResourceName());
            assertTrue(entry.isNotWritten());
            assertFalse(entry.getCompositionBlockingReasons().isEmpty());
            assertFalse(Files.exists(outputRoot.resolve(entry.getTargetMapFileName())));
            if (entry.isMissingSourceSpm()) {
                missingSourceSpm++;
            }
        }
        assertEquals(manifest.getBlockedByMissingSourceSpmCount(), missingSourceSpm);
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
