package com.giga.nexas.transfer.bhe2bsdx.meka.katou;

import com.giga.nexas.transfer.bhe2bsdx.model.katou.KatouGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.katou.KatouGraftResult;

public class KatouTransfer {

    public static KatouGraftResult process(KatouGraftRequest request) {
        return new KatouGraftPipeline().execute(request);
    }
}
