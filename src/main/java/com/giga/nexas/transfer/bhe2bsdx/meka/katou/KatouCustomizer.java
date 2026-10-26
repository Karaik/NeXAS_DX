package com.giga.nexas.transfer.bhe2bsdx.meka.katou;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

public final class KatouCustomizer implements FollowupMekaCustomizer {

    public static final KatouCustomizer INSTANCE = new KatouCustomizer();

    private KatouCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        Mek mek = requireMek(context, "Katou");
        Waz waz = requireWaz(context, "Katou");
        Waz.Skill original = skill(waz, "トンファー：トンファー連打", "T_AXE04", 6);
        Waz.Skill stance = skill(waz, "トンファー：構え中", "0010", 2);
        Waz.Skill storage = skill(waz, "トンファー：収納", "0009", 1);
        Mek.MekWeaponInfo source = weapon(mek, waz, original, "トンファー連打", "PUNCH2");
        Mek.MekWeaponInfo donor = weapon(mek, waz, storage, "トンファー収納", "0008");
        Integer group = mek.getMekBasicInfo().getWazFileSequence();
        if (group == null || group < 0 || !Integer.valueOf(0).equals(source.getHeatMinConsumption())
                || !Integer.valueOf(10).equals(source.getHeatMaxConsumption())
                || !Integer.valueOf(0).equals(source.getUpgradeExp())) {
            throw drift("最终WAZ组或原连打热耗不符");
        }
        requireChange(event(storage, 1, 71, CEventChange.class, 21, 21, 21, 21),
                List.of(0), List.of(), List.of(), List.of(0, 0), List.of());
        int stanceIndex = waz.getSkillList().indexOf(stance);
        int storageIndex = waz.getSkillList().indexOf(storage);
        CharacterWeaponVariants.append(mek, waz, source, original,
                "トンファー・二連打", "DX_TONFA_TWO_HIT", 18,
                "左右の二撃を出し切り、トンファーを収納する。長い連打に付き合わず、短い攻めから距離を取り直したい。",
                copy -> makeFixedTwoHits(copy, group, stanceIndex, storageIndex), donor);
    }

    private static void makeFixedTwoHits(Waz.Skill copy, int group, int stance, int storage) {
        CEventChange firstLoop = event(copy, 3, 71, CEventChange.class, 3, 3, 3, 3);
        CEventChange firstEnd = event(copy, 3, 71, CEventChange.class, 40, 40, 40, 40);
        CEventChange secondLoop = event(copy, 5, 71, CEventChange.class, 3, 3, 3, 3);
        CEventChange secondEnd = event(copy, 5, 71, CEventChange.class, 40, 40, 40, 40);
        CEventChange exit = event(copy, 6, 71, CEventChange.class, 15, 15, 15, 15);
        requireChange(firstLoop, List.of(2, 5, 3, 3), List.of(10), List.of(0, 0), List.of(0, 0), List.of());
        requireChange(firstEnd, List.of(0), List.of(), List.of(), List.of(0, 1), List.of(5));
        requireChange(secondLoop, List.of(2, 5, 3, 3), List.of(10), List.of(0, 0), List.of(0, 1), List.of(1));
        requireChange(secondEnd, List.of(0), List.of(), List.of(), List.of(0, 0), List.of());
        requireChange(exit, List.of(0), List.of(), List.of(), List.of(1), List.of(group, stance));
        // 校验两击的原生直接值，但保留其命中次数、补正、击退和全部攻击载荷。
        attack(event(copy, 3, 41, CEventHit.class, 1, 5, 1, 5), 1, 5, 30, 1, 5, 30);
        attack(event(copy, 5, 41, CEventHit.class, 1, 5, 1, 5), 1, 5, 30, 1, 5, 30);

        // 只移除已确认的按键循环；其余Change和事件保持原样。
        copy.getPhasesInfo().get(2).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(71).equals(u.getUnitQuantity())).findFirst().orElseThrow()
                .getSkillInfoObjectList().remove(firstLoop);
        copy.getPhasesInfo().get(4).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(71).equals(u.getUnitQuantity())).findFirst().orElseThrow()
                .getSkillInfoObjectList().remove(secondLoop);
        firstEnd.setStartFrame(12);
        firstEnd.setEndFrame(12);
        firstEnd.getBsdxInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(3)));
        secondEnd.setStartFrame(12);
        secondEnd.setEndFrame(12);
        exit.getBsdxInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(group, storage)));
    }

    private static void requireChange(CEventChange change, List<Integer> condition, List<Integer> parameters,
                                      List<Integer> objects, List<Integer> destination, List<Integer> target) {
        if (!Integer.valueOf(1).equals(change.getFlag()) || !Integer.valueOf(0).equals(change.getInt1())
                || change.getBsdxInfoCollectionList1() == null || change.getBsdxInfoCollectionList1().size() != 1
                || change.getBsdxInfoCollectionList2() == null || change.getBsdxInfoCollectionList2().size() != 1
                || !collection(change.getBsdxInfoCollectionList1().get(0), 0, condition, parameters, objects)
                || !collection(change.getBsdxInfoCollectionList2().get(0), 16, destination, target, List.of())) {
            throw drift("连打按键循环或退出签名不符");
        }
    }

    private static boolean collection(BsdxInfoCollection value, int root, List<Integer> types,
                                      List<Integer> parameters, List<Integer> objects) {
        return value != null && Integer.valueOf(root).equals(value.getInt1())
                && types.equals(value.getTypeList()) && parameters.equals(value.getParamList())
                && objects.equals(value.getIntList3()) && List.of().equals(value.getIntList4())
                && Integer.valueOf(0).equals(value.getInt2());
    }

    private static IllegalStateException drift(String message) {
        return new IllegalStateException("Katou变体输入版本不符: " + message);
    }
}
