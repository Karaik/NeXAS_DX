package com.giga.nexas.transfer.bhe2bsdx.meka.nagi;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.FollowupGraftPipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.nagi.NagiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.nagi.NagiGraftResult;

public class NagiGraftPipeline {

    private final FollowupGraftPipeline followupGraftPipeline = new FollowupGraftPipeline();

    public NagiGraftResult execute(NagiGraftRequest request) {
        return followupGraftPipeline.execute(request, NagiGraftResult::new, NagiCustomizer.INSTANCE);
    }
}
