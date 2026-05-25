package com.giga.nexas.transfer.bhe2bsdx.mapappend.composition;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MapCompatibilityAnalysis {

    private String sourceMapFileName;
    private int spriteMapListSize;
    private int inspectedScriptEntryCount;
    private int rewrittenScriptEntryCount;
    private List<MapCompatibilityIssue> blockingIssues = new ArrayList<>();

    public boolean isComposable() {
        return blockingIssues.isEmpty();
    }

    public void addIssue(MapCompatibilityIssue issue) {
        if (issue != null) {
            blockingIssues.add(issue);
        }
    }
}
