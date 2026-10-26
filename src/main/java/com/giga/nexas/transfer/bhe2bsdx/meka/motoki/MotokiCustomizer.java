package com.giga.nexas.transfer.bhe2bsdx.meka.motoki;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

public final class MotokiCustomizer implements FollowupMekaCustomizer {

    public static final MotokiCustomizer INSTANCE = new MotokiCustomizer();

    private MotokiCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        var mek = requireMek(context, "Motoki");
        var waz = requireWaz(context, "Motoki");
        var source = skill(waz, "飛行形態：集中爆撃", "0023", 1);
        var sourceWeapon = weapon(mek, waz, source, "飛行形態：集中爆撃", "0020");
        var flying = skill(waz, "飛行形態：飛行中", "BOOST_DASH5", 1);
        var ending = skill(waz, "飛行形態：終了", "BOOST_DASH6", 1);
        var endingWeapon = weapon(mek, waz, ending, "飛行形態：終了", "0015");
        requireNormalEnd(event(ending, 1, 71, CEventChange.class, 30, 30, 30, 30));
        int group = mek.getMekBasicInfo().getWazFileSequence();
        int expected = waz.getSkillList().indexOf(flying);
        int target = waz.getSkillList().indexOf(ending);
        CharacterWeaponVariants.append(mek, waz, sourceWeapon, source,
                "集中爆撃・帰投", "DX_FOCUSED_BOMB_RETURN", 40,
                "一度の集中爆撃を終えると飛行形態を解除する。地上の吸い込み攻撃と空からの爆撃を切り替え、同じ形態に留まり続けない。", copy -> {
                    requireSpeed(event(copy, 1, 18, CEventValRandom.class, 0, 11, 0, 11), List.of(1, 0, 0, 0, 12, 0));
                    requireSpeed(event(copy, 1, 18, CEventValRandom.class, 12, 59, 12, 59), List.of(1, 0, 0, 0, 8, 0));
                    var effects = copy.getPhasesInfo().get(0).getSkillUnitCollection().stream()
                            .filter(value -> Integer.valueOf(48).equals(value.getUnitQuantity()))
                            .flatMap(value -> value.getSkillInfoObjectList().stream()).toList();
                    if (effects.size() != 1 || !(effects.get(0) instanceof CEventEffect effect) || !frames(effect, 10, 10)) {
                        throw new IllegalStateException("集中爆撃Effect数量或帧漂移");
                    }
                    var amounts = effect.getCeventEffectUnitList().stream()
                            .filter(value -> Integer.valueOf(4).equals(value.getUnitSlotNum())).toList();
                    if (amounts.size() != 1 || !Integer.valueOf(1).equals(amounts.get(0).getBuffer())
                            || !(amounts.get(0).getData() instanceof CEventValRandom amount)
                            || !List.of(0, 1, 0, 0, 6, 0).equals(List.of(amount.getInt1(), amount.getInt2(),
                            amount.getInt3(), amount.getInt4(), amount.getInt5(), amount.getInt6()))) {
                        throw new IllegalStateException("集中爆撃六枚发射签名漂移");
                    }
                    CEventChange exit = event(copy, 1, 71, CEventChange.class, 89, 89, 89, 89);
                    requireJump(exit, group, expected);
                    exit.getBsdxInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(group, target)));
                }, endingWeapon);
    }

    private static void requireSpeed(CEventValRandom speed, List<Integer> expected) {
        if (!expected.equals(List.of(speed.getInt1(), speed.getInt2(), speed.getInt3(),
                speed.getInt4(), speed.getInt5(), speed.getInt6()))) throw new IllegalStateException("集中爆撃速度漂移");
    }

    private static void requireJump(CEventChange change, int group, int skill) {
        if (!Integer.valueOf(1).equals(change.getFlag()) || change.getBsdxInfoCollectionList1().size() != 1
                || change.getBsdxInfoCollectionList2().size() != 1) throw new IllegalStateException("集中爆撃退出结构漂移");
        var condition = change.getBsdxInfoCollectionList1().get(0);
        var jump = change.getBsdxInfoCollectionList2().get(0);
        if (!Integer.valueOf(0).equals(condition.getInt1()) || !List.of(0).equals(condition.getTypeList())
                || !condition.getParamList().isEmpty() || !Integer.valueOf(16).equals(jump.getInt1())
                || !List.of(1).equals(jump.getTypeList()) || !List.of(group, skill).equals(jump.getParamList())) {
            throw new IllegalStateException("集中爆撃退出目标不再是本角色飛行中");
        }
    }
}
