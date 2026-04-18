package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.Bsdx;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.service.engine.adapter.BsdxBinaryEngineAdapter;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.convert.ConvertBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.cross.CrossRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.self.SelfRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BheCommonProjectileBinaryRoundTripTest {

    private static final String CHARSET = "windows-31j";
    private static final Path OUTPUT_DIR =
            Paths.get("src/main/resources/tmp/bhe2bsdx/common-projectile-roundtrip");

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final BsdxBinaryEngineAdapter engineAdapter = new BsdxBinaryEngineAdapter();
    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final ConvertBheCommonProjectileResourcesStep convertStep =
            new ConvertBheCommonProjectileResourcesStep();
    private final SelfRedirectBheCommonProjectileResourcesStep selfRedirectStep =
            new SelfRedirectBheCommonProjectileResourcesStep();
    private final CrossRedirectBheCommonProjectileResourcesStep crossRedirectStep =
            new CrossRedirectBheCommonProjectileResourcesStep();

    @Test
    void roundTripConvertedCommonProjectileBinaryResourcesThroughBsdxGenerator() throws Exception {
        cleanOutputDir();
        Files.createDirectories(OUTPUT_DIR);

        TsukuyomiRawSourceBundle rawSourceBundle = loadRawSourceBundle();
        TsukuyomiBsdxBaselineBundle baseline = loadBsdxBaseline();
        TsukuyomiConvertedBundle convertedBundle = new TsukuyomiConvertedBundle();
        BheCommonProjectileAppendPlan appendPlan = new BheCommonProjectileAppendPlan();

        convertStep.convert(rawSourceBundle, convertedBundle);
        selfRedirectStep.redirect(null, rawSourceBundle, baseline, convertedBundle, appendPlan);
        crossRedirectStep.redirect(null, rawSourceBundle, baseline, convertedBundle, appendPlan);

        List<ResourceCase> resources = collectRoundTripResources(baseline, appendPlan);
        List<RoundTripRow> rows = new ArrayList<>();
        List<String> mismatches = new ArrayList<>();
        for (ResourceCase resource : resources) {
            RoundTripRow row = roundTripOne(resource);
            rows.add(row);
            if (!row.normalizedJsonEqual()) {
                mismatches.add(row.category() + "/" + row.fileName());
            }
        }

        writeSummary(rows, mismatches);
        assertEquals(25, rows.size(), "公共二进制资源 round-trip 数量应为 8 WAZ + 12 SPM + 5 GRP");
        assertTrue(mismatches.isEmpty(), "公共资源 generate -> parse 后 JSON 不一致: " + mismatches);
    }

    private RoundTripRow roundTripOne(ResourceCase resource) throws Exception {
        Path inputJson = OUTPUT_DIR.resolve("input-json").resolve(resource.category()).resolve(resource.fileName() + ".json");
        Path generatedDir = OUTPUT_DIR.resolve("generated-bin").resolve(resource.category());
        Path parsedJson = OUTPUT_DIR.resolve("parsed-json").resolve(resource.category()).resolve(resource.fileName() + ".json");
        Files.createDirectories(inputJson.getParent());
        Files.createDirectories(parsedJson.getParent());

        resource.payload().setExtensionName(extensionOf(resource.fileName()));
        mapper.writerWithDefaultPrettyPrinter().writeValue(inputJson.toFile(), resource.payload());

        Path binary = engineAdapter.generate(inputJson, generatedDir, CHARSET);
        ResponseDTO<?> parsed = bsdxBinService.parse(binary.toString(), CHARSET);
        mapper.writerWithDefaultPrettyPrinter().writeValue(parsedJson.toFile(), parsed.getData());

        JsonNode sourceJson = mapper.valueToTree(resource.payload());
        JsonNode parsedNode = mapper.valueToTree(parsed.getData());
        boolean exactJsonEqual = sourceJson.equals(parsedNode);
        boolean normalizedJsonEqual = normalizeForRoundTripCompare(sourceJson, "").equals(
                normalizeForRoundTripCompare(parsedNode, "")
        );
        return new RoundTripRow(
                resource.category(),
                resource.fileName(),
                inputJson.toString(),
                binary.toString(),
                parsedJson.toString(),
                Files.size(binary),
                exactJsonEqual,
                normalizedJsonEqual
        );
    }

    private List<ResourceCase> collectRoundTripResources(
            TsukuyomiBsdxBaselineBundle baseline,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        List<ResourceCase> resources = new ArrayList<>();

        // 这些 GRP 是公共资源接入直接改动的二进制表；写整表是为了验证容量同步后的真实 generator/parser 闭环。
        resources.add(new ResourceCase("grp", "wazagroup.grp", prepareGrp(baseline.getWazaGroupGrp(), "wazagroup")));
        resources.add(new ResourceCase("grp", "spritegroup.grp", prepareGrp(baseline.getSpriteGroupGrp(), "spritegroup")));
        resources.add(new ResourceCase("grp", "segroup.grp", prepareGrp(baseline.getSeGroupGrp(), "segroup")));
        resources.add(new ResourceCase("grp", "programmaterial.grp", prepareGrp(baseline.getProgramMaterialGrp(), "programmaterial")));
        resources.add(new ResourceCase("grp", "mapgroup.grp", prepareGrp(baseline.getMapGroupGrp(), "mapgroup")));

        for (String fileName : appendPlan.getCommonProjectileWazFiles()) {
            Waz waz = baseline.getWazByFileName().get(fileName);
            if (waz == null) {
                throw new IllegalStateException("preparedBaseline 缺少公共 WAZ: " + fileName);
            }
            waz.setExtensionName("waz");
            resources.add(new ResourceCase("waz", fileName, waz));
        }
        for (String fileName : appendPlan.getCommonProjectileSpmFiles()) {
            Spm spm = baseline.getSpmByFileName().get(fileName);
            if (spm == null) {
                throw new IllegalStateException("preparedBaseline 缺少公共 SPM: " + fileName);
            }
            spm.setExtensionName("spm");
            resources.add(new ResourceCase("spm", fileName, spm));
        }
        return resources;
    }

    private Grp prepareGrp(Grp grp, String fileName) {
        if (grp == null) {
            throw new IllegalStateException("preparedBaseline 缺少 GRP: " + fileName);
        }
        grp.setFileName(fileName);
        grp.setExtensionName("grp");
        return grp;
    }

    private TsukuyomiRawSourceBundle loadRawSourceBundle() throws Exception {
        TsukuyomiRawSourceBundle bundle = new TsukuyomiRawSourceBundle();
        bundle.setBatVoiceGrp(read("src/main/resources/grpBheJson/batvoice.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.class));
        bundle.setWazaGroupGrp(read("src/main/resources/grpBheJson/wazagroup.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.class));
        bundle.setSpriteGroupGrp(read("src/main/resources/grpBheJson/spritegroup.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.class));
        bundle.setSeGroupGrp(read("src/main/resources/grpBheJson/segroup.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.class));
        for (String fileName : BheCommonProjectileResources.COMMON_PROJECTILE_WAZ_FILES) {
            bundle.getCommonProjectileWazByFileName().put(fileName, readWaz(fileName));
        }
        for (String fileName : BheCommonProjectileResources.COMMON_PROJECTILE_SPM_FILES) {
            bundle.getCommonProjectileSpmByFileName().put(fileName, readSpm(fileName));
        }
        return bundle;
    }

    private TsukuyomiBsdxBaselineBundle loadBsdxBaseline() throws Exception {
        TsukuyomiBsdxBaselineBundle bundle = new TsukuyomiBsdxBaselineBundle();
        bundle.setWazaGroupGrp(read("src/main/resources/grpBsdxJson/WazaGroup.grp.json", WazaGroupGrp.class));
        bundle.setSpriteGroupGrp(read("src/main/resources/grpBsdxJson/SpriteGroup.grp.json", SpriteGroupGrp.class));
        bundle.setSeGroupGrp(read("src/main/resources/grpBsdxJson/SeGroup.grp.json", SeGroupGrp.class));
        bundle.setBatVoiceGrp(read("src/main/resources/grpBsdxJson/BatVoice.grp.json", BatVoiceGrp.class));
        bundle.setProgramMaterialGrp(read("src/main/resources/grpBsdxJson/ProgramMaterial.grp.json", ProgramMaterialGrp.class));
        bundle.setMapGroupGrp(read("src/main/resources/grpBsdxJson/MapGroup.grp.json", MapGroupGrp.class));
        bundle.getMekByFileName().put("aki.mek", read("src/main/resources/mekBsdxJson/Aki.mek.json", Mek.class));
        return bundle;
    }

    private com.giga.nexas.dto.bhe.waz.Waz readWaz(String fileName) throws Exception {
        String baseName = fileName.substring(0, fileName.length() - ".waz".length());
        return read("src/main/resources/wazBheJson/" + baseName + ".waz.json", com.giga.nexas.dto.bhe.waz.Waz.class);
    }

    private com.giga.nexas.dto.bhe.spm.Spm readSpm(String fileName) throws Exception {
        return read("src/main/resources/spmBheJson/" + fileName + ".json", com.giga.nexas.dto.bhe.spm.Spm.class);
    }

    private <T> T read(String path, Class<T> type) throws Exception {
        return mapper.readValue(Paths.get(path).toFile(), type);
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            throw new IllegalStateException("无法识别扩展名: " + fileName);
        }
        return fileName.substring(dot + 1).toLowerCase();
    }

    private void writeSummary(List<RoundTripRow> rows, List<String> mismatches) throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("# BHE common projectile binary round-trip\n\n");
        report.append("- resource count: ").append(rows.size()).append('\n');
        report.append("- exact json mismatch count: ").append(rows.stream().filter(row -> !row.exactJsonEqual()).count()).append('\n');
        report.append("- normalized json mismatch count: ").append(mismatches.size()).append('\n');
        report.append("- generated binary total bytes: ").append(rows.stream().mapToLong(RoundTripRow::binaryBytes).sum()).append('\n');
        report.append("- mismatches: ").append(mismatches).append('\n');
        Files.writeString(OUTPUT_DIR.resolve("roundtrip-summary.md"), report.toString(), StandardCharsets.UTF_8);

        StringBuilder table = new StringBuilder();
        table.append("category\tfileName\tbinaryBytes\texactJsonEqual\tnormalizedJsonEqual\tinputJson\tgeneratedBinary\tparsedJson\n");
        for (RoundTripRow row : rows) {
            table.append(row.category()).append('\t')
                    .append(row.fileName()).append('\t')
                    .append(row.binaryBytes()).append('\t')
                    .append(row.exactJsonEqual()).append('\t')
                    .append(row.normalizedJsonEqual()).append('\t')
                    .append(row.inputJson()).append('\t')
                    .append(row.generatedBinary()).append('\t')
                    .append(row.parsedJson()).append('\n');
        }
        Files.writeString(OUTPUT_DIR.resolve("roundtrip-details.tsv"), table.toString(), StandardCharsets.UTF_8);
    }

    private JsonNode normalizeForRoundTripCompare(JsonNode node, String fieldName) {
        if (node == null || node instanceof NullNode) {
            if (isNullableTextField(fieldName)) {
                return TextNode.valueOf("");
            }
            return NullNode.getInstance();
        }
        if (node.isObject()) {
            ObjectNode normalized = mapper.createObjectNode();
            node.fields().forEachRemaining(entry -> {
                String childName = entry.getKey();
                if (isGeneratedMetadataField(childName)) {
                    return;
                }
                normalized.set(childName, normalizeForRoundTripCompare(entry.getValue(), childName));
            });
            return normalized;
        }
        if (node.isArray()) {
            ArrayNode normalized = mapper.createArrayNode();
            for (JsonNode child : node) {
                normalized.add(normalizeForRoundTripCompare(child, fieldName));
            }
            return normalized;
        }
        if ("fileName".equals(fieldName) && node.isTextual()) {
            return TextNode.valueOf(node.asText().toLowerCase());
        }
        return node;
    }

    private boolean isGeneratedMetadataField(String fieldName) {
        return "offset".equals(fieldName)
                || "slotNum".equals(fieldName)
                || "unitDescription".equals(fieldName);
    }

    private boolean isNullableTextField(String fieldName) {
        return fieldName != null
                && (fieldName.endsWith("Name")
                || fieldName.endsWith("NameJapanese")
                || fieldName.endsWith("NameEnglish"));
    }

    private void cleanOutputDir() throws Exception {
        Path normalized = OUTPUT_DIR.toAbsolutePath().normalize();
        Path allowedRoot = Paths.get("src/main/resources/tmp/bhe2bsdx").toAbsolutePath().normalize();
        if (!normalized.startsWith(allowedRoot)) {
            throw new IllegalStateException("拒绝清理 tmp 约定目录以外路径: " + normalized);
        }
        if (!Files.exists(normalized)) {
            return;
        }
        try (var stream = Files.walk(normalized)) {
            for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
    }

    private record ResourceCase(
            String category,
            String fileName,
            Bsdx payload
    ) {
    }

    private record RoundTripRow(
            String category,
            String fileName,
            String inputJson,
            String generatedBinary,
            String parsedJson,
            long binaryBytes,
            boolean exactJsonEqual,
            boolean normalizedJsonEqual
    ) {
    }
}
