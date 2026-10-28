package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImportStaticAssetsStepBheOutputTest {

    private static final String CHARSET = "windows-31j";

    @TempDir
    Path tempDir;

    private final ImportStaticAssetsStep step = new ImportStaticAssetsStep();
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    @Test
    void bothImportersDeliverPreloadBudgetForTheWrittenRosterWithoutChangingInheritedRows() throws Exception {
        Path originalBudget = Path.of("src/main/resources/game/bsdx/dat/BQMekaMemory.dat");
        Path originalMek = Path.of("src/main/resources/game/bsdx/mek/Zako102a.mek");
        byte[] budgetBefore = Files.readAllBytes(originalBudget);
        byte[] mekBefore = Files.readAllBytes(originalMek);
        assertEquals(420, budgetBefore.length, "fixture必须是103行原版单列费用表");
        try {
            assertAll("两个真实Importer都必须交付按最终MekaGroup扩容的费用表",
                    () -> assertPreloadBudgetOutput(false, budgetBefore, originalMek),
                    () -> assertPreloadBudgetOutput(true, budgetBefore, originalMek));
        } finally {
            assertAll("真实输入保持只读",
                    () -> assertArrayEquals(budgetBefore, Files.readAllBytes(originalBudget)),
                    () -> assertArrayEquals(mekBefore, Files.readAllBytes(originalMek)));
        }
    }

    private void assertPreloadBudgetOutput(boolean followup, byte[] originalBytes, Path originalMek) throws Exception {
        Path caseRoot = Files.createDirectories(tempDir.resolve(followup ? "followup-budget" : "tsukuyomi-budget"));
        Path datDir = Files.createDirectories(caseRoot.resolve("original-dat"));
        Path sourceBudget = datDir.resolve("BQMekaMemory.dat");
        Files.write(sourceBudget, originalBytes);
        Path inheritedDir = Files.createDirectories(caseRoot.resolve("inherited"));
        byte[] expectedPrefix = originalBytes;
        if (followup) {
            // 已继承的行具有优先级；包括以前追加的ID103，不得被本轮默认策略覆盖。
            expectedPrefix = Arrays.copyOf(originalBytes, originalBytes.length + Integer.BYTES);
            ByteBuffer.wrap(expectedPrefix).order(ByteOrder.LITTLE_ENDIAN)
                    .putInt(8, 12_345_678).putInt(originalBytes.length, 98_765_432);
            Files.write(inheritedDir.resolve("BQMekaMemory.dat"), expectedPrefix);
        }
        byte[] inheritedBefore = expectedPrefix.clone();
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setExeOutputDir(caseRoot.resolve("out"));
        request.setJinkiGeneratedAssetDir(inheritedDir);
        request.setBsdxDatDir(datDir);
        request.setBsdxGrpDir(caseRoot.resolve("no-original-grp"));
        request.setBsdxMekDir(caseRoot.resolve("no-original-mek"));
        request.setBsdxSpmDir(caseRoot.resolve("no-original-spm"));
        request.setBsdxWazDir(caseRoot.resolve("no-original-waz"));
        request.setExternalStaticAssetRoot(caseRoot.resolve("no-donor-media"));
        request.setBsdxStaticAssetRoot(caseRoot.resolve("no-original-media"));
        request.setMekFileName("budget_fixture.mek");
        request.setWazFileName("budget_fixture.waz");
        request.setSpriteFileName("budget_fixture.spm");
        request.setPackUpdatePac(false);

        TsukuyomiBsdxBaselineBundle baseline = budgetBaseline();
        Mek mek = (Mek) bsdxBinService.parse(originalMek.toString(), CHARSET).getData();
        mek.setFileName("budget_fixture");
        mek.getMekBasicInfo().setWazFileSequence(0);
        mek.getMekBasicInfo().setSpmFileSequence(0);
        var weapon = mek.getMekWeaponInfoMap().values().iterator().next();
        weapon.setWazSequence(0);
        weapon.setWeaponCategory(0);
        mek.setMekWeaponInfoMap(new LinkedHashMap<>(Map.of(0, weapon)));
        mek.getMekAiInfoList().clear();
        mek.getMekVoiceInfo().setEmotions(List.of());
        mek.getMekVoiceInfo().setVoiceSlots(List.of());
        mek.getMekVoiceInfo().setTable(List.of());
        Mek.MekMaterialBlock material = new Mek.MekMaterialBlock();
        material.setExtraRegularCount(0);
        for (int i = 0; i < 8; i++) {
            var entry = new Mek.MekMaterialBlock.PluginEntry();
            entry.getSpriteGroups().add(new int[0]);
            material.getEntries().add(entry);
        }
        mek.setMekMaterialBlock(material);
        Waz waz = new Waz("budget_fixture");
        waz.setExtensionName("waz");
        var skill = new Waz.Skill();
        skill.setSkillNameJapanese("budget fixture");
        skill.setSkillNameEnglish("BUDGET_FIXTURE");
        skill.setPhaseQuantity(0);
        waz.getSkillList().add(skill);
        TsukuyomiPackageBundle selected = new TsukuyomiPackageBundle();
        selected.getSpmByFileName().put("budget_fixture.spm", minimalConvertedSpm("optional-image.png"));
        TsukuyomiImportPlan plan = new TsukuyomiImportPlan();
        plan.getRequiredSpmFiles().add("budget_fixture.spm");
        TsukuyomiGrpAppendPlan append = new TsukuyomiGrpAppendPlan();
        append.setMekaGroupIndex(113);
        append.setWazaGroupIndex(0);
        append.setSpriteGroupIndex(0);
        try {
            TsukuyomiImportedAssetSet output = followup
                    ? new com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.ImportStaticAssetsStep().importAssets(
                            request, selected, baseline, plan, baseline.getProgramMaterialGrp(), mek, waz,
                            append, new BheCommonProjectileAppendPlan(), Map.of())
                    : step.importAssets(request, selected, baseline, plan, baseline.getProgramMaterialGrp(), mek, waz,
                            append, new BheCommonProjectileAppendPlan(), Map.of());
            Path outputRoot = output.getOutputRootDir();
            assertTrue(outputRoot.toAbsolutePath().normalize().startsWith(tempDir.toAbsolutePath().normalize()));
            Path outputBudget = outputRoot.resolve("BQMekaMemory.dat");
            assertTrue(Files.isRegularFile(outputBudget), "Importer必须输出BQMekaMemory.dat供最终PAC消费");
            var writtenRoster = (MekaGroupGrp) bsdxBinService.parse(outputRoot.resolve("MekaGroup.grp").toString(), CHARSET).getData();
            var writtenBudget = (Dat) bsdxBinService.parse(outputBudget.toString(), CHARSET).getData();
            assertEquals(114, writtenRoster.getMekaList().size());
            assertEquals(writtenRoster.getMekaList().size(), writtenBudget.getData().size(), "费用行必须覆盖实际写出的最终机体ID");
            assertEquals(1, writtenBudget.getColumnCount());
            byte[] writtenBytes = Files.readAllBytes(outputBudget);
            assertEquals(464, writtenBytes.length);
            assertArrayEquals(expectedPrefix, Arrays.copyOf(writtenBytes, expectedPrefix.length), "既有header和费用行必须逐字节保留");
            for (int id = followup ? 104 : 103; id < 114; id++) {
                assertEquals(100_000_000, ((Number) writtenBudget.getData().get(id).get(0)).intValue(),
                        "新增ID" + id + "必须有显式正调度权重");
            }
            assertEquals(1, output.getGeneratedDatFiles().stream().filter(outputBudget::equals).count(),
                    "费用表必须注册一次到实际输出清单");
        } finally {
            assertArrayEquals(originalBytes, Files.readAllBytes(sourceBudget), "原费用源不得被原地扩容");
            if (followup) assertArrayEquals(inheritedBefore, Files.readAllBytes(inheritedDir.resolve("BQMekaMemory.dat")),
                    "继承费用源不得被原地修改");
        }
    }

    private TsukuyomiBsdxBaselineBundle budgetBaseline() {
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
        MekaGroupGrp mekas = budgetGroup("mekagroup", new MekaGroupGrp());
        for (int id = 0; id < 114; id++) {
            var entry = new MekaGroupGrp.MekaGroup();
            entry.setExistFlag(id == 113 ? 1 : 0);
            if (id == 113) { entry.setMekaName("budget_fixture"); entry.setMekaCodeName("BUDGET_FIXTURE"); }
            mekas.getMekaList().add(entry);
        }
        baseline.setMekaGroupGrp(mekas);
        WazaGroupGrp wazas = budgetGroup("wazagroup", new WazaGroupGrp());
        var waza = new WazaGroupGrp.WazaGroupEntry();
        waza.setExistFlag(1); waza.setWazaName("budget fixture"); waza.setWazaCodeName("BUDGET_FIXTURE");
        waza.setWazaDisplayName("budget_fixture"); waza.setParam(1); wazas.getWazaList().add(waza);
        baseline.setWazaGroupGrp(wazas);
        SpriteGroupGrp sprites = budgetGroup("spritegroup", new SpriteGroupGrp());
        var sprite = new SpriteGroupGrp.SpriteGroupEntry();
        sprite.setExistFlag(1); sprite.setSpriteFileName("budget_fixture.spm"); sprite.setSpriteCodeName("BUDGET_FIXTURE");
        sprite.setParam(0); sprites.getSpriteList().add(sprite); baseline.setSpriteGroupGrp(sprites);
        baseline.setSeGroupGrp(budgetGroup("segroup", new SeGroupGrp()));
        baseline.setBatVoiceGrp(budgetGroup("batvoice", new BatVoiceGrp()));
        baseline.setMapGroupGrp(budgetGroup("mapgroup", new MapGroupGrp()));
        ProgramMaterialGrp program = budgetGroup("programmaterial", new ProgramMaterialGrp());
        program.getArray1().add(new ProgramMaterialGrp.IntArray());
        baseline.setProgramMaterialGrp(program);
        return baseline;
    }

    private <T extends Grp> T budgetGroup(String name, T group) {
        group.setFileName(name);
        group.setExtensionName("grp");
        return group;
    }

    @Test
    void writeSelectedConvertedSpmBeforeRejectingIncompleteDelivery() throws Exception {
        Path externalRoot = tempDir.resolve("external");
        Files.createDirectories(externalRoot);
        Files.writeString(externalRoot.resolve("tsukuyomi_001.png"), "image");

        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setExeOutputDir(tempDir.resolve("out"));
        request.setExternalStaticAssetRoot(externalRoot);

        TsukuyomiPackageBundle selectedBundle = new TsukuyomiPackageBundle();
        selectedBundle.getSpmByFileName().put("tsukuyomi.spm", minimalConvertedSpm("tsukuyomi_001.png"));

        TsukuyomiImportPlan importPlan = new TsukuyomiImportPlan();
        importPlan.getRequiredSpmFiles().add("tsukuyomi.spm");

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> step.importAssets(
                request,
                selectedBundle,
                new TsukuyomiBsdxBaselineBundle(),
                importPlan,
                null,
                null,
                null,
                new TsukuyomiGrpAppendPlan(),
                new BheCommonProjectileAppendPlan(),
                java.util.Map.of()
        ));
        assertTrue(failure.getMessage().contains("必需资源缺失"));
        Path outputRoot;
        try (var paths = Files.list(request.getExeOutputDir())) { outputRoot = paths.filter(Files::isDirectory).findFirst().orElseThrow(); }
        Path outputSpm = outputRoot.resolve("tsukuyomi.spm");
        assertTrue(Files.exists(outputSpm));

        Spm parsed = (Spm) bsdxBinService.parse(outputSpm.toString(), CHARSET).getData();
        assertEquals("SPM VER-2.00", parsed.getSpmVersion());
        assertEquals("tsukuyomi_001.png", parsed.getImageData().get(0).getImageName());
    }

    private Spm minimalConvertedSpm(String imageName) {
        Spm spm = new Spm();
        spm.setExtensionName("spm");
        spm.setSpmVersion("SPM VER-2.00");
        spm.setNumPageData(1);
        spm.setPageData(List.of(page()));
        spm.setNumImageData(1);
        Spm.SPMImageData imageData = new Spm.SPMImageData();
        imageData.setImageName(imageName);
        spm.setImageData(List.of(imageData));
        spm.setPatPageNum(1);
        spm.setNumAnimData(1);
        spm.setAnimData(List.of(anim()));
        return spm;
    }

    private Spm.SPMPageData page() {
        Spm.SPMPageData page = new Spm.SPMPageData();
        page.setNumChipData(1);
        page.setPageWidth(1);
        page.setPageHeight(1);
        page.setPageRect(rect());
        page.setPageOption(0L);
        page.setRotateCenterX(0);
        page.setRotateCenterY(0);
        page.setHitFlag(0L);
        page.setHitRects(new ArrayList<>());
        page.setChipData(List.of(chip()));
        return page;
    }

    private Spm.SPMChipData chip() {
        Spm.SPMChipData chip = new Spm.SPMChipData();
        chip.setImageNo(0);
        chip.setDstRect(rect());
        chip.setChipWidth(1);
        chip.setChipHeight(1);
        chip.setSrcRect(rect());
        chip.setDrawOption(0L);
        chip.setDrawOptionValue(0L);
        chip.setOption(0);
        return chip;
    }

    private Spm.SPMAnimData anim() {
        Spm.SPMAnimData anim = new Spm.SPMAnimData();
        anim.setAnimName("default");
        anim.setNumPat(1);
        anim.setAnimRotateDirection(0);
        anim.setAnimReverseDirection(0);
        Spm.SPMPatData pat = new Spm.SPMPatData();
        pat.setWaitFrame(1);
        pat.setPageNo(List.of(0));
        anim.setPatData(List.of(pat));
        return anim;
    }

    private Spm.SPMRect rect() {
        Spm.SPMRect rect = new Spm.SPMRect();
        rect.setLeft(0);
        rect.setTop(0);
        rect.setRight(1);
        rect.setBottom(1);
        return rect;
    }
}
