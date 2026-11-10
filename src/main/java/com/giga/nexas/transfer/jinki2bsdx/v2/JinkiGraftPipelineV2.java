package com.giga.nexas.transfer.jinki2bsdx.v2;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.ExePatchPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiImportPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.PacPackPlan;
import com.giga.nexas.transfer.jinki2bsdx.v2.exe.PatchExeStepV2;
import com.giga.nexas.transfer.jinki2bsdx.v2.graft.AppendGrpEntriesStepV2;
import com.giga.nexas.transfer.jinki2bsdx.v2.graft.BuildResourceClosureStep;
import com.giga.nexas.transfer.jinki2bsdx.v2.graft.ImportStaticAssetsStepV2;
import com.giga.nexas.transfer.jinki2bsdx.v2.graft.LoadGraftBaselineStep;
import com.giga.nexas.transfer.jinki2bsdx.v2.graft.LoadJinkiSourceAssetsStep;
import com.giga.nexas.transfer.jinki2bsdx.v2.graft.PadBaselineMekMaterialStepV2;
import com.giga.nexas.transfer.jinki2bsdx.v2.graft.RebindMekStepV2;
import com.giga.nexas.transfer.jinki2bsdx.v2.graft.RebindWazStepV2;
import com.giga.nexas.transfer.jinki2bsdx.v2.graft.SyncProgramMaterialStepV2;
import com.giga.nexas.transfer.jinki2bsdx.v2.menu.MenuOverrideContext;
import com.giga.nexas.transfer.jinki2bsdx.v2.menu.MenuOverridePipelineV2;
import com.giga.nexas.transfer.jinki2bsdx.v2.menu.MenuOverrideSpec;
import com.giga.nexas.transfer.jinki2bsdx.v2.pack.PackUpdatePacStepV2;

import java.nio.file.Path;

/**
 * JINKI -> BSDX V2 orchestration.
 *
 * <p>这条 V2 主线保留旧 pipeline 作为验收基线，但自身不再直接调用旧 steps 包。
 * 每次内部替换都以最终输出目录和解包后的 Update3.pac 逐文件 byte parity 作为硬闸。</p>
 */
public class JinkiGraftPipelineV2 {

    /**
     * 源游戏转换层入口。
     *
     * <p>当前 JINKI 资产已经能直接解析成 BSDX DTO，所以这一步主要负责加载；
     * 未来 BHE 接入时，对应 converter 应在进入通用 graft 主线前完成。</p>
     */
    private final LoadJinkiSourceAssetsStep loadJinkiSourceAssetsStep = new LoadJinkiSourceAssetsStep();

    /**
     * 链式成果物 baseline 加载入口。
     *
     * <p>当前阶段读取原始 BSDX baseline；后续 BHE 阶段可以把这里的输入换成
     * BSDX+JINKI 成果物，从而继续继承上一层输出。</p>
     */
    private final LoadGraftBaselineStep loadGraftBaselineStep = new LoadGraftBaselineStep();

    /**
     * 前置客制化输入收束入口。
     *
     * <p>它把本次 AKAO graft 需要的 MEK/WAZ/SPM/SE 资源算成闭包，
     * 后续 step 只消费这个计划，不再自行猜测哪些资源应该被复制。</p>
     */
    private final BuildResourceClosureStep buildResourceClosureStep = new BuildResourceClosureStep();

    /**
     * GRP append / reuse 入口。
     *
     * <p>这是 source index 到 target index 的核心映射产地；
     * MEK/WAZ 重绑必须使用这里产出的 GrpAppendPlan。</p>
     */
    private final AppendGrpEntriesStepV2 appendGrpEntriesStepV2 = new AppendGrpEntriesStepV2();

    /**
     * GRP 扩容后的 ProgramMaterial 对齐入口。
     *
     * <p>JINKI 追加 group 后，BSDX 的 ProgramMaterial 外层数组必须同步扩容，
     * 否则运行时会出现“group 存在但 material 表长度不足”的不一致。</p>
     */
    private final SyncProgramMaterialStepV2 syncProgramMaterialStepV2 = new SyncProgramMaterialStepV2();

    /**
     * baseline MEK material padding 入口。
     *
     * <p>用于把旧 BSDX 机体的 material 块补到新 GRP 容量，
     * 避免只让新增机体正确而破坏既有机体的表结构。</p>
     */
    private final PadBaselineMekMaterialStepV2 padBaselineMekMaterialStepV2 = new PadBaselineMekMaterialStepV2();

