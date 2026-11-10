package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssueType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityBlockingReason;
import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Data
public class BheMapImportOutputResult {

    private int totalPlanEntries;
    private int convertedMapCount;
    private int writtenMapCount;
    private int directWrittenMapCount;
    private int compositionWrittenMapCount;
    private int composedSpmWrittenCount;
    private int appendedMapGroupCount;
    private int baselineMapGroupCount;
    private int outputMapGroupCount;
    private int blockedByMultipleSpriteMapListCount;
    private int blockedByUnsupportedScriptGroupCount;
    private int blockedByMissingSourceSpmCount;
    private int blockedByMissingFinalReferenceCount;
    private int writtenFileCount;
    private int existingIdenticalFileCount;
    private int outputConflictCount;
    private boolean outputCompleted;
    private Map<BheMapDataConversionIssueType, Integer> issueTypeCounts =
            new EnumMap<>(BheMapDataConversionIssueType.class);
    private Map<MapCompatibilityBlockingReason, Integer> compositionBlockingReasonCounts =
            new EnumMap<>(MapCompatibilityBlockingReason.class);
    private List<BheMapOutputConflict> conflicts = new ArrayList<>();
    private List<BheMapPendingOutputFile> pendingOutputFiles = new ArrayList<>();
    private List<Path> writtenMapFileList = new ArrayList<>();
    private List<Path> writtenComposedSpmFileList = new ArrayList<>();
    private List<BheMapImportWrittenMap> writtenMaps = new ArrayList<>();
    private List<BheMapImportBlockedMap> blockedMaps = new ArrayList<>();
    private List<String> appendedGroupResourceNames = new ArrayList<>();
    private Path outputRoot;
    private Path outputMapGroupPath;
    private boolean spmWritten;
    private boolean composedSpmWritten;
    private boolean previewWritten;
    private boolean packed;

    public int issueTypeCount(BheMapDataConversionIssueType type) {
        return issueTypeCounts.getOrDefault(type, 0);
    }

    public void recordIssueType(BheMapDataConversionIssueType type) {
        if (type != null) {
            issueTypeCounts.merge(type, 1, Integer::sum);
        }
    }

    public int compositionBlockingReasonCount(MapCompatibilityBlockingReason reason) {
        return compositionBlockingReasonCounts.getOrDefault(reason, 0);
    }

    public void recordCompositionBlockingReason(MapCompatibilityBlockingReason reason) {
        if (reason != null) {
            compositionBlockingReasonCounts.merge(reason, 1, Integer::sum);
        }
    }
}
