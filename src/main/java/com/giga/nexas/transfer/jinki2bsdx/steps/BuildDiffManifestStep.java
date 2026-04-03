package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiDiffManifest;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

/**
 * 负责判定共享同名资源是复用还是导入的步骤骨架。
 */
public class BuildDiffManifestStep {

    public JinkiDiffManifest buildManifest(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline
    ) {
        return new JinkiDiffManifest();
    }
}
