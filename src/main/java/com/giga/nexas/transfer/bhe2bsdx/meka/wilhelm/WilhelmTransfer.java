package com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm;

import com.giga.nexas.transfer.bhe2bsdx.model.wilhelm.WilhelmGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.wilhelm.WilhelmGraftResult;

public class WilhelmTransfer {

    public static WilhelmGraftResult process(WilhelmGraftRequest request) {
        return new WilhelmGraftPipeline().execute(request);
    }
}
