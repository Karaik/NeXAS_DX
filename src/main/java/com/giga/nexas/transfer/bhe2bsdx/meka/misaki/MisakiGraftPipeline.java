package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.FollowupGraftPipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.misaki.MisakiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.misaki.MisakiGraftResult;

public class MisakiGraftPipeline {

    private final FollowupGraftPipeline followupGraftPipeline = new FollowupGraftPipeline();

    public MisakiGraftResult execute(MisakiGraftRequest request) {
        return followupGraftPipeline.execute(request, MisakiGraftResult::new, MisakiCustomizer.INSTANCE);
    }
}
