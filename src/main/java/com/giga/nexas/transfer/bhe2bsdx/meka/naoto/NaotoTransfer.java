package com.giga.nexas.transfer.bhe2bsdx.meka.naoto;

import com.giga.nexas.transfer.bhe2bsdx.model.naoto.NaotoGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.naoto.NaotoGraftResult;

public class NaotoTransfer {

    public static NaotoGraftResult process(NaotoGraftRequest request) {
        return new NaotoGraftPipeline().execute(request);
    }
}
