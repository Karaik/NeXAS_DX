package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 输出 BHE -> BSDX term 迁移矩阵。
 *
 * <p>这个测试不做正式转换，只把 WAZ/MEK 审计结果整理成可以评审的规则清单。
 * term 转换器只能基于这份矩阵逐条落规则，不能在代码里写隐藏 fallback。</p>
 */
class TermMigrationMatrixAuditTest {

    private static final Path BHE_WAZ_PATH_SUMMARY =
            Paths.get("src/main/resources/bheInfoCollectionAnalysis/bhe-info-collection-path-summary.tsv");
    private static final Path BSDX_WAZ_PATH_SUMMARY =
            Paths.get("src/main/resources/bsdxInfoCollectionAnalysis/bsdx-info-collection-path-summary.tsv");
    private static final Path BHE_WAZ_OCCURRENCES =
            Paths.get("src/main/resources/bheInfoCollectionAnalysis/bhe-info-collection-occurrences.tsv");
    private static final Path BSDX_WAZ_OCCURRENCES =
            Paths.get("src/main/resources/bsdxInfoCollectionAnalysis/bsdx-info-collection-occurrences.tsv");
    private static final Path BHE_TERM_OVERVIEW =
            Paths.get("src/main/resources/bheInfoCollectionAnalysis/term-overview.tsv");
    private static final Path BSDX_TERM_OVERVIEW =
            Paths.get("src/main/resources/bsdxInfoCollectionAnalysis/term-overview.tsv");
    private static final Path MEK_AUDIT_MARKDOWN =
            Paths.get("src/main/resources/out/bhe2bsdx/term-audit/mek/audit.md");
    private static final Path OUTPUT_DIR =
            Paths.get("src/main/resources/out/bhe2bsdx/term-audit/migration-matrix");

    @Test
    void buildTermMigrationMatrixReports() throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        Map<String, Integer> bhePathSummary = readPathSummary(BHE_WAZ_PATH_SUMMARY);
        Map<String, Integer> bsdxPathSummary = readPathSummary(BSDX_WAZ_PATH_SUMMARY);
        assertFalse(bhePathSummary.isEmpty(), "BHE WAZ term 审计汇总为空");
        assertFalse(bsdxPathSummary.isEmpty(), "BSDX WAZ term 审计汇总为空");

        List<OccurrenceRow> bheOccurrences = readOccurrences(BHE_WAZ_OCCURRENCES);
        List<OccurrenceRow> bsdxOccurrences = readOccurrences(BSDX_WAZ_OCCURRENCES);
        TermCatalog bheTerm = readTermCatalog(BHE_TERM_OVERVIEW);
        TermCatalog bsdxTerm = readTermCatalog(BSDX_TERM_OVERVIEW);

