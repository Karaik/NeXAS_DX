package com.giga.nexas.transfer.bhe2bsdx.meka.misaki.exe;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;

public class PatchExeStep {

    
    private final BuildExePatchPlanStep buildTsukuyomiExePatchPlanStep;

    
    private final ApplyExePatchStep applyExePatchStep;

    
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
