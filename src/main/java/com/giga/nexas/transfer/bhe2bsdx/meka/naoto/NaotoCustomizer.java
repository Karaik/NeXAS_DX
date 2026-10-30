package com.giga.nexas.transfer.bhe2bsdx.meka.naoto;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaCustomizer;

import java.util.List;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

public final class NaotoCustomizer implements FollowupMekaCustomizer {

    public static final NaotoCustomizer INSTANCE = new NaotoCustomizer();

    private NaotoCustomizer() {
    }

    @Override
    public void customizeBeforeWrite(FollowupMekaContext context) {
        Mek mek = requireMek(context, "Naoto");
        Waz waz = requireWaz(context, "Naoto");
        Waz.Skill jab = skill(waz, "ジャブ", "STRAIGHT07", 1);
        Waz.Skill straight = skill(waz, "ストレートパンチ", "STRAIGHT04", 1);
        Mek.MekWeaponInfo source = weapon(mek, waz, jab, "ジャブ", "0001");
        Mek.MekWeaponInfo donor = weapon(mek, waz, straight, "ストレートパンチ", "0003");
        Integer group = mek.getMekBasicInfo().getWazFileSequence();
        if (group == null || group < 0 || !Integer.valueOf(0).equals(source.getHeatMinConsumption())
                || !Integer.valueOf(10).equals(source.getHeatMaxConsumption())
                || !Integer.valueOf(0).equals(source.getUpgradeExp())) {
            throw drift("最终WAZ组或原jab热耗不符");
        }
        // 终结技能保持原载荷，只把其当前身份和Material作为显式依赖。
        attack(event(straight, 1, 41, CEventHit.class, 17, 30, 17, 30), 17, 30, 30, 17, 30, 30);
        requireNormalEnd(event(straight, 1, 71, CEventChange.class, 57, 57, 57, 57));
        int straightIndex = waz.getSkillList().indexOf(straight);
        CharacterWeaponVariants.append(mek, waz, source, jab,
                "ワンツー・コンファーム", "DX_HIT_CONFIRM_ONE_TWO", 20,
                "ジャブの命中時だけストレートへつなぐ。空振りでは追撃せず、近距離の確認から次の展開を選ぶ。",
                copy -> addHitConfirm(copy, group, straightIndex), donor);
        NaotoAdditionalVariants.append(mek, waz);
    }

    private static void addHitConfirm(Waz.Skill copy, int group, int straightIndex) {
        attack(event(copy, 1, 41, CEventHit.class, 11, 16, 11, 16), 11, 16, 20, 11, 16, 20);
        requireNormalEnd(event(copy, 1, 71, CEventChange.class, 37, 37, 37, 37));
        var unit = copy.getPhasesInfo().get(0).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(71).equals(u.getUnitQuantity())).findFirst().orElseThrow();
        if (unit.getSkillInfoObjectList().size() != 1) throw drift("原jab存在未确认的额外Change");

        CEventChange hit = new CEventChange(0x22);
        hit.setSlotNum(71);
        hit.setStartFrame(11);
        hit.setEndFrame(16);
        hit.setFlag(1);
        hit.setInt1(0);
        // 原版Yuri命中派生经当前Term重编译后的条件；本角色测试独立解析该原生样本对照。
        BsdxInfoCollection condition = new BsdxInfoCollection();
        condition.setInt1(0);
        condition.setInt2(0);
        condition.getTypeList().addAll(List.of(2, 6, 0, 0));
        condition.getParamList().add(0);
        condition.getIntList3().addAll(List.of(0, 0));
        hit.getBsdxInfoCollectionList1().add(condition);
        BsdxInfoCollection destination = new BsdxInfoCollection();
        destination.setInt1(16);
        destination.setInt2(0);
        destination.getTypeList().add(1);
        destination.getParamList().addAll(List.of(group, straightIndex));
        hit.getBsdxInfoCollectionList2().add(destination);
        // 保持原37f落空出口；只有命中条件成立才提前进入既有Straight。
        unit.getSkillInfoObjectList().add(0, hit);
    }

    private static void requireNormalEnd(CEventChange change) {
        if (!Integer.valueOf(1).equals(change.getFlag()) || !Integer.valueOf(0).equals(change.getInt1())
                || change.getBsdxInfoCollectionList1() == null || change.getBsdxInfoCollectionList1().size() != 1
                || change.getBsdxInfoCollectionList2() == null || change.getBsdxInfoCollectionList2().size() != 1
                || !collection(change.getBsdxInfoCollectionList1().get(0), 0, List.of(0))
                || !collection(change.getBsdxInfoCollectionList2().get(0), 16, List.of(0, 0))) {
            throw drift("原生结束条件或跳转不符");
        }
    }

    private static boolean collection(BsdxInfoCollection c, int root, List<Integer> types) {
        return c != null && Integer.valueOf(root).equals(c.getInt1()) && Integer.valueOf(0).equals(c.getInt2())
                && types.equals(c.getTypeList()) && List.of().equals(c.getParamList())
                && List.of().equals(c.getIntList3()) && List.of().equals(c.getIntList4());
    }

    private static IllegalStateException drift(String message) {
        return new IllegalStateException("Naoto变体输入版本不符: " + message);
    }
}
