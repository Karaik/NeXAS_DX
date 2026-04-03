package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiDiffManifest;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

/**
 * 负责整理 graft 输出所需静态资源集合的步骤骨架。
 */
public class ImportStaticAssetsStep {

    public ImportedAssetSet importAssets(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            JinkiDiffManifest diffManifest
    ) {
        return new ImportedAssetSet();
    }
}
