package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term;

import com.giga.nexas.dto.bhe.BheInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionAnalyzer;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionSemantic;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BheInfoCollectionTermConverterSemanticTest {

    private static final Path BHE_WAZ_OCCURRENCES =
            Paths.get("src/main/resources/bheInfoCollectionAnalysis/bhe-info-collection-occurrences.tsv");
    private static final Path OUTPUT_DIR =
            Paths.get("src/main/resources/out/bhe2bsdx/term-audit/converter");

    private final BheInfoCollectionTermConverter converter = new BheInfoCollectionTermConverter();

    @Test
    void convertAllAuditedBheWazCollectionsToBsdxTermSpace() throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        List<OccurrenceRow> rows = readOccurrences(BHE_WAZ_OCCURRENCES);
        assertFalse(rows.isEmpty(), "BHE WAZ InfoCollection 审计明细为空");

        List<ConvertedRow> convertedRows = new ArrayList<>();
        int warningCount = 0;
        for (OccurrenceRow row : rows) {
            BsdxInfoCollection converted = converter.convert(toBheCollection(row), row.file() + " " + row.objectPath());
            BsdxInfoCollectionSemantic semantic = BsdxInfoCollectionAnalyzer.analyze(converted);
            if (!semantic.getWarnings().isEmpty()) {
                warningCount++;
            }
            convertedRows.add(new ConvertedRow(
                    row.file(),
                    row.objectPath(),
                    row.syntaxPathByDescription(),
                    semantic.getSyntaxPathByDescription(),
                    String.valueOf(converted.getInt1()),
                    String.valueOf(converted.getTypeList()),
                    String.valueOf(converted.getParamList()),
                    String.valueOf(converted.getIntList3()),
                    String.valueOf(converted.getIntList4()),
                    String.valueOf(converted.getInt2()),
                    String.join(" | ", semantic.getWarnings())
            ));
        }

        writeConvertedRows(convertedRows);
        writeSummary(rows.size(), convertedRows, warningCount);
        assertEquals(0, warningCount, "转换后的 BSDX InfoCollection 存在 analyzer warning");
    }

    @Test
    void knownObjectPositionAuxMappingsUseHitAndNone() {
        BheInfoCollection source = new BheInfoCollection();
        source.setInt1(20);
        source.getTypeList().add(1);
        source.getIntList3().add(requireBheItemIndex("OBJECTPOS", "HIT_WAZA"));
        source.getIntList3().add(requireBheItemIndex("OBJECTPOS2", "TOP"));
        source.setInt2(0);

        BsdxInfoCollection converted = converter.convert(source, "knownObjectPositionAuxMappingsUseHitAndNone");
        assertEquals(20, converted.getInt1());
        assertEquals(List.of(1), converted.getTypeList());
        assertEquals(List.of(
                requireBsdxItemIndex("OBJECTPOS", "HIT"),
                requireBsdxItemIndex("OBJECTPOS2", "NONE")
        ), converted.getIntList3());
        assertEquals("POS/OBJECT", BsdxInfoCollectionAnalyzer.analyze(converted).getSyntaxPathByDescription());
    }

    @Test
    void invalidObjectPositionAuxIndicesFailClosed() {
        BheInfoCollection nullIndex = positionCollection();
        nullIndex.getIntList3().add(null);
        assertThrows(NullPointerException.class, () -> converter.convert(nullIndex, "null aux"));

        BheInfoCollection outOfRange = positionCollection();
        outOfRange.getIntList3().add(Integer.MAX_VALUE);
        assertThrows(IndexOutOfBoundsException.class, () -> converter.convert(outOfRange, "out-of-range aux"));

        BheInfoCollection nullHeight = positionCollection();
        nullHeight.getIntList3().add(requireBheItemIndex("OBJECTPOS", "MINE"));
        nullHeight.getIntList3().add(null);
        assertThrows(NullPointerException.class, () -> converter.convert(nullHeight, "null height aux"));
    }

    @Test
    void parameterOperatorChainCollapsesToDirectBsdxParameterPath() {
        BheInfoCollection source = new BheInfoCollection();
        source.setInt1(22);
        source.getTypeList().addAll(List.of(1, 6, 3, 0));
        source.getParamList().addAll(List.of(15, 0));
        source.getIntList3().addAll(List.of(3, 0));
        source.getIntList4().addAll(List.of(8, 0));
        source.setInt2(0);

        BsdxInfoCollection converted = converter.convert(source, "parameterOperatorChainCollapsesToDirectBsdxParameterPath");
        BsdxInfoCollectionSemantic semantic = BsdxInfoCollectionAnalyzer.analyze(converted);
        assertEquals("PARAM/POS -> PARAM_POS/HEIGHT2", semantic.getSyntaxPathByDescription());
        assertEquals(List.of(), converted.getParamList());
    }

    @Test
    void unsupportedConditionFallsBackToUnconditional() {
        BheInfoCollection source = new BheInfoCollection();
        source.setInt1(0);
        source.getTypeList().addAll(List.of(2, 9, 8, 2));
        source.getParamList().add(0);
        source.getIntList3().addAll(List.of(0, 0));
        source.setInt2(0);

        BsdxInfoCollection converted = converter.convert(source, "unsupportedConditionFallsBackToUnconditional");
        assertEquals("PARENT/UNCONDITIONAL", BsdxInfoCollectionAnalyzer.analyze(converted).getSyntaxPathByDescription());
        assertEquals(List.of(), converted.getParamList());
    }

    private BheInfoCollection positionCollection() {
        BheInfoCollection source = new BheInfoCollection();
        source.setInt1(20);
        source.getTypeList().add(1);
        source.setInt2(0);
        return source;
    }

    private int requireBheItemIndex(String groupCodeName, String itemDescription) {
        var termGrp = com.giga.nexas.dto.bhe.BheInfoCollectionAnalyzer.getCachedTermGrp();
        for (var group : termGrp.getTermList()) {
            if (!groupCodeName.equals(group.getTermGroupCodeName())) {
                continue;
            }
            for (int i = 0; i < group.getTermItemList().size(); i++) {
                if (itemDescription.equals(group.getTermItemList().get(i).getTermItemDescription())) {
                    return i;
                }
            }
        }
        throw new AssertionError("BHE term 缺少 " + groupCodeName + "/" + itemDescription);
    }

    private int requireBsdxItemIndex(String groupCodeName, String itemDescription) {
        var termGrp = BsdxInfoCollectionAnalyzer.getCachedTermGrp();
        for (var group : termGrp.getTermList()) {
            if (!groupCodeName.equals(group.getTermGroupCodeName())) {
                continue;
            }
            for (int i = 0; i < group.getTermItemList().size(); i++) {
                if (itemDescription.equals(group.getTermItemList().get(i).getTermItemDescription())) {
                    return i;
                }
            }
        }
        throw new AssertionError("BSDX term 缺少 " + groupCodeName + "/" + itemDescription);
    }

    private void writeConvertedRows(List<ConvertedRow> rows) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("file\tpath\tsourceSyntaxPath\ttargetSyntaxPath\ttargetInt1\ttargetTypeList\ttargetParamList\ttargetIntList3\ttargetIntList4\ttargetInt2\twarnings\n");
        for (ConvertedRow row : rows) {
            sb.append(tsv(row.file())).append('\t')
                    .append(tsv(row.objectPath())).append('\t')
                    .append(tsv(row.sourceSyntaxPath())).append('\t')
                    .append(tsv(row.targetSyntaxPath())).append('\t')
                    .append(tsv(row.targetInt1())).append('\t')
                    .append(tsv(row.targetTypeList())).append('\t')
                    .append(tsv(row.targetParamList())).append('\t')
                    .append(tsv(row.targetIntList3())).append('\t')
                    .append(tsv(row.targetIntList4())).append('\t')
                    .append(tsv(row.targetInt2())).append('\t')
                    .append(tsv(row.warnings())).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve("bhe-waz-term-conversion-details.tsv"), sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeSummary(int sourceCount, List<ConvertedRow> rows, int warningCount) throws IOException {
        Map<String, Integer> targetPathCounts = new LinkedHashMap<>();
        Map<String, Integer> changedPathCounts = new LinkedHashMap<>();
        for (ConvertedRow row : rows) {
            targetPathCounts.merge(row.targetSyntaxPath(), 1, Integer::sum);
            if (!row.sourceSyntaxPath().equals(row.targetSyntaxPath())) {
                changedPathCounts.merge(row.sourceSyntaxPath() + " => " + row.targetSyntaxPath(), 1, Integer::sum);
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# BHE WAZ term conversion audit\n\n");
        sb.append("- Source collection count: ").append(sourceCount).append('\n');
        sb.append("- Converted collection count: ").append(rows.size()).append('\n');
        sb.append("- BSDX analyzer warning rows: ").append(warningCount).append('\n');
        sb.append("- Unique target syntax paths: ").append(targetPathCounts.size()).append('\n');
        sb.append("- Changed syntax mappings: ").append(changedPathCounts.size()).append('\n');
        Files.writeString(OUTPUT_DIR.resolve("summary.md"), sb.toString(), StandardCharsets.UTF_8);
    }

    private List<OccurrenceRow> readOccurrences(Path path) throws IOException {
        List<Map<String, String>> rows = readTsv(path);
        List<OccurrenceRow> result = new ArrayList<>();
        for (Map<String, String> row : rows) {
            result.add(new OccurrenceRow(
                    row.get("file"),
                    row.get("path"),
                    row.get("int1"),
                    row.get("typeList"),
                    row.get("paramList"),
                    row.get("intList3"),
                    row.get("intList4"),
                    row.get("int2"),
                    row.get("syntaxPathByDescription")
            ));
        }
        return result;
    }

    private List<Map<String, String>> readTsv(Path path) throws IOException {
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
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

    private BheInfoCollection toBheCollection(OccurrenceRow row) {
        BheInfoCollection collection = new BheInfoCollection();
        collection.setInt1(Integer.parseInt(row.int1()));
        collection.setTypeList(parseIntList(row.typeList()));
        collection.setParamList(parseIntList(row.paramList()));
        collection.setIntList3(parseIntList(row.intList3()));
        collection.setIntList4(parseIntList(row.intList4()));
        collection.setInt2(Integer.parseInt(row.int2()));
        return collection;
    }

    private List<Integer> parseIntList(String value) {
        if (value == null || value.isBlank() || "[]".equals(value)) {
            return new ArrayList<>();
        }
        String body = value.trim();
        if (body.startsWith("[") && body.endsWith("]")) {
            body = body.substring(1, body.length() - 1);
        }
        List<Integer> result = new ArrayList<>();
        if (body.isBlank()) {
            return result;
        }
        for (String part : body.split(",")) {
            String normalized = part.trim();
            if (!normalized.isEmpty()) {
                result.add(Integer.parseInt(normalized));
            }
        }
        return result;
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

    private record OccurrenceRow(
            String file,
            String objectPath,
            String int1,
            String typeList,
            String paramList,
            String intList3,
            String intList4,
            String int2,
            String syntaxPathByDescription
    ) {
    }

    private record ConvertedRow(
            String file,
            String objectPath,
            String sourceSyntaxPath,
            String targetSyntaxPath,
            String targetInt1,
            String targetTypeList,
            String targetParamList,
            String targetIntList3,
            String targetIntList4,
            String targetInt2,
            String warnings
    ) {
    }
}
