package com.giga.nexas.transfer.bhe2bsdx.mapappend.scriptgroup;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BheMapCatalogLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.plan.BheMapAppendPlanBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BheMapScriptGroupRebucketRuleTest {

    private static final int BSDX_SCRIPT_GROUP_COUNT = 9;

    @TempDir
    Path tempDir;

    @Test
    void realBheMapsExposeWhetherGroupIndexCanRebucketIntoBsdxScriptGroups() throws Exception {
        BheMapAppendRequest request = new BheMapAppendRequest();
        BheMapAppendPlan plan = buildRealImportPlanWithoutMaterializingOutput(request);
        GroupIndexRebucketProfile profile = profileGroupIndex(plan, request.getCharset());

        // 当前只做语义画像：允许 0..9 被记录，但大于 9 说明解析或假设出现新风险，必须失败。
        assertEquals(142, profile.validatedMapCount);
        assertTrue(profile.inspectedScriptEntryCount > 0);
        assertFalse(profile.groupIndexCounts.isEmpty());
        assertTrue(profile.groupIndexCounts.keySet().stream().allMatch(value -> value >= 0 && value <= 9));
        assertTrue(profile.groupIndexCounts.containsKey(9));
        assertFalse(profile.groupIndex9Occurrences.isEmpty());

        System.out.println("groupIndex 0..9 distribution: " + profile.groupIndexCounts);
        System.out.println("groupIndex=9 occurrences:");
        profile.groupIndex9Occurrences.forEach((key, count) -> System.out.println(key + " count=" + count));
    }

    @Test
    void syntheticBheScriptEntriesCanRebucketByGroupIndexWhenInsideBsdxRange() {
        MapData source = new MapData();
        source.setScriptEntryGroupBlocks(emptyBheScriptGroups());
        source.getScriptEntryGroupBlocks().get(0).getEntries().add(bheScriptEntry(0, 10, 100, 200));
        source.getScriptEntryGroupBlocks().get(0).getEntries().add(bheScriptEntry(3, 30, 300, 400));
        source.getScriptEntryGroupBlocks().get(0).setCount(2);

        List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> targetGroups = rebucketForPrototype(source);

        // 这个 prototype 只验证规则：groupIndex 决定 BSDX 外层组号，输出 entry 丢掉 groupIndex。
        assertEquals(1, targetGroups.get(0).getEntries().size());
        assertEquals(10, targetGroups.get(0).getEntries().get(0).getTypeId());
        assertEquals(100, targetGroups.get(0).getEntries().get(0).getX());
        assertEquals(200, targetGroups.get(0).getEntries().get(0).getY());
        assertEquals(1, targetGroups.get(3).getEntries().size());
        assertEquals(30, targetGroups.get(3).getEntries().get(0).getTypeId());
        assertEquals(300, targetGroups.get(3).getEntries().get(0).getX());
        assertEquals(400, targetGroups.get(3).getEntries().get(0).getY());
        assertEquals(0, targetGroups.get(1).getEntries().size());
        assertEquals(0, targetGroups.get(2).getEntries().size());
    }

    @Test
    void syntheticGroupIndex9CannotRebucketIntoBsdxNineGroupStructure() {
        MapData source = new MapData();
        source.setScriptEntryGroupBlocks(emptyBheScriptGroups());
        source.getScriptEntryGroupBlocks().get(0).getEntries().add(bheScriptEntry(9, 99, 900, 901));
        source.getScriptEntryGroupBlocks().get(0).setCount(1);

        // BSDX 只有 0..8 九个脚本组，groupIndex=9 不能静默塞进 group 8 或丢弃。
        assertThrows(IllegalStateException.class, () -> rebucketForPrototype(source));
    }

    private BheMapAppendPlan buildRealImportPlanWithoutMaterializingOutput(BheMapAppendRequest request) throws Exception {
        BheMapCatalog sourceCatalog = new BheMapCatalogLoader().load(
                request.resolveBheMapGroupPath(),
                request.getCharset()
        );
        Path staticResourceRoot = tempDir.resolve("empty-static-root");
        Files.createDirectories(staticResourceRoot);
        return new BheMapAppendPlanBuilder().build(
                sourceCatalog,
                request.resolveBheMapDir(),
                staticResourceRoot,
                new BsdxMapBaseline(),
                request.getCharset(),
                new BheMapAppendAudit()
        );
    }

    private GroupIndexRebucketProfile profileGroupIndex(BheMapAppendPlan plan, String charset) throws Exception {
        GroupIndexRebucketProfile profile = new GroupIndexRebucketProfile();
        BheBinService bheBinService = new BheBinService();
        for (BheMapEntryPlan entry : plan.getEntries()) {
            MapData mapData = (MapData) bheBinService.parse(entry.getSourceMapPath().toString(), charset).getData();
            profile.validatedMapCount++;
            if (mapData.getScriptEntryGroupBlocks() == null) {
                continue;
            }
            for (MapData.ScriptEntryGroupBlock group : mapData.getScriptEntryGroupBlocks()) {
                if (group == null || group.getEntries() == null) {
                    continue;
                }
                for (MapData.ScriptEntry scriptEntry : group.getEntries()) {
                    profile.inspectedScriptEntryCount++;
                    Integer groupIndex = scriptEntry.getGroupIndex();
                    profile.groupIndexCounts.merge(groupIndex, 1, Integer::sum);
                    if (groupIndex != null && groupIndex == 9) {
                        String key = entry.getSourceMapFileName()
                                + " typeId=" + scriptEntry.getTypeId()
                                + " x=" + scriptEntry.getX()
                                + " y=" + scriptEntry.getY();
                        profile.groupIndex9Occurrences.merge(key, 1, Integer::sum);
                    }
                }
            }
        }
        return profile;
    }

    private List<MapData.ScriptEntryGroupBlock> emptyBheScriptGroups() {
        List<MapData.ScriptEntryGroupBlock> groups = new ArrayList<>();
        for (int groupNum = 0; groupNum < BSDX_SCRIPT_GROUP_COUNT; groupNum++) {
            MapData.ScriptEntryGroupBlock group = new MapData.ScriptEntryGroupBlock();
            group.setGroupNum(groupNum);
            group.setCount(0);
            groups.add(group);
        }
        return groups;
    }

    private MapData.ScriptEntry bheScriptEntry(int groupIndex, int typeId, int x, int y) {
        MapData.ScriptEntry entry = new MapData.ScriptEntry();
        entry.setGroupIndex(groupIndex);
        entry.setTypeId(typeId);
        entry.setX(x);
        entry.setY(y);
        return entry;
    }

    private List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> rebucketForPrototype(MapData source) {
        List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> targetGroups = emptyBsdxScriptGroups();
        for (MapData.ScriptEntryGroupBlock sourceGroup : source.getScriptEntryGroupBlocks()) {
            if (sourceGroup == null || sourceGroup.getEntries() == null) {
                continue;
            }
            for (MapData.ScriptEntry sourceEntry : sourceGroup.getEntries()) {
                int targetGroupIndex = sourceEntry.getGroupIndex();
                if (targetGroupIndex < 0 || targetGroupIndex >= BSDX_SCRIPT_GROUP_COUNT) {
                    throw new IllegalStateException("BHE groupIndex 超出 BSDX 可表达范围: " + targetGroupIndex);
                }
                com.giga.nexas.dto.bsdx.map.MapData.ScriptEntry targetEntry =
                        new com.giga.nexas.dto.bsdx.map.MapData.ScriptEntry();
                targetEntry.setTypeId(sourceEntry.getTypeId());
                targetEntry.setX(sourceEntry.getX());
                targetEntry.setY(sourceEntry.getY());
                targetGroups.get(targetGroupIndex).getEntries().add(targetEntry);
                targetGroups.get(targetGroupIndex).setCount(targetGroups.get(targetGroupIndex).getEntries().size());
            }
        }
        return targetGroups;
    }

    private List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> emptyBsdxScriptGroups() {
        List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> groups = new ArrayList<>();
        for (int groupNum = 0; groupNum < BSDX_SCRIPT_GROUP_COUNT; groupNum++) {
            com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock group =
                    new com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock();
            group.setGroupNum(groupNum);
            group.setCount(0);
            groups.add(group);
        }
        return groups;
    }

    private static class GroupIndexRebucketProfile {

        private int validatedMapCount;
        private int inspectedScriptEntryCount;
        private Map<Integer, Integer> groupIndexCounts = new TreeMap<>();
        private Map<String, Integer> groupIndex9Occurrences = new LinkedHashMap<>();
    }
}
