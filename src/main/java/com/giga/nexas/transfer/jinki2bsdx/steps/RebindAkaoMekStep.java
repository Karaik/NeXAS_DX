package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

/**
 * 负责把 Akao.mek 重绑到 BSDX 最终顶层索引的步骤骨架。
 */
public class RebindAkaoMekStep {

    public Mek rebindAkaoMek(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            GrpAppendPlan grpAppendPlan
    ) {
        return jinkiPackage != null ? jinkiPackage.getAkaoMek() : null;
    }
}
