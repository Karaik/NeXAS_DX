package com.giga.nexas.transfer.bhe2bsdx.meka.motoki;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.initializer.EnsureInitializerWeaponStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.convert.BuildBaselineFromJinkiResultStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.convert.MotokiConvertOverviewStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.exe.PatchExeStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.graft.AppendGrpEntriesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.graft.BuildResourceClosureStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.graft.ImportStaticAssetsStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.graft.PadBaselineMekMaterialStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.graft.RebindMekStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.graft.RebindWazStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.graft.SyncProgramMaterialStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.pack.PackUpdatePacStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu.MenuOverrideContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu.MenuOverridePipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPacPackPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.motoki.MotokiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.motoki.MotokiGraftResult;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;

public class MotokiGraftPipeline {
    private static final String CHARSET = "windows-31j";

    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BuildBaselineFromJinkiResultStep buildBaselineFromJinkiResultStep = new BuildBaselineFromJinkiResultStep();
    private final MotokiConvertOverviewStep motokiConvertOverviewStep = new MotokiConvertOverviewStep();
    private final BuildResourceClosureStep buildResourceClosureStep = new BuildResourceClosureStep();
    private final AppendGrpEntriesStep appendGrpEntriesStep = new AppendGrpEntriesStep();
    private final SyncProgramMaterialStep syncProgramMaterialStep = new SyncProgramMaterialStep();
    private final PadBaselineMekMaterialStep padBaselineMekMaterialStep = new PadBaselineMekMaterialStep();
    private final RebindMekStep rebindMekStep = new RebindMekStep();
    private final RebindWazStep rebindWazStep = new RebindWazStep();
    private final EnsureInitializerWeaponStep ensureInitializerWeaponStep = new EnsureInitializerWeaponStep();
    private final ImportStaticAssetsStep importStaticAssetsStep = new ImportStaticAssetsStep();
    private final MenuOverridePipeline menuOverridePipeline = new MenuOverridePipeline();
    private final PatchExeStep patchExeStep = new PatchExeStep();
    private final PackUpdatePacStep packUpdatePacStep = new PackUpdatePacStep();

