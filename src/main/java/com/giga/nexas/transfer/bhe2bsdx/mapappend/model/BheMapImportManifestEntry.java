package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
public class BheMapImportManifestEntry {

    private String sourceMapFileName;
    private String targetMapFileName;
    private String targetGroupResourceName;
    private Path outputMapPath;
    private List<String> writtenResourceTargetFileNames = new ArrayList<>();
    private int foregroundReferenceCount;
    private int resourceSlotReferenceCount;
    private int spriteMapReferenceCount;
}
