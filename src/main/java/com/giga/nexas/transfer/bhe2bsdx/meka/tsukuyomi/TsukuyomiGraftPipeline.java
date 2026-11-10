package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.BuildBaselineFromJinkiResultStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.TsukuyomiConvertOverviewStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPacPackPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.meka.initializer.EnsureInitializerWeaponStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.exe.PatchExeStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.AppendGrpEntriesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.BuildResourceClosureStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.ImportStaticAssetsStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.LoadGraftBaselineStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.PadBaselineMekMaterialStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.RebindMekStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.RebindWazStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.SyncProgramMaterialStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu.MenuOverrideContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu.MenuOverridePipeline;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.pack.PackUpdatePacStep;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tsukuyomi -> BSDX 主流程编排。
 *
 * <p>这条 Tsukuyomi 主线保留共享主链结构作为验收基线，但自身不再直接调用旧 steps 包。
 * 每次内部替换都以最终输出目录和解包后的 Update3.pac 逐文件 byte parity 作为硬闸。</p>
 */
public class TsukuyomiGraftPipeline {

    /**
     * JINKI 结果 -> BHE 本层基线视图的转换入口。
     *
     * <p>这里刻意下沉到转换包，而不是继续把转换细节塞在主流程编排类里，
     * 原因是这一步本质上不是“跑主流程”，而是“把 JINKI 成果物改写成本层可消费的输入对象”。</p>
     */
    private final BuildBaselineFromJinkiResultStep buildBaselineFromJinkiResultStep = new BuildBaselineFromJinkiResultStep();

    /**
     * 第 0 步资源转换层总入口。
     *
     * <p>主流程只调用这个总览类；具体 raw / grp / mek / waz / spm / common 的细分步骤，
     * 都由总览类在转换包内部调度。</p>
     */
    private final TsukuyomiConvertOverviewStep tsukuyomiConvertOverviewStep = new TsukuyomiConvertOverviewStep();

    /**
     * 链式成果物基线加载入口。
     *
     * <p>读取链式输入基线；BHE 侧固定从继承结果构建本层可消费的资源视图。</p>
     */
    private final LoadGraftBaselineStep loadGraftBaselineStep = new LoadGraftBaselineStep();

    /**
     * 前置客制化输入收束入口。
     *
     * <p>它把本次 Tsukuyomi graft 需要的 MEK/WAZ/SPM/SE 资源算成闭包，
     * 下游 step 只消费这个计划，不再自行猜测哪些资源应该被复制。</p>
     */
    private final BuildResourceClosureStep buildResourceClosureStep = new BuildResourceClosureStep();

    /**
     * GRP append / reuse 入口。
     *
     * <p>这是 source index 到 target index 的核心映射产地；
     * MEK/WAZ 重绑必须使用这里产出的 TsukuyomiGrpAppendPlan。</p>
     */
    private final AppendGrpEntriesStep appendGrpEntriesStep = new AppendGrpEntriesStep();

    /**
     * GRP 扩容后的 ProgramMaterial 对齐入口。
     *
     * <p>TSUKUYOMI 追加 group 后，BSDX 的 ProgramMaterial 外层数组必须同步扩容，
     * 否则运行时会出现“group 存在但 material 表长度不足”的不一致。</p>
     */
    private final SyncProgramMaterialStep syncProgramMaterialStep = new SyncProgramMaterialStep();

    /**
     * 基线 MEK material 补齐入口。
     *
     * <p>用于把旧 BSDX 机体的 material 块补到新 GRP 容量，
     * 避免只让新增机体正确而破坏既有机体的表结构。</p>
     */
    private final PadBaselineMekMaterialStep padBaselineMekMaterialStep = new PadBaselineMekMaterialStep();

    /**
     * 主机体 MEK 重绑入口。
     *
     * <p>消费 TsukuyomiGrpAppendPlan，将 Tsukuyomi MEK 内部引用从 TSUKUYOMI 索引空间改写到 BSDX 目标索引空间。</p>
     */
    private final RebindMekStep rebindMekStep = new RebindMekStep();

