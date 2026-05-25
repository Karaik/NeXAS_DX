package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.nio.file.Path;

@Data
public class ImportBheMapsIntoCurrentResourceTreeRequest {

    private Path bheMapGroupPath;
    private Path bheMapDir;
    private Path bheStaticResourceRoot;
    private Path currentTargetMapGroupPath;
    private Path outputRoot;
    private String charset;
    private boolean previewMaterializationEnabled;
}
