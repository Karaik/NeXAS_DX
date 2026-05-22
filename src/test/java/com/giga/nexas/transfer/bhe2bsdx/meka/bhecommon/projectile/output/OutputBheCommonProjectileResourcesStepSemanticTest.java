package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.output;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.CommonProjectileTestSupport;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.convert.ConvertBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.cross.CrossRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.self.SelfRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputBheCommonProjectileResourcesStepSemanticTest {

    private static final Path OUTPUT_DIR =
            Paths.get("src/main/resources/out/bhe2bsdx/common-projectile-output/full");

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ConvertBheCommonProjectileResourcesStep convertStep =
            new ConvertBheCommonProjectileResourcesStep();
    private final SelfRedirectBheCommonProjectileResourcesStep selfRedirectStep =
            new SelfRedirectBheCommonProjectileResourcesStep();
    private final CrossRedirectBheCommonProjectileResourcesStep crossRedirectStep =
            new CrossRedirectBheCommonProjectileResourcesStep();
    private final OutputBheCommonProjectileResourcesStep outputStep =
            new OutputBheCommonProjectileResourcesStep();

    @Test
    void outputFullCommonProjectileResourcesAndWriteAudit() throws Exception {
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        Assumptions.assumeTrue(
                request.getExternalStaticAssetRoot() != null && Files.exists(request.getExternalStaticAssetRoot()),
                "外部 BHE 静态资源目录不存在，跳过真实输出审计"
        );
        cleanOutputDir();
        Files.createDirectories(OUTPUT_DIR);

        TsukuyomiRawSourceBundle rawSourceBundle = loadRawSourceBundle();
        TsukuyomiBsdxBaselineBundle baseline = loadBsdxBaseline();
        TsukuyomiConvertedBundle convertedBundle = new TsukuyomiConvertedBundle();
        BheCommonProjectileAppendPlan appendPlan = new BheCommonProjectileAppendPlan();

        convertStep.convert(rawSourceBundle, convertedBundle);
        selfRedirectStep.redirect(request, rawSourceBundle, baseline, convertedBundle, appendPlan);
        crossRedirectStep.redirect(request, rawSourceBundle, baseline, convertedBundle, appendPlan);

        TsukuyomiImportedAssetSet assetSet = new TsukuyomiImportedAssetSet();
        outputStep.output(request, baseline, appendPlan, java.util.Map.of(), OUTPUT_DIR, assetSet);

        assertEquals(6, assetSet.getGeneratedWazFiles().size());
        assertEquals(12, assetSet.getCopiedSpmFiles().size());
        assertEquals(668, assetSet.getCopiedAudioFiles().size());
        assertTrue(assetSet.getCopiedImageFiles().size() > 0);
        assertEquals(3, assetSet.getCopiedSpmFiles().stream()
                .filter(path -> path.getFileName().toString().startsWith("bhe_"))
                .count());
        assertTrue(assetSet.getCopiedImageFiles().stream().allMatch(path -> path.getFileName().toString().startsWith("bhe_")));
        assertTrue(assetSet.getCopiedAudioFiles().stream().allMatch(path -> path.getFileName().toString().startsWith("bhe_")));
        assertKnownMissingImagesOnly(assetSet);

        writeAudit(assetSet);
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
        CommonProjectileTestSupport.loadBsdxHostCommonResources(bundle);
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

    private void cleanOutputDir() throws Exception {
        Path normalized = OUTPUT_DIR.toAbsolutePath().normalize();
        Path allowedRoot = Paths.get("src/main/resources/out/bhe2bsdx").toAbsolutePath().normalize();
        if (!normalized.startsWith(allowedRoot)) {
            throw new IllegalStateException("拒绝清理 out 目录以外路径: " + normalized);
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

    private void writeAudit(TsukuyomiImportedAssetSet assetSet) throws Exception {
        StringBuilder report = new StringBuilder();
        report.append("# BHE common projectile output audit\n\n");
        report.append("- generated WAZ: ").append(assetSet.getGeneratedWazFiles().size()).append("\n");
        report.append("- generated/copied SPM: ").append(assetSet.getCopiedSpmFiles().size()).append("\n");
        report.append("- copied PNG: ").append(assetSet.getCopiedImageFiles().size()).append("\n");
        report.append("- copied audio: ").append(assetSet.getCopiedAudioFiles().size()).append("\n");
        report.append("- missing: ").append(assetSet.getMissingAssets()).append("\n");
        Files.writeString(OUTPUT_DIR.resolve("audit.md"), report.toString(), StandardCharsets.UTF_8);
    }

    private void assertKnownMissingImagesOnly(TsukuyomiImportedAssetSet assetSet) {
        Set<String> knownMissingImages = Set.of(
                "bhe_smoke_007_0001.png",
                "bhe_mark_maru038_0001.png",
                "bhe_mark_maru044_0001.png",
                "bhe_bomb_ball005_0002.png",
                "bhe_tama_138_0001.png",
                "bhe_ice_005_0002.png",
                "bhe_bomb_010_0001.png"
        );
        for (String missing : assetSet.getMissingAssets()) {
            boolean known = knownMissingImages.stream().anyMatch(missing::contains);
            assertTrue(known, "公共输出出现新的未知缺失资源: " + missing);
        }
    }
}
