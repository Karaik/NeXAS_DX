package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

/**
 * 负责在战斗版稳定后补菜单层数据的步骤骨架。
 */
public class PatchMenuDataStep {

    public void patchMenuData(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan,
            AkaoGraftResult result
    ) {
        if (!request.isPatchMenuData()) {
            return;
        }

        result.setPatchedMekaDat(jinkiPackage != null ? jinkiPackage.getMekaDat() : null);
        result.setPatchedMekaPilotDat(jinkiPackage != null ? jinkiPackage.getMekaPilotDat() : null);
    }
}
