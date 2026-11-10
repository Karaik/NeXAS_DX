package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

/**
 * Sou (sou / Schwertiger) 专属移植定制器入口
 *
 * <p>作为顶层定制入口，调度专门的修复器处理 Sou 专属的完全体技能重映射与数值规整。</p>
 */
public final class SouCustomizer implements FollowupMekaCustomizer {

    public static final SouCustomizer INSTANCE = new SouCustomizer();

    private SouCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // 委派至专门的技能重映射器处理 140 组常规武器升级至 Lv3 完全体及热量消耗规整
        SouSkillGlitchFixer.INSTANCE.fix(context);
    }
}
