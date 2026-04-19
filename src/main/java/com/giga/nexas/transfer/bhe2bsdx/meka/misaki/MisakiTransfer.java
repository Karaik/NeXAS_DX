package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.giga.nexas.transfer.bhe2bsdx.model.misaki.MisakiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.misaki.MisakiGraftResult;

public class MisakiTransfer {

    public static MisakiGraftResult process(MisakiGraftRequest request) {
        return new MisakiGraftPipeline().execute(request);
    }
}
