package com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapImportStaticValidationResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.ImportBheMapsIntoCurrentResourceTreeRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.ImportBheMapsIntoCurrentResourceTreeResult;

import java.nio.file.Path;

public class ImportBheMapsIntoCurrentResourceTreeStep {

    private final BheMapImportPipeline pipeline;

    public ImportBheMapsIntoCurrentResourceTreeStep() {
        this(new BheMapImportPipeline());
    }

    public ImportBheMapsIntoCurrentResourceTreeStep(BheMapImportPipeline pipeline) {
        this.pipeline = pipeline == null ? new BheMapImportPipeline() : pipeline;
    }

    public ImportBheMapsIntoCurrentResourceTreeResult importMaps(
            ImportBheMapsIntoCurrentResourceTreeRequest request
    ) {
        validate(request);

        BheMapAppendRequest pipelineRequest = new BheMapAppendRequest();
        pipelineRequest.setBheMapGroupPath(request.getBheMapGroupPath());
        pipelineRequest.setBheMapDir(request.getBheMapDir());
        pipelineRequest.setBheStaticResourceRoot(request.getBheStaticResourceRoot());
        pipelineRequest.setCurrentTargetMapGroupPath(request.getCurrentTargetMapGroupPath());
        pipelineRequest.setOutputRoot(request.getOutputRoot());
        pipelineRequest.setCharset(request.getCharset());
        pipelineRequest.setPreviewMaterializationEnabled(request.isPreviewMaterializationEnabled());

        BheMapAppendResult result = pipeline.importMapsIntoCurrentResourceTree(pipelineRequest);
        ImportBheMapsIntoCurrentResourceTreeResult stepResult =
                ImportBheMapsIntoCurrentResourceTreeResult.from(result);
        BheMapImportStaticValidationResult validation = new BheMapImportStaticValidator().validate(stepResult);
        stepResult.setStaticValidationResult(validation);
        stepResult.setValidForStaticSubset(validation.isValidForStaticSubset());
        stepResult.setCanContinueStaticPipeline(validation.isCanContinueStaticPipeline());
        return stepResult;
    }

    private void validate(ImportBheMapsIntoCurrentResourceTreeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        requirePath(request.getBheMapGroupPath(), "bheMapGroupPath");
        requirePath(request.getBheMapDir(), "bheMapDir");
        requirePath(request.getBheStaticResourceRoot(), "bheStaticResourceRoot");
        requirePath(request.getCurrentTargetMapGroupPath(), "currentTargetMapGroupPath");
        requirePath(request.getOutputRoot(), "outputRoot");
        if (request.getCharset() == null || request.getCharset().isBlank()) {
            throw new IllegalArgumentException("charset must be provided");
        }
    }

    private void requirePath(Path path, String fieldName) {
        if (path == null) {
            throw new IllegalArgumentException(fieldName + " must be provided");
        }
    }
}
