package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.convert.ConvertBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.self.SelfRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelfRedirectBheCommonProjectileResourcesStepSemanticTest {

    private static final Path OUTPUT_DIR =
            Paths.get("src/main/resources/out/bhe2bsdx/common-projectile-self-redirect");

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final ConvertBheCommonProjectileResourcesStep convertStep =
            new ConvertBheCommonProjectileResourcesStep();
    private final SelfRedirectBheCommonProjectileResourcesStep selfRedirectStep =
            new SelfRedirectBheCommonProjectileResourcesStep();

    @Test
    void appendCommonProjectileTargetEntriesAndWriteAudit() throws Exception {
        TsukuyomiRawSourceBundle rawSourceBundle = loadRawSourceBundle();
        TsukuyomiBsdxBaselineBundle baseline = loadBsdxBaseline();
        TsukuyomiConvertedBundle convertedBundle = new TsukuyomiConvertedBundle();

        convertStep.convert(rawSourceBundle, convertedBundle);
        assertTrue(convertedBundle.getMergedPackageBundle().getWazByFileName().isEmpty(),
                "公共 WAZ 不能进入 mergedPackageBundle");
        assertTrue(convertedBundle.getMergedPackageBundle().getSpmByFileName().isEmpty(),
                "公共 SPM 不能进入 mergedPackageBundle");

        int baseWazSize = baseline.getWazaGroupGrp().getWazaList().size();
        int baseSpriteSize = baseline.getSpriteGroupGrp().getSpriteList().size();
        int baseSeSize = baseline.getSeGroupGrp().getSeList().size();

        BheCommonProjectileAppendPlan appendPlan = new BheCommonProjectileAppendPlan();
        selfRedirectStep.redirect(null, rawSourceBundle, baseline, convertedBundle, appendPlan);

        assertCommonWazEntries(rawSourceBundle, baseline, appendPlan, baseWazSize);
        assertCommonSpmEntries(rawSourceBundle, baseline, appendPlan, baseSpriteSize);
        assertCommonSeGroup(baseline, appendPlan, baseSeSize);
        assertCapacitySynced(baseline);

        writeAuditReport(baseline, appendPlan, baseWazSize, baseSpriteSize, baseSeSize);
    }

    private void assertCommonWazEntries(
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle baseline,
            BheCommonProjectileAppendPlan appendPlan,
            int baseWazSize
    ) {
        assertEquals(baseWazSize, appendPlan.getBaseWazaGroupSize());
        assertEquals(8, appendPlan.getSourceWazIndexToTargetIndex().size());
        assertEquals(baseWazSize + 8, baseline.getWazaGroupGrp().getWazaList().size());

        for (int sourceIndex = 0; sourceIndex < BheCommonProjectileResources.COMMON_PROJECTILE_WAZ_FILES.size(); sourceIndex++) {
            Integer targetIndex = appendPlan.getSourceWazIndexToTargetIndex().get(sourceIndex);
            assertEquals(baseWazSize + sourceIndex, targetIndex);

            WazaGroupGrp.WazaGroupEntry targetEntry = baseline.getWazaGroupGrp().getWazaList().get(targetIndex);
            com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry sourceEntry =
                    rawSourceBundle.getWazaGroupGrp().getWazaList().get(sourceIndex);
            String targetFileName = appendPlan.getSourceWazIndexToTargetFileName().get(sourceIndex);

            assertEquals("bhe_" + sourceEntry.getWazaDisplayName() + ".waz", targetFileName);
            assertEquals("bhe_" + sourceEntry.getWazaDisplayName(), targetEntry.getWazaDisplayName());
            assertEquals(sourceEntry.getWazaName(), targetEntry.getWazaName());
            assertTrue(targetEntry.getWazaCodeName().startsWith("BHE_"));
            assertEquals(countSkills(baseline.getWazByFileName().get(targetFileName)), targetEntry.getParam());
            assertNotNull(baseline.getWazByFileName().get(targetFileName));
        }
    }

    private void assertCommonSpmEntries(
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle baseline,
            BheCommonProjectileAppendPlan appendPlan,
            int baseSpriteSize
    ) {
        assertEquals(baseSpriteSize, appendPlan.getBaseSpriteGroupSize());
        assertEquals(12, appendPlan.getSourceSpriteIndexToTargetIndex().size());
        assertEquals(baseSpriteSize + 12, baseline.getSpriteGroupGrp().getSpriteList().size());
        assertEquals(baseSpriteSize + 11, appendPlan.getSourceSpriteIndexToTargetIndex().get(173));

        for (String sourceFileName : BheCommonProjectileResources.COMMON_PROJECTILE_SPM_FILES) {
            int sourceIndex = findSourceSpriteIndex(rawSourceBundle, sourceFileName);
            Integer targetIndex = appendPlan.getSourceSpriteIndexToTargetIndex().get(sourceIndex);
            String targetFileName = appendPlan.getSourceSpriteIndexToTargetFileName().get(sourceIndex);
            SpriteGroupGrp.SpriteGroupEntry targetEntry = baseline.getSpriteGroupGrp().getSpriteList().get(targetIndex);
            Spm targetSpm = baseline.getSpmByFileName().get(targetFileName);

            assertEquals("bhe_" + sourceFileName, targetFileName);
            assertEquals(targetFileName, targetEntry.getSpriteFileName());
            assertTrue(targetEntry.getSpriteCodeName().startsWith("BHE_"));
            assertNotNull(targetSpm);
            assertTrue(targetSpm.getImageData().stream()
                    .allMatch(image -> image == null || image.getImageName() == null
                            || image.getImageName().startsWith("bhe_")));
        }
    }

    private void assertCommonSeGroup(
            TsukuyomiBsdxBaselineBundle baseline,
            BheCommonProjectileAppendPlan appendPlan,
            int baseSeSize
    ) {
        assertEquals(baseSeSize, appendPlan.getBaseSeGroupSize());
        assertEquals(baseSeSize, appendPlan.getCommonProjectileSeGroupIndex());
        assertEquals(baseSeSize + 1, baseline.getSeGroupGrp().getSeList().size());

        SeGroupGrp.SeGroupGroup group = baseline.getSeGroupGrp().getSeList().get(baseSeSize);
        assertEquals("BHE_SE_PUBLIC", group.getSeType());
        assertEquals("BHE_SE_PUBLIC", group.getSeTypeCodeName());
        assertEquals(668, group.getSeItems().size(), "公共 WAZ 实际引用到的唯一 SE pair 数量应保持审计结果");
        assertEquals(668, appendPlan.getSourceSePairToTargetItemIndex().size());
        assertTrue(group.getSeItems().stream().allMatch(item -> item.getSeFileName().startsWith("bhe_")));
        assertFalse(appendPlan.getGlobalSeReferences().isEmpty());
    }

    private void assertCapacitySynced(TsukuyomiBsdxBaselineBundle baseline) {
        int spriteSize = baseline.getSpriteGroupGrp().getSpriteList().size();
        int seSize = baseline.getSeGroupGrp().getSeList().size();
        int voiceSize = baseline.getBatVoiceGrp().getVoiceList().size();

        assertEquals(spriteSize, baseline.getProgramMaterialGrp().getArray1().size());
        assertEquals(seSize, baseline.getProgramMaterialGrp().getArray2().size());
        assertEquals(voiceSize, baseline.getProgramMaterialGrp().getArray3().size());

        baseline.getMapGroupGrp().getGroupList().forEach(group -> {
            assertEquals(spriteSize, group.getArray1().size());
            assertEquals(seSize, group.getArray2().size());
            assertEquals(voiceSize, group.getArray3().size());
        });

        baseline.getMekByFileName().values().forEach(mek -> assertMekMaterialCapacity(mek, spriteSize, seSize, voiceSize));
    }

    private void assertMekMaterialCapacity(Mek mek, int spriteSize, int seSize, int voiceSize) {
        if (mek == null || mek.getMekMaterialBlock() == null) {
            return;
        }
        assertMaterialEntries(mek.getMekMaterialBlock().getEntries(), spriteSize, seSize, voiceSize);
        assertMaterialEntries(mek.getMekMaterialBlock().getRegularEntries(), spriteSize, seSize, voiceSize);
        assertMaterialEntries(mek.getMekMaterialBlock().getTrailingEntries(), spriteSize, seSize, voiceSize);
    }

    private void assertMaterialEntries(
            List<Mek.MekMaterialBlock.PluginEntry> entries,
            int spriteSize,
            int seSize,
            int voiceSize
    ) {
        if (entries == null) {
            return;
        }
        entries.forEach(entry -> {
            if (entry == null) {
                return;
            }
            assertEquals(spriteSize, entry.getSpriteGroups().size());
            assertEquals(seSize, entry.getSeGroups().size());
            assertEquals(voiceSize, entry.getVoiceGroups().size());
        });
    }

    private TsukuyomiRawSourceBundle loadRawSourceBundle() throws Exception {
        TsukuyomiRawSourceBundle bundle = new TsukuyomiRawSourceBundle();
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

    private int findSourceSpriteIndex(TsukuyomiRawSourceBundle rawSourceBundle, String sourceFileName) {
        for (int i = 0; i < rawSourceBundle.getSpriteGroupGrp().getSpriteList().size(); i++) {
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry entry =
                    rawSourceBundle.getSpriteGroupGrp().getSpriteList().get(i);
            if (entry != null
                    && entry.getSpriteFileName() != null
                    && entry.getSpriteFileName().trim().toLowerCase(Locale.ROOT)
                    .equals(sourceFileName.trim().toLowerCase(Locale.ROOT))) {
                return i;
            }
        }
        throw new IllegalStateException("source sprite not found: " + sourceFileName);
    }

    private int countSkills(Waz waz) {
        return waz == null || waz.getSkillList() == null ? 0 : waz.getSkillList().size();
    }

    private void writeAuditReport(
            TsukuyomiBsdxBaselineBundle baseline,
            BheCommonProjectileAppendPlan appendPlan,
            int baseWazSize,
            int baseSpriteSize,
            int baseSeSize
    ) throws Exception {
        Files.createDirectories(OUTPUT_DIR);
        mapper.writerWithDefaultPrettyPrinter().writeValue(OUTPUT_DIR.resolve("append-plan.json").toFile(), appendPlan);

        StringBuilder report = new StringBuilder();
        report.append("# BHE common projectile self redirect audit\n\n");
        report.append("- base WazaGroup size: ").append(baseWazSize).append("\n");
        report.append("- target WazaGroup size: ").append(baseline.getWazaGroupGrp().getWazaList().size()).append("\n");
        report.append("- base SpriteGroup size: ").append(baseSpriteSize).append("\n");
        report.append("- target SpriteGroup size: ").append(baseline.getSpriteGroupGrp().getSpriteList().size()).append("\n");
        report.append("- base SeGroup size: ").append(baseSeSize).append("\n");
        report.append("- target SeGroup size: ").append(baseline.getSeGroupGrp().getSeList().size()).append("\n");
        report.append("- common SE item count: ")
                .append(baseline.getSeGroupGrp().getSeList().get(baseSeSize).getSeItems().size()).append("\n\n");
        report.append("## WAZ mapping\n\n");
        appendPlan.getSourceWazIndexToTargetIndex().forEach((source, target) ->
                report.append("- ").append(source).append(" -> ").append(target)
                        .append(" `").append(appendPlan.getSourceWazIndexToTargetFileName().get(source)).append("`\n"));
        report.append("\n## SPM mapping\n\n");
        appendPlan.getSourceSpriteIndexToTargetIndex().forEach((source, target) ->
                report.append("- ").append(source).append(" -> ").append(target)
                        .append(" `").append(appendPlan.getSourceSpriteIndexToTargetFileName().get(source)).append("`\n"));
        report.append("\n## SE mapping sample\n\n");
        appendPlan.getSourceSePairToTargetItemIndex().entrySet().stream().limit(40).forEach(entry ->
                report.append("- ").append(entry.getKey()).append(" -> ")
                        .append(appendPlan.getCommonProjectileSeGroupIndex()).append(":")
                        .append(entry.getValue()).append(" `")
                        .append(appendPlan.getSourceSePairToTargetFileName().get(entry.getKey())).append("`\n"));

        Files.writeString(OUTPUT_DIR.resolve("audit.md"), report.toString(), StandardCharsets.UTF_8);
    }
}