    /**
     * 技能 WAZ 重绑入口。
     *
     * <p>消费 importPlan 和 TsukuyomiGrpAppendPlan，将技能内的 MEK/SPM/SE 等引用改写到目标资源表。</p>
     */
    private final RebindWazStep rebindWazStep = new RebindWazStep();

    /**
     * initializer 武装补齐入口。
     *
     * <p>该步骤必须在 MEK/WAZ 重绑完成后执行。initializer 补齐会新增目标侧武装行和 material 行；
     * 如果提前放进 selected 转换层，前置资源闭包会把这些后置补齐内容误判为 BHE 源侧资源。</p>
     */
    private final EnsureInitializerWeaponStep ensureInitializerWeaponStep = new EnsureInitializerWeaponStep();

    /**
     * 主 graft 产物写出入口。
     *
     * <p>把 GRP/DAT/MEK/WAZ/SPM/PNG/SE 等资源沉淀到最终输出目录，
     * menu/exe/pack 都围绕同一个输出根目录工作。</p>
     */
    private final ImportStaticAssetsStep importStaticAssetsStep = new ImportStaticAssetsStep();

    /**
     * 菜单后置覆盖入口。
     *
     * <p>处理 `MekaPilot.spm`、`SelectMekaMenuMeka.spm` 及相关 DAT/PNG；
     * 它是角色客制化层，不应污染通用 graft 主线。</p>
     */
    private final MenuOverridePipeline menuOverridePipeline = new MenuOverridePipeline();

    /**
     * EXE 兼容 patch 入口。
     *
     * <p>负责容量、菜单行数、语音 gate 等二进制 patch；
     * 这里必须支持链式成果物中“已经 patch 过”的 target bytes。</p>
     */
    private final PatchExeStep patchExeStep = new PatchExeStep();

    /**
     * 最终 PAC 打包入口。
     *
     * <p>只负责把输出目录打包为 `Update3.pac`，业务正确性靠打包前目录 parity
     * 和打包后解包 parity 验证。</p>
     */
    private final PackUpdatePacStep packUpdatePacStep = new PackUpdatePacStep();