    public MotokiGraftResult execute(MotokiGraftRequest request) {
        MotokiGraftResult result = new MotokiGraftResult();
        if (request == null) {
            return result;
        }

        TsukuyomiBsdxBaselineBundle bsdxBaseline =
                request.getPreviousCharacterResult() != null
                        ? request.getPreviousCharacterResult().getBsdxBaseline()
                        : buildBaselineFromJinkiResult(request.getInheritedJinkiResult());
        overlayPreviousMenuOutputs(request, bsdxBaseline);
        result.setBsdxBaseline(bsdxBaseline);

        TsukuyomiConvertedBundle convertedBundle = motokiConvertOverviewStep.convert(request, bsdxBaseline);
        result.setRawSourceBundle(convertedBundle.getRawSourceBundle());
        result.setConvertedBundle(convertedBundle);
        result.setCommonProjectileAppendPlan(convertedBundle.getCommonProjectileAppendPlan());
        result.setPreparedBaselineBundle(convertedBundle.getPreparedBaselineBundle());
        bsdxBaseline = convertedBundle.getPreparedBaselineBundle();
        result.setBsdxBaseline(bsdxBaseline);

        TsukuyomiPackageBundle selectedPackage = convertedBundle.getSelectedResourceBundle();
        result.setTsukuyomiPackage(selectedPackage);

        TsukuyomiImportPlan importPlan = buildResourceClosureStep.buildResourceClosure(
                request,
                selectedPackage,
                bsdxBaseline,
                convertedBundle.getCommonProjectileAppendPlan()
        );
        result.setImportPlan(importPlan);

        TsukuyomiGrpAppendPlan grpAppendPlan = appendGrpEntriesStep.appendMotokiBranch(
                request,
                selectedPackage,
                bsdxBaseline,
                importPlan
        );
        result.setGrpAppendPlan(grpAppendPlan);

        result.setSyncedProgramMaterial(syncProgramMaterialStep.syncOuterArrays(request, bsdxBaseline, grpAppendPlan));
        padBaselineMekMaterialStep.padMaterialBlock(bsdxBaseline);

        Mek reboundMek = rebindMekStep.rebindMotokiMek(
                request,
                selectedPackage,
                grpAppendPlan,
                convertedBundle.getCommonProjectileAppendPlan()
        );
        Waz reboundWaz = rebindWazStep.rebindMotokiWaz(
                request,
                selectedPackage,
                importPlan,
                grpAppendPlan,
                convertedBundle.getCommonProjectileAppendPlan()
        );
        ensureInitializerWeaponStep.ensureAfterRebind(
                request.getWazFileName(),
                reboundMek,
                reboundWaz,
                convertedBundle.getNotes()
        );
        padBaselineMekMaterialStep.padMaterialBlock(reboundMek, bsdxBaseline);
        syncMainWazaSkillCountAfterInitializer(bsdxBaseline, grpAppendPlan, reboundWaz);
        result.setReboundTsukuyomiMek(reboundMek);
        result.setReboundTsukuyomiWaz(reboundWaz);

        TsukuyomiImportedAssetSet importedAssetSet = importStaticAssetsStep.importAssets(
                request,
                selectedPackage,
                bsdxBaseline,
                importPlan,
                result.getSyncedProgramMaterial(),
                result.getReboundTsukuyomiMek(),
                result.getReboundTsukuyomiWaz(),
                grpAppendPlan,
                convertedBundle.getCommonProjectileAppendPlan()
        );
        result.setImportedAssetSet(importedAssetSet);

        if (request.isPatchMenuData()) {
            MenuOverrideContext menuContext = menuOverridePipeline.execute(
                    request,
                    selectedPackage,
                    bsdxBaseline,
                    grpAppendPlan,
                    result.getReboundTsukuyomiMek(),
                    importedAssetSet.getOutputRootDir(),
                    request.getMenuOverrideSpec()
            );
            applyMenuContextToResultAndAssetSet(menuContext, result, importedAssetSet);
        }

        TsukuyomiExePatchPlan exePatchPlan = patchExeStep.patchExe(request, bsdxBaseline, grpAppendPlan, result);
        result.setExePatchPlan(exePatchPlan);
        TsukuyomiPacPackPlan pacPackPlan = packUpdatePacStep.packUpdatePac(request, importedAssetSet);
        result.setPacPackPlan(pacPackPlan);
        return result;
    }

    private TsukuyomiBsdxBaselineBundle buildBaselineFromJinkiResult(AkaoGraftResult inheritedJinkiResult) {
        return buildBaselineFromJinkiResultStep.buildBaselineFromJinkiResult(inheritedJinkiResult);
    }

    private void overlayPreviousMenuOutputs(
            MotokiGraftRequest request,
            TsukuyomiBsdxBaselineBundle bsdxBaseline
    ) {
        if (request == null || request.getPreviousCharacterResult() == null || bsdxBaseline == null) {
            return;
        }

        if (request.getPreviousCharacterResult().getPatchedMekaDat() != null) {
            bsdxBaseline.setMekaDat(request.getPreviousCharacterResult().getPatchedMekaDat());
        }
        if (request.getPreviousCharacterResult().getPatchedMekaPilotDat() != null) {
            bsdxBaseline.setMekaPilotDat(request.getPreviousCharacterResult().getPatchedMekaPilotDat());
        }
        if (request.getPreviousCharacterResult().getPatchedSelectMekaMenuDat() != null) {
            bsdxBaseline.setSelectMekaMenuDat(request.getPreviousCharacterResult().getPatchedSelectMekaMenuDat());
        }
        if (request.getPreviousCharacterResult().getPatchedMekaPilotSpm() != null) {
            bsdxBaseline.setMekaPilotSpm(request.getPreviousCharacterResult().getPatchedMekaPilotSpm());
        }
        if (request.getPreviousCharacterResult().getPatchedSelectMekaMenuMekaSpm() != null) {
            bsdxBaseline.setSelectMekaMenuMekaSpm(request.getPreviousCharacterResult().getPatchedSelectMekaMenuMekaSpm());
        }

        overlayPreviousWeaponEquipDat(request, bsdxBaseline);
    }

