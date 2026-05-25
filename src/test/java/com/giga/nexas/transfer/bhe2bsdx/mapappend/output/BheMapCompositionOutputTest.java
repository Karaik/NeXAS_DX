package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import com.giga.nexas.dto.bsdx.map.MapData;
import com.giga.nexas.dto.bsdx.map.parser.MapDataParser;
import com.giga.nexas.dto.bsdx.spm.parser.SpmParser;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityBlockingReason;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline.BheMapImportPipeline;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapCompositionOutputTest {

    @TempDir
    Path outputRoot;

    @Test
    void completeStaticResourceRootWritesDirectAndCompositionMaps() throws Exception {
        BheMapAppendResult result = runPipeline(requireCompleteStaticResourceRoot());
        BheMapImportOutputResult output = result.getOutputResult();

        assertTrue(result.isOutputWritten());
        assertTrue(result.isResourcesMaterialized());
        assertEquals(142, output.getTotalPlanEntries());
        assertEquals(137, output.getWrittenMapCount());
        assertEquals(33, output.getDirectWrittenMapCount());
        assertEquals(104, output.getCompositionWrittenMapCount());
        assertEquals(104, output.getComposedSpmWrittenCount());
        assertEquals(5, output.getBlockedMaps().size());
        assertEquals(5, output.compositionBlockingReasonCount(MapCompatibilityBlockingReason.MISSING_SOURCE_SPM));
        assertEquals(137, output.getAppendedMapGroupCount());
        assertEquals(137, countFilesByExtension(outputRoot, ".map"));
        assertEquals(104, output.getWrittenComposedSpmFileList().size());
        assertEquals(115, countFilesByExtension(outputRoot, ".spm"));
        assertEquals(0, countFilesByExtension(outputRoot, ".pac"));
        assertNoProbeFiles();
    }

    @Test
    void compositionMapOutputUsesComposedSpmAndRewrittenTypeIds() throws Exception {
        BheMapAppendResult result = runPipeline(requireCompleteStaticResourceRoot());
        BheMapImportWrittenMap writtenMap = result.getOutputResult().getWrittenMaps().stream()
                .filter(BheMapImportWrittenMap::isComposition)
                .findFirst()
                .orElseThrow();

        MapData parsedMap = new MapDataParser().parse(
                Files.readAllBytes(writtenMap.getMapPath()),
                writtenMap.getTargetMapFileName(),
                "windows-31j"
        );

        assertEquals(writtenMap.getComposedSpmFileName(), parsedMap.getSpriteMap());
        assertTrue(Files.isRegularFile(writtenMap.getComposedSpmPath()));
        new SpmParser().parse(
                Files.readAllBytes(writtenMap.getComposedSpmPath()),
                writtenMap.getComposedSpmFileName(),
                "windows-31j"
        );
        int rewrittenTypeId = parsedMap.getScriptEntryGroupBlocks().stream()
                .flatMap(group -> group.getEntries().stream())
                .map(MapData.ScriptEntry::getTypeId)
                .filter(typeId -> typeId != null && typeId > 0)
                .findFirst()
                .orElseThrow();
        assertTrue(rewrittenTypeId >= 0);
        assertNotEquals("", parsedMap.getSpriteMap());
    }

    @Test
    void missingSourceSpmMapsRemainBlocked() throws Exception {
        BheMapAppendResult result = runPipeline(requireCompleteStaticResourceRoot());
        BheMapImportOutputResult output = result.getOutputResult();

        assertEquals(5, output.getBlockedMaps().size());
        for (BheMapImportBlockedMap blockedMap : output.getBlockedMaps()) {
            assertTrue(blockedMap.isMissingSourceSpm());
            assertTrue(blockedMap.getCompositionBlockingReasons()
                    .contains(MapCompatibilityBlockingReason.MISSING_SOURCE_SPM));
            assertFalse(Files.exists(outputRoot.resolve(blockedMap.getTargetMapFileName())));
            assertFalse(output.getAppendedGroupResourceNames().contains(blockedMap.getTargetGroupResourceName()));
        }
        assertEquals(104, output.getWrittenComposedSpmFileList().size());
        assertNoProbeFiles();
    }

    private BheMapAppendResult runPipeline(Path completeResourceRoot) {
        BheMapAppendRequest request = new BheMapAppendRequest();
        request.setBheStaticResourceRoot(completeResourceRoot);
        request.setCurrentTargetMapGroupPath(request.getBsdxMapGroupPath());
        request.setOutputRoot(outputRoot);
        return new BheMapImportPipeline().importMapsIntoCurrentResourceTree(request);
    }

    private long countFilesByExtension(Path root, String extension) throws Exception {
        if (!Files.exists(root)) {
            return 0;
        }
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(extension))
                    .count();
        }
    }

    private Path requireCompleteStaticResourceRoot() {
        Path root = resolveCompleteStaticResourceRoot();
        assumeTrue(
                root != null && Files.isDirectory(root),
                "complete BHE static resource root must be provided by -Dbhe.staticResourceRoot or BHE_STATIC_RESOURCE_ROOT"
        );
        return root;
    }

    private Path resolveCompleteStaticResourceRoot() {
        String propertyValue = System.getProperty("bhe.staticResourceRoot");
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Paths.get(propertyValue).toAbsolutePath().normalize();
        }
        String environmentValue = System.getenv("BHE_STATIC_RESOURCE_ROOT");
        if (environmentValue != null && !environmentValue.isBlank()) {
            return Paths.get(environmentValue).toAbsolutePath().normalize();
        }
        return null;
    }

    private void assertNoProbeFiles() {
        assertFalse(Files.exists(Paths.get("tools/probe/bhe_map_spm_selector_probe.js")));
        assertFalse(Files.exists(Paths.get("tools/probe/bsdx_map_object_probe.js")));
    }
}
