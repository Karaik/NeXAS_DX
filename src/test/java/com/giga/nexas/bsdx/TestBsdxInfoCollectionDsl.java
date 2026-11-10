package com.giga.nexas.bsdx;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionAnalyzer;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionSemantic;
import com.giga.nexas.dto.bsdx.grp.groupmap.TermGrp;
import com.giga.nexas.dto.bsdx.waz.Waz;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestBsdxInfoCollectionDsl {

    private static final Path WAZ_BSDX_JSON_DIR = Paths.get("src/main/resources/wazBsdxJson");
    private static final Path ANALYSIS_RESOURCE_DIR = Paths.get("src/main/resources/bsdxInfoCollectionAnalysis");
    private static final Path OCCURRENCE_TSV_OUT = ANALYSIS_RESOURCE_DIR.resolve("bsdx-info-collection-occurrences.tsv");
    private static final Path PATH_SUMMARY_TSV_OUT = ANALYSIS_RESOURCE_DIR.resolve("bsdx-info-collection-path-summary.tsv");
    private static final Path TERM_OVERVIEW_OUT = ANALYSIS_RESOURCE_DIR.resolve("term-overview.tsv");
    private static final Path ANALYSIS_JSON_DIR = Paths.get("src/main/resources/wazBsdxSemanticJson");

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void testAnalyzeBsdxInfoCollectionDsl() throws IOException {
        Files.createDirectories(ANALYSIS_RESOURCE_DIR);
        Files.createDirectories(ANALYSIS_JSON_DIR);

        TermGrp termGrp = BsdxInfoCollectionAnalyzer.getCachedTermGrp();
        List<Occurrence> occurrences = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(WAZ_BSDX_JSON_DIR, "*.json")) {
            for (Path path : stream) {
                Waz waz = mapper.readValue(path.toFile(), Waz.class);
                IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
                visitObjectGraph(waz, "$", path.getFileName().toString(), visited, occurrences);
            }
        }

        occurrences.sort(Comparator
                .comparing((Occurrence o) -> o.fileName)
                .thenComparing(o -> o.objectPath));

        writeOccurrenceTsv(occurrences);
        writePathSummaryTsv(occurrences);
        writePerFileAnalysisJson(occurrences);
        writeTermOverviewTsv(termGrp);
    }

    private void visitObjectGraph(
            Object node,
            String path,
            String fileName,
            IdentityHashMap<Object, Boolean> visited,
            List<Occurrence> occurrences
    ) {
        if (node == null || isLeafValue(node)) {
            return;
        }
        if (visited.put(node, Boolean.TRUE) != null) {
            return;
        }

        if (node instanceof BsdxInfoCollection collection) {
            BsdxInfoCollectionSemantic semantic = BsdxInfoCollectionAnalyzer.analyze(collection);
            occurrences.add(toOccurrence(fileName, path, collection, semantic));
            return;
        }

        if (node instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                visitObjectGraph(list.get(i), path + "[" + i + "]", fileName, visited, occurrences);
            }
            return;
        }

        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                visitObjectGraph(entry.getValue(), path + "." + entry.getKey(), fileName, visited, occurrences);
            }
            return;
        }

        for (Field field : getAllFields(node.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(node);
                if (value != null) {
                    visitObjectGraph(value, path + "." + field.getName(), fileName, visited, occurrences);
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException("failed to read field: " + field, e);
            }
        }
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            for (Field field : cursor.getDeclaredFields()) {
                fields.add(field);
            }
            cursor = cursor.getSuperclass();
        }
        return fields;
    }

    private boolean isLeafValue(Object value) {
        Class<?> type = value.getClass();
        return type.isPrimitive()
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || type.isArray()
                || type.getName().startsWith("java.time.");
    }

    private Occurrence toOccurrence(
            String fileName,
            String objectPath,
            BsdxInfoCollection collection,
            BsdxInfoCollectionSemantic semantic
    ) {
        Occurrence occurrence = new Occurrence();
        occurrence.fileName = fileName;
        occurrence.objectPath = objectPath;
        occurrence.int1 = collection.getInt1();
        occurrence.typeList = String.valueOf(collection.getTypeList());
        occurrence.paramList = String.valueOf(collection.getParamList());
        occurrence.intList3 = String.valueOf(collection.getIntList3());
        occurrence.intList4 = String.valueOf(collection.getIntList4());
        occurrence.int2 = collection.getInt2();
        occurrence.syntaxPathByName = semantic.getSyntaxPathByName();
        occurrence.syntaxPathByDescription = semantic.getSyntaxPathByDescription();
        occurrence.trailingTypeList = String.valueOf(semantic.getTrailingTypeListSnapshot());
        occurrence.terminated = semantic.isTerminated();
        occurrence.warnings = String.join(" | ", semantic.getWarnings());
        occurrence.semantic = semantic;
        return occurrence;
    }

    private void writeOccurrenceTsv(List<Occurrence> occurrences) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("file\tpath\tint1\ttypeList\ttrailingTypeList\tparamList\tintList3\tintList4\tint2\tsyntaxPathByName\tsyntaxPathByDescription\tterminated\twarnings\n");
        for (Occurrence occurrence : occurrences) {
            sb.append(tsv(occurrence.fileName)).append('\t')
                    .append(tsv(occurrence.objectPath)).append('\t')
                    .append(tsv(occurrence.int1)).append('\t')
                    .append(tsv(occurrence.typeList)).append('\t')
                    .append(tsv(occurrence.trailingTypeList)).append('\t')
                    .append(tsv(occurrence.paramList)).append('\t')
                    .append(tsv(occurrence.intList3)).append('\t')
                    .append(tsv(occurrence.intList4)).append('\t')
                    .append(tsv(occurrence.int2)).append('\t')
                    .append(tsv(occurrence.syntaxPathByName)).append('\t')
                    .append(tsv(occurrence.syntaxPathByDescription)).append('\t')
                    .append(tsv(occurrence.terminated)).append('\t')
                    .append(tsv(occurrence.warnings)).append('\n');
        }
        Files.writeString(OCCURRENCE_TSV_OUT, sb.toString(), StandardCharsets.UTF_8);
    }

    private void writePathSummaryTsv(List<Occurrence> occurrences) throws IOException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Occurrence occurrence : occurrences) {
            String key = occurrence.syntaxPathByDescription == null || occurrence.syntaxPathByDescription.isBlank()
                    ? "<EMPTY>"
                    : occurrence.syntaxPathByDescription;
            counts.merge(key, 1, Integer::sum);
        }

        List<Map.Entry<String, Integer>> rows = new ArrayList<>(counts.entrySet());
        rows.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue(), a.getValue());
            return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
        });

        StringBuilder sb = new StringBuilder();
        sb.append("syntaxPathByDescription\tcount\n");
        for (Map.Entry<String, Integer> row : rows) {
            sb.append(tsv(row.getKey())).append('\t').append(row.getValue()).append('\n');
        }
        Files.writeString(PATH_SUMMARY_TSV_OUT, sb.toString(), StandardCharsets.UTF_8);
    }

    private void writePerFileAnalysisJson(List<Occurrence> occurrences) throws IOException {
        Map<String, List<AnalysisRow>> grouped = new LinkedHashMap<>();
        for (Occurrence occurrence : occurrences) {
            AnalysisRow row = new AnalysisRow();
            row.path = occurrence.objectPath;
            row.int1 = occurrence.int1;
            row.typeList = occurrence.typeList;
            row.trailingTypeList = occurrence.trailingTypeList;
            row.paramList = occurrence.paramList;
            row.intList3 = occurrence.intList3;
            row.intList4 = occurrence.intList4;
            row.int2 = occurrence.int2;
            row.syntaxPathByName = occurrence.syntaxPathByName;
            row.syntaxPathByDescription = occurrence.syntaxPathByDescription;
            row.terminated = occurrence.terminated;
            row.warnings = occurrence.warnings;
            row.semantic = occurrence.semantic;
            grouped.computeIfAbsent(occurrence.fileName, k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<AnalysisRow>> entry : grouped.entrySet()) {
            String outName = entry.getKey().replace(".json", ".semantic.json");
            Path out = ANALYSIS_JSON_DIR.resolve(outName);
            mapper.writerWithDefaultPrettyPrinter().writeValue(out.toFile(), entry.getValue());
        }
    }

    private void writeTermOverviewTsv(TermGrp termGrp) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("groupIndex\ttermGroupCodeName\ttermGroupName\titemIndex\ttermItemDescription\ttermItemName\tparam1\tparam2\n");
        for (int groupIndex = 0; groupIndex < termGrp.getTermList().size(); groupIndex++) {
            TermGrp.TermGroup group = termGrp.getTermList().get(groupIndex);
            for (int itemIndex = 0; itemIndex < group.getTermItemList().size(); itemIndex++) {
                TermGrp.TermItem item = group.getTermItemList().get(itemIndex);
                sb.append(groupIndex).append('\t')
                        .append(tsv(group.getTermGroupCodeName())).append('\t')
                        .append(tsv(group.getTermGroupName())).append('\t')
                        .append(itemIndex).append('\t')
                        .append(tsv(item.getTermItemDescription())).append('\t')
                        .append(tsv(item.getTermItemName())).append('\t')
                        .append(tsv(item.getParam1())).append('\t')
                        .append(tsv(item.getParam2())).append('\n');
            }
        }
        Files.writeString(TERM_OVERVIEW_OUT, sb.toString(), StandardCharsets.UTF_8);
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

    private static class Occurrence {
        public String fileName;
        public String objectPath;
        public Integer int1;
        public String typeList;
        public String trailingTypeList;
        public String paramList;
        public String intList3;
        public String intList4;
        public Integer int2;
        public String syntaxPathByName;
        public String syntaxPathByDescription;
        public boolean terminated;
        public String warnings;
        public BsdxInfoCollectionSemantic semantic;
    }

    private static class AnalysisRow {
        public String path;
        public Integer int1;
        public String typeList;
        public String trailingTypeList;
        public String paramList;
        public String intList3;
        public String intList4;
        public Integer int2;
        public String syntaxPathByName;
        public String syntaxPathByDescription;
        public boolean terminated;
        public String warnings;
        public BsdxInfoCollectionSemantic semantic;
    }
}
