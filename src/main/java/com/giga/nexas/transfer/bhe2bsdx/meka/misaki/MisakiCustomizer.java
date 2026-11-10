package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

/**
 * みさき（Misaki）专属移植定制器入口
 *
 * <p>作为顶层定制入口，调度专门的修复器处理みさき专属的移植逻辑。</p>
 */
public final class MisakiCustomizer implements FollowupMekaCustomizer {

    public static final MisakiCustomizer INSTANCE = new MisakiCustomizer();

    private MisakiCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // 委派至专门的技能漏洞修复器处理 Tama05[488] 及 Tama05[491] 的单帧 999 发射异常
        MisakiSkillGlitchFixer.INSTANCE.fix(context);
    }
}