    public TsukuyomiGraftResult execute(TsukuyomiGraftRequest request) {
        TsukuyomiGraftResult result = new TsukuyomiGraftResult();
        if (request == null) {
            return result;
        }

        // 0.1 链式基线输入层。
        // BHE 本层必须接手 JINKI 成果物基线，不再从目录回退加载原始 BSDX 基线。
        TsukuyomiBsdxBaselineBundle bsdxBaseline =
                buildBaselineFromJinkiResultStep.buildBaselineFromJinkiResult(request.getInheritedJinkiResult());
        result.setBsdxBaseline(bsdxBaseline);

        // 0.2 资源转换层。
        // 第 0 步转换层只有这一个入口；内部细分步骤放在 convert 包的对应子目录中。
        TsukuyomiConvertedBundle convertedBundle = tsukuyomiConvertOverviewStep.convert(request, bsdxBaseline);
        result.setRawSourceBundle(convertedBundle.getRawSourceBundle());
        result.setConvertedBundle(convertedBundle);
        result.setCommonProjectileAppendPlan(convertedBundle.getCommonProjectileAppendPlan());
        result.setPreparedBaselineBundle(convertedBundle.getPreparedBaselineBundle());
        bsdxBaseline = convertedBundle.getPreparedBaselineBundle();
        result.setBsdxBaseline(bsdxBaseline);

        // 1. 单机体 selected 资源视图。
        // 第 0 步把 BHE 私有资源转换成 BSDX DTO 形状；主线 graft 只消费这个 selected 视图。
        // 这里禁止绕回源目录重新加载 BHE 文件，避免绕过公共资源护栏和 term/格式转换结果。
        TsukuyomiPackageBundle tsukuyomiPackage = convertedBundle.getSelectedResourceBundle();
        result.setTsukuyomiPackage(tsukuyomiPackage);

        // 2. 前置客制化输入收束。
        // 这一步只产出“本次要 graft 哪些资源”的闭包和审计信息，
        // 不修改任何目标资源；真正的复用/尾插决策放到 GRP append 阶段。
        TsukuyomiImportPlan importPlan = buildResourceClosureStep.buildResourceClosure(
                request,
                tsukuyomiPackage,
                bsdxBaseline,
                convertedBundle.getCommonProjectileAppendPlan()
        );
        result.setImportPlan(importPlan);

        // 3. 通用 graft 主线的核心映射阶段。
        // 从这里开始，MEK/WAZ/SPM/DAT 重绑都只能消费 TsukuyomiGrpAppendPlan，
        // 不能再各自猜测“源 index 应该落到哪个目标 index”。
        TsukuyomiGrpAppendPlan grpAppendPlan = appendGrpEntriesStep.appendTsukuyomiBranch(request, tsukuyomiPackage, bsdxBaseline, importPlan);
        result.setGrpAppendPlan(grpAppendPlan);
        result.setCumulativeSourceBatVoiceGroupIndexToTargetIndex(buildCumulativeVoiceGroupMap(null, grpAppendPlan));

        // 4. GRP 扩容后的全局结构补齐。
        // ProgramMaterial / MapGroup / 基线 MEK 的数组长度都依赖本层 GRP 顶层容量；
        // 只追加 group 而不补齐这些结构，游戏加载时会在长度不一致处崩溃。
        result.setSyncedProgramMaterial(syncProgramMaterialStep.syncOuterArrays(request, bsdxBaseline, grpAppendPlan));
        padBaselineMekMaterialStep.padMaterialBlock(bsdxBaseline);

        // 5. 主机体资源重绑。
        // MEK/WAZ 重建不是在源对象上原地 patch，而是按 DTO 层级重建目标对象，
        // 这样每个外部引用点都能明确说明消费的是哪张映射表。
        Mek reboundTsukuyomiMek = rebindMekStep.rebindTsukuyomiMek(
                request,
                tsukuyomiPackage,
                grpAppendPlan,
                convertedBundle.getCommonProjectileAppendPlan()
        );
        Waz reboundTsukuyomiWaz = rebindWazStep.rebindTsukuyomiWaz(
                request,
                tsukuyomiPackage,
                importPlan,
                grpAppendPlan,
                convertedBundle.getCommonProjectileAppendPlan()
        );

        // 5-1. initializer 补齐。
        // 该补齐只操作重绑后的目标 MEK/WAZ。模板 skill 内部引用的是 BSDX 原生资源，
        // 不能再回到 BHE 源侧资源闭包，也不能被 RebindWazStep 按 BHE 索引重写。
        ensureInitializerWeaponStep.ensureAfterRebind(
                request.getWazFileName(),
                reboundTsukuyomiMek,
                reboundTsukuyomiWaz,
                convertedBundle.getNotes()
        );
        // initializer 位于 WazaGroup.param 回写之后；当前策略不强塞外部 WAZ skill，但如果未来某机体已有
        // initializer skill 或策略变化导致 skill 数变化，这里仍统一回写，避免 WazaGroup.param 与 WAZ 实际数量分叉。
        syncMainWazaSkillCountAfterInitializer(bsdxBaseline, grpAppendPlan, reboundTsukuyomiWaz);
        result.setReboundTsukuyomiMek(reboundTsukuyomiMek);
        result.setReboundTsukuyomiWaz(reboundTsukuyomiWaz);

        // 6. 主线输出沉淀。
        // 这里写出的目录是最终打包输入；所有后置覆盖都必须写回同一目录，
        // 否则目录 parity 可能通过局部对象测试，却在打包对象里漏文件。
        TsukuyomiImportedAssetSet importedAssetSet = importStaticAssetsStep.importAssets(
                request,
                tsukuyomiPackage,
                bsdxBaseline,
                importPlan,
                result.getSyncedProgramMaterial(),
                result.getReboundTsukuyomiMek(),
                result.getReboundTsukuyomiWaz(),
                grpAppendPlan,
                convertedBundle.getCommonProjectileAppendPlan(),
                result.getCumulativeSourceBatVoiceGroupIndexToTargetIndex()
        );
        result.setImportedAssetSet(importedAssetSet);

        // 7. 后置客制化覆盖。
        // 菜单不是主 MEK/WAZ 闭包自然能推导出的资源链，所以放在后置覆盖阶段。
        // 它仍然必须产出可审计的 dat/spm/png，并覆盖同一个 outputRoot。
        if (request.isPatchMenuData()) {
            MenuOverrideContext menuContext = menuOverridePipeline.execute(
                    request,
                    tsukuyomiPackage,
                    bsdxBaseline,
                    grpAppendPlan,
                    result.getReboundTsukuyomiMek(),
                    importedAssetSet.getOutputRootDir(),
                    request.getMenuOverrideSpec()
            );
            applyMenuContextToResultAndAssetSet(menuContext, result, importedAssetSet);
        }

        // 8. 兼容 patch 累加。
        // EXE 补丁的输入是本层基线 exe，输出是本层成果物 exe；
        // patch site 允许 expected 或 target，用于支持链式成果物累加。
        TsukuyomiExePatchPlan exePatchPlan = patchExeStep.patchExe(request, bsdxBaseline, grpAppendPlan, result);
        result.setExePatchPlan(exePatchPlan);

        // 9. 最终打包。
        // 最终验收不以 pac 文件本身字节为唯一标准，
        // 而是先打包，再解包比较内部文件集合和逐文件 bytes。
        TsukuyomiPacPackPlan pacPackPlan = packUpdatePacStep.packUpdatePac(request, importedAssetSet);
        result.setPacPackPlan(pacPackPlan);
        return result;
    }

