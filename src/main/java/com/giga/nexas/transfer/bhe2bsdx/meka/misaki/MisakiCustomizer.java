package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

import java.util.Arrays;
import java.util.List;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

/**
 * みさき（Misaki）专属移植定制器入口
 *
 * <p>作为顶层定制入口，调度专门的修复器处理みさき专属的移植逻辑。</p>
 */
public final class MisakiCustomizer implements FollowupMekaCustomizer {

    public static final MisakiCustomizer INSTANCE = new MisakiCustomizer();
    // 与共享武装副本工具一致，忽略isEmpty等只读派生属性，不改变全局DTO或解析器配置。
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private MisakiCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        // 保留公共源身份与append计划驱动的发射数修复。
        MisakiSkillGlitchFixer.INSTANCE.fix(context);
        // 既有公共修复探针不包含角色载荷；真实落盘上下文不能只缺其中一项。
        if (context.getReboundMek() == null && context.getReboundWaz() == null) return;
        Mek mek = requireMek(context, "Misaki");
        Waz waz = requireWaz(context, "Misaki");
        Waz.Skill front = skill(waz, "転移手：前方ビターン", "WARP_P1_11", 3);
        Waz.Skill rear = skill(waz, "転移手：後方ビターン", "WARP_P1_09", 3);
        Mek.MekWeaponInfo source = weapon(mek, waz, front, "転移手：前方ビターン", "0037");
        Mek.MekWeaponInfo donor = weapon(mek, waz, rear, "転移手：後方ビターン", "0033");
        if (!Integer.valueOf(0).equals(source.getHeatMinConsumption())
                || !Integer.valueOf(20).equals(source.getHeatMaxConsumption())
                || !Integer.valueOf(0).equals(source.getUpgradeExp())) throw drift("原前方手热耗不符");
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama05.waz");
        var plan = context.getConvertedBundle().getCommonProjectileAppendPlan();
        Integer targetGroup = plan == null ? null : plan.getSourceWazIndexToTargetIndex().get(sourceGroup);
        Integer base = plan == null ? null : plan.getSourceWazIndexToTargetSkillBase().get(sourceGroup);
        Integer count = plan == null ? null : plan.getSourceWazIndexToTargetSkillCount().get(sourceGroup);
        if (targetGroup == null || targetGroup < 0 || base == null || base < 0 || count == null || count <= 332) {
            throw drift("公共手技能的最终追加映射缺失");
        }
        int hand = Math.addExact(base, 332), trail = Math.addExact(base, 331);
        validateEmitters(rear, targetGroup, hand, trail);
        CharacterWeaponVariants.append(mek, waz, source, front,
                "転移手・挟撃", "DX_PINCER_HANDS", 36,
                "前方、続いて後方から転移手を叩き込む二段の挟撃。床召還で動きを制限し、手を避けた相手には掴みを重ねたい。",
                copy -> addRearBeat(copy, rear, targetGroup, hand, trail), donor);
    }

    private static void addRearBeat(Waz.Skill copy, Waz.Skill rear, int group, int hand, int trail) {
        validateEmitters(copy, group, hand, trail);
        CEventSprite lastPose = event(copy, 2, 0, CEventSprite.class, 3, 20, 3, 20);
        CEventChange end = event(copy, 2, 71, CEventChange.class, 20, 20, 20, 20);
        requireNormalEnd(end);
        requireNormalEnd(event(copy, 3, 71, CEventChange.class, 27, 27, 27, 27));
        // donor技能也深复制；新增四条Effect及其动态位置/方向对象不能与原后方手共享。
        Waz.Skill rearCopy = JSON.convertValue(rear, Waz.Skill.class);
        List<CEventEffect> delayed = validateEmitters(rearCopy, group, hand, trail);
        for (CEventEffect effect : delayed) {
            effect.setStartFrame(12);
            effect.setEndFrame(12);
        }
        effectUnit(copy).getSkillInfoObjectList().addAll(delayed);
        lastPose.setEndFrame(32);
        end.setStartFrame(32);
        end.setEndFrame(32);
    }

    private static List<CEventEffect> validateEmitters(Waz.Skill skill, int group, int hand, int trail) {
        SkillUnit unit = effectUnit(skill);
        List<CEventEffect> effects = unit.getSkillInfoObjectList().stream()
                .filter(CEventEffect.class::isInstance).map(CEventEffect.class::cast).toList();
        if (effects.size() != 4) throw drift("P2发射批次不再是四条Effect");
        for (int i = 0; i < effects.size(); i++) {
            CEventEffect effect = effects.get(i);
            if (!frames(effect, 0, 0) || effect.getCeventEffectUnitList() == null) throw drift("P2发射帧不符");
            var targets = effect.getCeventEffectUnitList().stream()
                    .filter(v -> Integer.valueOf(3).equals(v.getUnitSlotNum())).toList();
            if (targets.size() != 1 || !Integer.valueOf(1).equals(targets.get(0).getBuffer())
                    || !(targets.get(0).getData() instanceof CEventWazaSelect target)
                    || !frames(target, 0, 0) || !Integer.valueOf(group).equals(target.getWazFileNo())
                    || !Integer.valueOf(i < 2 ? hand : trail).equals(target.getWazSequenceNo())) {
                throw drift("P2手/轨迹目标不在已确认的最终追加段");
            }
            requireEmitterValue(effect, 4, List.of(0, 1, 0, 0, 1, 0));
            requireEmitterValue(effect, 6, List.of(0, 0, 0, 0, 0, 0));
        }
        return effects;
    }

    private static SkillUnit effectUnit(Waz.Skill skill) {
        var units = skill.getPhasesInfo().get(1).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(48).equals(u.getUnitQuantity())).toList();
        if (units.size() != 1 || units.get(0).getSkillInfoObjectList() == null) throw drift("P2 Effect槽缺失或重复");
        return units.get(0);
    }

    private static void requireEmitterValue(CEventEffect effect, int slot, List<Integer> values) {
        var units = effect.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum())).toList();
        if (units.size() != 1 || !Integer.valueOf(1).equals(units.get(0).getBuffer())
                || !(units.get(0).getData() instanceof CEventValRandom value) || !frames(value, 0, 0)
                || !values.equals(Arrays.asList(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4(), value.getInt5(), value.getInt6()))) {
            throw drift("P2发射数或间隔载荷不符");
        }
    }

    private static void requireNormalEnd(CEventChange change) {
        if (!Integer.valueOf(1).equals(change.getFlag()) || !Integer.valueOf(0).equals(change.getInt1())
                || change.getBsdxInfoCollectionList1() == null || change.getBsdxInfoCollectionList1().size() != 1
                || change.getBsdxInfoCollectionList2() == null || change.getBsdxInfoCollectionList2().size() != 1
                || !collection(change.getBsdxInfoCollectionList1().get(0), 0, List.of(0))
                || !collection(change.getBsdxInfoCollectionList2().get(0), 16, List.of(0, 0))) {
            throw drift("原生阶段出口签名不符");
        }
    }

    private static boolean collection(BsdxInfoCollection c, int root, List<Integer> types) {
        return c != null && Integer.valueOf(root).equals(c.getInt1()) && Integer.valueOf(0).equals(c.getInt2())
                && types.equals(c.getTypeList()) && List.of().equals(c.getParamList())
                && List.of().equals(c.getIntList3()) && List.of().equals(c.getIntList4());
    }

    private static IllegalStateException drift(String message) {
        return new IllegalStateException("Misaki变体输入版本不符: " + message);
    }
}
