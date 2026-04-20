package com.giga.nexas.transfer.bhe2bsdx.meka.motoki;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.FollowupGraftPipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.motoki.MotokiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.motoki.MotokiGraftResult;

public class MotokiGraftPipeline {

    private final FollowupGraftPipeline followupGraftPipeline = new FollowupGraftPipeline();

    public MotokiGraftResult execute(MotokiGraftRequest request) {
        return followupGraftPipeline.execute(request, MotokiGraftResult::new, MotokiCustomizer.INSTANCE);
    }
}
