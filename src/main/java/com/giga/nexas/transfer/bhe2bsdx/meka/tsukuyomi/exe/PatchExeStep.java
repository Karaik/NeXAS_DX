package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.exe;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;

public class PatchExeStep {

    /**
     * 根据资源追加后的基线计算 EXE 需要承载的容量。
     *
     * <p>BHE 链式移植会继承 JINKI 产物，机体容量不能写死成 104；
     * 本 step 会先从 MekaGroup.grp 的实际条目数得到目标容量，再交给 profile 生成 patch bytes。</p>
     */
    private final BuildExePatchPlanStep buildTsukuyomiExePatchPlanStep;

    /**
     * 读取、校验并写出 patched exe 的执行 step。
     */
    private final ApplyExePatchStep applyExePatchStep;

    /**
     * 测试专用的固定 profile 注入点。
     *
     * <p>生产流程保持为空，让 patch profile 按 {@link TsukuyomiExePatchPlan#getRequiredMekaCapacity()} 动态生成。
     * 这样 JINKI 的 103->104 和 BHE 的 104->105 都能沿同一套 offset 审计结果继续递增。</p>
     */
    private final ExePatchProfile profile;

    public PatchExeStep() {
        this(new BuildExePatchPlanStep(), new ApplyExePatchStep(), null);
    }

    public PatchExeStep(
            BuildExePatchPlanStep buildTsukuyomiExePatchPlanStep,
            ApplyExePatchStep applyExePatchStep,
            ExePatchProfile profile
    ) {
        this.buildTsukuyomiExePatchPlanStep = buildTsukuyomiExePatchPlanStep == null ? new BuildExePatchPlanStep() : buildTsukuyomiExePatchPlanStep;
        this.applyExePatchStep = applyExePatchStep == null ? new ApplyExePatchStep() : applyExePatchStep;
        this.profile = profile;
    }

    public TsukuyomiExePatchPlan patchExe(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle baseline,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            TsukuyomiGraftResult result
    ) {
        TsukuyomiExePatchPlan plan = buildTsukuyomiExePatchPlanStep.build(
                request == null ? null : request.resolveTargetExePath(),
                baseline,
                grpAppendPlan,
                result
        );
        ExePatchProfile patchProfile = profile == null
                ? ExePatchProfile.forCapacities(plan.getRequiredMekaCapacity(), plan.getRequiredWeaponEquipRows())
                : profile;
        applyExePatchStep.apply(request, plan, patchProfile);
        return plan;
    }
}
