package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;

/**
 * 负责加载 AKAO 将要并入的 BSDX 基线容器的步骤骨架。
 */
public class LoadBsdxBaselineStep {

    public BsdxBaselineBundle loadBaseline(AkaoGraftRequest request) {
        return new BsdxBaselineBundle();
    }
}
