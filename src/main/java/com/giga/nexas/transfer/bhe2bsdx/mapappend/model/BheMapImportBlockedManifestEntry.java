package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssue;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssueType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityBlockingReason;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityIssue;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BheMapImportBlockedManifestEntry {

    private String sourceMapFileName;
    private String targetMapFileName;
    private String targetGroupResourceName;
    private List<BheMapDataConversionIssueType> issueTypes = new ArrayList<>();
    private List<BheMapDataConversionIssue> blockingIssues = new ArrayList<>();
    private List<MapCompatibilityBlockingReason> compositionBlockingReasons = new ArrayList<>();
    private List<MapCompatibilityIssue> compositionBlockingIssues = new ArrayList<>();
    private boolean missingSourceSpm;
    private boolean notWritten = true;
}
