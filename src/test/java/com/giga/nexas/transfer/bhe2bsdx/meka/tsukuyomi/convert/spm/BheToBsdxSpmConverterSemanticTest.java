package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.spm;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bhe.spm.hitbox.CRotatableBox;
import com.giga.nexas.dto.bhe.spm.hitbox.CRotatableRect;
import com.giga.nexas.dto.bsdx.spm.Spm;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

class BheToBsdxSpmConverterSemanticTest {

    private static final Path BHE_JSON_DIR = Paths.get("src/main/resources/spmBheJson");
    private static final Path OUTPUT_DIR = Paths.get("src/main/resources/out/bhe2bsdx/spm-convert/review");

    private static final List<String> REVIEW_SAMPLES = List.of(
            "tsukuyomi",
            "c_tsukuyomi",
            "g_tsukuyomi",
            "m_tsukuyomi",
            "s_tsukuyomi",
            "makoto",
            "sora",
            "gregory",
            "isao",
            "neunzehn"
    );

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final BheToBsdxSpmConverter converter = new BheToBsdxSpmConverter();

    @Test
    void convertReviewSamplesAndWriteReport() throws Exception {
        Map<String, Path> sourceFiles = indexSpmJson(BHE_JSON_DIR);
        Path convertedDir = OUTPUT_DIR.resolve("converted");
        Files.createDirectories(convertedDir);

        StringBuilder report = new StringBuilder();
        report.append("# BHE -> BSDX SPM convert review report\n\n");
        report.append("本报告由测试生成，只用于审查第一阶段 SPM 源侧转换，不参与生产流程。\n\n");

        for (String sample : REVIEW_SAMPLES) {
            Path sourcePath = sourceFiles.get(sample.toLowerCase(Locale.ROOT));
            if (sourcePath == null) {
                report.append("## ").append(sample).append("\n\n");
                report.append("- skipped: 缺少 BHE SPM JSON 样本\n\n");
                continue;
            }

            com.giga.nexas.dto.bhe.spm.Spm source =
                    mapper.readValue(sourcePath.toFile(), com.giga.nexas.dto.bhe.spm.Spm.class);
            Spm converted = converter.convert(source);

            validateConvertedShape(sample, source, converted);
            validateTsukuyomiHitboxMapping(sample, source, converted);

            Path convertedPath = convertedDir.resolve(sample + ".converted.spm.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(convertedPath.toFile(), converted);
            appendReport(report, sample, source, converted, convertedPath);
        }

        Files.writeString(OUTPUT_DIR.resolve("semantic-report.md"), report.toString(), StandardCharsets.UTF_8);
    }

    private Map<String, Path> indexSpmJson(Path dir) throws Exception {
        Map<String, Path> result = new TreeMap<>();
        try (var stream = Files.newDirectoryStream(dir, "*.spm.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.length() - ".spm.json".length());
                result.put(baseName.toLowerCase(Locale.ROOT), path);
            }
        }
        return result;
    }

    private void validateConvertedShape(
            String sample,
            com.giga.nexas.dto.bhe.spm.Spm source,
            Spm converted
    ) {
        assertEquals(source.getSpmVersion(), converted.getSpmVersion(), sample + " spmVersion");
        assertEquals(source.getPatPageNum(), converted.getPatPageNum(), sample + " patPageNum");
        assertEquals(source.getPageData().size(), converted.getPageData().size(), sample + " page count");
        assertEquals(source.getImageData().size(), converted.getImageData().size(), sample + " image count");
        assertEquals(source.getAnimData().size(), converted.getAnimData().size(), sample + " anim count");

        for (int pageIndex = 0; pageIndex < source.getPageData().size(); pageIndex++) {
            var sourcePage = source.getPageData().get(pageIndex);
            var convertedPage = converted.getPageData().get(pageIndex);
            assertEquals(sourcePage.getHitFlag(), convertedPage.getHitFlag(),
                    sample + " hitFlag 必须保留源侧稀疏 bit 位图: page=" + pageIndex);
            assertEquals(hitCount(sourcePage), hitCount(convertedPage),
                    sample + " hitbox count: page=" + pageIndex);
            assertEquals(sourcePage.getChipData().size(), convertedPage.getChipData().size(),
                    sample + " chip count: page=" + pageIndex);
        }
    }

    private void validateTsukuyomiHitboxMapping(
            String sample,
            com.giga.nexas.dto.bhe.spm.Spm source,
            Spm converted
    ) {
        int shape1 = 0;
        int shape10 = 0;
        for (int pageIndex = 0; pageIndex < source.getPageData().size(); pageIndex++) {
            var sourceHits = source.getPageData().get(pageIndex).getHitRects();
            var convertedHits = converted.getPageData().get(pageIndex).getHitRects();
            for (int hitIndex = 0; hitIndex < sourceHits.size(); hitIndex++) {
                var sourceHit = sourceHits.get(hitIndex);
                var convertedHit = convertedHits.get(hitIndex);
                if (sourceHit instanceof CRotatableRect) {
                    shape1++;
                    // Tsukuyomi 实际主力命中体：CRotatableRect 必须落到 BSDX 常规判定 unk0=2。
                    assertEquals(2, convertedHit.getUnk0(),
                            sample + " shapeType=1 应映射为 BSDX unk0=2");
                }
                if (sourceHit instanceof CRotatableBox) {
                    shape10++;
                    // Tsukuyomi 实际厚度命中体：CRotatableBox 必须落到 BSDX 厚度判定 unk0=7。
                    assertEquals(7, convertedHit.getUnk0(),
                            sample + " shapeType=10 应映射为 BSDX unk0=7");
                }
            }
        }

        if ("tsukuyomi".equals(sample)) {
            assertEquals(2210, shape1, "tsukuyomi shapeType=1 count");
            assertEquals(30, shape10, "tsukuyomi shapeType=10 count");
        }
    }

    private int hitCount(com.giga.nexas.dto.bhe.spm.Spm.SPMPageData page) {
        return page.getHitRects() == null ? 0 : page.getHitRects().size();
    }

    private int hitCount(Spm.SPMPageData page) {
        return page.getHitRects() == null ? 0 : page.getHitRects().size();
    }

    private void appendReport(
            StringBuilder report,
            String sample,
            com.giga.nexas.dto.bhe.spm.Spm source,
            Spm converted,
            Path convertedPath
    ) {
        HitSummary sourceSummary = summarizeSourceHits(source);
        HitSummary convertedSummary = summarizeConvertedHits(converted);
        report.append("## ").append(sample).append("\n\n");
        report.append("- converted: `").append(convertedPath).append("`\n");
        report.append("- version source/converted: ")
                .append(source.getSpmVersion()).append("/")
                .append(converted.getSpmVersion()).append("\n");
        report.append("- pages source/converted: ")
                .append(source.getPageData().size()).append("/")
                .append(converted.getPageData().size()).append("\n");
        report.append("- hits source/converted: ")
                .append(sourceSummary.total()).append("/")
                .append(convertedSummary.total()).append("\n");
        report.append("- source shape histogram: ").append(sourceSummary.histogram()).append("\n");
        report.append("- converted unk0 histogram: ").append(convertedSummary.histogram()).append("\n");
        report.append("- sparse hitFlag pages: ").append(countSparseHitFlagPages(source)).append("\n\n");
    }

    private HitSummary summarizeSourceHits(com.giga.nexas.dto.bhe.spm.Spm source) {
        Map<String, Integer> histogram = new LinkedHashMap<>();
        int total = 0;
        for (var page : source.getPageData()) {
            if (page.getHitRects() == null) {
                continue;
            }
            for (var hit : page.getHitRects()) {
                total++;
                String key = hit.getShapeType() == null ? "legacy" : String.valueOf(hit.getShapeType());
                histogram.merge(key, 1, Integer::sum);
            }
        }
        return new HitSummary(total, histogram);
    }

    private HitSummary summarizeConvertedHits(Spm converted) {
        Map<String, Integer> histogram = new LinkedHashMap<>();
        int total = 0;
        for (var page : converted.getPageData()) {
            if (page.getHitRects() == null) {
                continue;
            }
            for (var hit : page.getHitRects()) {
                total++;
                histogram.merge(String.valueOf(hit.getUnk0()), 1, Integer::sum);
            }
        }
        return new HitSummary(total, histogram);
    }

    private int countSparseHitFlagPages(com.giga.nexas.dto.bhe.spm.Spm source) {
        int count = 0;
        for (var page : source.getPageData()) {
            int hitCount = page.getHitRects() == null ? 0 : page.getHitRects().size();
            if (hitCount == 0 || hitCount >= 32) {
                continue;
            }
            long lowBitsByCount = (1L << hitCount) - 1;
            if (page.getHitFlag() != null && page.getHitFlag() != lowBitsByCount) {
                count++;
            }
        }
        return count;
    }

    private record HitSummary(int total, Map<String, Integer> histogram) {
    }
}
