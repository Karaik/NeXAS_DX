package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import com.giga.nexas.dto.bsdx.map.MapData;
import com.giga.nexas.dto.bsdx.map.parser.MapDataParser;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapOutputResourceReferenceConsistencyTest {

    @TempDir
    Path outputRoot;

    private final BheMapOutputResourceReferenceResolver resolver = new BheMapOutputResourceReferenceResolver();

    @Test
    void writtenMapResourceReferencesResolveToMaterializedOutputFiles() throws Exception {
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
        assertEquals(97, resources.getMaterializedResourceCount());

        ReferenceCheckStats stats = new ReferenceCheckStats();
        MapDataParser parser = new MapDataParser();
        for (BheMapImportWrittenMap writtenMap : output.getWrittenMaps()) {
            assertTrue(Files.isRegularFile(writtenMap.getMapPath()));
            stats.writtenMapCount++;
            MapData mapData = parser.parse(
                    Files.readAllBytes(writtenMap.getMapPath()),
                    writtenMap.getTargetMapFileName(),
                    request.getCharset()
            );
            checkReference(mapData.getForegroundImage(), stats, ReferenceKind.FOREGROUND);
            for (MapData.NamedResourceSlotBlock slot : mapData.getNamedResourceSlotBlocks()) {
                checkReference(slot.getSlotText(), stats, ReferenceKind.RESOURCE_SLOT);
            }
            checkReference(mapData.getSpriteMap(), stats, ReferenceKind.SPRITE_MAP);
        }

        assertEquals(137, stats.writtenMapCount);
        assertEquals(137, stats.foregroundReferenceCount);
        assertEquals(72, stats.resourceSlotReferenceCount);
        assertEquals(129, stats.spriteMapReferenceCount);
        assertEquals(338, stats.totalReferenceCount());
        assertTrue(stats.uniqueResolvedOutputResourceCount() >= resources.getMaterializedResourceCount());
        assertTrue(stats.uniqueResolvedOutputResourceCount()
                <= resources.getMaterializedResourceCount() + output.getComposedSpmWrittenCount());
        assertEquals(stats.totalReferenceCount() - stats.uniqueResolvedOutputResourceCount(),
                stats.duplicateResolvedOutputResourceCount());
        assertEquals(0, stats.missingResolvedOutputResourceCount);

        for (BheMapImportBlockedMap blockedMap : output.getBlockedMaps()) {
            assertFalse(Files.exists(outputRoot.resolve(blockedMap.getTargetMapFileName())));
        }
        assertFalse(resources.isComposedSpmWritten());
        assertTrue(output.isComposedSpmWritten());
        assertFalse(resources.isPacked());
        assertFalse(output.isPacked());
        assertFalse(Files.exists(Paths.get("tools/probe/bhe_map_spm_selector_probe.js")));
        assertFalse(Files.exists(Paths.get("tools/probe/bsdx_map_object_probe.js")));
    }

    private void checkReference(String mapInternalResourceText, ReferenceCheckStats stats, ReferenceKind kind) {
        if (mapInternalResourceText == null || mapInternalResourceText.isBlank()) {
            return;
        }

        if (kind == ReferenceKind.FOREGROUND) {
            stats.foregroundReferenceCount++;
        } else if (kind == ReferenceKind.RESOURCE_SLOT) {
            stats.resourceSlotReferenceCount++;
        } else if (kind == ReferenceKind.SPRITE_MAP) {
            stats.spriteMapReferenceCount++;
        }

        Path resolvedPath = resolver.resolveOutputResourcePath(outputRoot, mapInternalResourceText);
        if (!stats.resolvedOutputPaths.add(resolvedPath)) {
            stats.duplicateResolvedOutputResourceCount++;
        }
        if (!Files.isRegularFile(resolvedPath)) {
            stats.missingResolvedOutputResourceCount++;
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

    private enum ReferenceKind {
        FOREGROUND,
        RESOURCE_SLOT,
        SPRITE_MAP
    }

    private static class ReferenceCheckStats {
        private int writtenMapCount;
        private int foregroundReferenceCount;
        private int resourceSlotReferenceCount;
        private int spriteMapReferenceCount;
        private int missingResolvedOutputResourceCount;
        private int duplicateResolvedOutputResourceCount;
        private Set<Path> resolvedOutputPaths = new HashSet<>();

        private int totalReferenceCount() {
            return foregroundReferenceCount + resourceSlotReferenceCount + spriteMapReferenceCount;
        }

        private int uniqueResolvedOutputResourceCount() {
            return resolvedOutputPaths.size();
        }

        private int duplicateResolvedOutputResourceCount() {
            return duplicateResolvedOutputResourceCount;
        }
    }
}
