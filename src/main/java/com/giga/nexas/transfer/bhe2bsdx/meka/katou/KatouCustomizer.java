package com.giga.nexas.transfer.bhe2bsdx.meka.katou;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

public final class KatouCustomizer implements FollowupMekaCustomizer {

    public static final KatouCustomizer INSTANCE = new KatouCustomizer();

    private KatouCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // no-op
    }
}
