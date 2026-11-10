package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;

public class TsukuyomiTransfer {

    public static TsukuyomiGraftResult process(TsukuyomiGraftRequest request) {
        return new TsukuyomiGraftPipeline().execute(request);
    }
}