    /**
     * 主机体 MEK 重绑入口。
     *
     * <p>消费 GrpAppendPlan，将 AKAO MEK 内部引用从 JINKI 索引空间改写到 BSDX 目标索引空间。</p>
     */
    private final RebindMekStepV2 rebindMekStepV2 = new RebindMekStepV2();

    /**
     * 技能 WAZ 重绑入口。
     *
     * <p>消费 importPlan 和 GrpAppendPlan，将技能内的 MEK/SPM/SE 等引用改写到目标资源表。</p>
     */
    private final RebindWazStepV2 rebindWazStepV2 = new RebindWazStepV2();

    /**
     * 主 graft 产物写出入口。
     *
     * <p>把 GRP/DAT/MEK/WAZ/SPM/PNG/SE 等资源沉淀到最终输出目录，
     * 后续 menu/exe/pack 都围绕同一个输出根目录继续工作。</p>
     */
    private final ImportStaticAssetsStepV2 importStaticAssetsStepV2 = new ImportStaticAssetsStepV2();

    /**
     * 菜单后置覆盖入口。
     *
     * <p>处理 `MekaPilot.spm`、`SelectMekaMenuMeka.spm` 及相关 DAT/PNG；
     * 它是角色客制化层，不应污染通用 graft 主线。</p>
     */
    private final MenuOverridePipelineV2 menuOverridePipelineV2 = new MenuOverridePipelineV2();

    /**
     * EXE 兼容 patch 入口。
     *
     * <p>负责容量、菜单行数、语音 gate 等二进制 patch；
     * 这里必须支持链式成果物中“已经 patch 过”的 target bytes。</p>
     */
    private final PatchExeStepV2 patchExeStepV2 = new PatchExeStepV2();

    /**
     * 最终 PAC 打包入口。
     *
     * <p>只负责把输出目录打包为 `Update3.pac`，业务正确性靠打包前目录 parity
     * 和打包后解包 parity 验证。</p>
     */
    private final PackUpdatePacStepV2 packUpdatePacStepV2 = new PackUpdatePacStepV2();

