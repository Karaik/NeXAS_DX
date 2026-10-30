package com.giga.nexas.transfer.bhe2bsdx.meka.motoki;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

public final class MotokiCustomizer implements FollowupMekaCustomizer {

    public static final MotokiCustomizer INSTANCE = new MotokiCustomizer();

    private MotokiCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        var mek = requireMek(context, "Motoki");
        var waz = requireWaz(context, "Motoki");
        MotokiAdditionalVariants.appendAll(mek, waz);
    }
}
