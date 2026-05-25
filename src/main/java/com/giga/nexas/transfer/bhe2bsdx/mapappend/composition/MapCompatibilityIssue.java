package com.giga.nexas.transfer.bhe2bsdx.mapappend.composition;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import lombok.Data;

@Data
public class MapCompatibilityIssue {

    private MapCompatibilityBlockingReason reason;
    private String sourceMapFileName;
    private BheMapReferenceType referenceType;
    private String referenceText;
    private Integer spriteMapIndex;
    private String spriteMapName;
    private Integer outerGroupIndex;
    private Integer entryIndex;
    private Integer groupIndex;
    private Integer typeId;
    private Integer x;
    private Integer y;
    private String message;

    public static MapCompatibilityIssue blocking(
            MapCompatibilityBlockingReason reason,
            String sourceMapFileName,
            String message
    ) {
        MapCompatibilityIssue issue = new MapCompatibilityIssue();
        issue.setReason(reason);
        issue.setSourceMapFileName(sourceMapFileName);
        issue.setMessage(message);
        return issue;
    }
}
