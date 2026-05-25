package com.giga.nexas.transfer.bhe2bsdx.meka.followup;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.ImportBheMapsIntoCurrentResourceTreeRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.ImportBheMapsIntoCurrentResourceTreeResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline.ImportBheMapsIntoCurrentResourceTreeStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.BuildBaselineFromJinkiResultStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.FollowupConvertOverviewStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;
import com.giga.nexas.transfer.bhe2bsdx.meka.initializer.EnsureInitializerWeaponStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.exe.PatchExeStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.AppendGrpEntriesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.BuildResourceClosureStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.ImportStaticAssetsStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.PadBaselineMekMaterialStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.RebindMekStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.RebindWazStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.SyncProgramMaterialStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.pack.PackUpdatePacStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu.MenuOverrideContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu.MenuOverridePipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPacPackPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 后续单机体公共主线。
 *
 * <p>这里承接 `tsukuyomi` 之后的所有 follow-up 机体：
 * `yuri / freja / nagi / misaki / naoto / katou / wilhelm / motoki / sou`。
 * 主线职责固定为：
 * 基线继承 -> convert -> closure -> grp append -> rebind -> 落盘 -> 菜单覆盖 -> exe patch -> pack。
 * 各机体如果以后出现单独特例，只允许通过 `FollowupMekaCustomizer` 在落盘前插一次客制化，
 * 不再把 if 散回主线步骤里。</p>
 */
public class FollowupGraftPipeline {

    private static final String CHARSET = "windows-31j";

    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BuildBaselineFromJinkiResultStep buildBaselineFromJinkiResultStep = new BuildBaselineFromJinkiResultStep();
    private final FollowupConvertOverviewStep followupConvertOverviewStep = new FollowupConvertOverviewStep();
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

