package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
public class BheMapResourceMaterializationResult {

    private int materializedResourceCount;
    private int materializedForegroundCount;
    private int materializedResourceSlotCount;
    private int materializedSpriteMapCount;
    private int servicedResourceReferenceCount;
    private int servicedForegroundReferenceCount;
    private int servicedResourceSlotReferenceCount;
    private int servicedSpriteMapReferenceCount;
    private int skippedBlockedResourceCount;
    private int missingSourceResourceCount;
    private int duplicateTargetResourceCount;
    private int writtenFileCount;
    private int existingIdenticalFileCount;
    private int outputConflictCount;
    private List<com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapOutputConflict> conflicts = new ArrayList<>();
    private List<com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapPendingOutputFile> pendingOutputFiles = new ArrayList<>();
    private List<Path> writtenResourceFileList = new ArrayList<>();
    private boolean spmWritten;
    private boolean composedSpmWritten;
    private boolean previewWritten;
    private boolean packed;
}
