package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.mek;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BheToBsdxMekConverterSemanticTest {

    private static final Path BHE_JSON_DIR = Paths.get("src/main/resources/mekBheJson");
    private static final Path BSDX_JSON_DIR = Paths.get("src/main/resources/mekBsdxJson");
    private static final Path OUTPUT_DIR = Paths.get("src/main/resources/out/bhe2bsdx/mek-convert/same-name");

    /**
     * zako* 和 kou 不作为 MEK 语义对比样本。
     * 这里选用双方同名、武装/AI/material 条目数基本同位的正常机体。
     */
    private static final List<String> SAME_NAME_SAMPLES = List.of(
            "gregory",
            "isao",
            "makoto",
            "neunzehn",
            "neunzehn2",
            "sora"
    );

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final BheToBsdxMekConverter converter = new BheToBsdxMekConverter();

    @Test
    void convertSameNameMeksAndWriteSemanticReport() throws Exception {
        Map<String, Path> bheFiles = indexMekJson(BHE_JSON_DIR);
        Map<String, Path> bsdxFiles = indexMekJson(BSDX_JSON_DIR);

        Path convertedDir = OUTPUT_DIR.resolve("converted");
        Files.createDirectories(convertedDir);

        StringBuilder report = new StringBuilder();
        report.append("# BHE -> BSDX MEK convert same-name semantic report\n\n");
        report.append("本报告由测试生成，只用于审查第一阶段 MEK 源侧转换，不参与生产流程。\n\n");

        for (String sample : SAME_NAME_SAMPLES) {
            assertFalse(sample.startsWith("zako"), "zako* 不作为本测试样本");
            assertEquals(-1, sample.indexOf("kou"), "kou 不作为本测试样本");

            com.giga.nexas.dto.bhe.mek.Mek source =
                    mapper.readValue(requireFile(bheFiles, sample).toFile(), com.giga.nexas.dto.bhe.mek.Mek.class);
            Mek expectedBsdx =
                    mapper.readValue(requireFile(bsdxFiles, sample).toFile(), Mek.class);

            Mek converted = converter.convert(source);
            Path convertedPath = convertedDir.resolve(sample + ".converted.mek.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(convertedPath.toFile(), converted);

            assertTopLevelShape(sample, source, converted, expectedBsdx);
            assertAiTailSemantic(sample, converted, expectedBsdx);
            assertMaterialSourceShape(sample, source, converted);

            appendReport(report, sample, source, converted, expectedBsdx, convertedPath);
        }

        Files.writeString(OUTPUT_DIR.resolve("semantic-report.md"), report.toString(), StandardCharsets.UTF_8);
    }

    private Map<String, Path> indexMekJson(Path dir) throws Exception {
        Map<String, Path> result = new TreeMap<>();
        try (var stream = Files.newDirectoryStream(dir, "*.mek.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.length() - ".mek.json".length());
                result.put(baseName.toLowerCase(Locale.ROOT), path);
            }
        }
        return result;
    }

    private Path requireFile(Map<String, Path> files, String sample) {
        Path path = files.get(sample.toLowerCase(Locale.ROOT));
        assertNotNull(path, "缺少同名 MEK JSON：" + sample);
        return path;
    }

    private void assertTopLevelShape(
            String sample,
            com.giga.nexas.dto.bhe.mek.Mek source,
            Mek converted,
            Mek expectedBsdx
    ) {
        assertEquals(source.getFileName(), converted.getFileName(), sample + " fileName");
        assertEquals(countWeapons(source), countWeapons(converted), sample + " converted weapon count");
        assertEquals(countWeapons(expectedBsdx), countWeapons(converted), sample + " same-name weapon count");
        assertEquals(countAi(source), countAi(converted), sample + " converted ai count");
        assertEquals(countAi(expectedBsdx), countAi(converted), sample + " same-name ai count");
        assertEquals(materialRegularCount(expectedBsdx), materialRegularCount(converted), sample + " regular entries");
        assertEquals(materialTrailingCount(expectedBsdx), materialTrailingCount(converted), sample + " trailing entries");
    }

    private void assertAiTailSemantic(String sample, Mek converted, Mek expectedBsdx) {
        for (int aiIndex = 0; aiIndex < countAi(expectedBsdx); aiIndex++) {
            List<CCpuEvent> convertedEvents = converted.getMekAiInfoList().get(aiIndex).getCpuEventList();
            List<CCpuEvent> expectedEvents = expectedBsdx.getMekAiInfoList().get(aiIndex).getCpuEventList();
            assertEquals(expectedEvents.size(), convertedEvents.size(), sample + " ai event count " + aiIndex);

            for (int eventIndex = 0; eventIndex < expectedEvents.size(); eventIndex++) {
                CCpuEvent convertedEvent = convertedEvents.get(eventIndex);
                CCpuEvent expectedEvent = expectedEvents.get(eventIndex);
                assertEquals(expectedEvent.getType(), convertedEvent.getType(),
                        sample + " ai type " + aiIndex + "/" + eventIndex);
                assertEquals(tailSignature(expectedEvent), tailSignature(convertedEvent),
                        sample + " ai tail " + aiIndex + "/" + eventIndex);
            }
        }
    }

    private List<Object> tailSignature(CCpuEvent event) {
        return List.of(
                event.getInt25(),
                event.getInt26(),
                event.getShort2(),
                event.getInt27(),
                event.getInt28(),
                event.getShort3(),
                event.getShort4()
        );
    }

    private void assertMaterialSourceShape(
            String sample,
            com.giga.nexas.dto.bhe.mek.Mek source,
            Mek converted
    ) {
        assertMaterialEntries(sample + " entries",
                source.getMekMaterialBlock().getEntries(),
                converted.getMekMaterialBlock().getEntries());
        assertMaterialEntries(sample + " regular",
                source.getMekMaterialBlock().getRegularEntries(),
                converted.getMekMaterialBlock().getRegularEntries());
        assertMaterialEntries(sample + " trailing",
                source.getMekMaterialBlock().getTrailingEntries(),
                converted.getMekMaterialBlock().getTrailingEntries());
    }

    private void assertMaterialEntries(
            String label,
            List<com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry> sourceEntries,
            List<Mek.MekMaterialBlock.PluginEntry> convertedEntries
    ) {
        assertEquals(sourceEntries.size(), convertedEntries.size(), label + " entry count");
        for (int entryIndex = 0; entryIndex < sourceEntries.size(); entryIndex++) {
            var sourceEntry = sourceEntries.get(entryIndex);
            var convertedEntry = convertedEntries.get(entryIndex);
            assertEquals(sourceEntry.getSpriteGroups().size(), convertedEntry.getSpriteGroups().size(),
                    label + " sprite group count " + entryIndex);
            assertEquals(sourceEntry.getSeGroups().size(), convertedEntry.getSeGroups().size(),
                    label + " se group count " + entryIndex);
            assertEquals(sourceEntry.getVoiceGroups().size(), convertedEntry.getVoiceGroups().size(),
                    label + " voice group count " + entryIndex);

            for (int groupIndex = 0; groupIndex < sourceEntry.getSpriteGroups().size(); groupIndex++) {
                assertArrayEquals(
                        expectedConvertedSpritePayload(sourceEntry.getSpriteGroups().get(groupIndex)),
                        convertedEntry.getSpriteGroups().get(groupIndex),
                        label + " sprite payload " + entryIndex + "/" + groupIndex
                );
            }
        }
    }

    private int[] expectedConvertedSpritePayload(int[] sourcePayload) {
        if (sourcePayload == null || sourcePayload.length == 0) {
            return new int[0];
        }
        int[] result = new int[sourcePayload.length / 2];
        for (int i = 0; i < result.length; i++) {
            result[i] = sourcePayload[i * 2];
        }
        return result;
    }

    private void appendReport(
            StringBuilder report,
            String sample,
            com.giga.nexas.dto.bhe.mek.Mek source,
            Mek converted,
            Mek expectedBsdx,
            Path convertedPath
    ) {
        report.append("## ").append(sample).append("\n\n");
        report.append("- converted: `").append(convertedPath).append("`\n");
        report.append("- weapons BHE/converted/BSDX: ")
                .append(countWeapons(source)).append("/")
                .append(countWeapons(converted)).append("/")
                .append(countWeapons(expectedBsdx)).append("\n");
        report.append("- ai events converted/BSDX: ")
                .append(aiEventCounts(converted)).append("/")
                .append(aiEventCounts(expectedBsdx)).append("\n");
        report.append("- material regular converted/BSDX: ")
                .append(materialRegularCount(converted)).append("/")
                .append(materialRegularCount(expectedBsdx)).append("\n");
        report.append("- material trailing converted/BSDX: ")
                .append(materialTrailingCount(converted)).append("/")
                .append(materialTrailingCount(expectedBsdx)).append("\n");
        report.append("- first regular group shape converted/BSDX: ")
                .append(firstRegularGroupShape(converted)).append("/")
                .append(firstRegularGroupShape(expectedBsdx)).append("\n\n");
    }

    private int countWeapons(com.giga.nexas.dto.bhe.mek.Mek mek) {
        return mek.getMekWeaponInfoMap() == null ? 0 : mek.getMekWeaponInfoMap().size();
    }

    private int countWeapons(Mek mek) {
        return mek.getMekWeaponInfoMap() == null ? 0 : mek.getMekWeaponInfoMap().size();
    }

    private int countAi(com.giga.nexas.dto.bhe.mek.Mek mek) {
        return mek.getMekAiInfoList() == null ? 0 : mek.getMekAiInfoList().size();
    }

    private int countAi(Mek mek) {
        return mek.getMekAiInfoList() == null ? 0 : mek.getMekAiInfoList().size();
    }

    private List<Integer> aiEventCounts(Mek mek) {
        if (mek.getMekAiInfoList() == null) {
            return List.of();
        }
        return mek.getMekAiInfoList().stream()
                .map(ai -> ai.getCpuEventList() == null ? 0 : ai.getCpuEventList().size())
                .toList();
    }

    private int materialRegularCount(Mek mek) {
        return mek.getMekMaterialBlock() == null || mek.getMekMaterialBlock().getRegularEntries() == null
                ? 0
                : mek.getMekMaterialBlock().getRegularEntries().size();
    }

    private int materialTrailingCount(Mek mek) {
        return mek.getMekMaterialBlock() == null || mek.getMekMaterialBlock().getTrailingEntries() == null
                ? 0
                : mek.getMekMaterialBlock().getTrailingEntries().size();
    }

    private String firstRegularGroupShape(Mek mek) {
        if (mek.getMekMaterialBlock() == null
                || mek.getMekMaterialBlock().getRegularEntries() == null
                || mek.getMekMaterialBlock().getRegularEntries().isEmpty()) {
            return "0/0/0";
        }

        Mek.MekMaterialBlock.PluginEntry entry = mek.getMekMaterialBlock().getRegularEntries().get(0);
        return Arrays.asList(
                entry.getSpriteGroups() == null ? 0 : entry.getSpriteGroups().size(),
                entry.getSeGroups() == null ? 0 : entry.getSeGroups().size(),
                entry.getVoiceGroups() == null ? 0 : entry.getVoiceGroups().size()
        ).toString();
    }
}
