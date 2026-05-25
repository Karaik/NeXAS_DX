package com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportManifest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportStaticValidationProblem;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportStaticValidationProblemSeverity;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportStaticValidationResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.ImportBheMapsIntoCurrentResourceTreeResult;

public class BheMapImportStaticValidator {

    public BheMapImportStaticValidationResult validate(ImportBheMapsIntoCurrentResourceTreeResult result) {
        if (result == null) {
            return validate((BheMapImportManifest) null);
        }
        return validate(result.getImportManifest());
    }

    public BheMapImportStaticValidationResult validate(BheMapAppendResult result) {
        if (result == null) {
            return validate((BheMapImportManifest) null);
        }
        return validate(result.getImportManifest());
    }

    public BheMapImportStaticValidationResult validate(BheMapImportManifest manifest) {
        BheMapImportStaticValidationResult result = new BheMapImportStaticValidationResult();
        if (manifest == null) {
            fatal(result, "MANIFEST_MISSING", "import manifest is required");
            finish(result);
            return result;
        }

        requireZero(result, "OUTPUT_CONFLICT", manifest.getOutputConflictCount());
        requireTrue(result, "OUTPUT_NOT_COMPLETED", manifest.isOutputCompleted());
        requireTrue(result, "RESOURCES_NOT_MATERIALIZED", manifest.isResourcesMaterialized());
        requirePositive(result, "NO_MAPS_WRITTEN", manifest.getWrittenMapCount());
        requireEqual(result, "APPENDED_MAPGROUP_COUNT_MISMATCH",
                manifest.getWrittenMapCount(),
                manifest.getAppendedMapGroupCount());
        requireEqual(result, "PLAN_ACCOUNTING_MISMATCH",
                manifest.getTotalPlanEntries(),
                manifest.getWrittenMapCount() + manifest.getBlockedMapCount());
        requireFalse(result, "PACK_WRITTEN", manifest.isPack());
        requireFalse(result, "DYNAMIC_VALIDATION_ENABLED", manifest.isDynamicValidation());
        requireFalse(result, "PROBE_RESTORED", manifest.isProbeRestored());
        requireFalse(
                result,
                "PREVIEW_MATERIALIZATION_TRANSACTIONAL_CHANGED",
                manifest.isPreviewMaterializationTransactional()
        );
        if (!BheMapImportManifest.TRANSACTIONAL_OUTPUT_SCOPE_MAP_FILES_AND_MAPGROUP_AND_RESOURCES.equals(
                manifest.getTransactionalOutputScope()
        )) {
            fatal(
                    result,
                    "TRANSACTIONAL_OUTPUT_SCOPE_CHANGED",
                    "transactional output scope must cover map files, MapGroup, and resources"
            );
        }

        warnIfPositive(result, "BLOCKED_MAPS_EXPECTED_LIMITATION", manifest.getBlockedMapCount());
        warnIfPositive(result, "MULTIPLE_SPRITE_MAP_LIST_EXPECTED_LIMITATION",
                manifest.getBlockedByMultipleSpriteMapListCount());
        warnIfPositive(result, "UNSUPPORTED_SCRIPT_GROUP_EXPECTED_LIMITATION",
                manifest.getBlockedByUnsupportedScriptGroupCount());
        warnIfPositive(result, "MISSING_SOURCE_SPM_EXPECTED_LIMITATION",
                manifest.getBlockedByMissingSourceSpmCount());
        if (!manifest.isPreviewMaterializationTransactional()) {
            warning(
                    result,
                    "PREVIEW_NOT_TRANSACTIONAL",
                    "preview materialization is not part of the transactional static output scope"
            );
        }

        finish(result);
        return result;
    }

    private void requireZero(BheMapImportStaticValidationResult result, String key, int actual) {
        if (actual != 0) {
            fatal(result, key, key + ": expected=0 actual=" + actual);
        }
    }

    private void requireTrue(BheMapImportStaticValidationResult result, String key, boolean actual) {
        if (!actual) {
            fatal(result, key, key + ": expected=true actual=false");
        }
    }

    private void requireFalse(BheMapImportStaticValidationResult result, String key, boolean actual) {
        if (actual) {
            fatal(result, key, key + ": expected=false actual=true");
        }
    }

    private void requireEqual(BheMapImportStaticValidationResult result, String key, int expected, int actual) {
        if (expected != actual) {
            fatal(result, key, key + ": expected=" + expected + " actual=" + actual);
        }
    }

    private void requirePositive(BheMapImportStaticValidationResult result, String key, int actual) {
        if (actual <= 0) {
            fatal(result, key, key + ": expected>0 actual=" + actual);
        }
    }

    private void warnIfPositive(BheMapImportStaticValidationResult result, String key, int actual) {
        if (actual > 0) {
            warning(result, key, key + ": count=" + actual);
        }
    }

    private void fatal(BheMapImportStaticValidationResult result, String key, String message) {
        problem(result, BheMapImportStaticValidationProblemSeverity.FATAL, key, message);
    }

    private void warning(BheMapImportStaticValidationResult result, String key, String message) {
        problem(result, BheMapImportStaticValidationProblemSeverity.WARNING, key, message);
    }

    private void problem(
            BheMapImportStaticValidationResult result,
            BheMapImportStaticValidationProblemSeverity severity,
            String key,
            String message
    ) {
        BheMapImportStaticValidationProblem problem = new BheMapImportStaticValidationProblem();
        problem.setSeverity(severity);
        problem.setKey(key);
        problem.setMessage(message);
        result.getValidationProblems().add(problem);
    }

    private void finish(BheMapImportStaticValidationResult result) {
        int fatalCount = 0;
        int warningCount = 0;
        for (BheMapImportStaticValidationProblem problem : result.getValidationProblems()) {
            if (problem.getSeverity() == BheMapImportStaticValidationProblemSeverity.FATAL) {
                fatalCount++;
            } else if (problem.getSeverity() == BheMapImportStaticValidationProblemSeverity.WARNING) {
                warningCount++;
            }
        }
        result.setFatalProblemCount(fatalCount);
        result.setWarningCount(warningCount);
        result.setValidForStaticSubset(fatalCount == 0);
        result.setCanContinueStaticPipeline(result.isValidForStaticSubset());
    }
}
