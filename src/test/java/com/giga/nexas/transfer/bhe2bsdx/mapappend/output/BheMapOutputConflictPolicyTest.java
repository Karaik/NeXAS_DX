package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline.BheMapImportPipeline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceMaterializationResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapOutputConflictPolicyTest {

    @TempDir
    Path tempRoot;

    @Test
    void emptyOutputRootWritesWithoutConflicts() throws Exception {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();
        Path outputRoot = tempRoot.resolve("empty-output");

        BheMapAppendResult result = runPipeline(completeResourceRoot, outputRoot);
        BheMapImportOutputResult output = result.getOutputResult();
        BheMapResourceMaterializationResult resources = result.getResourceMaterializationResult();

        assertTrue(result.isOutputWritten());
        assertTrue(result.isResourcesMaterialized());
        assertEquals(137, output.getWrittenMapCount());
        assertEquals(137, output.getAppendedMapGroupCount());
        assertEquals(242, output.getWrittenFileCount());
        assertEquals(0, output.getExistingIdenticalFileCount());
        assertEquals(0, output.getOutputConflictCount());
        assertTrue(output.isOutputCompleted());
        assertEquals(97, resources.getMaterializedResourceCount());
        assertEquals(97, resources.getWrittenFileCount());
        assertEquals(0, resources.getExistingIdenticalFileCount());
        assertEquals(0, resources.getOutputConflictCount());
        assertNoProbeFiles();
    }

    @Test
    void identicalExistingResourceDoesNotFail() throws Exception {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();
        Path baselineRoot = tempRoot.resolve("baseline-identical-resource");
        BheMapAppendResult baseline = runPipeline(completeResourceRoot, baselineRoot);
        Path baselineResource = baseline.getResourceMaterializationResult().getWrittenResourceFileList().get(0);
        byte[] resourceBytes = Files.readAllBytes(baselineResource);

        Path outputRoot = tempRoot.resolve("target-identical-resource");
        Files.createDirectories(outputRoot);
        Path existingResource = outputRoot.resolve(baselineResource.getFileName().toString());
        Files.write(existingResource, resourceBytes);

        BheMapAppendResult result = runPipeline(completeResourceRoot, outputRoot);
        BheMapImportOutputResult output = result.getOutputResult();
        BheMapResourceMaterializationResult resources = result.getResourceMaterializationResult();

        assertTrue(result.isOutputWritten());
        assertTrue(result.isResourcesMaterialized());
        assertEquals(0, output.getOutputConflictCount());
        assertEquals(1, resources.getExistingIdenticalFileCount());
        assertEquals(0, resources.getOutputConflictCount());
        assertArrayEquals(resourceBytes, Files.readAllBytes(existingResource));
        assertNoProbeFiles();
    }

    @Test
    void differentExistingResourceBlocksWithoutOverwrite() throws Exception {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();
        Path baselineRoot = tempRoot.resolve("baseline-resource-conflict");
        BheMapAppendResult baseline = runPipeline(completeResourceRoot, baselineRoot);
        Path baselineResource = baseline.getResourceMaterializationResult().getWrittenResourceFileList().get(0);

        Path outputRoot = tempRoot.resolve("target-resource-conflict");
        Files.createDirectories(outputRoot);
        Path existingResource = outputRoot.resolve(baselineResource.getFileName().toString());
        byte[] originalBytes = differentBytes(Files.readAllBytes(baselineResource));
        Files.write(existingResource, originalBytes);

        BheMapAppendResult result = runPipeline(completeResourceRoot, outputRoot);

        assertFalse(result.isResourcesMaterialized());
        assertNotNull(result.getOutputResult());
        assertEquals(1, result.getOutputResult().getOutputConflictCount());
        assertEquals(BheMapOutputConflictType.RESOURCE_FILE_CONFLICT, result.getOutputResult().getConflicts().get(0).getType());
        assertArrayEquals(originalBytes, Files.readAllBytes(existingResource));
        assertEquals(0, countFilesByExtension(outputRoot, ".map"));
        assertFalse(Files.exists(outputRoot.resolve("MapGroup.grp")));
        assertEquals(1, countNonPreviewTransactionFiles(outputRoot));
        assertNoProbeFiles();
    }

    @Test
    void differentExistingMapFileBlocksWithoutOverwrite() throws Exception {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();
        Path baselineRoot = tempRoot.resolve("baseline-map-conflict");
        BheMapAppendResult baseline = runPipeline(completeResourceRoot, baselineRoot);
        BheMapImportWrittenMap writtenMap = baseline.getOutputResult().getWrittenMaps().get(0);

        Path outputRoot = tempRoot.resolve("target-map-conflict");
        Files.createDirectories(outputRoot);
        Path existingMap = outputRoot.resolve(writtenMap.getTargetMapFileName());
        byte[] originalBytes = "different existing map content".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        Files.write(existingMap, originalBytes);

        BheMapAppendResult result = runPipeline(completeResourceRoot, outputRoot);

        assertFalse(result.isOutputWritten());
        assertNull(result.getResourceMaterializationResult());
        assertNotNull(result.getOutputResult());
        assertEquals(1, result.getOutputResult().getOutputConflictCount());
        assertEquals(BheMapOutputConflictType.MAP_FILE_CONFLICT, result.getOutputResult().getConflicts().get(0).getType());
        assertArrayEquals(originalBytes, Files.readAllBytes(existingMap));
        assertEquals(1, countFilesByExtension(outputRoot, ".map"));
        assertFalse(Files.exists(outputRoot.resolve("MapGroup.grp")));
        assertEquals(1, countNonPreviewTransactionFiles(outputRoot));
        assertNoProbeFiles();
    }

    @Test
    void differentExistingMapGroupBlocksWithoutOverwrite() throws Exception {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();
        Path baselineRoot = tempRoot.resolve("baseline-mapgroup-conflict");
        BheMapAppendResult baseline = runPipeline(completeResourceRoot, baselineRoot);

        Path outputRoot = tempRoot.resolve("target-mapgroup-conflict");
        Files.createDirectories(outputRoot);
        Path existingMapGroup = outputRoot.resolve("MapGroup.grp");
        byte[] originalBytes = "different existing mapgroup content".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        Files.write(existingMapGroup, originalBytes);

        BheMapAppendResult result = runPipeline(completeResourceRoot, outputRoot);

        assertFalse(result.isOutputWritten());
        assertNull(result.getResourceMaterializationResult());
        assertNotNull(result.getOutputResult());
        assertEquals(1, result.getOutputResult().getOutputConflictCount());
        assertEquals(BheMapOutputConflictType.MAPGROUP_FILE_CONFLICT, result.getOutputResult().getConflicts().get(0).getType());
        assertArrayEquals(originalBytes, Files.readAllBytes(existingMapGroup));
        assertEquals(0, countFilesByExtension(outputRoot, ".map"));
        assertEquals(1, countNonPreviewTransactionFiles(outputRoot));
        assertNoProbeFiles();
    }

    @Test
    void samePathMapGroupWritebackAllowsPlannedReplacement() throws Exception {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();
        Path outputRoot = tempRoot.resolve("same-path-mapgroup-writeback");
        Files.createDirectories(outputRoot);
        Path currentMapGroup = outputRoot.resolve("MapGroup.grp");
        Files.copy(Paths.get("src/main/resources/game/bsdx/grp/MapGroup.grp"), currentMapGroup);

        BheMapAppendResult result = runPipeline(completeResourceRoot, outputRoot, currentMapGroup);
        BheMapImportOutputResult output = result.getOutputResult();

        assertTrue(result.isOutputWritten());
        assertTrue(result.isResourcesMaterialized());
        assertEquals(0, output.getOutputConflictCount());
        assertEquals(137, output.getWrittenMapCount());
        assertEquals(137, output.getAppendedMapGroupCount());
        assertTrue(Files.size(currentMapGroup) > 0);
        assertNoProbeFiles();
    }

    @Test
    void staleSamePathMapGroupWritebackBlocksWithoutPartialWrites() throws Exception {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();
        Path outputRoot = tempRoot.resolve("stale-same-path-mapgroup");
        Files.createDirectories(outputRoot);
        Path currentMapGroup = outputRoot.resolve("MapGroup.grp");
        Files.copy(Paths.get("src/main/resources/game/bsdx/grp/MapGroup.grp"), currentMapGroup);

        BheMapAppendRequest request = request(completeResourceRoot, outputRoot, currentMapGroup);
        BheMapImportPipeline pipeline = new BheMapImportPipeline();
        BheMapAppendResult planResult = pipeline.buildImportPlan(request);
        BheMapImportOutputWriter outputWriter = new BheMapImportOutputWriter();
        BheMapImportOutputResult output = outputWriter.prepare(
                planResult.getAppendPlan(),
                currentMapGroup,
                outputRoot,
                request.getCharset()
        );

        byte[] staleBytes = differentBytes(Files.readAllBytes(currentMapGroup));
        Files.write(currentMapGroup, staleBytes);
        BheMapOutputPreflightResult preflight = new BheMapSafeOutputWriter().preflight(output.getPendingOutputFiles());

        assertEquals(1, preflight.getConflictCount());
        assertEquals(BheMapOutputConflictType.MAPGROUP_FILE_CONFLICT, preflight.getConflicts().get(0).getType());
        assertArrayEquals(staleBytes, Files.readAllBytes(currentMapGroup));
        assertEquals(0, countFilesByExtension(outputRoot, ".map"));
        assertEquals(1, countNonPreviewTransactionFiles(outputRoot));
        assertNoProbeFiles();
    }

    @Test
    void differentExistingComposedSpmBlocksWithoutOverwrite() throws Exception {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();
        Path baselineRoot = tempRoot.resolve("baseline-composed-spm-conflict");
        BheMapAppendResult baseline = runPipeline(completeResourceRoot, baselineRoot);
        BheMapImportWrittenMap compositionMap = baseline.getOutputResult().getWrittenMaps().stream()
                .filter(BheMapImportWrittenMap::isComposition)
                .findFirst()
                .orElseThrow();

        Path outputRoot = tempRoot.resolve("target-composed-spm-conflict");
        Files.createDirectories(outputRoot);
        Path existingSpm = outputRoot.resolve(compositionMap.getComposedSpmFileName());
        byte[] originalBytes = "different existing composed spm content".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        Files.write(existingSpm, originalBytes);

        BheMapAppendResult result = runPipeline(completeResourceRoot, outputRoot);

        assertFalse(result.isOutputWritten());
        assertNull(result.getResourceMaterializationResult());
        assertNotNull(result.getOutputResult());
        assertEquals(1, result.getOutputResult().getOutputConflictCount());
        assertEquals(BheMapOutputConflictType.COMPOSED_SPM_FILE_CONFLICT, result.getOutputResult().getConflicts().get(0).getType());
        assertArrayEquals(originalBytes, Files.readAllBytes(existingSpm));
        assertEquals(0, countFilesByExtension(outputRoot, ".map"));
        assertFalse(Files.exists(outputRoot.resolve("MapGroup.grp")));
        assertEquals(1, countNonPreviewTransactionFiles(outputRoot));
        assertNoProbeFiles();
    }

    private BheMapAppendResult runPipeline(Path completeResourceRoot, Path outputRoot) {
        return runPipeline(completeResourceRoot, outputRoot, null);
    }

    private BheMapAppendResult runPipeline(Path completeResourceRoot, Path outputRoot, Path currentTargetMapGroupPath) {
        return new BheMapImportPipeline().importMapsIntoCurrentResourceTree(
                request(completeResourceRoot, outputRoot, currentTargetMapGroupPath)
        );
    }

    private BheMapAppendRequest request(Path completeResourceRoot, Path outputRoot, Path currentTargetMapGroupPath) {
        BheMapAppendRequest request = new BheMapAppendRequest();
        request.setBheStaticResourceRoot(completeResourceRoot);
        request.setCurrentTargetMapGroupPath(
                currentTargetMapGroupPath == null ? request.getBsdxMapGroupPath() : currentTargetMapGroupPath
        );
        request.setOutputRoot(outputRoot);
        return request;
    }

    private byte[] differentBytes(byte[] baselineBytes) {
        byte[] bytes = Arrays.copyOf(baselineBytes, Math.max(1, baselineBytes.length));
        bytes[0] = (byte) (bytes[0] + 1);
        return bytes;
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

    private long countNonPreviewTransactionFiles(Path root) throws Exception {
        if (!Files.exists(root)) {
            return 0;
        }
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".bmp"))
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
        if (!isBlank(propertyValue)) {
            return Paths.get(propertyValue).toAbsolutePath().normalize();
        }
        String environmentValue = System.getenv("BHE_STATIC_RESOURCE_ROOT");
        if (!isBlank(environmentValue)) {
            return Paths.get(environmentValue).toAbsolutePath().normalize();
        }
        return null;
    }

    private void assertNoProbeFiles() {
        assertFalse(Files.exists(Paths.get("tools/probe/bhe_map_spm_selector_probe.js")));
        assertFalse(Files.exists(Paths.get("tools/probe/bsdx_map_object_probe.js")));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
