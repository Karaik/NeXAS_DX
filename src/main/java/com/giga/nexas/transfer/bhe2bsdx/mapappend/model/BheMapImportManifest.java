package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Data
public class BheMapImportManifest {

    public static final String TRANSACTIONAL_OUTPUT_SCOPE_MAP_FILES_AND_MAPGROUP_AND_RESOURCES =
            "MAP_FILES_AND_MAPGROUP_AND_RESOURCES";

    private int totalPlanEntries;
    private int writtenMapCount;
    private int directWrittenMapCount;
    private int compositionWrittenMapCount;
    private int composedSpmWrittenCount;
    private int appendedMapGroupCount;
    private int materializedResourceCount;
    private int foregroundResourceCount;
    private int resourceSlotResourceCount;
    private int spriteMapResourceCount;
    private int internalResourceReferenceCount;
    private int duplicateTargetReferenceCount;
    private int blockedMapCount;
    private int blockedByMultipleSpriteMapListCount;
    private int blockedByUnsupportedScriptGroupCount;
    private int blockedByMissingSourceSpmCount;
    private int blockedByMissingFinalReferenceCount;
    private int outputConflictCount;
    private int existingIdenticalFileCount;
    private boolean outputCompleted;
    private boolean resourcesMaterialized;
    private boolean pack;
    private boolean composedSpmWritten;
    private boolean dynamicValidation;
    private boolean probeRestored;
    private boolean previewMaterialized;
    private boolean previewMaterializationEnabled;
    private String transactionalOutputScope = TRANSACTIONAL_OUTPUT_SCOPE_MAP_FILES_AND_MAPGROUP_AND_RESOURCES;
    private boolean previewMaterializationTransactional;
    private Path outputRoot;
    private Path outputMapGroupPath;
    private List<BheMapImportManifestEntry> writtenEntries = new ArrayList<>();
    private List<BheMapImportBlockedManifestEntry> blockedEntries = new ArrayList<>();
}
