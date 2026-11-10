package com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.FollowupGraftPipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.wilhelm.WilhelmGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.wilhelm.WilhelmGraftResult;

public class WilhelmGraftPipeline {

    private final FollowupGraftPipeline followupGraftPipeline = new FollowupGraftPipeline();

    public WilhelmGraftResult execute(WilhelmGraftRequest request) {
        return followupGraftPipeline.execute(request, WilhelmGraftResult::new, WilhelmCustomizer.INSTANCE);
    }
}
