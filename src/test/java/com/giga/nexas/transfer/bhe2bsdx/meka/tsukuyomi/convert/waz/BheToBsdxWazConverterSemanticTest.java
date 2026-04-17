package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventFreeParam;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BheToBsdxWazConverterSemanticTest {

    private static final Path BHE_JSON_DIR = Paths.get("src/main/resources/wazBheJson");
    private static final Path BSDX_JSON_DIR = Paths.get("src/main/resources/wazBsdxJson");
    private static final Path BSDX_WAZ_DIR = Paths.get("src/main/resources/game/bsdx/waz");
    private static final Path OUTPUT_DIR = Paths.get("src/main/resources/out/bhe2bsdx/waz-convert/review");

    private static final List<String> REVIEW_SAMPLES = List.of(
            "tsukuyomi",
            "effect",
            "tama01",
            "tama02",
            "tama03",
            "tama04",
            "tama05",
            "laser",
            "bomb",
            "gregory",
            "isao",
            "makoto",
            "neunzehn",
            "neunzehn2",
            "sora"
    );

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final BheToBsdxWazConverter converter = new BheToBsdxWazConverter();
    private final com.giga.nexas.bhe2bsdx.steps.WazConverter legacyConverter =
            new com.giga.nexas.bhe2bsdx.steps.WazConverter();
    private final BheToBsdxWazSlotMap slotMap = new BheToBsdxWazSlotMap();
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    @Test
    void convertReviewSamplesAndWriteReport() throws Exception {
        Map<String, Path> sourceFiles = indexWazJson(BHE_JSON_DIR);
        Map<String, Path> bsdxFiles = indexWazJson(BSDX_JSON_DIR);
        Map<String, Path> bsdxBinaryFiles = indexWazBinary(BSDX_WAZ_DIR);
        Path convertedDir = OUTPUT_DIR.resolve("converted");
        Files.createDirectories(convertedDir);

        StringBuilder report = new StringBuilder();
        report.append("# BHE -> BSDX WAZ convert review report\n\n");
        report.append("本报告由测试生成，只用于审查第一阶段 WAZ 源侧转换，不参与生产流程。\n\n");

        for (String sample : REVIEW_SAMPLES) {
            Path sourcePath = sourceFiles.get(sample.toLowerCase(Locale.ROOT));
            if (sourcePath == null) {
                report.append("## ").append(sample).append("\n\n");
                report.append("- skipped: 缺少 BHE WAZ JSON 样本\n\n");
                continue;
            }
            com.giga.nexas.dto.bhe.waz.Waz source =
                    mapper.readValue(sourcePath.toFile(), com.giga.nexas.dto.bhe.waz.Waz.class);
            Waz converted = converter.convert(source);
            Waz legacyConverted = legacyConverter.convert(source);
            Waz sameNameBsdx = readOptionalBsdx(bsdxFiles, bsdxBinaryFiles, sample);

            validateSourceSlots(sourcePath.getFileName().toString(), source);
            validateConvertedSlots(sourcePath.getFileName().toString(), converted);
            // 旧 src/test trans 是当前最高优先级代码参考；新实现必须先和它完全对齐。
            assertLegacyTransAligned(sample, converted, legacyConverted);

            Path convertedPath = convertedDir.resolve(sample + ".converted.waz.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(convertedPath.toFile(), converted);
            appendReport(report, sample, source, converted, sameNameBsdx, convertedPath);
        }

        Files.writeString(OUTPUT_DIR.resolve("semantic-report.md"), report.toString(), StandardCharsets.UTF_8);
    }

    private Map<String, Path> indexWazJson(Path dir) throws Exception {
        Map<String, Path> result = new TreeMap<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (var stream = Files.newDirectoryStream(dir, "*.waz.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.length() - ".waz.json".length());
                result.put(baseName.toLowerCase(Locale.ROOT), path);
            }
        }
        return result;
    }

    private Map<String, Path> indexWazBinary(Path dir) throws Exception {
        Map<String, Path> result = new TreeMap<>();
        if (!Files.isDirectory(dir)) {
            return result;
        }
        try (var stream = Files.newDirectoryStream(dir, "*.waz")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.length() - ".waz".length());
                result.put(baseName.toLowerCase(Locale.ROOT), path);
            }
        }
        return result;
    }

    private Waz readOptionalBsdx(
            Map<String, Path> bsdxJsonFiles,
            Map<String, Path> bsdxBinaryFiles,
            String sample
    ) throws Exception {
        Path jsonPath = bsdxJsonFiles.get(sample.toLowerCase(Locale.ROOT));
        if (jsonPath != null) {
            return mapper.readValue(jsonPath.toFile(), Waz.class);
        }

        Path binaryPath = bsdxBinaryFiles.get(sample.toLowerCase(Locale.ROOT));
        if (binaryPath == null) {
            return null;
        }
        return (Waz) bsdxBinService.parse(binaryPath.toString(), "windows-31j").getData();
    }

    private void validateSourceSlots(String label, com.giga.nexas.dto.bhe.waz.Waz source) {
        for (com.giga.nexas.dto.bhe.waz.Waz.Skill skill : source.getSkillList()) {
            for (com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit unit
                        : phase.getSkillUnitCollection()) {
                    Integer sourceSlot = unit.getUnitQuantity();
                    Integer targetSlot = slotMap.resolveTargetSlot(sourceSlot);
                    assertNotNull(targetSlot, label + " 存在未声明映射的 BHE slot: " + sourceSlot);
                    if (sourceSlot != null && sourceSlot == 37) {
                        assertFreeParamHasBufferZero(label, unit);
                    }
                }
            }
        }
    }

    private void assertFreeParamHasBufferZero(
            String label,
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit unit
    ) {
        if (unit.getSkillInfoObjectList() == null) {
            return;
        }
        for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject object
                : unit.getSkillInfoObjectList()) {
            if (object instanceof CEventFreeParam freeParam) {
                boolean found = freeParam.getUnitList() != null && freeParam.getUnitList().stream()
                        .anyMatch(inner -> inner.getBuffer() == 0);
                assertTrue(found, label + " CEventFreeParam 缺少 buffer==0");
            }
        }
    }

    private void validateConvertedSlots(String label, Waz converted) {
        for (Waz.Skill skill : converted.getSkillList()) {
            for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                for (com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit unit
                        : phase.getSkillUnitCollection()) {
                    Integer targetSlot = unit.getUnitQuantity();
                    assertNotNull(targetSlot, label + " converted slot 为空");
                    assertTrue(targetSlot >= 0 && targetSlot < 72,
                            label + " converted slot 越界: " + targetSlot);
                }
            }
        }
    }

    private void assertLegacyTransAligned(String sample, Waz converted, Waz legacyConverted) throws Exception {
        String convertedJson = mapper.writeValueAsString(converted);
        String legacyJson = mapper.writeValueAsString(legacyConverted);
        assertTrue(convertedJson.equals(legacyJson), sample + " 新 WAZ convert 与 src/test 旧 trans 输出不一致");
    }

    private void appendReport(
            StringBuilder report,
            String sample,
            com.giga.nexas.dto.bhe.waz.Waz source,
            Waz converted,
            Waz sameNameBsdx,
            Path convertedPath
    ) {
        SlotSummary sourceSummary = summarizeSourceSlots(source);
        SlotSummary convertedSummary = summarizeConvertedSlots(converted);
        SlotSummary bsdxSummary = sameNameBsdx == null ? null : summarizeConvertedSlots(sameNameBsdx);
        report.append("## ").append(sample).append("\n\n");
        report.append("- converted: `").append(convertedPath).append("`\n");
        report.append("- skills source/converted/bsdx: ")
                .append(source.getSkillList().size()).append("/")
                .append(converted.getSkillList().size()).append("/")
                .append(sameNameBsdx == null ? "missing" : sameNameBsdx.getSkillList().size()).append("\n");
        report.append("- source slot count: ").append(sourceSummary.total()).append("\n");
        report.append("- converted slot count: ").append(convertedSummary.total()).append("\n");
        if (bsdxSummary != null) {
            report.append("- bsdx same-name slot count: ").append(bsdxSummary.total()).append("\n");
        }
        report.append("- legacy trans aligned: true\n");
        report.append("- declared dropped source slots: ").append(sourceSummary.droppedSlots()).append("\n");
        report.append("- source slot histogram: ").append(sourceSummary.histogram()).append("\n");
        report.append("- converted slot histogram: ").append(convertedSummary.histogram()).append("\n\n");
        if (bsdxSummary != null) {
            // BSDX 同名摘要只用于人工 review，两个游戏数据允许存在少量差异，不做强一致断言。
            report.append("- bsdx same-name slot histogram: ").append(bsdxSummary.histogram()).append("\n\n");
        }
    }

    private SlotSummary summarizeSourceSlots(com.giga.nexas.dto.bhe.waz.Waz source) {
        Map<Integer, Integer> histogram = new LinkedHashMap<>();
        Map<Integer, Integer> dropped = new LinkedHashMap<>();
        int total = 0;
        for (com.giga.nexas.dto.bhe.waz.Waz.Skill skill : source.getSkillList()) {
            for (com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit unit
                        : phase.getSkillUnitCollection()) {
                    Integer slot = unit.getUnitQuantity();
                    total++;
                    histogram.merge(slot, 1, Integer::sum);
                    if (Integer.valueOf(-1).equals(slotMap.resolveTargetSlot(slot))) {
                        dropped.merge(slot, 1, Integer::sum);
                    }
                }
            }
        }
        return new SlotSummary(total, histogram, dropped);
    }

    private SlotSummary summarizeConvertedSlots(Waz converted) {
        Map<Integer, Integer> histogram = new LinkedHashMap<>();
        int total = 0;
        for (Waz.Skill skill : converted.getSkillList()) {
            for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                for (com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit unit
                        : phase.getSkillUnitCollection()) {
                    Integer slot = unit.getUnitQuantity();
                    total++;
                    histogram.merge(slot, 1, Integer::sum);
                }
            }
        }
        return new SlotSummary(total, histogram, Map.of());
    }

    private record SlotSummary(int total, Map<Integer, Integer> histogram, Map<Integer, Integer> droppedSlots) {
    }
}
