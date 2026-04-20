package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

public final class SouCustomizer implements FollowupMekaCustomizer {

    public static final SouCustomizer INSTANCE = new SouCustomizer();

    private SouCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // no-op
    }
}
