package com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata;

import lombok.Data;

import java.util.Map;
import java.util.TreeMap;

/**
 * MapData conversion 的可断言审计信息。
 */
@Data
public class BheMapDataConversionAudit {

    private int inspectedScriptEntryCount;
    private int rebucketedScriptEntryCount;
    private Map<Integer, Integer> targetScriptGroupEntryCounts = new TreeMap<>();
    private int rewrittenResourceReferenceCount;

    public void recordRebucket(int targetGroupIndex) {
        rebucketedScriptEntryCount++;
        targetScriptGroupEntryCounts.merge(targetGroupIndex, 1, Integer::sum);
    }

    public void recordInspectedScriptEntry() {
        inspectedScriptEntryCount++;
    }

    public void recordResourceRewrite() {
        rewrittenResourceReferenceCount++;
    }
}
