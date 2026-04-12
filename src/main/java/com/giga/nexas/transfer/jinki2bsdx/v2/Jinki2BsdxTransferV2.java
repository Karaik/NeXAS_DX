package com.giga.nexas.transfer.jinki2bsdx.v2;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;

public class Jinki2BsdxTransferV2 {

    public static AkaoGraftResult process(AkaoGraftRequest request) {
        return new JinkiGraftPipelineV2().execute(request);
    }
}