        MatrixStats stats = buildStats(bhePathSummary, bsdxPathSummary);
        writeSyntaxPathMatrix(bhePathSummary, bsdxPathSummary);
        writeBheOnlyPathSamples(bhePathSummary, bsdxPathSummary, bheOccurrences);
        writeGroupMatrix(bheTerm, bsdxTerm);
        writeItemDiff(bheTerm, bsdxTerm);
        writeAuxListProbe("bhe-aux-list-probe.tsv", "BHE", bheOccurrences, bheTerm, bsdxTerm);
        writeAuxListProbe("bsdx-aux-list-probe.tsv", "BSDX", bsdxOccurrences, bsdxTerm, bsdxTerm);
        writeRuleCandidates(bhePathSummary, bsdxPathSummary);
        writeSummary(stats, bheTerm, bsdxTerm);
    }

    private MatrixStats buildStats(Map<String, Integer> bhePathSummary, Map<String, Integer> bsdxPathSummary) {
        int commonUnique = 0;
        int bheOnlyUnique = 0;
        int bsdxOnlyUnique = 0;
        int bheOnlyTotal = 0;
        int bsdxOnlyTotal = 0;

        for (Map.Entry<String, Integer> entry : bhePathSummary.entrySet()) {
            if (bsdxPathSummary.containsKey(entry.getKey())) {
                commonUnique++;
            } else {
                bheOnlyUnique++;
                bheOnlyTotal += entry.getValue();
            }
        }
        for (Map.Entry<String, Integer> entry : bsdxPathSummary.entrySet()) {
            if (!bhePathSummary.containsKey(entry.getKey())) {
                bsdxOnlyUnique++;
                bsdxOnlyTotal += entry.getValue();
            }
        }

        return new MatrixStats(
                sumCounts(bhePathSummary),
                bhePathSummary.size(),
                sumCounts(bsdxPathSummary),
                bsdxPathSummary.size(),
                commonUnique,
                bheOnlyUnique,
                bheOnlyTotal,
                bsdxOnlyUnique,
                bsdxOnlyTotal
        );
    }

    private void writeSyntaxPathMatrix(
            Map<String, Integer> bhePathSummary,
            Map<String, Integer> bsdxPathSummary
    ) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(bhePathSummary.keySet());
        keys.addAll(bsdxPathSummary.keySet());

        List<String> rows = new ArrayList<>(keys);
        rows.sort(Comparator
                .comparing((String key) -> migrationStatus(bhePathSummary, bsdxPathSummary, key))
                .thenComparing(Comparator.comparingInt((String key) ->
                        Math.max(bhePathSummary.getOrDefault(key, 0), bsdxPathSummary.getOrDefault(key, 0))).reversed())
                .thenComparing(String::compareTo));

        StringBuilder sb = new StringBuilder();
        sb.append("syntaxPathByDescription\tbheWazCount\tbsdxWazCount\tfirstGroup\tstatus\tcandidateAction\n");
        for (String key : rows) {
            String status = migrationStatus(bhePathSummary, bsdxPathSummary, key);
            sb.append(tsv(key)).append('\t')
                    .append(bhePathSummary.getOrDefault(key, 0)).append('\t')
                    .append(bsdxPathSummary.getOrDefault(key, 0)).append('\t')
                    .append(tsv(firstGroup(key))).append('\t')
                    .append(status).append('\t')
                    .append(tsv(candidateActionForStatus(status))).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve("syntax-path-migration-matrix.tsv"), sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeBheOnlyPathSamples(
            Map<String, Integer> bhePathSummary,
            Map<String, Integer> bsdxPathSummary,
            List<OccurrenceRow> bheOccurrences
    ) throws IOException {
        Map<String, OccurrenceRow> firstSampleByPath = new LinkedHashMap<>();
        for (OccurrenceRow row : bheOccurrences) {
            firstSampleByPath.putIfAbsent(row.syntaxPathByDescription(), row);
        }

        List<String> bheOnlyPaths = new ArrayList<>();
        for (String path : bhePathSummary.keySet()) {
            if (!bsdxPathSummary.containsKey(path)) {
                bheOnlyPaths.add(path);
            }
        }
        bheOnlyPaths.sort(Comparator
                .comparingInt((String path) -> bhePathSummary.getOrDefault(path, 0)).reversed()
                .thenComparing(String::compareTo));

        StringBuilder sb = new StringBuilder();
        sb.append("syntaxPathByDescription\tcount\tcategory\tcandidateAction\tsampleFile\tsamplePath\tsampleInt1\tsampleTypeList\tsampleParamList\tsampleIntList3\tsampleIntList4\tsampleInt2\n");
        for (String path : bheOnlyPaths) {
            OccurrenceRow sample = firstSampleByPath.get(path);
            sb.append(tsv(path)).append('\t')
                    .append(bhePathSummary.getOrDefault(path, 0)).append('\t')
                    .append(tsv(classifyBheOnlyPath(path))).append('\t')
                    .append(tsv(candidateActionForBheOnlyPath(path))).append('\t');
            if (sample == null) {
                sb.append("\t\t\t\t\t\t\n");
            } else {
                sb.append(tsv(sample.file())).append('\t')
                        .append(tsv(sample.objectPath())).append('\t')
                        .append(tsv(sample.int1())).append('\t')
                        .append(tsv(sample.typeList())).append('\t')
                        .append(tsv(sample.paramList())).append('\t')
                        .append(tsv(sample.intList3())).append('\t')
                        .append(tsv(sample.intList4())).append('\t')
                        .append(tsv(sample.int2())).append('\n');
            }
        }
        Files.writeString(OUTPUT_DIR.resolve("bhe-only-path-rule-candidates.tsv"), sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeGroupMatrix(TermCatalog bheTerm, TermCatalog bsdxTerm) throws IOException {
        Set<String> groupCodes = new LinkedHashSet<>();
        groupCodes.addAll(bheTerm.groupsByCode.keySet());
        groupCodes.addAll(bsdxTerm.groupsByCode.keySet());
        List<String> sortedCodes = new ArrayList<>(groupCodes);
        sortedCodes.sort(String::compareTo);

        StringBuilder sb = new StringBuilder();
        sb.append("groupCodeName\tbheGroupIndex\tbsdxGroupIndex\tbheItemCount\tbsdxItemCount\tstatus\n");
        for (String groupCode : sortedCodes) {
            TermGroupRow bheGroup = bheTerm.groupsByCode.get(groupCode);
            TermGroupRow bsdxGroup = bsdxTerm.groupsByCode.get(groupCode);
            sb.append(tsv(groupCode)).append('\t')
                    .append(bheGroup == null ? "" : bheGroup.groupIndex()).append('\t')
                    .append(bsdxGroup == null ? "" : bsdxGroup.groupIndex()).append('\t')
                    .append(bheTerm.itemsByGroupCode.getOrDefault(groupCode, List.of()).size()).append('\t')
                    .append(bsdxTerm.itemsByGroupCode.getOrDefault(groupCode, List.of()).size()).append('\t')
                    .append(groupStatus(bheGroup, bsdxGroup)).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve("term-group-migration-matrix.tsv"), sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeItemDiff(TermCatalog bheTerm, TermCatalog bsdxTerm) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(bheTerm.itemsByGroupAndDescription.keySet());
        keys.addAll(bsdxTerm.itemsByGroupAndDescription.keySet());
        List<String> sortedKeys = new ArrayList<>(keys);
        sortedKeys.sort(String::compareTo);

        StringBuilder sb = new StringBuilder();
        sb.append("groupCodeName\titemDescription\tbheGroupIndex\tbheItemIndex\tbheParam1\tbheParam2\tbsdxGroupIndex\tbsdxItemIndex\tbsdxParam1\tbsdxParam2\tstatus\n");
        for (String key : sortedKeys) {
            TermItemRow bheItem = bheTerm.itemsByGroupAndDescription.get(key);
            TermItemRow bsdxItem = bsdxTerm.itemsByGroupAndDescription.get(key);
            TermItemRow display = bheItem != null ? bheItem : bsdxItem;
            sb.append(tsv(display.groupCodeName())).append('\t')
                    .append(tsv(display.itemDescription())).append('\t')
                    .append(bheItem == null ? "" : bheItem.groupIndex()).append('\t')
                    .append(bheItem == null ? "" : bheItem.itemIndex()).append('\t')
                    .append(bheItem == null ? "" : bheItem.param1()).append('\t')
                    .append(bheItem == null ? "" : bheItem.param2()).append('\t')
                    .append(bsdxItem == null ? "" : bsdxItem.groupIndex()).append('\t')
                    .append(bsdxItem == null ? "" : bsdxItem.itemIndex()).append('\t')
                    .append(bsdxItem == null ? "" : bsdxItem.param1()).append('\t')
                    .append(bsdxItem == null ? "" : bsdxItem.param2()).append('\t')
                    .append(itemStatus(bheItem, bsdxItem)).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve("term-item-migration-diff.tsv"), sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeAuxListProbe(
            String fileName,
            String side,
            List<OccurrenceRow> rows,
            TermCatalog sourceTerm,
            TermCatalog bsdxTerm
    ) throws IOException {
        Map<AuxProbeKey, Integer> counts = new LinkedHashMap<>();
        for (OccurrenceRow row : rows) {
            recordAuxProbe(counts, side, "intList3", row);
            recordAuxProbe(counts, side, "intList4", row);
        }

        List<Map.Entry<AuxProbeKey, Integer>> sortedRows = new ArrayList<>(counts.entrySet());
        sortedRows.sort(Comparator
                .comparingInt((Map.Entry<AuxProbeKey, Integer> entry) -> entry.getValue()).reversed()
                .thenComparing(entry -> entry.getKey().syntaxPath())
                .thenComparing(entry -> entry.getKey().listName())
                .thenComparingInt(entry -> entry.getKey().firstValue()));

        StringBuilder sb = new StringBuilder();
        sb.append("side\tlistName\tsyntaxPathByDescription\tfirstValue\tsecondValue\tcount\tsourceObjectPosDescription\tsourceObjectPos2Description\tbsdxObjectPosIndexByObjectPosDescription\tbsdxObjectPos2IndexByObjectPos2Description\tnote\n");
        for (Map.Entry<AuxProbeKey, Integer> entry : sortedRows) {
            AuxProbeKey key = entry.getKey();
            TermItemRow sourceObjectPos = sourceTerm.objectPosByIndex.get(key.firstValue());
            TermItemRow sourceObjectPos2 = sourceTerm.objectPos2ByIndex.get(key.firstValue());
            TermItemRow bsdxObjectPos = sourceObjectPos == null ? null :
                    bsdxTerm.objectPosByDescription.get(sourceObjectPos.itemDescription());
            TermItemRow bsdxObjectPos2 = sourceObjectPos2 == null ? null :
                    bsdxTerm.objectPos2ByDescription.get(sourceObjectPos2.itemDescription());

            sb.append(key.side()).append('\t')
                    .append(key.listName()).append('\t')
                    .append(tsv(key.syntaxPath())).append('\t')
                    .append(key.firstValue()).append('\t')
                    .append(key.secondValue()).append('\t')
                    .append(entry.getValue()).append('\t')
                    .append(tsv(sourceObjectPos == null ? "" : sourceObjectPos.itemDescription())).append('\t')
                    .append(tsv(sourceObjectPos2 == null ? "" : sourceObjectPos2.itemDescription())).append('\t')
                    .append(bsdxObjectPos == null ? "" : bsdxObjectPos.itemIndex()).append('\t')
                    .append(bsdxObjectPos2 == null ? "" : bsdxObjectPos2.itemIndex()).append('\t')
                    .append(tsv(auxProbeNote(key, sourceObjectPos, sourceObjectPos2, bsdxObjectPos, bsdxObjectPos2))).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve(fileName), sb.toString(), StandardCharsets.UTF_8);
    }

    private void recordAuxProbe(Map<AuxProbeKey, Integer> counts, String side, String listName, OccurrenceRow row) {
        List<Integer> values = parseIntList("intList3".equals(listName) ? row.intList3() : row.intList4());
        if (values.isEmpty()) {
            return;
        }
        int secondValue = values.size() > 1 ? values.get(1) : Integer.MIN_VALUE;
        AuxProbeKey key = new AuxProbeKey(side, listName, row.syntaxPathByDescription(), values.get(0), secondValue);
        counts.merge(key, 1, Integer::sum);
    }

    private void writeRuleCandidates(
            Map<String, Integer> bhePathSummary,
            Map<String, Integer> bsdxPathSummary
    ) throws IOException {
        List<String> bheOnlyPaths = new ArrayList<>();
        for (String path : bhePathSummary.keySet()) {
            if (!bsdxPathSummary.containsKey(path)) {
                bheOnlyPaths.add(path);
            }
        }
        bheOnlyPaths.sort(Comparator
                .comparing((String path) -> classifyBheOnlyPath(path))
                .thenComparing(Comparator.comparingInt((String path) -> bhePathSummary.getOrDefault(path, 0)).reversed())
                .thenComparing(String::compareTo));

        StringBuilder sb = new StringBuilder();
        sb.append("category\tsyntaxPathByDescription\tcount\tcandidateAction\n");
        for (String path : bheOnlyPaths) {
            sb.append(tsv(classifyBheOnlyPath(path))).append('\t')
                    .append(tsv(path)).append('\t')
                    .append(bhePathSummary.getOrDefault(path, 0)).append('\t')
                    .append(tsv(candidateActionForBheOnlyPath(path))).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve("converter-rule-candidate-matrix.tsv"), sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeSummary(MatrixStats stats, TermCatalog bheTerm, TermCatalog bsdxTerm) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# Term migration matrix audit\n\n");
        sb.append("## WAZ path coverage\n\n");
        sb.append("- BHE WAZ occurrences: ").append(stats.bheTotal()).append('\n');
        sb.append("- BHE WAZ unique syntax paths: ").append(stats.bheUnique()).append('\n');
        sb.append("- BSDX WAZ occurrences: ").append(stats.bsdxTotal()).append('\n');
        sb.append("- BSDX WAZ unique syntax paths: ").append(stats.bsdxUnique()).append('\n');
        sb.append("- Shared syntax paths: ").append(stats.commonUnique()).append('\n');
        sb.append("- BHE-only syntax paths: ").append(stats.bheOnlyUnique())
                .append(" / occurrences ").append(stats.bheOnlyTotal()).append('\n');
        sb.append("- BSDX-only syntax paths: ").append(stats.bsdxOnlyUnique())
                .append(" / occurrences ").append(stats.bsdxOnlyTotal()).append('\n');

        sb.append("\n## Term group coverage\n\n");
        sb.append("- BHE term groups: ").append(bheTerm.groupsByCode.size()).append('\n');
        sb.append("- BSDX term groups: ").append(bsdxTerm.groupsByCode.size()).append('\n');
        sb.append("- BHE-only groups: ").append(String.join(", ", onlyLeft(bheTerm.groupsByCode.keySet(), bsdxTerm.groupsByCode.keySet()))).append('\n');
        sb.append("- BSDX-only groups: ").append(String.join(", ", onlyLeft(bsdxTerm.groupsByCode.keySet(), bheTerm.groupsByCode.keySet()))).append('\n');

        sb.append("\n## MEK audit bridge\n\n");
        if (Files.exists(MEK_AUDIT_MARKDOWN)) {
            sb.append(Files.readString(MEK_AUDIT_MARKDOWN, StandardCharsets.UTF_8)).append('\n');
        } else {
            sb.append("- MEK audit markdown not found. Run `MekInfoCollectionTermAuditTest` before reviewing MEK numbers.\n");
        }

        sb.append("\n## Review rule\n\n");
        sb.append("- Shared paths should be recompiled by `groupCodeName + itemDescription`, not copied by source index.\n");
        sb.append("- BHE-only paths must enter an explicit rule bucket: equivalent rewrite, documented approximation, or fail.\n");
        sb.append("- Aux lists are probes only; `intList3/intList4` must be interpreted by syntax path and host event, not by a global field rule.\n");
        sb.append("- Missing rules must fail fast with file/path/raw collection information.\n");

        Files.writeString(OUTPUT_DIR.resolve("summary.md"), sb.toString(), StandardCharsets.UTF_8);
    }

    private Map<String, Integer> readPathSummary(Path path) throws IOException {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Map<String, String> row : readTsv(path)) {
            result.put(pathKey(row.get("syntaxPathByDescription")), Integer.parseInt(row.get("count")));
        }
        return result;
    }

    private List<OccurrenceRow> readOccurrences(Path path) throws IOException {
        List<OccurrenceRow> result = new ArrayList<>();
        for (Map<String, String> row : readTsv(path)) {
            result.add(new OccurrenceRow(
                    row.get("file"),
                    row.get("path"),
                    row.get("int1"),
                    row.get("typeList"),
                    row.get("trailingTypeList"),
                    row.get("paramList"),
                    row.get("intList3"),
                    row.get("intList4"),
                    row.get("int2"),
                    pathKey(row.get("syntaxPathByDescription"))
            ));
        }
        return result;
    }

    private TermCatalog readTermCatalog(Path path) throws IOException {
        TermCatalog catalog = new TermCatalog();
        for (Map<String, String> row : readTsv(path)) {
            TermGroupRow group = new TermGroupRow(
                    Integer.parseInt(row.get("groupIndex")),
                    row.get("termGroupCodeName")
            );
            catalog.groupsByCode.putIfAbsent(group.groupCodeName(), group);

            TermItemRow item = new TermItemRow(
                    Integer.parseInt(row.get("groupIndex")),
                    row.get("termGroupCodeName"),
                    Integer.parseInt(row.get("itemIndex")),
                    row.get("termItemDescription"),
                    parseInteger(row.get("param1")),
                    parseInteger(row.get("param2"))
            );
            catalog.itemsByGroupCode.computeIfAbsent(item.groupCodeName(), key -> new ArrayList<>()).add(item);
            catalog.itemsByGroupAndDescription.put(item.groupCodeName() + "/" + item.itemDescription(), item);
            if ("OBJECTPOS".equals(item.groupCodeName())) {
                catalog.objectPosByIndex.put(item.itemIndex(), item);
                catalog.objectPosByDescription.put(item.itemDescription(), item);
            } else if ("OBJECTPOS2".equals(item.groupCodeName())) {
                catalog.objectPos2ByIndex.put(item.itemIndex(), item);
                catalog.objectPos2ByDescription.put(item.itemDescription(), item);
            }
        }
        return catalog;
    }

    private List<Map<String, String>> readTsv(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            return List.of();
        }
        String[] headers = lines.get(0).split("\t", -1);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String[] values = lines.get(i).split("\t", -1);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.length; j++) {
                row.put(headers[j], j < values.length ? values[j] : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private String migrationStatus(Map<String, Integer> bhe, Map<String, Integer> bsdx, String key) {
        boolean hasBhe = bhe.containsKey(key);
        boolean hasBsdx = bsdx.containsKey(key);
        if (hasBhe && hasBsdx) {
            return "SHARED";
        }
        if (hasBhe) {
            return "BHE_ONLY";
        }
        return "BSDX_ONLY";
    }

    private String candidateActionForStatus(String status) {
        return switch (status) {
            case "SHARED" -> "按语义路径重编译到 BSDX term index";
            case "BHE_ONLY" -> "必须进入显式规则：等价重写、近似重写或 fail";
            case "BSDX_ONLY" -> "仅作为目标侧参考，不从 BHE 主动生成";
            default -> "未知状态直接 fail";
        };
    }

    private String classifyBheOnlyPath(String path) {
        if (path.contains("OPERATOR_TWO") || path.contains("OPERATOR/")) {
            return "PARAM_OPERATOR_CHAIN";
        }
        if (path.matches(".*PARAMCOUNT[1-4].*")) {
            return "PARAMCOUNT_SPLIT";
        }
        if (path.contains("MULTILOCK")) {
            return "MULTILOCK_SELECTOR";
        }
        if (path.contains("HIT_WAZA")) {
            return "HIT_WAZA_SELECTOR";
        }
        if (path.contains("OBJECT1/ATTR") || path.contains("OBJECT1/FLAG")) {
            return "ATTR_FLAG_SELECTOR";
        }
        if (path.contains("ACTION_")) {
            return "ACTION_SELECTOR";
        }
        return "BHE_ONLY_REVIEW";
    }

    private String candidateActionForBheOnlyPath(String path) {
        return switch (classifyBheOnlyPath(path)) {
            case "PARAM_OPERATOR_CHAIN" -> "审计是否能折叠到 BSDX 直接 PARAM/POS；不能折叠则 fail";
            case "PARAMCOUNT_SPLIT" -> "候选等价重写：PARAMCOUNT1/2/3/4 -> PARAMCOUNT，保留 compare/param";
            case "MULTILOCK_SELECTOR" -> "BHE 独有多锁定选择器，必须按出现上下文定显式规则或 fail";
            case "HIT_WAZA_SELECTOR" -> "BHE 独有命中技选择器，必须按出现上下文定显式规则或 fail";
            case "ATTR_FLAG_SELECTOR" -> "BHE 独有属性/flag 分支，必须按实际用途定显式规则或 fail";
            case "ACTION_SELECTOR" -> "BHE 独有动作分支，必须按 BSDX 可承载动作定显式规则或 fail";
            default -> "没有候选规则，进入保护性失败";
        };
    }

    private String auxProbeNote(
            AuxProbeKey key,
            TermItemRow sourceObjectPos,
            TermItemRow sourceObjectPos2,
            TermItemRow bsdxObjectPos,
            TermItemRow bsdxObjectPos2
    ) {
        /*
         * ANGLE/OBJECT 是本阶段确认出的反例：
         * 它的 intList4 会出现可以按 OBJECTPOS 命中的值，不能先用 OBJECTPOS2 判定风险。
         */
        if (key.syntaxPath().startsWith("ANGLE/OBJECT")) {
            return "ANGLE/OBJECT 的 aux list 不能全局硬套 OBJECTPOS2，需要按角度源语义解释";
        }
        if ("intList3".equals(key.listName())) {
            if (sourceObjectPos == null) {
                return "intList3 按 OBJECTPOS 无法解析，必须按宿主语义单独审计";
            }
            if (bsdxObjectPos == null) {
                return "intList3 按 OBJECTPOS 解释时 BSDX 缺少同名语义，不能默认 fallback";
            }
            return "intList3 可作为 OBJECTPOS 候选映射，仍需由 syntaxPath 决定最终含义";
        }
        if ("intList4".equals(key.listName())) {
            if (sourceObjectPos2 == null) {
                return "intList4 按 OBJECTPOS2 无法解析，必须按宿主语义单独审计";
            }
            if (bsdxObjectPos2 == null) {
                return "intList4 按 OBJECTPOS2 解释时 BSDX 缺少同名语义，不能默认 fallback";
            }
            return "intList4 可作为 OBJECTPOS2 候选映射，仍需由 syntaxPath 决定最终含义";
        }
        return "未知 aux list，必须按宿主语义单独审计";
    }

    private String groupStatus(TermGroupRow bheGroup, TermGroupRow bsdxGroup) {
        if (bheGroup != null && bsdxGroup != null) {
            return "SHARED_GROUP";
        }
        if (bheGroup != null) {
            return "BHE_ONLY_GROUP";
        }
        return "BSDX_ONLY_GROUP";
    }

    private String itemStatus(TermItemRow bheItem, TermItemRow bsdxItem) {
        if (bheItem != null && bsdxItem != null) {
            if (bheItem.groupIndex() == bsdxItem.groupIndex()
                    && bheItem.itemIndex() == bsdxItem.itemIndex()
                    && safeEquals(bheItem.param1(), bsdxItem.param1())
                    && safeEquals(bheItem.param2(), bsdxItem.param2())) {
                return "SAME_INDEX_AND_PARAM";
            }
            return "SHARED_DESCRIPTION_REINDEX_OR_PARAM_DIFF";
        }
        if (bheItem != null) {
            return "BHE_ONLY_ITEM";
        }
        return "BSDX_ONLY_ITEM";
    }

    private List<Integer> parseIntList(String value) {
        if (value == null || value.isBlank() || "[]".equals(value)) {
            return List.of();
        }
        String body = value.trim();
        if (body.startsWith("[") && body.endsWith("]")) {
            body = body.substring(1, body.length() - 1);
        }
        if (body.isBlank()) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>();
        for (String part : body.split(",")) {
            String normalized = part.trim();
            if (!normalized.isEmpty()) {
                result.add(Integer.parseInt(normalized));
            }
        }
        return result;
    }

    private Integer parseInteger(String value) {
        return value == null || value.isBlank() ? null : Integer.parseInt(value);
    }

    private List<String> onlyLeft(Set<String> left, Set<String> right) {
        List<String> result = new ArrayList<>();
        for (String value : left) {
            if (!right.contains(value)) {
                result.add(value);
            }
        }
        result.sort(String::compareTo);
        return result;
    }

    private String firstGroup(String syntaxPath) {
        int slash = syntaxPath.indexOf('/');
        return slash < 0 ? syntaxPath : syntaxPath.substring(0, slash);
    }

    private int sumCounts(Map<String, Integer> counts) {
        int total = 0;
        for (Integer value : counts.values()) {
            total += value;
        }
        return total;
    }

    private boolean safeEquals(Integer left, Integer right) {
        return left == null ? right == null : left.equals(right);
    }

    private String pathKey(String value) {
        return value == null || value.isBlank() ? "<EMPTY>" : value;
    }

    private String tsv(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value)
                .replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    private record MatrixStats(
            int bheTotal,
            int bheUnique,
            int bsdxTotal,
            int bsdxUnique,
            int commonUnique,
            int bheOnlyUnique,
            int bheOnlyTotal,
            int bsdxOnlyUnique,
            int bsdxOnlyTotal
    ) {
    }

    private static class TermCatalog {
        private final Map<String, TermGroupRow> groupsByCode = new LinkedHashMap<>();
        private final Map<String, List<TermItemRow>> itemsByGroupCode = new LinkedHashMap<>();
        private final Map<String, TermItemRow> itemsByGroupAndDescription = new LinkedHashMap<>();
        private final Map<Integer, TermItemRow> objectPosByIndex = new LinkedHashMap<>();
        private final Map<String, TermItemRow> objectPosByDescription = new LinkedHashMap<>();
        private final Map<Integer, TermItemRow> objectPos2ByIndex = new LinkedHashMap<>();
        private final Map<String, TermItemRow> objectPos2ByDescription = new LinkedHashMap<>();
    }

    private record TermGroupRow(
            int groupIndex,
            String groupCodeName
    ) {
    }

    private record TermItemRow(
            int groupIndex,
            String groupCodeName,
            int itemIndex,
            String itemDescription,
            Integer param1,
            Integer param2
    ) {
    }

    private record OccurrenceRow(
            String file,
            String objectPath,
            String int1,
            String typeList,
            String trailingTypeList,
            String paramList,
            String intList3,
            String intList4,
            String int2,
            String syntaxPathByDescription
    ) {
    }

    private record AuxProbeKey(
            String side,
            String listName,
            String syntaxPath,
            int firstValue,
            int secondValue
    ) {
    }
}
