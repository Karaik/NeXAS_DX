package com.giga.nexas.transfer.bhe2bsdx.meka.nagi;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

public final class NagiCustomizer implements FollowupMekaCustomizer {

    public static final NagiCustomizer INSTANCE = new NagiCustomizer();

    private NagiCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // no-op
    }
}
