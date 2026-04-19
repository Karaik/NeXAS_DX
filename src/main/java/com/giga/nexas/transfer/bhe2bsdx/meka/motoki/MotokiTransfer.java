package com.giga.nexas.transfer.bhe2bsdx.meka.motoki;

import com.giga.nexas.transfer.bhe2bsdx.model.motoki.MotokiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.motoki.MotokiGraftResult;

public class MotokiTransfer {

    public static MotokiGraftResult process(MotokiGraftRequest request) {
        return new MotokiGraftPipeline().execute(request);
    }
}
