package com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportManifest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportStaticValidationProblemSeverity;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportStaticValidationResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.ImportBheMapsIntoCurrentResourceTreeRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.ImportBheMapsIntoCurrentResourceTreeResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapImportStaticValidatorTest {

    @TempDir
    Path tempRoot;

    private final BheMapImportStaticValidator validator = new BheMapImportStaticValidator();

    @Test
    void successfulStaticSubsetValidationPassesWithExpectedLimitations() {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();

        ImportBheMapsIntoCurrentResourceTreeResult importResult = new ImportBheMapsIntoCurrentResourceTreeStep()
                .importMaps(completeRequest(completeResourceRoot, tempRoot.resolve("success")));

        BheMapImportStaticValidationResult validation = validator.validate(importResult);

        assertTrue(validation.isValidForStaticSubset());
        assertTrue(validation.isCanContinueStaticPipeline());
        assertEquals(0, validation.getFatalProblemCount());
        assertTrue(validation.getWarningCount() > 0);
        assertTrue(validation.hasProblem("BLOCKED_MAPS_EXPECTED_LIMITATION"));
        assertTrue(validation.hasProblem("MISSING_SOURCE_SPM_EXPECTED_LIMITATION"));
        assertTrue(validation.hasProblem("PREVIEW_NOT_TRANSACTIONAL"));
        assertFalse(importResult.isPacked());
        assertFalse(importResult.getImportManifest().isDynamicValidation());
    }

    @Test
    void conflictResultFailsStaticSubsetValidation() throws Exception {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();
        Path baselineRoot = tempRoot.resolve("baseline");
        ImportBheMapsIntoCurrentResourceTreeResult baseline = new ImportBheMapsIntoCurrentResourceTreeStep()
                .importMaps(completeRequest(completeResourceRoot, baselineRoot));
        Path baselineResource = baseline.getMapAppendResult()
                .getResourceMaterializationResult()
                .getWrittenResourceFileList()
                .get(0);

        Path conflictRoot = tempRoot.resolve("conflict");
        Files.createDirectories(conflictRoot);
        Path conflictResource = conflictRoot.resolve(baselineResource.getFileName().toString());
        byte[] originalBytes = differentBytes(Files.readAllBytes(baselineResource));
        Files.write(conflictResource, originalBytes);

        ImportBheMapsIntoCurrentResourceTreeResult importResult = new ImportBheMapsIntoCurrentResourceTreeStep()
                .importMaps(completeRequest(completeResourceRoot, conflictRoot));
        BheMapImportStaticValidationResult validation = validator.validate(importResult);

        assertFalse(validation.isValidForStaticSubset());
        assertFalse(validation.isCanContinueStaticPipeline());
        assertTrue(validation.getFatalProblemCount() > 0);
        assertTrue(validation.hasProblem("OUTPUT_CONFLICT"));
        assertArrayEquals(originalBytes, Files.readAllBytes(conflictResource));
    }

    @Test
    void missingManifestFailsStaticSubsetValidation() {
        ImportBheMapsIntoCurrentResourceTreeResult importResult = new ImportBheMapsIntoCurrentResourceTreeResult();

        BheMapImportStaticValidationResult validation = validator.validate(importResult);

        assertFalse(validation.isValidForStaticSubset());
        assertFalse(validation.isCanContinueStaticPipeline());
        assertEquals(1, validation.getFatalProblemCount());
        assertEquals(0, validation.getWarningCount());
        assertTrue(validation.hasProblem("MANIFEST_MISSING"));
    }

    @Test
    void changedCountFailsStaticSubsetValidation() {
        BheMapImportManifest manifest = successfulManifest();
        manifest.setWrittenMapCount(32);

        BheMapImportStaticValidationResult validation = validator.validate(manifest);

        assertFalse(validation.isValidForStaticSubset());
        assertFalse(validation.isCanContinueStaticPipeline());
        assertTrue(validation.hasProblem("APPENDED_MAPGROUP_COUNT_MISMATCH"));
        assertTrue(validation.hasProblem("PLAN_ACCOUNTING_MISMATCH"));
        assertEquals(BheMapImportStaticValidationProblemSeverity.FATAL,
                validation.getValidationProblems().stream()
                        .filter(problem -> "APPENDED_MAPGROUP_COUNT_MISMATCH".equals(problem.getKey()))
                        .findFirst()
                        .orElseThrow()
                        .getSeverity());
    }

    private BheMapImportManifest successfulManifest() {
        BheMapImportManifest manifest = new BheMapImportManifest();
        manifest.setOutputConflictCount(0);
        manifest.setOutputCompleted(true);
        manifest.setResourcesMaterialized(true);
        manifest.setTotalPlanEntries(142);
        manifest.setWrittenMapCount(137);
        manifest.setDirectWrittenMapCount(33);
        manifest.setCompositionWrittenMapCount(104);
        manifest.setComposedSpmWrittenCount(104);
        manifest.setAppendedMapGroupCount(137);
        manifest.setMaterializedResourceCount(97);
        manifest.setBlockedMapCount(5);
        manifest.setBlockedByMultipleSpriteMapListCount(0);
        manifest.setBlockedByUnsupportedScriptGroupCount(0);
        manifest.setBlockedByMissingSourceSpmCount(5);
        manifest.setPack(false);
        manifest.setComposedSpmWritten(true);
        manifest.setDynamicValidation(false);
        manifest.setProbeRestored(false);
        manifest.setPreviewMaterializationTransactional(false);
        manifest.setTransactionalOutputScope(
                BheMapImportManifest.TRANSACTIONAL_OUTPUT_SCOPE_MAP_FILES_AND_MAPGROUP_AND_RESOURCES
        );
        return manifest;
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
