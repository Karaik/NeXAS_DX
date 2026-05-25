package com.giga.nexas.transfer.bhe2bsdx.mapappend.scriptgroup;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssue;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssueType;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE script group 到 BSDX script group 的兼容策略。
 *
 * <p>0..8 进入对应 BSDX 外层组；9 以及其它不可表达值记录为阻塞问题。</p>
 */
public class BheScriptGroupCompatibilityPolicy {

    private static final int BSDX_MIN_GROUP_INDEX = 0;
    private static final int BSDX_MAX_GROUP_INDEX = com.giga.nexas.dto.bsdx.map.MapData.SCRIPT_GROUP_COUNT - 1;

    public ScriptGroupConversion convert(
            String sourceMapFileName,
            List<MapData.ScriptEntryGroupBlock> sourceGroups,
            BheMapDataConversionAudit audit
    ) {
        ScriptGroupConversion conversion = new ScriptGroupConversion();
        List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> targetGroups = emptyTargetGroups();
        conversion.setTargetGroups(targetGroups);
        if (sourceGroups == null) {
            return conversion;
        }

        for (MapData.ScriptEntryGroupBlock sourceGroup : sourceGroups) {
            if (sourceGroup == null || sourceGroup.getEntries() == null) {
                continue;
            }
            for (MapData.ScriptEntry sourceEntry : sourceGroup.getEntries()) {
                if (audit != null) {
                    audit.recordInspectedScriptEntry();
                }
                convertOne(sourceMapFileName, sourceEntry, targetGroups, conversion, audit);
            }
        }
        return conversion;
    }

    private void convertOne(
            String sourceMapFileName,
            MapData.ScriptEntry sourceEntry,
            List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> targetGroups,
            ScriptGroupConversion conversion,
            BheMapDataConversionAudit audit
    ) {
        if (sourceEntry == null || !isBsdxRepresentable(sourceEntry.getGroupIndex())) {
            conversion.getBlockingIssues().add(unsupportedIssue(sourceMapFileName, sourceEntry));
            return;
        }

        int targetGroupIndex = sourceEntry.getGroupIndex();
        com.giga.nexas.dto.bsdx.map.MapData.ScriptEntry targetEntry =
                new com.giga.nexas.dto.bsdx.map.MapData.ScriptEntry();
        targetEntry.setTypeId(sourceEntry.getTypeId());
        targetEntry.setX(sourceEntry.getX());
        targetEntry.setY(sourceEntry.getY());

        com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock targetGroup = targetGroups.get(targetGroupIndex);
        targetGroup.getEntries().add(targetEntry);
        targetGroup.setCount(targetGroup.getEntries().size());
        if (audit != null) {
            audit.recordRebucket(targetGroupIndex);
        }
    }

    private BheMapDataConversionIssue unsupportedIssue(String sourceMapFileName, MapData.ScriptEntry sourceEntry) {
        Integer groupIndex = sourceEntry == null ? null : sourceEntry.getGroupIndex();
        BheMapDataConversionIssue issue = BheMapDataConversionIssue.blocking(
                BheMapDataConversionIssueType.UNSUPPORTED_SCRIPT_GROUP_INDEX,
                sourceMapFileName,
                "BHE script groupIndex 无法写入 BSDX script group: " + groupIndex
        );
        issue.setGroupIndex(groupIndex);
        if (sourceEntry != null) {
            issue.setTypeId(sourceEntry.getTypeId());
            issue.setX(sourceEntry.getX());
            issue.setY(sourceEntry.getY());
        }
        return issue;
    }

    private boolean isBsdxRepresentable(Integer groupIndex) {
        return groupIndex != null && groupIndex >= BSDX_MIN_GROUP_INDEX && groupIndex <= BSDX_MAX_GROUP_INDEX;
    }

    private List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> emptyTargetGroups() {
        List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> groups = new ArrayList<>();
        for (int groupNum = 0; groupNum < com.giga.nexas.dto.bsdx.map.MapData.SCRIPT_GROUP_COUNT; groupNum++) {
            com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock group =
                    new com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock();
            group.setGroupNum(groupNum);
            group.setCount(0);
            groups.add(group);
        }
        return groups;
    }

    public static class ScriptGroupConversion {

        private List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> targetGroups = new ArrayList<>();
        private List<BheMapDataConversionIssue> blockingIssues = new ArrayList<>();

        public List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> getTargetGroups() {
            return targetGroups;
        }

        public void setTargetGroups(List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> targetGroups) {
            this.targetGroups = targetGroups;
        }

        public List<BheMapDataConversionIssue> getBlockingIssues() {
            return blockingIssues;
        }
    }
}
