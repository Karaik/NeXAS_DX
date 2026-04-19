package com.giga.nexas.transfer.bhe2bsdx.meka.nagi;

import com.giga.nexas.transfer.bhe2bsdx.model.nagi.NagiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.nagi.NagiGraftResult;

public class NagiTransfer {

    public static NagiGraftResult process(NagiGraftRequest request) {
        return new NagiGraftPipeline().execute(request);
    }
}
