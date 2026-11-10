package com.giga.nexas.transfer.bhe2bsdx.meka.katou;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.FollowupGraftPipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.katou.KatouGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.katou.KatouGraftResult;

public class KatouGraftPipeline {

    private final FollowupGraftPipeline followupGraftPipeline = new FollowupGraftPipeline();

    public KatouGraftResult execute(KatouGraftRequest request) {
        return followupGraftPipeline.execute(request, KatouGraftResult::new, KatouCustomizer.INSTANCE);
    }
}
