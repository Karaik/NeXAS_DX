package com.giga.nexas.transfer.bhe2bsdx.meka.freja;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.FollowupGraftPipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.freja.FrejaGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.freja.FrejaGraftResult;

public class FrejaGraftPipeline {

    private final FollowupGraftPipeline followupGraftPipeline = new FollowupGraftPipeline();

    public FrejaGraftResult execute(FrejaGraftRequest request) {
        return followupGraftPipeline.execute(request, FrejaGraftResult::new, FrejaCustomizer.INSTANCE);
    }
}
