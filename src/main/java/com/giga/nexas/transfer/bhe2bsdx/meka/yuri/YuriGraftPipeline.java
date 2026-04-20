package com.giga.nexas.transfer.bhe2bsdx.meka.yuri;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.FollowupGraftPipeline;
import com.giga.nexas.transfer.bhe2bsdx.model.yuri.YuriGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.yuri.YuriGraftResult;

public class YuriGraftPipeline {

    private final FollowupGraftPipeline followupGraftPipeline = new FollowupGraftPipeline();

    public YuriGraftResult execute(YuriGraftRequest request) {
        return followupGraftPipeline.execute(request, YuriGraftResult::new, YuriCustomizer.INSTANCE);
    }
}
