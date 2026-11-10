package com.giga.nexas.transfer.bhe2bsdx.meka.naoto;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.FollowupGraftPipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.naoto.NaotoGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.naoto.NaotoGraftResult;

public class NaotoGraftPipeline {

    private final FollowupGraftPipeline followupGraftPipeline = new FollowupGraftPipeline();

    public NaotoGraftResult execute(NaotoGraftRequest request) {
        return followupGraftPipeline.execute(request, NaotoGraftResult::new, NaotoCustomizer.INSTANCE);
    }
}
