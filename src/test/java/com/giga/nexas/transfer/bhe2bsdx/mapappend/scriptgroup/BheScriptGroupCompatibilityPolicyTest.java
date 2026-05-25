package com.giga.nexas.transfer.bhe2bsdx.mapappend.scriptgroup;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssueType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BheScriptGroupCompatibilityPolicyTest {

    @Test
    void representableGroupIndexRebucketsToMatchingBsdxOuterGroup() {
        BheMapDataConversionAudit audit = new BheMapDataConversionAudit();
        List<MapData.ScriptEntryGroupBlock> sourceGroups = emptyBheGroups();
        sourceGroups.get(0).getEntries().add(scriptEntry(0, 10, 100, 200));
        sourceGroups.get(0).getEntries().add(scriptEntry(3, 30, 300, 400));

        BheScriptGroupCompatibilityPolicy.ScriptGroupConversion conversion =
                new BheScriptGroupCompatibilityPolicy().convert("synthetic.map", sourceGroups, audit);

        assertTrue(conversion.getBlockingIssues().isEmpty());
        assertEquals(1, conversion.getTargetGroups().get(0).getEntries().size());
        assertEquals(10, conversion.getTargetGroups().get(0).getEntries().get(0).getTypeId());
        assertEquals(1, conversion.getTargetGroups().get(3).getEntries().size());
        assertEquals(30, conversion.getTargetGroups().get(3).getEntries().get(0).getTypeId());
        assertEquals(2, audit.getRebucketedScriptEntryCount());
        assertEquals(2, audit.getInspectedScriptEntryCount());
    }

    @Test
    void unsupportedGroupIndexProducesBlockingIssueWithoutSyntheticRemap() {
        BheMapDataConversionAudit audit = new BheMapDataConversionAudit();
        List<MapData.ScriptEntryGroupBlock> sourceGroups = emptyBheGroups();
        sourceGroups.get(0).getEntries().add(scriptEntry(9, 90, 900, 901));

        BheScriptGroupCompatibilityPolicy.ScriptGroupConversion conversion =
                new BheScriptGroupCompatibilityPolicy().convert("unsupported.map", sourceGroups, audit);

        assertEquals(1, conversion.getBlockingIssues().size());
        assertEquals(BheMapDataConversionIssueType.UNSUPPORTED_SCRIPT_GROUP_INDEX,
                conversion.getBlockingIssues().get(0).getType());
        assertEquals(9, conversion.getBlockingIssues().get(0).getGroupIndex());
        assertEquals(90, conversion.getBlockingIssues().get(0).getTypeId());
        assertEquals(900, conversion.getBlockingIssues().get(0).getX());
        assertEquals(901, conversion.getBlockingIssues().get(0).getY());
        assertEquals(0, audit.getRebucketedScriptEntryCount());
        assertEquals(1, audit.getInspectedScriptEntryCount());
    }

    private List<MapData.ScriptEntryGroupBlock> emptyBheGroups() {
        List<MapData.ScriptEntryGroupBlock> groups = new ArrayList<>();
        for (int groupNum = 0; groupNum < MapData.SCRIPT_GROUP_COUNT; groupNum++) {
            MapData.ScriptEntryGroupBlock group = new MapData.ScriptEntryGroupBlock();
            group.setGroupNum(groupNum);
            group.setCount(0);
            groups.add(group);
        }
        return groups;
    }

    private MapData.ScriptEntry scriptEntry(int groupIndex, int typeId, int x, int y) {
        MapData.ScriptEntry entry = new MapData.ScriptEntry();
        entry.setGroupIndex(groupIndex);
        entry.setTypeId(typeId);
        entry.setX(x);
        entry.setY(y);
        return entry;
    }
}
