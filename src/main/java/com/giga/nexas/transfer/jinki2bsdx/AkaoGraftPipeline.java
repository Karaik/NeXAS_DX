package com.giga.nexas.transfer.jinki2bsdx;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.ExePatchPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiImportPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.PacPackPlan;
import com.giga.nexas.transfer.jinki2bsdx.steps.AppendGrpEntriesStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.BuildImportPlanStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.DeserializeJinkiPackageStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.ImportStaticAssetsStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.LoadBsdxBaselineStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.PackUpdatePacStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.PadBaselineMekMaterialStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.PatchExeCapacitiesStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.PatchMenuDataStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.RebindAkaoMekStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.RebindAkaoWazStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.SyncProgramMaterialStep;

/**
 * AKAO / moribito_2 从 JINKI 并入 BSDX 的主流程。
 */
public class AkaoGraftPipeline {

    private final DeserializeJinkiPackageStep deserializeJinkiPackageStep = new DeserializeJinkiPackageStep();
    private final LoadBsdxBaselineStep loadBsdxBaselineStep = new LoadBsdxBaselineStep();
    private final BuildImportPlanStep buildImportPlanStep = new BuildImportPlanStep();
    private final AppendGrpEntriesStep appendGrpEntriesStep = new AppendGrpEntriesStep();
    private final SyncProgramMaterialStep syncProgramMaterialStep = new SyncProgramMaterialStep();
    private final RebindAkaoMekStep rebindAkaoMekStep = new RebindAkaoMekStep();
    private final RebindAkaoWazStep rebindAkaoWazStep = new RebindAkaoWazStep();
    private final ImportStaticAssetsStep importStaticAssetsStep = new ImportStaticAssetsStep();
    private final PatchMenuDataStep patchMenuDataStep = new PatchMenuDataStep();
    private final PadBaselineMekMaterialStep padBaselineMekMaterialStep = new PadBaselineMekMaterialStep();
    private final PatchExeCapacitiesStep patchExeCapacitiesStep = new PatchExeCapacitiesStep();
    private final PackUpdatePacStep packUpdatePacStep = new PackUpdatePacStep();

    public AkaoGraftResult execute(AkaoGraftRequest request) {
        AkaoGraftResult result = new AkaoGraftResult();
        if (request == null) {
            return result;
        }

        // Step 1: 反序列化 JINKI 包内资源。
        JinkiPackageBundle jinkiPackage = deserializeJinkiPackageStep.deserializePackage(request);
        result.setJinkiPackage(jinkiPackage);

        // Step 2: 加载 BSDX 基线容器。
        BsdxBaselineBundle bsdxBaseline = loadBsdxBaselineStep.loadBaseline(request);
        result.setBsdxBaseline(bsdxBaseline);

        // Step 3: 生成按当前机体资源链驱动的 ImportPlan。
        JinkiImportPlan importPlan = buildImportPlanStep.buildImportPlan(request, jinkiPackage, bsdxBaseline);
        result.setImportPlan(importPlan);

        // Step 4: 把当前机体链涉及到的 grp 条目挂进 BSDX，并产出源到目标索引映射。
        GrpAppendPlan grpAppendPlan =
                appendGrpEntriesStep.appendAkaoBranch(request, jinkiPackage, bsdxBaseline, importPlan);
        result.setGrpAppendPlan(grpAppendPlan);

        // Step 5: 同步 ProgramMaterial 外层长度。
        result.setSyncedProgramMaterial(
                syncProgramMaterialStep.syncOuterArrays(request, bsdxBaseline, grpAppendPlan)
        );

        // Step 5.5: 补齐所有基线机体的 CMaterial 组数，对齐追加后的 grp。
        padBaselineMekMaterialStep.padMaterialBlock(bsdxBaseline);

        // Step 6: 重绑 Akao.mek。
        result.setReboundAkaoMek(
                rebindAkaoMekStep.rebindAkaoMek(request, jinkiPackage, grpAppendPlan)
        );

        // Step 7: 重绑 Akao.waz。
        result.setReboundAkaoWaz(
                rebindAkaoWazStep.rebindAkaoWaz(request, jinkiPackage, importPlan, grpAppendPlan)
        );

        // Step 8: 平铺落盘当前机体链的静态资源。
        ImportedAssetSet importedAssetSet =
                importStaticAssetsStep.importAssets(
                        request,
                        jinkiPackage,
                        bsdxBaseline,
                        importPlan,
                        result.getSyncedProgramMaterial(),
                        result.getReboundAkaoMek(),
                        result.getReboundAkaoWaz()
                );
        result.setImportedAssetSet(importedAssetSet);

        // Step 9: 补菜单层 dat。
        patchMenuDataStep.patchMenuData(request, jinkiPackage, bsdxBaseline, grpAppendPlan, result);

        // Step 10: 汇总容量并 patch 当前已确认的 exe 位点。
        ExePatchPlan exePatchPlan =
                patchExeCapacitiesStep.patchExeCapacities(request, bsdxBaseline, grpAppendPlan, result);
        result.setExePatchPlan(exePatchPlan);

        // Step 11: 把平铺后的输出目录打包成 Update3.pac。
        PacPackPlan pacPackPlan = packUpdatePacStep.packUpdatePac(request, importedAssetSet);
        result.setPacPackPlan(pacPackPlan);

        return result;
    }
}
