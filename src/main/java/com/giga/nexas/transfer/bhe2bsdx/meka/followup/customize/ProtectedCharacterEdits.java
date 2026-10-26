package com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** 单机体参数修改的定位和签名保护；返回的写操作由调用方在整批校验后执行。 */
public final class ProtectedCharacterEdits {

    private ProtectedCharacterEdits() { }

    public static Mek requireMek(FollowupMekaContext context, String role) {
        if (context == null || context.getReboundMek() == null
                || context.getReboundMek().getMekBasicInfo() == null) {
            throw drift(role + " 目标MEK缺失");
        }
        return context.getReboundMek();
    }

    public static Waz requireWaz(FollowupMekaContext context, String role) {
        if (context == null || context.getReboundWaz() == null
                || context.getReboundWaz().getSkillList() == null) {
            throw drift(role + " 目标WAZ缺失");
        }
        return context.getReboundWaz();
    }

    public static Waz.Skill skill(Waz waz, String japanese, String english, int phases) {
        List<Waz.Skill> matches = waz.getSkillList().stream().filter(Objects::nonNull)
                .filter(value -> japanese.equals(value.getSkillNameJapanese())
                        && english.equals(value.getSkillNameEnglish())).toList();
        if (matches.size() != 1 || matches.get(0).getPhasesInfo() == null
                || matches.get(0).getPhasesInfo().size() != phases) {
            throw drift(japanese + "/" + english + " 身份或phase数量不符");
        }
        return matches.get(0);
    }

    public static Mek.MekWeaponInfo weapon(Mek mek, Waz waz, Waz.Skill skill,
                                           String japanese, String sequence) {
        if (mek.getMekWeaponInfoMap() == null) throw drift(japanese + " 武装表缺失");
        List<Mek.MekWeaponInfo> matches = mek.getMekWeaponInfoMap().values().stream()
                .filter(Objects::nonNull).filter(value -> japanese.equals(value.getWeaponName())
                        && sequence.equals(value.getWeaponSequence())).toList();
        if (matches.size() != 1 || !Integer.valueOf(waz.getSkillList().indexOf(skill))
                .equals(matches.get(0).getWazSequence())) {
            throw drift(japanese + "/" + sequence + " 武装身份或WAZ引用不符");
        }
        return matches.get(0);
    }

    public static <T extends SkillInfoObject> T event(Waz.Skill skill, int phase, int slot,
                                                     Class<T> type, int start, int end,
                                                     int targetStart, int targetEnd) {
        var units = skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection();
        if (units == null) throw drift(label(skill, phase, slot) + " 槽位表缺失");
        var matches = units.stream().filter(Objects::nonNull)
                .filter(value -> Integer.valueOf(slot).equals(value.getUnitQuantity())).toList();
        if (matches.size() != 1 || matches.get(0).getSkillInfoObjectList() == null) {
            throw drift(label(skill, phase, slot) + " 槽位不唯一");
        }
        List<T> events = matches.get(0).getSkillInfoObjectList().stream().filter(type::isInstance)
                .map(type::cast).filter(value -> frames(value, start, end)
                        || frames(value, targetStart, targetEnd)).toList();
        if (events.size() != 1) throw drift(label(skill, phase, slot) + " 事件帧签名不唯一");
        return events.get(0);
    }

    public static Runnable speed(CEventValRandom event, List<Integer> original, List<Integer> target) {
        List<Integer> actual = Arrays.asList(event.getInt1(), event.getInt2(), event.getInt3(),
                event.getInt4(), event.getInt5(), event.getInt6());
        if (!actual.equals(original) && !actual.equals(target)) throw drift("速度六整数载荷不符: " + actual);
        return () -> {
            event.setInt1(target.get(0));
            event.setInt2(target.get(1));
            event.setInt3(target.get(2));
            event.setInt4(target.get(3));
            event.setInt5(target.get(4));
            event.setInt6(target.get(5));
        };
    }

    public static Runnable attack(CEventHit hit, int start, int end, int originalPower,
                                   int targetStart, int targetEnd, int targetPower) {
        BsdxInfoCollection collection = directPower(hit);
        Integer value = collection.getParamList().get(0);
        boolean original = frames(hit, start, end) && Integer.valueOf(originalPower).equals(value);
        boolean target = frames(hit, targetStart, targetEnd) && Integer.valueOf(targetPower).equals(value);
        if (!original && !target) throw drift("攻击事件帧窗或直接攻击值不符");
        return () -> {
            hit.setStartFrame(targetStart);
            hit.setEndFrame(targetEnd);
            collection.setParamList(new ArrayList<>(List.of(targetPower)));
        };
    }

