package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft;

import com.giga.nexas.dto.bsdx.spm.Spm;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportStaticAssetsStepBheOutputTest {

    private static final String CHARSET = "windows-31j";

    @TempDir
    Path tempDir;

    private final ImportStaticAssetsStep step = new ImportStaticAssetsStep();
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    @Test
    void writeSelectedSpmFromConvertedBundleInsteadOfCopyingBheBinary() throws Exception {
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

        TsukuyomiImportedAssetSet result = step.importAssets(
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
        );

        Path outputSpm = result.getOutputRootDir().resolve("tsukuyomi.spm");
        assertTrue(Files.exists(outputSpm));
        assertTrue(result.getCopiedSpmFiles().contains(outputSpm));
        assertFalse(result.getMissingAssets().stream().anyMatch(item -> item.contains("spm")));

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
