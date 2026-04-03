package com.giga.nexas.transfer.jinki2bsdx;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.ExePatchPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiDiffManifest;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;
import com.giga.nexas.transfer.jinki2bsdx.steps.AppendGrpEntriesStep;
import com.giga.nexas.transfer.jinki2bsdx.steps.BuildDiffManifestStep;
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
 * <p>当前还是骨架阶段，但执行顺序已经固定，后续真实实现应当严格按这条主链填充。</p>
 *
 * <ol>
 *   <li>反序列化 {@code src/main/resources/game/jinki} 下的资源包</li>
 *   <li>对比 BSDX 基线资源，生成复用/导入 manifest</li>
 *   <li>追加 {@code MekaGroup / WazaGroup / SpriteGroup / BatVoice} 顶层条目</li>
 *   <li>同步 {@code ProgramMaterial.grp} 的外层数组长度</li>
 *   <li>重绑 {@code Akao.mek} 的外部序号</li>
 *   <li>重绑 {@code Akao.waz} 的外部 {@code spm/waz} 引用</li>
 *   <li>整理静态资源导入集</li>
 *   <li>需要菜单可见时，再补菜单层数据</li>
 *   <li>真正追加触发容量上限时，再规划 exe patch</li>
 * </ol>
 */
public class AkaoGraftPipeline {

    private final DeserializeJinkiPackageStep deserializeJinkiPackageStep = new DeserializeJinkiPackageStep();
    private final LoadBsdxBaselineStep loadBsdxBaselineStep = new LoadBsdxBaselineStep();
    private final BuildDiffManifestStep buildDiffManifestStep = new BuildDiffManifestStep();
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

        // Step 1: 通过 deserializePackage(...) 反序列化 JINKI 包内资源。
        JinkiPackageBundle jinkiPackage = deserializeJinkiPackageStep.deserializePackage(request);
        result.setJinkiPackage(jinkiPackage);

        // Step 2: 通过 loadBaseline(...) 读取 BSDX 基线资源。
        BsdxBaselineBundle bsdxBaseline = loadBsdxBaselineStep.loadBaseline(request);
        result.setBsdxBaseline(bsdxBaseline);

        // Step 3: 通过 buildManifest(...) 生成复用/导入差异清单。
        JinkiDiffManifest diffManifest = buildDiffManifestStep.buildManifest(request, jinkiPackage, bsdxBaseline);
        result.setDiffManifest(diffManifest);

        // Step 4: 通过 appendAkaoBranch(...) 追加 AKAO 分支所需的 grp 顶层条目。
        GrpAppendPlan grpAppendPlan = appendGrpEntriesStep.appendAkaoBranch(request, jinkiPackage, bsdxBaseline, diffManifest);
        result.setGrpAppendPlan(grpAppendPlan);

        // Step 5: 通过 syncOuterArrays(...) 同步 ProgramMaterial 外层数组长度。
        result.setSyncedProgramMaterial(
                syncProgramMaterialStep.syncOuterArrays(request, bsdxBaseline, grpAppendPlan)
        );

        // Step 6: 通过 rebindAkaoMek(...) 回写 Akao.mek 的外部序号。
        result.setReboundAkaoMek(
                rebindAkaoMekStep.rebindAkaoMek(request, jinkiPackage, grpAppendPlan)
        );

        // Step 7: 通过 rebindAkaoWaz(...) 回写 Akao.waz 的外部引用。
        result.setReboundAkaoWaz(
                rebindAkaoWazStep.rebindAkaoWaz(request, jinkiPackage, diffManifest, grpAppendPlan)
        );

        // Step 8: 通过 importAssets(...) 整理需要补入的静态资源集合。
        ImportedAssetSet importedAssetSet =
                importStaticAssetsStep.importAssets(request, jinkiPackage, diffManifest);
        result.setImportedAssetSet(importedAssetSet);

        // Step 9: 通过 patchMenuData(...) 预留菜单层补丁输出。
        patchMenuDataStep.patchMenuData(request, jinkiPackage, bsdxBaseline, grpAppendPlan, result);

        // Step 10: 通过 planCapacityPatches(...) 规划 exe 容量补丁位点。
        ExePatchPlan exePatchPlan =
                planExeCapacityPatchesStep.planCapacityPatches(request, bsdxBaseline, grpAppendPlan);
        result.setExePatchPlan(exePatchPlan);

        return result;
    }
}
