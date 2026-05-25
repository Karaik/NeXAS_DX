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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BheMapDataGroupIndexAuditTest {

    @TempDir
    Path tempDir;

    @Test
    void realBheMapsCharacterizeScriptGroupIndexAsIndependentField() throws Exception {
        BheMapAppendRequest request = new BheMapAppendRequest();
        BheMapCatalog sourceCatalog = new BheMapCatalogLoader().load(
                request.resolveBheMapGroupPath(),
                request.getCharset()
        );
        Path staticResourceRoot = tempDir.resolve("empty-static-root");
        Files.createDirectories(staticResourceRoot);

        BheMapAppendPlan plan = new BheMapAppendPlanBuilder().build(
                sourceCatalog,
                request.resolveBheMapDir(),
                staticResourceRoot,
                new BsdxMapBaseline(),
                request.getCharset(),
                new BheMapAppendAudit()
        );
        BheMapGroupIndexValidationResult result = new BheMapGroupIndexValidator().validate(plan, request.getCharset());
        GroupIndexProfile profile = profileGroupIndex(plan, request.getCharset());

        // 真实数据已经否定 groupIndex == 外层 groupNum；这个测试固定该事实，防止后续误把它当成可丢弃字段。
        assertFalse(result.isPassed());
        assertTrue(result.getIssues().size() > 0);
        assertEquals(142, result.getValidatedMapCount());
        assertEquals(profile.getValidatedMapCount(), result.getValidatedMapCount());
        assertEquals(profile.getInspectedScriptEntryCount(), result.getInspectedScriptEntryCount());
        assertTrue(result.getInspectedScriptEntryCount() > 0);

        System.out.println("Validated BHE map count: " + result.getValidatedMapCount());
        System.out.println("Inspected script entries: " + result.getInspectedScriptEntryCount());
        System.out.println("GroupIndex issues: " + result.getIssues().size());
        System.out.println("Distinct groupIndex values: " + profile.getDistinctGroupIndexValues());
        System.out.println("groupNum -> groupIndex frequency:");
        System.out.println(formatNestedFrequency(profile.getGroupNumToGroupIndexFrequency()));
        System.out.println("typeId -> groupIndex frequency:");
        System.out.println(formatNestedFrequency(profile.getTypeIdToGroupIndexFrequency()));
        System.out.println("sourceMapFileName -> groupIndex frequency:");
        System.out.println(formatNestedFrequency(profile.getSourceMapFileNameToGroupIndexFrequency()));
        System.out.println("First 20 groupIndex mismatches:");
        result.getIssues().stream()
                .limit(20)
                .forEach(this::printIssue);
    }

    private GroupIndexProfile profileGroupIndex(BheMapAppendPlan plan, String charset) throws Exception {
        GroupIndexProfile profile = new GroupIndexProfile();
        BheBinService bheBinService = new BheBinService();
        for (BheMapEntryPlan entry : plan.getEntries()) {
            MapData mapData = (MapData) bheBinService.parse(entry.getSourceMapPath().toString(), charset).getData();
            profile.validatedMapCount++;
            if (mapData.getScriptEntryGroupBlocks() == null) {
                continue;
            }

            for (int groupNum = 0; groupNum < mapData.getScriptEntryGroupBlocks().size(); groupNum++) {
                MapData.ScriptEntryGroupBlock group = mapData.getScriptEntryGroupBlocks().get(groupNum);
                if (group == null || group.getEntries() == null) {
                    continue;
                }
                for (MapData.ScriptEntry scriptEntry : group.getEntries()) {
                    profile.inspectedScriptEntryCount++;
                    String groupIndex = label(scriptEntry == null ? null : scriptEntry.getGroupIndex());
                    String typeId = label(scriptEntry == null ? null : scriptEntry.getTypeId());
                    profile.distinctGroupIndexValues.add(groupIndex);
                    increment(profile.groupNumToGroupIndexFrequency, String.valueOf(groupNum), groupIndex);
                    increment(profile.typeIdToGroupIndexFrequency, typeId, groupIndex);
                    increment(profile.sourceMapFileNameToGroupIndexFrequency, entry.getSourceMapFileName(), groupIndex);
                }
            }
        }
        return profile;
    }

    private void increment(Map<String, Map<String, Integer>> table, String outerKey, String innerKey) {
        table.computeIfAbsent(outerKey, ignored -> new TreeMap<>())
                .merge(innerKey, 1, Integer::sum);
    }

    private String formatNestedFrequency(Map<String, Map<String, Integer>> table) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Map<String, Integer>> entry : table.entrySet()) {
            builder.append(entry.getKey()).append(" -> ").append(entry.getValue()).append(System.lineSeparator());
        }
        return builder.toString();
    }

    private void printIssue(BheMapGroupIndexIssue issue) {
        System.out.println(issue.getSourceMapFileName()
                + " groupNum=" + issue.getGroupNum()
                + " groupIndex=" + issue.getGroupIndex()
                + " typeId=" + issue.getTypeId());
    }

    private String label(Integer value) {
        return value == null ? "null" : String.valueOf(value);
    }

    private static class GroupIndexProfile {

        private int validatedMapCount;
        private int inspectedScriptEntryCount;
        private Set<String> distinctGroupIndexValues = new LinkedHashSet<>();
        private Map<String, Map<String, Integer>> groupNumToGroupIndexFrequency = new TreeMap<>();
        private Map<String, Map<String, Integer>> typeIdToGroupIndexFrequency = new TreeMap<>();
        private Map<String, Map<String, Integer>> sourceMapFileNameToGroupIndexFrequency = new LinkedHashMap<>();

        public int getValidatedMapCount() {
            return validatedMapCount;
        }

        public int getInspectedScriptEntryCount() {
            return inspectedScriptEntryCount;
        }

        public Set<String> getDistinctGroupIndexValues() {
            return distinctGroupIndexValues;
        }

        public Map<String, Map<String, Integer>> getGroupNumToGroupIndexFrequency() {
            return groupNumToGroupIndexFrequency;
        }

        public Map<String, Map<String, Integer>> getTypeIdToGroupIndexFrequency() {
            return typeIdToGroupIndexFrequency;
        }

        public Map<String, Map<String, Integer>> getSourceMapFileNameToGroupIndexFrequency() {
            return sourceMapFileNameToGroupIndexFrequency;
        }
    }
}
