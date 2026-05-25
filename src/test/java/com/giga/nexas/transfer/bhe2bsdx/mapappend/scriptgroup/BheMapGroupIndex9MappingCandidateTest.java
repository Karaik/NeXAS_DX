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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BheMapGroupIndex9MappingCandidateTest {

    private static final int GROUP_INDEX_9 = 9;
    private static final int BSDX_MIN_GROUP_INDEX = 0;
    private static final int BSDX_MAX_GROUP_INDEX = 8;

    @TempDir
    Path tempDir;

    @Test
    void realBheGroupIndex9RowsProduceStableMappingCandidatesOnly() throws Exception {
        BheMapAppendRequest request = new BheMapAppendRequest();
        BheMapAppendPlan plan = buildRealImportPlanWithoutMaterializingOutput(request);
        ScriptGroupProfile profile = collectScriptGroupProfile(plan, request.getCharset());

        List<CandidateDecision> fixedTo8 = fixedTo8(profile.groupIndex9Rows);
        List<CandidateDecision> sameMapTypeMode = sameMapTypeMode(profile);
        List<CandidateDecision> globalTypeMode = globalTypeMode(profile);
        List<CandidateDecision> nearestCoordinate = nearestCoordinate(profile);
        List<CandidateDecision> unsupported = unsupported(profile.groupIndex9Rows);

        assertEquals(142, profile.validatedMapCount);
        assertEquals(27562, profile.inspectedScriptEntryCount);
        assertEquals(38, profile.groupIndex9Rows.size());
        assertEquals(Map.of(13, 2, 16, 18, 17, 6, 18, 6, 41, 3, 42, 3), profile.groupIndex9TypeCounts);
        assertEquals(7, profile.groupIndex9MapCounts.size());
        assertTrue(profile.groupIndex9Rows.stream().allMatch(row -> row.outerGroupNum == 0));
        assertFalse(profile.groupIndex9Rows.isEmpty());

        assertEquals(38, fixedTo8.size());
        assertEquals(38, mappedCount(fixedTo8));
        assertEquals(Map.of(8, 38), targetDistribution(fixedTo8));

        assertEquals(38, sameMapTypeMode.size());
        assertEquals(33, mappedCount(sameMapTypeMode));
        assertEquals(5, unsupportedCount(sameMapTypeMode));
        assertEquals(Map.of(1, 6, 6, 3, 7, 18, 8, 6), targetDistribution(sameMapTypeMode));

        assertEquals(38, globalTypeMode.size());
        assertEquals(38, mappedCount(globalTypeMode));
        assertEquals(Map.of(1, 20, 2, 18), targetDistribution(globalTypeMode));

        assertEquals(38, nearestCoordinate.size());
        assertEquals(38, mappedCount(nearestCoordinate));
        assertTrue(nearestCoordinate.stream().allMatch(CandidateDecision::hasBsdxRepresentableTarget));

        assertEquals(38, unsupported.size());
        assertEquals(0, mappedCount(unsupported));
        assertEquals(38, unsupportedCount(unsupported));

        printEvidence(profile, sameMapTypeMode);
        printStrategy("FIXED_TO_8", fixedTo8);
        printStrategy("SAME_MAP_TYPE_MODE", sameMapTypeMode);
        printStrategy("GLOBAL_TYPE_MODE", globalTypeMode);
        printStrategy("NEAREST_COORDINATE", nearestCoordinate);
        printStrategy("UNSUPPORTED", unsupported);
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

    private ScriptGroupProfile collectScriptGroupProfile(BheMapAppendPlan plan, String charset) throws Exception {
        ScriptGroupProfile profile = new ScriptGroupProfile();
        BheBinService bheBinService = new BheBinService();
        for (BheMapEntryPlan entry : plan.getEntries()) {
            MapData mapData = (MapData) bheBinService.parse(entry.getSourceMapPath().toString(), charset).getData();
            profile.validatedMapCount++;
            if (mapData.getScriptEntryGroupBlocks() == null) {
                continue;
            }

            for (int outerGroupNum = 0; outerGroupNum < mapData.getScriptEntryGroupBlocks().size(); outerGroupNum++) {
                MapData.ScriptEntryGroupBlock group = mapData.getScriptEntryGroupBlocks().get(outerGroupNum);
                if (group == null || group.getEntries() == null) {
                    continue;
                }
                for (MapData.ScriptEntry scriptEntry : group.getEntries()) {
                    ScriptRow row = new ScriptRow(
                            entry.getSourceMapFileName(),
                            outerGroupNum,
                            scriptEntry.getGroupIndex(),
                            scriptEntry.getTypeId(),
                            scriptEntry.getX(),
                            scriptEntry.getY()
                    );
                    profile.add(row);
                }
            }
        }
        profile.sort();
        return profile;
    }

    private List<CandidateDecision> fixedTo8(List<ScriptRow> rows) {
        return rows.stream()
                .map(row -> decision(
                        row,
                        "FIXED_TO_8",
                        8,
                        "LOW",
                        "只按 BSDX 最大组号收缩，缺少 typeId、同 map 或坐标证据"
                ))
                .collect(Collectors.toList());
    }

    private List<CandidateDecision> sameMapTypeMode(ScriptGroupProfile profile) {
        List<CandidateDecision> decisions = new ArrayList<>();
        for (ScriptRow row : profile.groupIndex9Rows) {
            ModeResult mode = mode(profile.sameMapTypeGroupCounts
                    .getOrDefault(row.sourceMapFileName, Map.of())
                    .get(row.typeId));
            if (mode == null) {
                decisions.add(decision(
                        row,
                        "SAME_MAP_TYPE_MODE",
                        null,
                        "UNSUPPORTED",
                        "同 map 内该 typeId 没有 0..8 组样本，不能从局部同类对象降级"
                ));
                continue;
            }
            decisions.add(decision(
                    row,
                    "SAME_MAP_TYPE_MODE",
                    mode.groupIndex,
                    "MEDIUM",
                    "同 map 同 typeId 在 0..8 中最常见: groupIndex="
                            + mode.groupIndex
                            + " count="
                            + mode.count
            ));
        }
        return decisions;
    }

    private List<CandidateDecision> globalTypeMode(ScriptGroupProfile profile) {
        List<CandidateDecision> decisions = new ArrayList<>();
        for (ScriptRow row : profile.groupIndex9Rows) {
            ModeResult mode = mode(profile.globalTypeGroupCounts.get(row.typeId));
            if (mode == null) {
                decisions.add(decision(
                        row,
                        "GLOBAL_TYPE_MODE",
                        null,
                        "UNSUPPORTED",
                        "全局该 typeId 没有 0..8 组样本"
                ));
                continue;
            }
            decisions.add(decision(
                    row,
                    "GLOBAL_TYPE_MODE",
                    mode.groupIndex,
                    "LOW_MEDIUM",
                    "全局同 typeId 在 0..8 中最常见: groupIndex="
                            + mode.groupIndex
                            + " count="
                            + mode.count
                            + "，但忽略局部布局"
            ));
        }
        return decisions;
    }

    private List<CandidateDecision> nearestCoordinate(ScriptGroupProfile profile) {
        List<CandidateDecision> decisions = new ArrayList<>();
        for (ScriptRow row : profile.groupIndex9Rows) {
            ScriptRow nearest = profile.rowsByMap
                    .getOrDefault(row.sourceMapFileName, List.of())
                    .stream()
                    .filter(ScriptRow::hasBsdxRepresentableGroupIndex)
                    .min(Comparator
                            .comparingLong((ScriptRow candidate) -> distanceSquared(row, candidate))
                            .thenComparing(candidate -> candidate.typeId == row.typeId ? 0 : 1)
                            .thenComparingInt(candidate -> candidate.groupIndex)
                            .thenComparingInt(candidate -> candidate.typeId)
                            .thenComparingInt(candidate -> candidate.x)
                            .thenComparingInt(candidate -> candidate.y))
                    .orElse(null);
            if (nearest == null) {
                decisions.add(decision(
                        row,
                        "NEAREST_COORDINATE",
                        null,
                        "UNSUPPORTED",
                        "同 map 内没有 0..8 组对象可作坐标参照"
                ));
                continue;
            }
            decisions.add(decision(
                    row,
                    "NEAREST_COORDINATE",
                    nearest.groupIndex,
                    nearest.typeId == row.typeId ? "LOW_MEDIUM" : "LOW",
                    "最近 0..8 组对象: groupIndex="
                            + nearest.groupIndex
                            + " typeId="
                            + nearest.typeId
                            + " x="
                            + nearest.x
                            + " y="
                            + nearest.y
                            + " distanceSquared="
                            + distanceSquared(row, nearest)
            ));
        }
        return decisions;
    }

    private List<CandidateDecision> unsupported(List<ScriptRow> rows) {
        return rows.stream()
                .map(row -> decision(
                        row,
                        "UNSUPPORTED",
                        null,
                        "HIGH_BLOCK",
                        "BSDX 只表达 0..8，保留 unsupported 直到有 IDA 或样本对照证据"
                ))
                .collect(Collectors.toList());
    }

    private ModeResult mode(Map<Integer, Integer> counts) {
        if (counts == null || counts.isEmpty()) {
            return null;
        }
        return counts.entrySet().stream()
                .max(Comparator
                        .comparingInt((Map.Entry<Integer, Integer> entry) -> entry.getValue())
                        .thenComparing(entry -> -entry.getKey()))
                .map(entry -> new ModeResult(entry.getKey(), entry.getValue()))
                .orElse(null);
    }

    private CandidateDecision decision(
            ScriptRow row,
            String strategyName,
            Integer candidateTargetGroupIndex,
            String confidence,
            String riskReason
    ) {
        return new CandidateDecision(
                row.sourceMapFileName,
                row.typeId,
                row.x,
                row.y,
                row.groupIndex,
                candidateTargetGroupIndex,
                strategyName,
                confidence,
                riskReason
        );
    }

    private long distanceSquared(ScriptRow left, ScriptRow right) {
        long dx = (long) left.x - right.x;
        long dy = (long) left.y - right.y;
        return dx * dx + dy * dy;
    }

    private long mappedCount(List<CandidateDecision> decisions) {
        return decisions.stream().filter(CandidateDecision::hasBsdxRepresentableTarget).count();
    }

    private long unsupportedCount(List<CandidateDecision> decisions) {
        return decisions.stream().filter(decision -> decision.candidateTargetGroupIndex == null).count();
    }

    private Map<Integer, Integer> targetDistribution(List<CandidateDecision> decisions) {
        Map<Integer, Integer> distribution = new TreeMap<>();
        for (CandidateDecision decision : decisions) {
            if (decision.candidateTargetGroupIndex != null) {
                distribution.merge(decision.candidateTargetGroupIndex, 1, Integer::sum);
            }
        }
        return distribution;
    }

    private void printEvidence(ScriptGroupProfile profile, List<CandidateDecision> sameMapTypeMode) {
        System.out.println("groupIndex=9 map counts: " + profile.groupIndex9MapCounts);
        System.out.println("groupIndex=9 typeId counts: " + profile.groupIndex9TypeCounts);
        System.out.println("same map same type mapped count: " + mappedCount(sameMapTypeMode));
        System.out.println("same map same type unsupported count: " + unsupportedCount(sameMapTypeMode));
        System.out.println("visible layer/object layer evidence: concentrated maps="
                + profile.groupIndex9MapCounts.size()
                + ", affected typeIds="
                + profile.groupIndex9TypeCounts.keySet()
                + ", all affected typeIds also appear in 0..8 globally="
                + allAffectedTypesAppearInRepresentableGroups(profile));
    }

    private boolean allAffectedTypesAppearInRepresentableGroups(ScriptGroupProfile profile) {
        return profile.groupIndex9TypeCounts.keySet().stream()
                .allMatch(typeId -> profile.globalTypeGroupCounts.containsKey(typeId));
    }

    private void printStrategy(String name, List<CandidateDecision> decisions) {
        System.out.println(name + " mapped=" + mappedCount(decisions)
                + " unsupported=" + unsupportedCount(decisions)
                + " targets=" + targetDistribution(decisions));
        for (CandidateDecision decision : decisions) {
            System.out.println(decision.format());
        }
    }

    private static class ScriptGroupProfile {

        private int validatedMapCount;
        private int inspectedScriptEntryCount;
        private final List<ScriptRow> groupIndex9Rows = new ArrayList<>();
        private final Map<String, List<ScriptRow>> rowsByMap = new LinkedHashMap<>();
        private final Map<String, Integer> groupIndex9MapCounts = new LinkedHashMap<>();
        private final Map<Integer, Integer> groupIndex9TypeCounts = new TreeMap<>();
        private final Map<String, Map<Integer, Map<Integer, Integer>>> sameMapTypeGroupCounts = new LinkedHashMap<>();
        private final Map<Integer, Map<Integer, Integer>> globalTypeGroupCounts = new TreeMap<>();

        private void add(ScriptRow row) {
            inspectedScriptEntryCount++;
            rowsByMap.computeIfAbsent(row.sourceMapFileName, ignored -> new ArrayList<>()).add(row);
            if (row.groupIndex == GROUP_INDEX_9) {
                groupIndex9Rows.add(row);
                groupIndex9MapCounts.merge(row.sourceMapFileName, 1, Integer::sum);
                groupIndex9TypeCounts.merge(row.typeId, 1, Integer::sum);
            }
            if (row.hasBsdxRepresentableGroupIndex()) {
                sameMapTypeGroupCounts
                        .computeIfAbsent(row.sourceMapFileName, ignored -> new TreeMap<>())
                        .computeIfAbsent(row.typeId, ignored -> new TreeMap<>())
                        .merge(row.groupIndex, 1, Integer::sum);
                globalTypeGroupCounts
                        .computeIfAbsent(row.typeId, ignored -> new TreeMap<>())
                        .merge(row.groupIndex, 1, Integer::sum);
            }
        }

        private void sort() {
            Comparator<ScriptRow> comparator = Comparator
                    .comparing((ScriptRow row) -> row.sourceMapFileName)
                    .thenComparingInt(row -> row.typeId)
                    .thenComparingInt(row -> row.x)
                    .thenComparingInt(row -> row.y)
                    .thenComparingInt(row -> row.groupIndex);
            groupIndex9Rows.sort(comparator);
            for (List<ScriptRow> rows : rowsByMap.values()) {
                rows.sort(comparator);
            }
        }
    }

    private record ScriptRow(
            String sourceMapFileName,
            int outerGroupNum,
            int groupIndex,
            int typeId,
            int x,
            int y
    ) {

        private boolean hasBsdxRepresentableGroupIndex() {
            return groupIndex >= BSDX_MIN_GROUP_INDEX && groupIndex <= BSDX_MAX_GROUP_INDEX;
        }
    }

    private record ModeResult(int groupIndex, int count) {
    }

    private record CandidateDecision(
            String sourceMapFileName,
            int typeId,
            int x,
            int y,
            int originalGroupIndex,
            Integer candidateTargetGroupIndex,
            String strategyName,
            String confidence,
            String riskReason
    ) {

        private boolean hasBsdxRepresentableTarget() {
            return candidateTargetGroupIndex != null
                    && candidateTargetGroupIndex >= BSDX_MIN_GROUP_INDEX
                    && candidateTargetGroupIndex <= BSDX_MAX_GROUP_INDEX;
        }

        private String format() {
            return sourceMapFileName
                    + " typeId=" + typeId
                    + " x=" + x
                    + " y=" + y
                    + " originalGroupIndex=" + originalGroupIndex
                    + " candidateTargetGroupIndex=" + (candidateTargetGroupIndex == null ? "UNSUPPORTED" : candidateTargetGroupIndex)
                    + " strategyName=" + strategyName
                    + " confidence=" + confidence
                    + " riskReason=" + riskReason;
        }
    }
}
