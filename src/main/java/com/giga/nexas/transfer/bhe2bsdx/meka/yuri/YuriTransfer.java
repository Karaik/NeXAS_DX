package com.giga.nexas.transfer.bhe2bsdx.meka.yuri;

import com.giga.nexas.transfer.bhe2bsdx.model.yuri.YuriGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.yuri.YuriGraftResult;

public class YuriTransfer {

    public static YuriGraftResult process(YuriGraftRequest request) {
        return new YuriGraftPipeline().execute(request);
    }
}
