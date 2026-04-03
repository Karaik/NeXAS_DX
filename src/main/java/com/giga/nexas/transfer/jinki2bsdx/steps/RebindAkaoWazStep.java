package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiDiffManifest;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

/**
 * 负责把 Akao.waz 内部外部引用重绑到目标 BSDX 索引的步骤骨架。
 */
public class RebindAkaoWazStep {

    public Waz rebindAkaoWaz(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            JinkiDiffManifest diffManifest,
            GrpAppendPlan grpAppendPlan
    ) {
        return jinkiPackage != null ? jinkiPackage.getWazByFileName().get(request.getWazFileName()) : null;
    }
}
