package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiDiffManifest;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

/**
 * 负责把 AKAO 相关顶层条目追加进 BSDX grp 容器的步骤骨架。
 */
public class AppendGrpEntriesStep {

    public GrpAppendPlan appendAkaoBranch(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiDiffManifest diffManifest
    ) {
        return new GrpAppendPlan();
    }
}
