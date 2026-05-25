package com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.ImportBheMapsIntoCurrentResourceTreeRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.ImportBheMapsIntoCurrentResourceTreeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.util.Arrays;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ImportBheMapsIntoCurrentResourceTreeStepTest {

    @TempDir
    Path outputRoot;

    @Test
    void explicitStaticImportRequestWritesConvertibleMapsWithoutPreviewOrPack() throws Exception {
        Path completeResourceRoot = resolveCompleteStaticResourceRoot();
        assumeTrue(
                completeResourceRoot != null && Files.isDirectory(completeResourceRoot),
                "complete BHE static resource root must be provided by -Dbhe.staticResourceRoot or BHE_STATIC_RESOURCE_ROOT"
        );

        ImportBheMapsIntoCurrentResourceTreeRequest request = completeRequest(completeResourceRoot);
        ImportBheMapsIntoCurrentResourceTreeResult result =
                new ImportBheMapsIntoCurrentResourceTreeStep().importMaps(request);

        assertNotNull(result.getMapAppendResult());
        assertNotNull(result.getImportManifest());
        assertEquals(outputRoot.toAbsolutePath().normalize(), result.getOutputRoot());
        assertEquals(outputRoot.resolve("MapGroup.grp").toAbsolutePath().normalize(), result.getOutputMapGroupPath());
        assertEquals(137, result.getWrittenMapCount());
        assertEquals(33, result.getDirectWrittenMapCount());
        assertEquals(104, result.getCompositionWrittenMapCount());
        assertEquals(104, result.getComposedSpmWrittenCount());
        assertEquals(137, result.getAppendedMapGroupCount());
        assertEquals(97, result.getMaterializedResourceCount());
        assertEquals(5, result.getBlockedMapCount());
        assertEquals(0, result.getOutputConflictCount());
        assertFalse(result.isPacked());
        assertNotNull(result.getStaticValidationResult());
        assertTrue(result.isValidForStaticSubset());
        assertTrue(result.isCanContinueStaticPipeline());
        assertEquals(0, result.getStaticValidationResult().getFatalProblemCount());
        assertTrue(result.getStaticValidationResult().getWarningCount() > 0);
        assertTrue(result.getStaticValidationResult().hasProblem("BLOCKED_MAPS_EXPECTED_LIMITATION"));
        assertTrue(result.getStaticValidationResult().hasProblem("MISSING_SOURCE_SPM_EXPECTED_LIMITATION"));
        assertFalse(result.getMapAppendResult().isPreviewMaterialized());
        assertFalse(result.getMapAppendResult().isPreviewMaterializationEnabled());
        assertFalse(result.getImportManifest().isPreviewMaterialized());
        assertFalse(result.getImportManifest().isPreviewMaterializationEnabled());
        assertFalse(result.getImportManifest().isPack());
        assertEquals(137, countFilesByExtension(outputRoot, ".map"));
        assertEquals(0, countFilesByExtension(outputRoot, ".bmp"));
        assertEquals(0, countFilesByExtension(outputRoot, ".pac"));
        assertFalse(Files.exists(Paths.get("tools/probe/bhe_map_spm_selector_probe.js")));
        assertFalse(Files.exists(Paths.get("tools/probe/bsdx_map_object_probe.js")));
    }

    @Test
    void explicitStaticImportRequestConflictResultCarriesFatalValidation() throws Exception {
        Path completeResourceRoot = resolveCompleteStaticResourceRoot();
        assumeTrue(
                completeResourceRoot != null && Files.isDirectory(completeResourceRoot),
                "complete BHE static resource root must be provided by -Dbhe.staticResourceRoot or BHE_STATIC_RESOURCE_ROOT"
        );

        ImportBheMapsIntoCurrentResourceTreeResult baseline =
                new ImportBheMapsIntoCurrentResourceTreeStep().importMaps(completeRequest(
                        completeResourceRoot,
                        outputRoot.resolve("baseline")
                ));
        Path baselineResource = baseline.getMapAppendResult()
                .getResourceMaterializationResult()
                .getWrittenResourceFileList()
                .get(0);

        Path conflictRoot = outputRoot.resolve("conflict");
        Files.createDirectories(conflictRoot);
        Path conflictResource = conflictRoot.resolve(baselineResource.getFileName().toString());
        byte[] originalBytes = differentBytes(Files.readAllBytes(baselineResource));
        Files.write(conflictResource, originalBytes);

        ImportBheMapsIntoCurrentResourceTreeResult result =
                new ImportBheMapsIntoCurrentResourceTreeStep().importMaps(completeRequest(
                        completeResourceRoot,
                        conflictRoot
                ));

        assertNotNull(result.getStaticValidationResult());
        assertFalse(result.isValidForStaticSubset());
        assertFalse(result.isCanContinueStaticPipeline());
        assertTrue(result.getStaticValidationResult().hasProblem("OUTPUT_CONFLICT"));
        assertArrayEquals(originalBytes, Files.readAllBytes(conflictResource));
    }

    @Test
    void explicitStaticImportRequestFailsFastForMissingRequiredPaths() {
        Path completeResourceRoot = Paths.get("complete-resource-root");

        ImportBheMapsIntoCurrentResourceTreeRequest missingCurrentTarget = completeRequest(completeResourceRoot);
        missingCurrentTarget.setCurrentTargetMapGroupPath(null);
        IllegalArgumentException currentTargetError = assertThrows(
                IllegalArgumentException.class,
                () -> new ImportBheMapsIntoCurrentResourceTreeStep().importMaps(missingCurrentTarget)
        );
        assertEquals("currentTargetMapGroupPath must be provided", currentTargetError.getMessage());

        ImportBheMapsIntoCurrentResourceTreeRequest missingOutputRoot = completeRequest(completeResourceRoot);
        missingOutputRoot.setOutputRoot(null);
        IllegalArgumentException outputRootError = assertThrows(
                IllegalArgumentException.class,
                () -> new ImportBheMapsIntoCurrentResourceTreeStep().importMaps(missingOutputRoot)
        );
        assertEquals("outputRoot must be provided", outputRootError.getMessage());

        ImportBheMapsIntoCurrentResourceTreeRequest missingStaticRoot = completeRequest(completeResourceRoot);
        missingStaticRoot.setBheStaticResourceRoot(null);
        IllegalArgumentException staticRootError = assertThrows(
                IllegalArgumentException.class,
                () -> new ImportBheMapsIntoCurrentResourceTreeStep().importMaps(missingStaticRoot)
        );
        assertEquals("bheStaticResourceRoot must be provided", staticRootError.getMessage());
    }

    private ImportBheMapsIntoCurrentResourceTreeRequest completeRequest(Path completeResourceRoot) {
        return completeRequest(completeResourceRoot, outputRoot);
    }

    private ImportBheMapsIntoCurrentResourceTreeRequest completeRequest(Path completeResourceRoot, Path outputRoot) {
        ImportBheMapsIntoCurrentResourceTreeRequest request = new ImportBheMapsIntoCurrentResourceTreeRequest();
        request.setBheMapGroupPath(Paths.get("src/main/resources/game/bhe/grp/mapgroup.grp"));
        request.setBheMapDir(Paths.get("src/main/resources/game/bhe/map"));
        request.setBheStaticResourceRoot(completeResourceRoot);
        request.setCurrentTargetMapGroupPath(Paths.get("src/main/resources/game/bsdx/grp/MapGroup.grp"));
        request.setOutputRoot(outputRoot);
        request.setCharset("windows-31j");
        return request;
    }

    private byte[] differentBytes(byte[] baselineBytes) {
        byte[] bytes = Arrays.copyOf(baselineBytes, Math.max(1, baselineBytes.length));
        bytes[0] = (byte) (bytes[0] + 1);
        return bytes;
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
