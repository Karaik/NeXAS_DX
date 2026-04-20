package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

public final class MisakiCustomizer implements FollowupMekaCustomizer {

    public static final MisakiCustomizer INSTANCE = new MisakiCustomizer();

    private MisakiCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // no-op
    }
}
