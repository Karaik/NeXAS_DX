package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.FollowupGraftPipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.sou.SouGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.sou.SouGraftResult;

public class SouGraftPipeline {

    private final FollowupGraftPipeline followupGraftPipeline = new FollowupGraftPipeline();

    public SouGraftResult execute(SouGraftRequest request) {
        return followupGraftPipeline.execute(request, SouGraftResult::new, SouCustomizer.INSTANCE);
    }
}
