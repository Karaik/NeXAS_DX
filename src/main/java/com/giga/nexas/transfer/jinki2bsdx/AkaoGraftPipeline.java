package com.giga.nexas.transfer.jinki2bsdx;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.ExePatchPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiImportPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;
import com.giga.nexas.transfer.jinki2bsdx.steps.AppendGrpEntriesStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.BuildImportPlanStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.DeserializeJinkiPackageStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.ImportStaticAssetsStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.LoadBsdxBaselineStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.PatchMenuDataStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.PlanExeCapacityPatchesStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.RebindAkaoMekStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.RebindAkaoWazStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.SyncProgramMaterialStep;

/**
 * AKAO / moribito_2 从 JINKI 并入 BSDX 的主流程骨架。
 *
 * <p>当前已经落地到 step1/2/3，后续步骤仍是骨架，但执行顺序已经固定。</p>
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
    private final PlanExeCapacityPatchesStep planExeCapacityPatchesStep = new PlanExeCapacityPatchesStep();

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

        // Step 3: 生成按 JINKI 真源整体导入的计划。
        JinkiImportPlan importPlan = buildImportPlanStep.buildImportPlan(request, jinkiPackage, bsdxBaseline);
        result.setImportPlan(importPlan);

        // Step 4: 追加 AKAO 分支所需的 grp 顶层条目。
        GrpAppendPlan grpAppendPlan =
                appendGrpEntriesStep.appendAkaoBranch(request, jinkiPackage, bsdxBaseline, importPlan);
        result.setGrpAppendPlan(grpAppendPlan);

        // Step 5: 同步 ProgramMaterial 外层数组长度。
        result.setSyncedProgramMaterial(
                syncProgramMaterialStep.syncOuterArrays(request, bsdxBaseline, grpAppendPlan)
        );

        // Step 6: 回写 Akao.mek 的外部序号。
        result.setReboundAkaoMek(
                rebindAkaoMekStep.rebindAkaoMek(request, jinkiPackage, grpAppendPlan)
        );

        // Step 7: 回写 Akao.waz 的外部引用。
        result.setReboundAkaoWaz(
                rebindAkaoWazStep.rebindAkaoWaz(request, jinkiPackage, importPlan, grpAppendPlan)
        );

        // Step 8: 整理需要补入的静态资源集合。
        ImportedAssetSet importedAssetSet =
                importStaticAssetsStep.importAssets(request, jinkiPackage, importPlan);
        result.setImportedAssetSet(importedAssetSet);

        // Step 9: 预留菜单层补丁输出。
        patchMenuDataStep.patchMenuData(request, jinkiPackage, bsdxBaseline, grpAppendPlan, result);

        // Step 10: 规划 exe 容量补丁位点。
        ExePatchPlan exePatchPlan =
                planExeCapacityPatchesStep.planCapacityPatches(request, bsdxBaseline, grpAppendPlan);
        result.setExePatchPlan(exePatchPlan);

        return result;
    }
}
