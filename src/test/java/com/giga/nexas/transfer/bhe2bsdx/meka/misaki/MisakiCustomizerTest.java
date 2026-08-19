package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MisakiCustomizerTest {

    @Test
    @DisplayName("测试 MisakiCustomizer 入口与 MisakiSkillGlitchFixer 定向修复 Tama05[488] 与 Tama05[491]")
    public void testFixTama05SpecificSkills() {
        // 构造模拟的 Tama05.waz，填充至 495 个技能槽位
        Waz tama05 = new Waz("Tama05.waz");
        for (int i = 0; i < 495; i++) {
            tama05.getSkillList().add(new Waz.Skill());
        }

        // 配置 Skill[488]（床召喚）
        CEventValRandom randomVal488 = createSlot4Random(999);
        tama05.getSkillList().set(488, createSkillWithEffectSlot4(randomVal488));

        // 配置 Skill[491]（あたためる）
        CEventValRandom randomVal491 = createSlot4Random(999);
        tama05.getSkillList().set(491, createSkillWithEffectSlot4(randomVal491));

        // 构建 Context 并执行入口定制器
        FollowupMekaContext context = new FollowupMekaContext();
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
        baseline.getWazByFileName().put("Tama05.waz", tama05);
        context.setBaseline(baseline);

        MisakiCustomizer.INSTANCE.customizeBeforeWrite(context);

        // 验证两个目标技能的 Slot 4 int5 均已被精准规整为 1
        assertEquals(1, randomVal488.getInt5(), "Skill[488] Slot 4 int5 应该被规整为 1");
        assertEquals(1, randomVal491.getInt5(), "Skill[491] Slot 4 int5 应该被规整为 1");
    }

    private Waz.Skill createSkillWithEffectSlot4(CEventValRandom randomVal) {
        Waz.Skill skill = new Waz.Skill();
        Waz.Skill.SkillPhase phase = new Waz.Skill.SkillPhase();
        SkillUnit unit = new SkillUnit(54, "特效生成");

        CEventEffect effect = new CEventEffect();
        CEventEffect.CEventEffectUnit countUnit = new CEventEffect.CEventEffectUnit();
        countUnit.setUnitSlotNum(4);
        countUnit.setData(randomVal);

        effect.getCeventEffectUnitList().add(countUnit);
        unit.getSkillInfoObjectList().add(effect);
        phase.getSkillUnitCollection().add(unit);
        skill.getPhasesInfo().add(phase);
        return skill;
    }

    private CEventValRandom createSlot4Random(int int5) {
        CEventValRandom val = new CEventValRandom();
        val.setStartFrame(0);
        val.setEndFrame(0);
        val.setInt1(0);
        val.setInt2(1);
        val.setInt3(0);
        val.setInt4(0);
        val.setInt5(int5);
        val.setInt6(0);
        return val;
    }
}
