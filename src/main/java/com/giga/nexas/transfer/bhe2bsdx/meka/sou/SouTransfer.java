package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.giga.nexas.transfer.bhe2bsdx.model.sou.SouGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.sou.SouGraftResult;

public class SouTransfer {

    public static SouGraftResult process(SouGraftRequest request) {
        return new SouGraftPipeline().execute(request);
    }
}
