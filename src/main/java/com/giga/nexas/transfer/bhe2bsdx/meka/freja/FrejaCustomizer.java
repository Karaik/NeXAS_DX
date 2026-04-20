package com.giga.nexas.transfer.bhe2bsdx.meka.freja;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

public final class FrejaCustomizer implements FollowupMekaCustomizer {

    public static final FrejaCustomizer INSTANCE = new FrejaCustomizer();

    private FrejaCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // no-op
    }
}
