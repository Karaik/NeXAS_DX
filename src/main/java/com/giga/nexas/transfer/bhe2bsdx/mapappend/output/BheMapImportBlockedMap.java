package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssue;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssueType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityBlockingReason;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityIssue;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class BheMapImportBlockedMap {

    private String sourceMapFileName;
    private String targetMapFileName;
    private String targetGroupResourceName;
    private boolean missingSourceSpm;
    private List<BheMapDataConversionIssueType> issueTypes = new ArrayList<>();
    private List<BheMapDataConversionIssue> blockingIssues = new ArrayList<>();
    private List<MapCompatibilityBlockingReason> compositionBlockingReasons = new ArrayList<>();
    private List<MapCompatibilityIssue> compositionBlockingIssues = new ArrayList<>();
}