    private TsukuyomiBsdxBaselineBundle buildBaselineFromJinkiResult(AkaoGraftResult inheritedJinkiResult) {
        // 这里保留一个很薄的代理，只负责把主流程编排层的调用转发到转换层。
        // 具体“如何从 JINKI 结果拼出本层基线”的规则，统一收敛到转换包内维护。
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

    private void syncMainWazaSkillCountAfterInitializer(
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            Waz reboundTsukuyomiWaz
    ) {
        if (bsdxBaseline == null
                || bsdxBaseline.getWazaGroupGrp() == null
                || bsdxBaseline.getWazaGroupGrp().getWazaList() == null
                || grpAppendPlan == null
                || reboundTsukuyomiWaz == null
                || reboundTsukuyomiWaz.getSkillList() == null) {
            return;
        }

        int targetWazaIndex = grpAppendPlan.getWazaGroupIndex();
        if (targetWazaIndex < 0 || targetWazaIndex >= bsdxBaseline.getWazaGroupGrp().getWazaList().size()) {
            throw new IllegalStateException("initializer 后同步 WazaGroup.param 时目标索引越界: " + targetWazaIndex);
        }

        bsdxBaseline.getWazaGroupGrp().getWazaList()
                .get(targetWazaIndex)
                .setParam(reboundTsukuyomiWaz.getSkillList().size());
    }

    private void applyMenuContextToResultAndAssetSet(
            MenuOverrideContext menuContext,
            TsukuyomiGraftResult result,
            TsukuyomiImportedAssetSet importedAssetSet
    ) {
        result.setPatchedMekaDat(menuContext.getPatchedMekaDat());
        result.setPatchedMekaPilotDat(menuContext.getPatchedMekaPilotDat());
        result.setPatchedSelectMekaMenuDat(menuContext.getPatchedSelectMekaMenuDat());
        result.setPatchedMekaPilotSpm(menuContext.getPatchedMekaPilotSpm());
        result.setPatchedSelectMekaMenuMekaSpm(menuContext.getPatchedSelectMekaMenuMekaSpm());

        // 菜单 pipeline 自己负责落盘和审计，这里还要同步 TsukuyomiImportedAssetSet。
        // 原因是 step10/step11 只知道 TsukuyomiImportedAssetSet 和 outputRoot：
        // 这里缺少同步时，pac 仍能打包文件，但运行摘要和 manifest 会漏掉来源记录。
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