    /**
     * 执行一轮 follow-up 单机体 graft。
     *
     * <p>这里保留一个泛型 `resultFactory`，原因是外层仍希望拿到各机体自己的 Result 子类，
     * 但主线内部不再复制 9 份相同实现。`customizer` 是唯一保留的机体扩展点，
     * 当前默认都接 no-op。</p>
     */
    public <R extends FollowupGraftResult> R execute(
            FollowupGraftRequest request,
            Supplier<R> resultFactory,
            FollowupMekaCustomizer customizer
    ) {
        R result = resultFactory.get();
        if (request == null) {
            return result;
        }

        TsukuyomiBsdxBaselineBundle bsdxBaseline =
                request.getPreviousCharacterResult() != null
                        ? request.getPreviousCharacterResult().getBsdxBaseline()
                        : buildBaselineFromJinkiResult(request.getInheritedJinkiResult());
        // follow-up 机体都站在“上一机体已经写回菜单覆盖”的成果物上继续追加，
        // 所以本层基线不是固定原始 BSDX，而是上一路结果。
        overlayPreviousMenuOutputs(request, bsdxBaseline);
        result.setBsdxBaseline(bsdxBaseline);

        TsukuyomiConvertedBundle convertedBundle = followupConvertOverviewStep.convert(
                request,
                request.getPreviousCharacterResult(),
                bsdxBaseline
        );
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

        TsukuyomiGrpAppendPlan grpAppendPlan = appendGrpEntriesStep.appendFollowupBranch(
                request,
                selectedPackage,
                bsdxBaseline,
                importPlan
        );
        result.setGrpAppendPlan(grpAppendPlan);
        result.setCumulativeSourceBatVoiceGroupIndexToTargetIndex(
                buildCumulativeVoiceGroupMap(request.getPreviousCharacterResult(), grpAppendPlan)
        );

        ProgramMaterialGrp syncedProgramMaterial = syncProgramMaterialStep.syncOuterArrays(request, bsdxBaseline, grpAppendPlan);
        result.setSyncedProgramMaterial(syncedProgramMaterial);
        padBaselineMekMaterialStep.padMaterialBlock(bsdxBaseline);

        // 主机体 MEK/WAZ 在这里完成目标索引空间重绑。
        // 这之后才允许跑 initializer 和客制化，因为它们都必须作用在目标侧 DTO 上。
        Mek reboundMek = rebindMekStep.rebindFollowupMek(
                request,
                selectedPackage,
                grpAppendPlan,
                convertedBundle.getCommonProjectileAppendPlan()
        );
        Waz reboundWaz = rebindWazStep.rebindFollowupWaz(
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

        // 这是公共主线唯一保留的机体特例入口。
        // 约束：只能做单机体的落盘前修正，不能回头改公共资源层或主 rebind 规则。
        FollowupMekaContext context = new FollowupMekaContext();
        context.setRequest(request);
        context.setBaseline(bsdxBaseline);
        context.setConvertedBundle(convertedBundle);
        context.setSelectedPackage(selectedPackage);
        context.setImportPlan(importPlan);
        context.setGrpAppendPlan(grpAppendPlan);
        context.setSyncedProgramMaterial(syncedProgramMaterial);
        context.setReboundMek(reboundMek);
        context.setReboundWaz(reboundWaz);
        customizer.customizeBeforeWrite(context);

        bsdxBaseline = context.getBaseline();
        selectedPackage = context.getSelectedPackage();
        importPlan = context.getImportPlan();
        grpAppendPlan = context.getGrpAppendPlan();
        syncedProgramMaterial = context.getSyncedProgramMaterial();
        reboundMek = context.getReboundMek();
        reboundWaz = context.getReboundWaz();

        result.setBsdxBaseline(bsdxBaseline);
        result.setTsukuyomiPackage(selectedPackage);
        result.setImportPlan(importPlan);
        result.setGrpAppendPlan(grpAppendPlan);
        result.setCumulativeSourceBatVoiceGroupIndexToTargetIndex(
                buildCumulativeVoiceGroupMap(request.getPreviousCharacterResult(), grpAppendPlan)
        );
        result.setSyncedProgramMaterial(syncedProgramMaterial);
        result.setReboundTsukuyomiMek(reboundMek);
        result.setReboundTsukuyomiWaz(reboundWaz);

        // 从这里开始进入“产物沉淀层”。后续 pack 只打这棵目录，
        // 所以所有最终物料必须先统一落到 importedAssetSet.outputRootDir。
        TsukuyomiImportedAssetSet importedAssetSet = importStaticAssetsStep.importAssets(
                request,
                selectedPackage,
                bsdxBaseline,
                importPlan,
                syncedProgramMaterial,
                reboundMek,
                reboundWaz,
                grpAppendPlan,
                convertedBundle.getCommonProjectileAppendPlan(),
                result.getCumulativeSourceBatVoiceGroupIndexToTargetIndex()
        );
        result.setImportedAssetSet(importedAssetSet);

        ImportBheMapsIntoCurrentResourceTreeResult mapAppendResult = importMapsIntoCurrentResourceTree(request, importedAssetSet);
        result.setMapAppendResult(mapAppendResult);
        if (mapAppendResult != null && !mapAppendResult.isCanContinueStaticPipeline()) {
            return result;
        }

        if (request.isPatchMenuData()) {
            MenuOverrideContext menuContext = menuOverridePipeline.execute(
                    request,
                    selectedPackage,
                    bsdxBaseline,
                    grpAppendPlan,
                    reboundMek,
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

    private ImportBheMapsIntoCurrentResourceTreeResult importMapsIntoCurrentResourceTree(
            FollowupGraftRequest request,
            TsukuyomiImportedAssetSet importedAssetSet
    ) {
        if (request == null || !request.isMapAppendEnabled()) {
            return null;
        }
        if (importedAssetSet == null || importedAssetSet.getOutputRootDir() == null) {
            throw new IllegalArgumentException("mapappend requires importedAssetSet.outputRootDir");
        }
        Objects.requireNonNull(request.getBheMapGroupPath(), "bheMapGroupPath must be provided when mapappend is enabled");
        Objects.requireNonNull(request.getBheMapDir(), "bheMapDir must be provided when mapappend is enabled");
        Objects.requireNonNull(request.getBheStaticResourceRoot(), "bheStaticResourceRoot must be provided when mapappend is enabled");

        Path outputRoot = importedAssetSet.getOutputRootDir();
        ImportBheMapsIntoCurrentResourceTreeRequest mapRequest = new ImportBheMapsIntoCurrentResourceTreeRequest();
        mapRequest.setBheMapGroupPath(request.getBheMapGroupPath());
        mapRequest.setBheMapDir(request.getBheMapDir());
        mapRequest.setBheStaticResourceRoot(request.getBheStaticResourceRoot());
        mapRequest.setCurrentTargetMapGroupPath(outputRoot.resolve("MapGroup.grp"));
        mapRequest.setOutputRoot(outputRoot);
        mapRequest.setCharset(CHARSET);
        mapRequest.setPreviewMaterializationEnabled(false);
        return new ImportBheMapsIntoCurrentResourceTreeStep().importMaps(mapRequest);
    }

    /**
     * 把 JINKI 结果翻译成当前 follow-up 层可继续追加的基线视图。
     */
    private TsukuyomiBsdxBaselineBundle buildBaselineFromJinkiResult(AkaoGraftResult inheritedJinkiResult) {
        return buildBaselineFromJinkiResultStep.buildBaselineFromJinkiResult(inheritedJinkiResult);
    }

    private Map<Integer, Integer> buildCumulativeVoiceGroupMap(
            TsukuyomiGraftResult previousResult,
            TsukuyomiGrpAppendPlan currentPlan
    ) {
        Map<Integer, Integer> merged = new LinkedHashMap<>();
        if (previousResult != null && previousResult.getCumulativeSourceBatVoiceGroupIndexToTargetIndex() != null) {
            merged.putAll(previousResult.getCumulativeSourceBatVoiceGroupIndexToTargetIndex());
        }
        if (currentPlan != null && currentPlan.getSourceBatVoiceGroupIndexToTargetIndex() != null) {
            merged.putAll(currentPlan.getSourceBatVoiceGroupIndexToTargetIndex());
        }
        return merged;
    }

    /**
     * 继承上一机体已经写回的菜单产物。
     *
     * <p>follow-up 角色是链式追加，不是各跑各的菜单覆盖。
     * 所以这里必须先把上一机体已经改过的 DAT/SPM 再叠到当前基线上，
     * 否则后一个角色会把前一个角色的菜单结果覆盖回旧值。</p>
     */
    private void overlayPreviousMenuOutputs(
            FollowupGraftRequest request,
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

    /**
     * `WeaponEquip.dat` 不在结果对象里长期挂 DTO，所以这里按上一轮输出目录反解回来。
     */
    private void overlayPreviousWeaponEquipDat(
            FollowupGraftRequest request,
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

    /**
     * initializer 可能追加新 skill，因此要把主 WazaGroup.param 同步到真实 skill 数。
     */
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
            throw new IllegalStateException("followup initializer sync target index out of range: " + targetWazaIndex);
        }
        bsdxBaseline.getWazaGroupGrp().getWazaList().get(targetWazaIndex).setParam(reboundWaz.getSkillList().size());
    }

    /**
     * 菜单覆盖步骤自己会写文件，但结果对象和 importedAssetSet 也要同步记录，
     * 否则后续 manifest / pack / 审计看不到这些菜单产物来自哪里。
     */
    private void applyMenuContextToResultAndAssetSet(
            MenuOverrideContext menuContext,
            FollowupGraftResult result,
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
