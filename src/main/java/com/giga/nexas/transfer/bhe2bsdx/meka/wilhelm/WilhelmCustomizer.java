package com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

public final class WilhelmCustomizer implements FollowupMekaCustomizer {

    public static final WilhelmCustomizer INSTANCE = new WilhelmCustomizer();

    private WilhelmCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // no-op
    }
}
