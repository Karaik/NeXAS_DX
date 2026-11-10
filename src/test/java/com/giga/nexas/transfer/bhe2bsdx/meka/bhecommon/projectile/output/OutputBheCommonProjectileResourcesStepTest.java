package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.output;

import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputBheCommonProjectileResourcesStepTest {

    @TempDir
    Path tempDir;

    private final OutputBheCommonProjectileResourcesStep step = new OutputBheCommonProjectileResourcesStep();

    @Test
    void outputCommonProjectileFilesAndSidecars() throws Exception {
        Path externalRoot = tempDir.resolve("external");
        Path outputRoot = tempDir.resolve("output");
        Files.createDirectories(externalRoot);
        Files.createDirectories(outputRoot);
        Files.writeString(externalRoot.resolve("Tama_001_0001.png"), "image");
        Files.writeString(externalRoot.resolve("RE_tama10.wav"), "wav");
        Files.writeString(externalRoot.resolve("RE_tama10.ogg"), "ogg");

        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setExternalStaticAssetRoot(externalRoot);
        TsukuyomiImportedAssetSet assetSet = new TsukuyomiImportedAssetSet();
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
        baseline.setSpriteGroupGrp(spriteGroup("bhe_Tama.spm"));
        baseline.getWazByFileName().put("bhe_Effect.waz", minimalWazWithSpriteRef());
        baseline.getSpmByFileName().put("bhe_Tama.spm", minimalSpm("bhe_Tama_001_0001.png"));

        BheCommonProjectileAppendPlan plan = new BheCommonProjectileAppendPlan();
        plan.getCommonProjectileWazFiles().add("bhe_Effect.waz");
        plan.getCommonProjectileSpmFiles().add("bhe_Tama.spm");
        plan.getSourceWazIndexToTargetFileName().put(0, "bhe_Effect.waz");
        plan.getSourceWazIndexToTargetSkillBase().put(0, 0);
        plan.getSourceWazIndexToTargetSkillCount().put(0, 1);
        plan.getSourceSePairToTargetFileName().put(BheCommonProjectileAppendPlan.sePairKey(1, 123), "bhe_RE_tama10");

        step.output(request, baseline, plan, java.util.Map.of(), outputRoot, assetSet);

        assertTrue(Files.exists(outputRoot.resolve("bhe_Effect.waz")));
        assertTrue(Files.exists(outputRoot.resolve("bhe_Tama.spm")));
        assertTrue(Files.exists(outputRoot.resolve("bhe_Tama_001_0001.png")));
        assertTrue(Files.exists(outputRoot.resolve("bhe_RE_tama10.ogg")));
        assertTrue(assetSet.getGeneratedWazFiles().contains(outputRoot.resolve("bhe_Effect.waz")));
        assertTrue(assetSet.getCopiedSpmFiles().contains(outputRoot.resolve("bhe_Tama.spm")));
        assertTrue(assetSet.getCopiedImageFiles().contains(outputRoot.resolve("bhe_Tama_001_0001.png")));
        assertTrue(assetSet.getCopiedAudioFiles().contains(outputRoot.resolve("bhe_RE_tama10.ogg")));
        assertTrue(assetSet.getMissingAssets().isEmpty());
    }

    @Test
    void recordMissingSidecars() throws Exception {
        Path externalRoot = tempDir.resolve("missingExternal");
        Path outputRoot = tempDir.resolve("missingOutput");
        Files.createDirectories(externalRoot);
        Files.createDirectories(outputRoot);

        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setExternalStaticAssetRoot(externalRoot);
        TsukuyomiImportedAssetSet assetSet = new TsukuyomiImportedAssetSet();
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
        baseline.setSpriteGroupGrp(spriteGroup("bhe_Tama.spm"));
        baseline.getWazByFileName().put("bhe_Effect.waz", minimalWazWithSpriteRef());
        baseline.getSpmByFileName().put("bhe_Tama.spm", minimalSpm("bhe_Missing.png"));

        BheCommonProjectileAppendPlan plan = new BheCommonProjectileAppendPlan();
        plan.getCommonProjectileWazFiles().add("bhe_Effect.waz");
        plan.getCommonProjectileSpmFiles().add("bhe_Tama.spm");
        plan.getSourceWazIndexToTargetFileName().put(0, "bhe_Effect.waz");
        plan.getSourceWazIndexToTargetSkillBase().put(0, 0);
        plan.getSourceWazIndexToTargetSkillCount().put(0, 1);
        plan.getSourceSePairToTargetFileName().put(BheCommonProjectileAppendPlan.sePairKey(1, 124), "bhe_MissingAudio");

        step.output(request, baseline, plan, java.util.Map.of(), outputRoot, assetSet);

        assertTrue(assetSet.getMissingAssets().stream().anyMatch(item -> item.contains("bhe_Missing.png")));
        assertTrue(assetSet.getMissingAssets().stream().anyMatch(item -> item.contains("bhe_MissingAudio")));
    }

    private Waz minimalWaz() {
        Waz waz = new Waz();
        waz.setExtensionName("waz");
        waz.setSkillList(new ArrayList<>());
        return waz;
    }

    private Waz minimalWazWithSpriteRef() {
        Waz waz = minimalWaz();
        Waz.Skill skill = new Waz.Skill();
        skill.setPhaseQuantity(1);
        skill.setSkillNameJapanese("test");
        skill.setSkillNameEnglish("test");
        Waz.Skill.SkillPhase phase = new Waz.Skill.SkillPhase();
        SkillUnit unit = new SkillUnit(0, "sprite");
        CEventSprite sprite = new CEventSprite(13);
        sprite.setStartFrame(0);
        sprite.setEndFrame(0);
        sprite.setSpmFileSequence(0);
        sprite.setActionGroupNumber(0);
        sprite.setActionNumber(0);
        unit.getSkillInfoObjectList().add(sprite);
        phase.getSkillUnitCollection().add(unit);
        skill.getPhasesInfo().add(phase);
        waz.getSkillList().add(skill);
        return waz;
    }

    private SpriteGroupGrp spriteGroup(String fileName) {
        SpriteGroupGrp group = new SpriteGroupGrp();
        SpriteGroupGrp.SpriteGroupEntry entry = new SpriteGroupGrp.SpriteGroupEntry();
        entry.setExistFlag(1);
        entry.setSpriteFileName(fileName);
        entry.setSpriteCodeName("BHE_TEST");
        entry.setParam(0);
        group.getSpriteList().add(entry);
        return group;
    }

    private Spm minimalSpm(String imageName) {
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