    public AkaoGraftResult execute(AkaoGraftRequest request) {
        AkaoGraftResult result = new AkaoGraftResult();
        if (request == null) {
            return result;
        }

        // 1. 源游戏转换层。
        // JINKI 和 BSDX 是同系对象格式，所以这里不是做跨格式语义转换，
        // 而是把源侧二进制稳定反序列化为 BSDX DTO，给后续通用 graft 主线消费。
        JinkiPackageBundle jinkiPackage = loadJinkiSourceAssetsStep.load(request);
        result.setJinkiPackage(jinkiPackage);

        // 2. 链式 baseline 输入层。
        // 当前测试仍用原始 BSDX 作为 baseline；后续 BHE 接入时，这个输入可以换成
        // BSDX+JINKI 的成果物目录，而不是重新从原始 BSDX 开一条独立线。
        BsdxBaselineBundle bsdxBaseline = loadGraftBaselineStep.load(request);
        result.setBsdxBaseline(bsdxBaseline);

        // 3. 前置客制化输入收束。
        // 这一步只产出“本次要 graft 哪些资源”的闭包和审计信息，
        // 不修改任何目标资源；真正的复用/尾插决策放到 GRP append 阶段。
        JinkiImportPlan importPlan = buildResourceClosureStep.buildResourceClosure(request, jinkiPackage, bsdxBaseline);
        result.setImportPlan(importPlan);

        // 4. 通用 graft 主线的核心映射阶段。
        // 从这里开始，后续所有 MEK/WAZ/SPM/DAT 重绑都只能消费 GrpAppendPlan，
        // 不能再各自猜测“源 index 应该落到哪个目标 index”。
        GrpAppendPlan grpAppendPlan = appendGrpEntriesStepV2.appendAkaoBranch(request, jinkiPackage, bsdxBaseline, importPlan);
        result.setGrpAppendPlan(grpAppendPlan);

        // 5. GRP 扩容后的全局结构补齐。
        // ProgramMaterial / MapGroup / 基线 MEK 的数组长度都依赖当前 GRP 顶层容量；
        // 如果只追加 group 而不补齐这些结构，游戏加载时会在长度不一致处崩溃。
        result.setSyncedProgramMaterial(syncProgramMaterialStepV2.syncOuterArrays(request, bsdxBaseline, grpAppendPlan));
        padBaselineMekMaterialStepV2.padMaterialBlock(bsdxBaseline);

        // 6. 主机体资源重绑。
        // MEK/WAZ 重建不是在源对象上原地 patch，而是按 DTO 层级重建目标对象，
        // 这样每个外部引用点都能明确说明消费的是哪张映射表。
        result.setReboundAkaoMek(rebindMekStepV2.rebindAkaoMek(request, jinkiPackage, grpAppendPlan));
        result.setReboundAkaoWaz(rebindWazStepV2.rebindAkaoWaz(request, jinkiPackage, importPlan, grpAppendPlan));

        // 7. 主线输出沉淀。
        // 这里写出的目录是最终打包输入；后续所有后置覆盖都必须写回同一目录，
        // 否则目录 parity 可能通过局部对象测试，却在打包对象里漏文件。
        ImportedAssetSet importedAssetSet = importStaticAssetsStepV2.importAssets(
                request,
                jinkiPackage,
                bsdxBaseline,
                importPlan,
                result.getSyncedProgramMaterial(),
                result.getReboundAkaoMek(),
                result.getReboundAkaoWaz(),
                grpAppendPlan
        );
        result.setImportedAssetSet(importedAssetSet);

        // 8. 后置客制化覆盖。
        // 菜单不是主 MEK/WAZ 闭包自然能推导出的资源链，所以放在后置覆盖阶段。
        // 它仍然必须产出可审计的 dat/spm/png，并覆盖同一个 outputRoot。
        if (request.isPatchMenuData()) {
            MenuOverrideContext menuContext = menuOverridePipelineV2.execute(
                    request,
                    jinkiPackage,
                    bsdxBaseline,
                    grpAppendPlan,
                    result.getReboundAkaoMek(),
                    importedAssetSet.getOutputRootDir(),
                    MenuOverrideSpec.defaultAkaoMenuOverride()
            );
            applyMenuContextToResultAndAssetSet(menuContext, result, importedAssetSet);
        }

        // 9. 兼容 patch 累加。
        // exe patch 的输入是当前 baseline exe，输出是本层成果物 exe；
        // patch site 允许 expected 或 target，是为了支持后续链式成果物继续累加。
        ExePatchPlan exePatchPlan = patchExeStepV2.patchExe(request, bsdxBaseline, grpAppendPlan, result);
        result.setExePatchPlan(exePatchPlan);

        // 10. 最终打包。
        // V2 的最终验收不以 pac 文件本身字节为唯一标准，
        // 而是先打包，再解包比较内部文件集合和逐文件 bytes。
        PacPackPlan pacPackPlan = packUpdatePacStepV2.packUpdatePac(request, importedAssetSet);
        result.setPacPackPlan(pacPackPlan);
        return result;
    }

    private void applyMenuContextToResultAndAssetSet(
            MenuOverrideContext menuContext,
            AkaoGraftResult result,
            ImportedAssetSet importedAssetSet
    ) {
        result.setPatchedMekaDat(menuContext.getPatchedMekaDat());
        result.setPatchedMekaPilotDat(menuContext.getPatchedMekaPilotDat());
        result.setPatchedSelectMekaMenuDat(menuContext.getPatchedSelectMekaMenuDat());
        result.setPatchedMekaPilotSpm(menuContext.getPatchedMekaPilotSpm());
        result.setPatchedSelectMekaMenuMekaSpm(menuContext.getPatchedSelectMekaMenuMekaSpm());

        // 菜单 pipeline 自己负责落盘和审计，这里还要同步 ImportedAssetSet。
        // 原因是 step10/step11 只知道 ImportedAssetSet 和 outputRoot：
        // 如果这里不同步，最终 pac 仍能打包文件，但运行摘要/后续 manifest 会漏掉来源记录。
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

    private void addGeneratedDat(ImportedAssetSet importedAssetSet, Path outputRoot, String fileName, Dat dat) {
        if (dat == null) {
            return;
        }
        Path path = outputRoot.resolve(fileName);
        if (!importedAssetSet.getGeneratedDatFiles().contains(path)) {
            importedAssetSet.getGeneratedDatFiles().add(path);
        }
    }

    private void addCopiedSpm(ImportedAssetSet importedAssetSet, Path outputRoot, String fileName, Spm spm) {
        if (spm == null) {
            return;
        }
        Path path = outputRoot.resolve(fileName);
        if (!importedAssetSet.getCopiedSpmFiles().contains(path)) {
            importedAssetSet.getCopiedSpmFiles().add(path);
        }
    }
}
