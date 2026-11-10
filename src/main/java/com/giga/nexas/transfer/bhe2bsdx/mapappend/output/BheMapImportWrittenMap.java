package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import lombok.Data;

import java.nio.file.Path;

@Data
public class BheMapImportWrittenMap {

    private String sourceMapFileName;
    private String targetMapFileName;
    private String targetGroupResourceName;
    private Path mapPath;
    private boolean composition;
    private String composedSpmFileName;
    private Path composedSpmPath;
}
