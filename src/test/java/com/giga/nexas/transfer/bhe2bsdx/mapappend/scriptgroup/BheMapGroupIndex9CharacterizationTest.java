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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BheMapGroupIndex9CharacterizationTest {

    private static final int GROUP_INDEX_9 = 9;

    private static final Set<String> EXPECTED_GROUP_INDEX_9_MAPS = Set.of(
            "mapB06_Ruin07.map",
            "mapB06_Outsider06.map",
            "mapB06_OutsiderE06.map",
            "mapB06_OutsiderN06.map",
            "mapB06_Outsider07.map",
            "mapB06_OutsiderE07.map",
            "mapB06_OutsiderN07.map"
    );

    private static final Set<Integer> EXPECTED_GROUP_INDEX_9_TYPE_IDS = Set.of(13, 16, 17, 18, 41, 42);

    @TempDir
    Path tempDir;

    @Test
    void realBheMapsCharacterizeGroupIndex9ClusterBeforeAnyMapDataConversion() throws Exception {
        BheMapAppendRequest request = new BheMapAppendRequest();
        BheMapAppendPlan plan = buildRealImportPlanWithoutMaterializingOutput(request);
        GroupIndex9Profile profile = profileGroupIndex9(plan, request.getCharset());

        // 这个测试固定 groupIndex=9 的真实画像；它不是转换器测试，不能把 9 静默降级成 0..8。
        assertEquals(142, profile.validatedMapCount);
        assertEquals(27562, profile.inspectedScriptEntryCount);
        assertEquals(38, profile.groupIndex9Rows.size());
        assertEquals(EXPECTED_GROUP_INDEX_9_MAPS, profile.groupIndex9MapCounts.keySet());
        assertEquals(EXPECTED_GROUP_INDEX_9_TYPE_IDS, profile.groupIndex9TypeCounts.keySet());
        assertTrue(profile.groupIndex9Rows.stream().allMatch(row -> row.outerGroupNum == 0));
        assertFalse(profile.sameMapTargetTypeGroupIndexCounts.isEmpty());
        assertTrue(profile.groupIndex9TypeCounts.keySet().stream()
                .allMatch(typeId -> appearsInBsdxRepresentableGroup(profile, typeId)));

        System.out.println("groupIndex distribution: " + profile.groupIndexCounts);
        System.out.println("groupIndex=9 map counts: " + profile.groupIndex9MapCounts);
        System.out.println("groupIndex=9 typeId counts: " + profile.groupIndex9TypeCounts);
        System.out.println("groupIndex=9 full rows:");
        profile.groupIndex9Rows.forEach(row -> System.out.println(row.format()));
        System.out.println("same map target typeId -> groupIndex counts:");
        printNestedTypeDistribution(profile.sameMapTargetTypeGroupIndexCounts);
        System.out.println("global target typeId -> groupIndex counts:");
        printGlobalTypeDistribution(profile.globalTargetTypeGroupIndexCounts);
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

    private GroupIndex9Profile profileGroupIndex9(BheMapAppendPlan plan, String charset) throws Exception {
        GroupIndex9Profile profile = new GroupIndex9Profile();
        Map<String, List<ScriptRow>> rowsByMap = collectScriptRows(plan, charset, profile);
        List<ScriptRow> groupIndex9Rows = rowsByMap.values().stream()
                .flatMap(List::stream)
                .filter(row -> row.groupIndex == GROUP_INDEX_9)
                .collect(Collectors.toList());

        profile.groupIndex9Rows.addAll(groupIndex9Rows);
        for (ScriptRow row : groupIndex9Rows) {
            profile.groupIndex9MapCounts.merge(row.sourceMapFileName, 1, Integer::sum);
            profile.groupIndex9TypeCounts.merge(row.typeId, 1, Integer::sum);
        }

        Set<String> affectedMaps = new LinkedHashSet<>(profile.groupIndex9MapCounts.keySet());
        Set<Integer> affectedTypeIds = new TreeSet<>(profile.groupIndex9TypeCounts.keySet());
        for (String sourceMapFileName : affectedMaps) {
            List<ScriptRow> sameMapRows = rowsByMap.getOrDefault(sourceMapFileName, List.of());
            Map<Integer, Map<Integer, Integer>> typeTable =
                    profile.sameMapTargetTypeGroupIndexCounts.computeIfAbsent(sourceMapFileName, ignored -> new TreeMap<>());
            for (ScriptRow row : sameMapRows) {
                if (affectedTypeIds.contains(row.typeId)) {
                    typeTable.computeIfAbsent(row.typeId, ignored -> new TreeMap<>())
                            .merge(row.groupIndex, 1, Integer::sum);
                }
            }
        }

        for (List<ScriptRow> rows : rowsByMap.values()) {
            for (ScriptRow row : rows) {
                if (affectedTypeIds.contains(row.typeId)) {
                    profile.globalTargetTypeGroupIndexCounts.computeIfAbsent(row.typeId, ignored -> new TreeMap<>())
                            .merge(row.groupIndex, 1, Integer::sum);
                }
            }
        }
        return profile;
    }

    private Map<String, List<ScriptRow>> collectScriptRows(
            BheMapAppendPlan plan,
            String charset,
            GroupIndex9Profile profile
    ) throws Exception {
        Map<String, List<ScriptRow>> rowsByMap = new LinkedHashMap<>();
        BheBinService bheBinService = new BheBinService();
        for (BheMapEntryPlan entry : plan.getEntries()) {
            MapData mapData = (MapData) bheBinService.parse(entry.getSourceMapPath().toString(), charset).getData();
            profile.validatedMapCount++;
            if (mapData.getScriptEntryGroupBlocks() == null) {
                continue;
            }

            List<ScriptRow> rows = rowsByMap.computeIfAbsent(entry.getSourceMapFileName(), ignored -> new ArrayList<>());
            for (int groupNum = 0; groupNum < mapData.getScriptEntryGroupBlocks().size(); groupNum++) {
                MapData.ScriptEntryGroupBlock group = mapData.getScriptEntryGroupBlocks().get(groupNum);
                if (group == null || group.getEntries() == null) {
                    continue;
                }
                for (MapData.ScriptEntry scriptEntry : group.getEntries()) {
                    profile.inspectedScriptEntryCount++;
                    ScriptRow row = new ScriptRow(
                            entry.getSourceMapFileName(),
                            groupNum,
                            scriptEntry.getGroupIndex(),
                            scriptEntry.getTypeId(),
                            scriptEntry.getX(),
                            scriptEntry.getY()
                    );
                    rows.add(row);
                    profile.groupIndexCounts.merge(row.groupIndex, 1, Integer::sum);
                }
            }
        }

        for (List<ScriptRow> rows : rowsByMap.values()) {
            rows.sort(Comparator
                    .comparingInt((ScriptRow row) -> row.outerGroupNum)
                    .thenComparingInt(row -> row.groupIndex)
                    .thenComparingInt(row -> row.typeId)
                    .thenComparingInt(row -> row.x)
                    .thenComparingInt(row -> row.y));
        }
        return rowsByMap;
    }

    private void printNestedTypeDistribution(Map<String, Map<Integer, Map<Integer, Integer>>> table) {
        for (Map.Entry<String, Map<Integer, Map<Integer, Integer>>> mapEntry : table.entrySet()) {
            System.out.println(mapEntry.getKey());
            for (Map.Entry<Integer, Map<Integer, Integer>> typeEntry : mapEntry.getValue().entrySet()) {
                System.out.println("  typeId=" + typeEntry.getKey() + " -> " + typeEntry.getValue());
            }
        }
    }

    private boolean appearsInBsdxRepresentableGroup(GroupIndex9Profile profile, Integer typeId) {
        Map<Integer, Integer> groupIndexCounts = profile.globalTargetTypeGroupIndexCounts.get(typeId);
        if (groupIndexCounts == null) {
            return false;
        }
        return groupIndexCounts.keySet().stream().anyMatch(groupIndex -> groupIndex >= 0 && groupIndex <= 8);
    }

    private void printGlobalTypeDistribution(Map<Integer, Map<Integer, Integer>> table) {
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : table.entrySet()) {
            System.out.println("typeId=" + entry.getKey() + " -> " + entry.getValue());
        }
    }

    private static class GroupIndex9Profile {

        private int validatedMapCount;
        private int inspectedScriptEntryCount;
        private Map<Integer, Integer> groupIndexCounts = new TreeMap<>();
        private List<ScriptRow> groupIndex9Rows = new ArrayList<>();
        private Map<String, Integer> groupIndex9MapCounts = new LinkedHashMap<>();
        private Map<Integer, Integer> groupIndex9TypeCounts = new TreeMap<>();
        private Map<String, Map<Integer, Map<Integer, Integer>>> sameMapTargetTypeGroupIndexCounts =
                new LinkedHashMap<>();
        private Map<Integer, Map<Integer, Integer>> globalTargetTypeGroupIndexCounts = new TreeMap<>();
    }

    private record ScriptRow(
            String sourceMapFileName,
            int outerGroupNum,
            int groupIndex,
            int typeId,
            int x,
            int y
    ) {

        private String format() {
            return sourceMapFileName
                    + " outerGroupNum=" + outerGroupNum
                    + " groupIndex=" + groupIndex
                    + " typeId=" + typeId
                    + " x=" + x
                    + " y=" + y;
        }
    }
}
