package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.ExePatchPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;

/**
 * 负责规划真追加场景下运行时容量补丁的步骤骨架。
 */
public class PlanExeCapacityPatchesStep {

    public ExePatchPlan planCapacityPatches(
            AkaoGraftRequest request,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        ExePatchPlan plan = new ExePatchPlan();

        if (!request.isPlanExeCapacityPatch()) {
            return plan;
        }

        plan.getNotes().add("在认定真追加完成之前，先盯住机体侧 103 容量这一组运行时上限。");
        plan.getNotes().add("153 武装页容量仍是后续风险点，但不是 AKAO 第一优先阻塞。");
        return plan;
    }
}
