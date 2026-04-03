package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;

/**
 * 负责让 ProgramMaterial 外层数组长度与追加后的 grp 顶层条目保持一致的步骤骨架。
 */
public class SyncProgramMaterialStep {

    public ProgramMaterialGrp syncOuterArrays(
            AkaoGraftRequest request,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        return bsdxBaseline != null ? bsdxBaseline.getProgramMaterialGrp() : null;
    }
}
