package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.nio.file.Path;

@Data
public class ImportBheMapsIntoCurrentResourceTreeResult {

    private BheMapAppendResult mapAppendResult;
    private BheMapImportManifest importManifest;
    private Path outputRoot;
    private Path outputMapGroupPath;
    private int writtenMapCount;
    private int directWrittenMapCount;
    private int compositionWrittenMapCount;
    private int composedSpmWrittenCount;
    private int appendedMapGroupCount;
    private int materializedResourceCount;
    private int blockedMapCount;
    private int outputConflictCount;
    private boolean packed;
    private BheMapImportStaticValidationResult staticValidationResult;
    private boolean validForStaticSubset;
    private boolean canContinueStaticPipeline;

    public static ImportBheMapsIntoCurrentResourceTreeResult from(BheMapAppendResult mapAppendResult) {
        ImportBheMapsIntoCurrentResourceTreeResult result = new ImportBheMapsIntoCurrentResourceTreeResult();
        result.setMapAppendResult(mapAppendResult);
        if (mapAppendResult == null) {
            return result;
        }
        result.setImportManifest(mapAppendResult.getImportManifest());
        result.setOutputRoot(mapAppendResult.getOutputRoot());
        if (mapAppendResult.getOutputResult() != null) {
            result.setOutputMapGroupPath(mapAppendResult.getOutputResult().getOutputMapGroupPath());
            result.setWrittenMapCount(mapAppendResult.getOutputResult().getWrittenMapCount());
            result.setDirectWrittenMapCount(mapAppendResult.getOutputResult().getDirectWrittenMapCount());
            result.setCompositionWrittenMapCount(mapAppendResult.getOutputResult().getCompositionWrittenMapCount());
            result.setComposedSpmWrittenCount(mapAppendResult.getOutputResult().getComposedSpmWrittenCount());
            result.setAppendedMapGroupCount(mapAppendResult.getOutputResult().getAppendedMapGroupCount());
            result.setBlockedMapCount(mapAppendResult.getOutputResult().getBlockedMaps().size());
            result.setOutputConflictCount(mapAppendResult.getOutputResult().getOutputConflictCount());
            result.setPacked(mapAppendResult.getOutputResult().isPacked());
        }
        if (mapAppendResult.getResourceMaterializationResult() != null) {
            result.setMaterializedResourceCount(
                    mapAppendResult.getResourceMaterializationResult().getMaterializedResourceCount()
            );
            result.setOutputConflictCount(
                    result.getOutputConflictCount()
                            + mapAppendResult.getResourceMaterializationResult().getOutputConflictCount()
            );
            result.setPacked(result.isPacked() || mapAppendResult.getResourceMaterializationResult().isPacked());
        }
        return result;
    }
}
