package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bhe.BheInfoCollection;
import com.giga.nexas.dto.bhe.BheInfoCollectionAnalyzer;
import com.giga.nexas.dto.bhe.BheInfoCollectionSemantic;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionAnalyzer;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionSemantic;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 全量审计 MEK/AI 内的 InfoCollection term 形态。
 *
 * <p>公共资源 term 转换不能只看 WAZ，因为 MEK 的 AI 里也会持有 InfoCollection。
 * 这个测试只做审计输出，不参与正式迁移流程，用来确认 MEK/AI 是否引入了 WAZ 审计之外的新 term 语义形态。</p>
 */
class MekInfoCollectionTermAuditTest {

    private static final Path BHE_MEK_JSON_DIR = Paths.get("src/main/resources/mekBheJson");
    private static final Path BSDX_MEK_JSON_DIR = Paths.get("src/main/resources/mekBsdxJson");
    /**
     * WAZ 审计结果是 term 转换设计的主依据；MEK 审计要和 WAZ 审计交叉比对，
     * 防止 MEK/AI 隐藏了单独的 term 方言。
     */
    private static final Path BHE_WAZ_PATH_SUMMARY =
            Paths.get("src/main/resources/bheInfoCollectionAnalysis/bhe-info-collection-path-summary.tsv");
    private static final Path BSDX_WAZ_PATH_SUMMARY =
            Paths.get("src/main/resources/bsdxInfoCollectionAnalysis/bsdx-info-collection-path-summary.tsv");
    /**
     * 审计产物只写到 out 目录，方便人工复盘，不进入正式资源链路。
     */
    private static final Path OUTPUT_DIR = Paths.get("src/main/resources/out/bhe2bsdx/term-audit/mek");

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Test
    void auditMekInfoCollectionsAndWriteReports() throws IOException {
        Files.createDirectories(OUTPUT_DIR);

        // 两侧 MEK JSON 都按真实 DTO 反序列化，避免只从文本层面 grep 漏掉嵌套对象。
        List<Occurrence> bheOccurrences = scanBheMek();
        List<Occurrence> bsdxOccurrences = scanBsdxMek();
        assertFalse(bheOccurrences.isEmpty(), "BHE MEK InfoCollection 审计结果为空");
        assertFalse(bsdxOccurrences.isEmpty(), "BSDX MEK InfoCollection 审计结果为空");

        // 明细表用于定位具体字段；汇总表用于判断语义路径是否新增、是否和 WAZ 方言一致。
        writeOccurrences("bhe-mek-info-collection-occurrences.tsv", bheOccurrences);
        writeOccurrences("bsdx-mek-info-collection-occurrences.tsv", bsdxOccurrences);
        Map<String, Integer> bheSummary = writePathSummary("bhe-mek-info-collection-path-summary.tsv", bheOccurrences);
        Map<String, Integer> bsdxSummary = writePathSummary("bsdx-mek-info-collection-path-summary.tsv", bsdxOccurrences);
        writeFileSummary("bhe-mek-info-collection-file-summary.tsv", bheOccurrences);
        writeFileSummary("bsdx-mek-info-collection-file-summary.tsv", bsdxOccurrences);
        writeComparison("mek-bhe-vs-bsdx-path-comparison.tsv", bheSummary, bsdxSummary);
        writeMekVsWazComparison("bhe-mek-vs-waz-path-comparison.tsv", bheSummary, readPathSummary(BHE_WAZ_PATH_SUMMARY));
        writeMekVsWazComparison("bsdx-mek-vs-waz-path-comparison.tsv", bsdxSummary, readPathSummary(BSDX_WAZ_PATH_SUMMARY));
        writeAuditMarkdown(bheOccurrences, bsdxOccurrences, bheSummary, bsdxSummary);
    }

