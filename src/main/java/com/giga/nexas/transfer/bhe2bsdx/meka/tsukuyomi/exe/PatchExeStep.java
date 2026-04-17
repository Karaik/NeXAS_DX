package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.exe;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;

public class PatchExeStep {

    /**
     * 根据 graft 结果计算“这层成果物需要多大容量”的计划构建 step。
     *
     * <p>它只算需求和 notes，不直接改 exe bytes。</p>
     */
    private final BuildExePatchPlanStep buildTsukuyomiExePatchPlanStep;

    /**
     * 实际读取、校验、写入 exe bytes 的执行 step。
     *
     * <p>它消费 build step 的计划和 profile 中的 patch site。</p>
     */
    private final ApplyExePatchStep applyExePatchStep;

    /**
     * 当前角色/阶段使用的 EXE patch profile。
     *
     * <p>Tsukuyomi 默认 profile 包含 meka 容量、WeaponEquip 上限、战斗语音 gate 等补丁。
     * 未来 BHE 需要新的二进制补丁时，应新增 profile 或扩展这里的构造输入。</p>
     */
    private final ExePatchProfile profile;

    public PatchExeStep() {
        this(new BuildExePatchPlanStep(), new ApplyExePatchStep(), ExePatchProfile.defaultTsukuyomiCompatibilityProfile());
    }

    public PatchExeStep(
            BuildExePatchPlanStep buildTsukuyomiExePatchPlanStep,
            ApplyExePatchStep applyExePatchStep,
            ExePatchProfile profile
    ) {
        this.buildTsukuyomiExePatchPlanStep = buildTsukuyomiExePatchPlanStep == null ? new BuildExePatchPlanStep() : buildTsukuyomiExePatchPlanStep;
        this.applyExePatchStep = applyExePatchStep == null ? new ApplyExePatchStep() : applyExePatchStep;
        this.profile = profile == null ? ExePatchProfile.defaultTsukuyomiCompatibilityProfile() : profile;
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
        applyExePatchStep.apply(request, plan, profile);
        return plan;
    }
}