    public static Runnable weaponCost(Mek.MekWeaponInfo weapon, int oldMin, int oldMax,
                                       int targetCost, String description) {
        boolean original = Integer.valueOf(oldMin).equals(weapon.getHeatMinConsumption())
                && Integer.valueOf(oldMax).equals(weapon.getHeatMaxConsumption());
        boolean target = Integer.valueOf(targetCost).equals(weapon.getHeatMinConsumption())
                && Integer.valueOf(targetCost).equals(weapon.getHeatMaxConsumption());
        if ((!original && !target) || !Integer.valueOf(0).equals(weapon.getUpgradeExp())) {
            throw drift(weapon.getWeaponName() + " 热耗或升级值不符");
        }
        return () -> {
            weapon.setHeatMinConsumption(targetCost);
            weapon.setHeatMaxConsumption(targetCost);
            weapon.setUpgradeExp(0);
            weapon.setWeaponDescription(description);
        };
    }

    public static boolean frames(SkillInfoObject event, int start, int end) {
        return Integer.valueOf(start).equals(event.getStartFrame())
                && Integer.valueOf(end).equals(event.getEndFrame());
    }

    /** 本批三个退出donor原始资源的正常结束签名，不接受跳回持続姿态的同名技能。 */
    public static void requireNormalEnd(CEventChange change) {
        if (change == null || !Integer.valueOf(1).equals(change.getFlag())
                || !Integer.valueOf(0).equals(change.getInt1())
                || change.getBsdxInfoCollectionList1() == null || change.getBsdxInfoCollectionList1().size() != 1
                || change.getBsdxInfoCollectionList2() == null || change.getBsdxInfoCollectionList2().size() != 1
                || !normalEndCollection(change.getBsdxInfoCollectionList1().get(0), 0, List.of(0))
                || !normalEndCollection(change.getBsdxInfoCollectionList2().get(0), 16, List.of(0, 0))) {
            throw drift("退出donor不是原始正常结束Change");
        }
    }

    private static boolean normalEndCollection(BsdxInfoCollection collection, int root, List<Integer> types) {
        return collection != null && Integer.valueOf(root).equals(collection.getInt1())
                && types.equals(collection.getTypeList())
                && List.of().equals(collection.getParamList())
                && List.of().equals(collection.getIntList3())
                && List.of().equals(collection.getIntList4())
                && Integer.valueOf(0).equals(collection.getInt2());
    }

    private static BsdxInfoCollection directPower(CEventHit hit) {
        if (hit.getCeventHitUnitList() == null) throw drift("Hit内层槽位缺失");
        var units = hit.getCeventHitUnitList().stream().filter(Objects::nonNull)
                .filter(value -> Integer.valueOf(6).equals(value.getUnitSlotNum())).toList();
        if (units.size() != 1 || !Integer.valueOf(1).equals(units.get(0).getBuffer())
                || !(units.get(0).getData() instanceof CEventTerm term)
                || !Integer.valueOf(32).equals(term.getTypeId()) || !frames(term, 0, 0)
                || term.getBsdxInfoCollectionList() == null || term.getBsdxInfoCollectionList().size() != 1) {
            throw drift("Hit Unit6不是唯一启用的直接攻击参数");
        }
        BsdxInfoCollection collection = term.getBsdxInfoCollectionList().get(0);
        if (collection == null || !Integer.valueOf(23).equals(collection.getInt1()) // 本批真实目标的PARAM/VAL root。
                || !List.of(0).equals(collection.getTypeList())
                || collection.getParamList() == null || collection.getParamList().size() != 1
                || collection.getParamList().get(0) == null
                || collection.getIntList3() == null || !collection.getIntList3().isEmpty()
                || collection.getIntList4() == null || !collection.getIntList4().isEmpty()
                || !Integer.valueOf(0).equals(collection.getInt2())) {
            throw drift("Hit Unit6表达式不是单个直接值");
        }
        return collection;
    }

    private static String label(Waz.Skill skill, int phase, int slot) {
        return skill.getSkillNameJapanese() + "/" + skill.getSkillNameEnglish() + " P" + phase + " slot" + slot;
    }

    private static IllegalStateException drift(String message) {
        return new IllegalStateException("角色重做输入版本不符: " + message);
    }
}
