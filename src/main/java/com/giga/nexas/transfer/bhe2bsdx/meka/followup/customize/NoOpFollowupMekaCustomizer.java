package com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize;

public final class NoOpFollowupMekaCustomizer implements FollowupMekaCustomizer {

    public static final NoOpFollowupMekaCustomizer INSTANCE = new NoOpFollowupMekaCustomizer();

    private NoOpFollowupMekaCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // no-op
    }
}