    private void overlayPreviousWeaponEquipDat(
            MotokiGraftRequest request,
            TsukuyomiBsdxBaselineBundle bsdxBaseline
    ) {
        if (request == null
                || request.getPreviousCharacterResult() == null
                || request.getPreviousCharacterResult().getImportedAssetSet() == null
                || request.getPreviousCharacterResult().getImportedAssetSet().getOutputRootDir() == null
                || bsdxBaseline == null) {
            return;
        }

        Path previousWeaponEquip = request.getPreviousCharacterResult()
                .getImportedAssetSet()
                .getOutputRootDir()
                .resolve("WeaponEquip.dat");
        if (!Files.exists(previousWeaponEquip)) {
            return;
        }

        try {
            Dat parsed = (Dat) bsdxBinService.parse(previousWeaponEquip.toString(), CHARSET).getData();
            bsdxBaseline.setWeaponEquipDat(parsed);
        } catch (IOException e) {
            throw new IllegalStateException("failed to overlay previous WeaponEquip.dat: " + previousWeaponEquip, e);
        }
    }

    private void syncMainWazaSkillCountAfterInitializer(
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            Waz reboundWaz
    ) {
        if (bsdxBaseline == null
                || bsdxBaseline.getWazaGroupGrp() == null
                || bsdxBaseline.getWazaGroupGrp().getWazaList() == null
                || grpAppendPlan == null
                || reboundWaz == null
                || reboundWaz.getSkillList() == null) {
            return;
        }
        int targetWazaIndex = grpAppendPlan.getWazaGroupIndex();
        if (targetWazaIndex < 0 || targetWazaIndex >= bsdxBaseline.getWazaGroupGrp().getWazaList().size()) {
            throw new IllegalStateException("motoki initializer sync target index out of range: " + targetWazaIndex);
        }
        bsdxBaseline.getWazaGroupGrp().getWazaList().get(targetWazaIndex).setParam(reboundWaz.getSkillList().size());
    }

    private void applyMenuContextToResultAndAssetSet(
            MenuOverrideContext menuContext,
            MotokiGraftResult result,
            TsukuyomiImportedAssetSet importedAssetSet
    ) {
        result.setPatchedMekaDat(menuContext.getPatchedMekaDat());
        result.setPatchedMekaPilotDat(menuContext.getPatchedMekaPilotDat());
        result.setPatchedSelectMekaMenuDat(menuContext.getPatchedSelectMekaMenuDat());
        result.setPatchedMekaPilotSpm(menuContext.getPatchedMekaPilotSpm());
        result.setPatchedSelectMekaMenuMekaSpm(menuContext.getPatchedSelectMekaMenuMekaSpm());
        Path outputRoot = importedAssetSet.getOutputRootDir();
        addGeneratedDat(importedAssetSet, outputRoot, "Meka.dat", menuContext.getPatchedMekaDat());
        addGeneratedDat(importedAssetSet, outputRoot, "MekaPilot.dat", menuContext.getPatchedMekaPilotDat());
        addGeneratedDat(importedAssetSet, outputRoot, "SelectMekaMenu.dat", menuContext.getPatchedSelectMekaMenuDat());
        addCopiedSpm(importedAssetSet, outputRoot, "MekaPilot.spm", menuContext.getPatchedMekaPilotSpm());
        addCopiedSpm(importedAssetSet, outputRoot, "SelectMekaMenuMeka.spm", menuContext.getPatchedSelectMekaMenuMekaSpm());
        for (Path copiedImage : menuContext.getAudit().getCopiedImages()) {
            if (!importedAssetSet.getCopiedImageFiles().contains(copiedImage)) {
                importedAssetSet.getCopiedImageFiles().add(copiedImage);
            }
        }
        importedAssetSet.getMissingAssets().addAll(menuContext.getAudit().getMissingImages());
    }

    private void addGeneratedDat(TsukuyomiImportedAssetSet importedAssetSet, Path outputRoot, String fileName, Dat dat) {
        if (dat == null) {
            return;
        }
        Path path = outputRoot.resolve(fileName);
        if (!importedAssetSet.getGeneratedDatFiles().contains(path)) {
            importedAssetSet.getGeneratedDatFiles().add(path);
        }
    }

    private void addCopiedSpm(TsukuyomiImportedAssetSet importedAssetSet, Path outputRoot, String fileName, Spm spm) {
        if (spm == null) {
            return;
        }
        Path path = outputRoot.resolve(fileName);
        if (!importedAssetSet.getCopiedSpmFiles().contains(path)) {
            importedAssetSet.getCopiedSpmFiles().add(path);
        }
    }
}
