package com.giga.nexas.transfer.jinki2bsdx.v2.exe;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.ExePatchPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;

public class PatchExeStepV2 {

    /**
     * 根据 graft 结果计算“这层成果物需要多大容量”的计划构建 step。
     *
     * <p>它只算需求和 notes，不直接改 exe bytes。</p>
     */
    private final BuildExePatchPlanStep buildExePatchPlanStep;

    /**
     * 实际读取、校验、写入 exe bytes 的执行 step。
     *
     * <p>它消费 build step 的计划和 profile 中的 patch site。</p>
     */
    private final ApplyExePatchStep applyExePatchStep;

    /**
     * 当前角色/阶段使用的 EXE patch profile。
     *
     * <p>AKAO 默认 profile 包含 meka 容量、WeaponEquip 上限、战斗语音 gate 等补丁。
     * 未来 BHE 需要新的二进制补丁时，应新增 profile 或扩展这里的构造输入。</p>
     */
    private final ExePatchProfile profile;

    public PatchExeStepV2() {
        this(new BuildExePatchPlanStep(), new ApplyExePatchStep(), ExePatchProfile.defaultAkaoCompatibilityProfile());
    }

    public PatchExeStepV2(
            BuildExePatchPlanStep buildExePatchPlanStep,
            ApplyExePatchStep applyExePatchStep,
            ExePatchProfile profile
    ) {
        this.buildExePatchPlanStep = buildExePatchPlanStep == null ? new BuildExePatchPlanStep() : buildExePatchPlanStep;
        this.applyExePatchStep = applyExePatchStep == null ? new ApplyExePatchStep() : applyExePatchStep;
        this.profile = profile == null ? ExePatchProfile.defaultAkaoCompatibilityProfile() : profile;
    }

    public ExePatchPlan patchExe(
            AkaoGraftRequest request,
            BsdxBaselineBundle baseline,
            GrpAppendPlan grpAppendPlan,
            AkaoGraftResult result
    ) {
        ExePatchPlan plan = buildExePatchPlanStep.build(
                request == null ? null : request.getTargetExePath(),
                baseline,
                grpAppendPlan,
                result
        );
        applyExePatchStep.apply(request, plan, profile);
        return plan;
    }
}