    private List<Occurrence> scanBheMek() throws IOException {
        List<Occurrence> occurrences = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BHE_MEK_JSON_DIR, "*.json")) {
            for (Path path : stream) {
                com.giga.nexas.dto.bhe.mek.Mek mek =
                        mapper.readValue(path.toFile(), com.giga.nexas.dto.bhe.mek.Mek.class);
                visitObjectGraph(mek, "$", path.getFileName().toString(), "BHE", new IdentityHashMap<>(), occurrences);
            }
        }
        sortOccurrences(occurrences);
        return occurrences;
    }

    private List<Occurrence> scanBsdxMek() throws IOException {
        List<Occurrence> occurrences = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BSDX_MEK_JSON_DIR, "*.json")) {
            for (Path path : stream) {
                com.giga.nexas.dto.bsdx.mek.Mek mek =
                        mapper.readValue(path.toFile(), com.giga.nexas.dto.bsdx.mek.Mek.class);
                visitObjectGraph(mek, "$", path.getFileName().toString(), "BSDX", new IdentityHashMap<>(), occurrences);
            }
        }
        sortOccurrences(occurrences);
        return occurrences;
    }

    private void visitObjectGraph(
            Object node,
            String path,
            String fileName,
            String side,
            IdentityHashMap<Object, Boolean> visited,
            List<Occurrence> occurrences
    ) {
        if (node == null || isLeafValue(node)) {
            return;
        }
        if (visited.put(node, Boolean.TRUE) != null) {
            return;
        }

        // 命中 BHE/BSDX InfoCollection 后立即停止向下递归，避免把 term 内部字段当成普通对象重复统计。
        if (node instanceof BheInfoCollection collection) {
            BheInfoCollectionSemantic semantic = BheInfoCollectionAnalyzer.analyze(collection);
            occurrences.add(toOccurrence(side, fileName, path, collection, semantic));
            return;
        }

        if (node instanceof BsdxInfoCollection collection) {
            BsdxInfoCollectionSemantic semantic = BsdxInfoCollectionAnalyzer.analyze(collection);
            occurrences.add(toOccurrence(side, fileName, path, collection, semantic));
            return;
        }

        // DTO 里大量嵌套 List，路径里保留下标，方便直接回到具体 AI 分支或动作片段。
        if (node instanceof List<?> list) {
            for (int i = 0; i < list.size(); i++) {
                visitObjectGraph(list.get(i), path + "[" + i + "]", fileName, side, visited, occurrences);
            }
            return;
        }

        // Map 不是 MEK DTO 的主结构，但保留处理能力，防止 DTO 结构扩展时审计漏字段。
        if (node instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                visitObjectGraph(entry.getValue(), path + "." + entry.getKey(), fileName, side, visited, occurrences);
            }
            return;
        }

        // 反射扫描所有实例字段，保证新增 AI 子结构时不需要手动补字段清单。
        for (Field field : getAllFields(node.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(node);
                if (value != null) {
                    visitObjectGraph(value, path + "." + field.getName(), fileName, side, visited, occurrences);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("读取字段失败: " + field, e);
            }
        }
    }

    private Occurrence toOccurrence(
            String side,
            String fileName,
            String objectPath,
            BheInfoCollection collection,
            BheInfoCollectionSemantic semantic
    ) {
        return new Occurrence(
                side,
                fileName,
                objectPath,
                collection.getInt1(),
                String.valueOf(collection.getTypeList()),
                String.valueOf(semantic.getTrailingTypeListSnapshot()),
                String.valueOf(collection.getParamList()),
                String.valueOf(collection.getIntList3()),
                String.valueOf(collection.getIntList4()),
                collection.getInt2(),
                semantic.getSyntaxPathByName(),
                semantic.getSyntaxPathByDescription(),
                semantic.isTerminated(),
                String.join(" | ", semantic.getWarnings())
        );
    }

    private Occurrence toOccurrence(
            String side,
            String fileName,
            String objectPath,
            BsdxInfoCollection collection,
            BsdxInfoCollectionSemantic semantic
    ) {
        return new Occurrence(
                side,
                fileName,
                objectPath,
                collection.getInt1(),
                String.valueOf(collection.getTypeList()),
                String.valueOf(semantic.getTrailingTypeListSnapshot()),
                String.valueOf(collection.getParamList()),
                String.valueOf(collection.getIntList3()),
                String.valueOf(collection.getIntList4()),
                collection.getInt2(),
                semantic.getSyntaxPathByName(),
                semantic.getSyntaxPathByDescription(),
                semantic.isTerminated(),
                String.join(" | ", semantic.getWarnings())
        );
    }

    private void writeOccurrences(String fileName, List<Occurrence> occurrences) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("side\tfile\tpath\tint1\ttypeList\ttrailingTypeList\tparamList\tintList3\tintList4\tint2\tsyntaxPathByName\tsyntaxPathByDescription\tterminated\twarnings\n");
        for (Occurrence occurrence : occurrences) {
            sb.append(tsv(occurrence.side())).append('\t')
                    .append(tsv(occurrence.fileName())).append('\t')
                    .append(tsv(occurrence.objectPath())).append('\t')
                    .append(tsv(occurrence.int1())).append('\t')
                    .append(tsv(occurrence.typeList())).append('\t')
                    .append(tsv(occurrence.trailingTypeList())).append('\t')
                    .append(tsv(occurrence.paramList())).append('\t')
                    .append(tsv(occurrence.intList3())).append('\t')
                    .append(tsv(occurrence.intList4())).append('\t')
                    .append(tsv(occurrence.int2())).append('\t')
                    .append(tsv(occurrence.syntaxPathByName())).append('\t')
                    .append(tsv(occurrence.syntaxPathByDescription())).append('\t')
                    .append(tsv(occurrence.terminated())).append('\t')
                    .append(tsv(occurrence.warnings())).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve(fileName), sb.toString(), StandardCharsets.UTF_8);
    }

    private Map<String, Integer> writePathSummary(String fileName, List<Occurrence> occurrences) throws IOException {
        Map<String, Integer> counts = buildPathSummary(occurrences);
        StringBuilder sb = new StringBuilder();
        sb.append("syntaxPathByDescription\tcount\n");
        for (Map.Entry<String, Integer> row : sortSummary(counts)) {
            sb.append(tsv(row.getKey())).append('\t').append(row.getValue()).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve(fileName), sb.toString(), StandardCharsets.UTF_8);
        return counts;
    }

    private void writeFileSummary(String fileName, List<Occurrence> occurrences) throws IOException {
        Map<String, List<Occurrence>> byFile = new LinkedHashMap<>();
        for (Occurrence occurrence : occurrences) {
            byFile.computeIfAbsent(occurrence.fileName(), key -> new ArrayList<>()).add(occurrence);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("file\tcount\tuniqueSyntaxPathCount\twarningCount\n");
        for (Map.Entry<String, List<Occurrence>> entry : byFile.entrySet()) {
            Set<String> uniquePaths = new LinkedHashSet<>();
            int warningCount = 0;
            for (Occurrence occurrence : entry.getValue()) {
                uniquePaths.add(pathKey(occurrence.syntaxPathByDescription()));
                if (occurrence.warnings() != null && !occurrence.warnings().isBlank()) {
                    warningCount++;
                }
            }
            sb.append(tsv(entry.getKey())).append('\t')
                    .append(entry.getValue().size()).append('\t')
                    .append(uniquePaths.size()).append('\t')
                    .append(warningCount).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve(fileName), sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeComparison(
            String fileName,
            Map<String, Integer> left,
            Map<String, Integer> right
    ) throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(left.keySet());
        keys.addAll(right.keySet());
        List<String> sortedKeys = new ArrayList<>(keys);
        sortedKeys.sort(String::compareTo);

        StringBuilder sb = new StringBuilder();
        sb.append("syntaxPathByDescription\tbheMekCount\tbsdxMekCount\tstatus\n");
        for (String key : sortedKeys) {
            int leftCount = left.getOrDefault(key, 0);
            int rightCount = right.getOrDefault(key, 0);
            sb.append(tsv(key)).append('\t')
                    .append(leftCount).append('\t')
                    .append(rightCount).append('\t')
                    .append(leftCount > 0 && rightCount > 0 ? "BOTH" : leftCount > 0 ? "BHE_ONLY" : "BSDX_ONLY")
                    .append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve(fileName), sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeMekVsWazComparison(
            String fileName,
            Map<String, Integer> mekSummary,
            Map<String, Integer> wazSummary
    ) throws IOException {
        // WAZ 审计文件不属于本测试生成物；缺失时只输出表头，避免把环境缺文件误判为 MEK 语义差异。
        if (wazSummary.isEmpty()) {
            Files.writeString(OUTPUT_DIR.resolve(fileName), "syntaxPathByDescription\tmekCount\twazCount\tstatus\n", StandardCharsets.UTF_8);
            return;
        }
        Set<String> keys = new LinkedHashSet<>();
        keys.addAll(mekSummary.keySet());
        keys.addAll(wazSummary.keySet());
        List<String> sortedKeys = new ArrayList<>(keys);
        sortedKeys.sort(String::compareTo);

        StringBuilder sb = new StringBuilder();
        sb.append("syntaxPathByDescription\tmekCount\twazCount\tstatus\n");
        for (String key : sortedKeys) {
            int mekCount = mekSummary.getOrDefault(key, 0);
            int wazCount = wazSummary.getOrDefault(key, 0);
            sb.append(tsv(key)).append('\t')
                    .append(mekCount).append('\t')
                    .append(wazCount).append('\t')
                    .append(mekCount > 0 && wazCount > 0 ? "BOTH" : mekCount > 0 ? "MEK_ONLY" : "WAZ_ONLY")
                    .append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve(fileName), sb.toString(), StandardCharsets.UTF_8);
    }

    private void writeAuditMarkdown(
            List<Occurrence> bheOccurrences,
            List<Occurrence> bsdxOccurrences,
            Map<String, Integer> bheSummary,
            Map<String, Integer> bsdxSummary
    ) throws IOException {
        Map<String, Integer> bheWazSummary = readPathSummary(BHE_WAZ_PATH_SUMMARY);
        Map<String, Integer> bsdxWazSummary = readPathSummary(BSDX_WAZ_PATH_SUMMARY);

        StringBuilder sb = new StringBuilder();
        sb.append("# MEK InfoCollection term audit\n\n");
        sb.append("- BHE MEK occurrences: ").append(bheOccurrences.size()).append('\n');
        sb.append("- BHE MEK unique syntax paths: ").append(bheSummary.size()).append('\n');
        sb.append("- BSDX MEK occurrences: ").append(bsdxOccurrences.size()).append('\n');
        sb.append("- BSDX MEK unique syntax paths: ").append(bsdxSummary.size()).append('\n');
        sb.append("- BHE MEK paths not seen in BHE WAZ audit: ").append(countOnlyLeft(bheSummary, bheWazSummary)).append('\n');
        sb.append("- BSDX MEK paths not seen in BSDX WAZ audit: ").append(countOnlyLeft(bsdxSummary, bsdxWazSummary)).append('\n');
        sb.append("- BHE MEK paths absent from BSDX MEK audit: ").append(countOnlyLeft(bheSummary, bsdxSummary)).append('\n');
        sb.append("- BSDX MEK paths absent from BHE MEK audit: ").append(countOnlyLeft(bsdxSummary, bheSummary)).append('\n');
        Files.writeString(OUTPUT_DIR.resolve("audit.md"), sb.toString(), StandardCharsets.UTF_8);
    }

    private Map<String, Integer> buildPathSummary(List<Occurrence> occurrences) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Occurrence occurrence : occurrences) {
            counts.merge(pathKey(occurrence.syntaxPathByDescription()), 1, Integer::sum);
        }
        return counts;
    }

    private Map<String, Integer> readPathSummary(Path path) throws IOException {
        Map<String, Integer> result = new LinkedHashMap<>();
        // 允许单独运行 MEK 审计；WAZ 审计缺失只影响交叉对比，不影响 MEK 自身统计。
        if (path == null || !Files.exists(path)) {
            return result;
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split("\t", -1);
            if (parts.length < 2) {
                continue;
            }
            result.put(pathKey(parts[0]), Integer.parseInt(parts[1]));
        }
        return result;
    }

    private List<Map.Entry<String, Integer>> sortSummary(Map<String, Integer> counts) {
        List<Map.Entry<String, Integer>> rows = new ArrayList<>(counts.entrySet());
        rows.sort((a, b) -> {
            int cmp = Integer.compare(b.getValue(), a.getValue());
            return cmp != 0 ? cmp : a.getKey().compareTo(b.getKey());
        });
        return rows;
    }

    private int countOnlyLeft(Map<String, Integer> left, Map<String, Integer> right) {
        int count = 0;
        for (String key : left.keySet()) {
            if (!right.containsKey(key)) {
                count++;
            }
        }
        return count;
    }

    private void sortOccurrences(List<Occurrence> occurrences) {
        occurrences.sort(Comparator
                .comparing(Occurrence::fileName)
                .thenComparing(Occurrence::objectPath));
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
        // 数组字段按叶子处理；MEK DTO 的 term 载体是对象/List，不靠数组承载 InfoCollection。
        return type.isPrimitive()
                || value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>
                || type.isArray()
                || type.getName().startsWith("java.time.");
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

    private record Occurrence(
            String side,
            String fileName,
            String objectPath,
            Integer int1,
            String typeList,
            String trailingTypeList,
            String paramList,
            String intList3,
            String intList4,
            Integer int2,
            String syntaxPathByName,
            String syntaxPathByDescription,
            boolean terminated,
            String warnings
    ) {
    }
}
