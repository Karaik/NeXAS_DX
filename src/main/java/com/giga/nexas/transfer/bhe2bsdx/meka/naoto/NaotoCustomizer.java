package com.giga.nexas.transfer.bhe2bsdx.meka.naoto;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

public final class NaotoCustomizer implements FollowupMekaCustomizer {

    public static final NaotoCustomizer INSTANCE = new NaotoCustomizer();

    private NaotoCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // no-op
    }
}
