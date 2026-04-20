package com.giga.nexas.transfer.bhe2bsdx.meka.yuri;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

public final class YuriCustomizer implements FollowupMekaCustomizer {

    public static final YuriCustomizer INSTANCE = new YuriCustomizer();

    private YuriCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // no-op
    }
}
