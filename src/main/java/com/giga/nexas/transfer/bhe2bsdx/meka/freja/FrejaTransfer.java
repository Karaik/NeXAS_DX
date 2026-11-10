package com.giga.nexas.transfer.bhe2bsdx.meka.freja;

import com.giga.nexas.transfer.bhe2bsdx.model.freja.FrejaGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.freja.FrejaGraftResult;

public class FrejaTransfer {

    public static FrejaGraftResult process(FrejaGraftRequest request) {
        return new FrejaGraftPipeline().execute(request);
    }
}
