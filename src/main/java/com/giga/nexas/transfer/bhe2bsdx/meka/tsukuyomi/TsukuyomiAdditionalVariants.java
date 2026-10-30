package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

import static com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.ProtectedCharacterEdits.*;

/** この役割だけの追加分を独立コピー上で検証し、成功した表だけ公開する。 */
final class TsukuyomiAdditionalVariants {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private TsukuyomiAdditionalVariants() { }

    static void appendAll(Mek targetMek, Waz targetWaz) {
        Mek mek = copy(targetMek, Mek.class);
        Waz waz = copy(targetWaz, Waz.class);
        Waz.Skill swift = skill(waz, "疾風", "0011", 2);
        CEventVal gate = cancelDonor(swift, 1, 43, List.of(0, 0, 0, 1));
        Waz.Skill drawn = skill(waz, "抜刀中", "0009", 2);
        Waz.Skill sheath = skill(waz, "納刀", "0008", 1);
        requireReviewedSuffix(skill(waz, "袈裟", "PUNCH", 1));
        requireReviewedSuffix(swift);
        requireReviewedSuffix(drawn);
        requireReviewedSuffix(sheath);
        Mek.MekWeaponInfo sheathWeapon = weapon(mek, waz, sheath, "納刀", "0003");
        requireNormalEnd(at(sheath, 1, 71, CEventChange.class, 30, 30));
        int group = mek.getMekBasicInfo().getWazFileSequence();
        int drawnIndex = waz.getSkillList().indexOf(drawn);
        int sheathIndex = waz.getSkillList().indexOf(sheath);
        CEventValRandom gravityTemplate = at(swift, 1, 21, CEventValRandom.class, 0, 0);
        if (!List.of(0, 0, 0, 0, 10, 0).equals(six(gravityTemplate))) throw invalid("重力供体が変更されています");

        append(mek, waz, "打突", "0001", "NAGE", 1, "打突・留め", "DX_DATOTSU_HOLD", 14,
                "低く踏み込んで打突を放ち、相手の近くで足を止める。命中後は別の技へつなぐか、納刀して次の攻めを選べる。", copy -> {
                    replace(at(copy, 1, 18, CEventValRandom.class, 9, 31),
                            List.of(0, 24, 0, 0, 6, 0), List.of(0, 12, 0, 0, 4, 0));
                    replace(at(copy, 1, 18, CEventValRandom.class, 40, 48),
                            List.of(0, 6, 0, 0, 24, 0), List.of(0, 6, 0, 0, 8, 0));
                    replace(at(copy, 1, 19, CEventValRandom.class, 9, 31),
                            List.of(0, 4, 0, 0, 1, 0), List.of(0, 0, 0, 0, 0, 0));
                    replace(at(copy, 1, 19, CEventValRandom.class, 42, 42),
                            List.of(1, 0, 0, 0, -3, 0), List.of(1, 0, 0, 0, 0, 0));
                    gravity(at(copy, 1, 21, CEventValRandom.class, 0, 0), 10);
                    directHit(at(copy, 1, 41, CEventHit.class, 43, 50), 30, 30, 43, 50);
                    cancel(copy, 1, 51, gate);
                    CEventChange exit = at(copy, 1, 71, CEventChange.class, 76, 76);
                    requireJump(exit, group, drawnIndex);
                    exit.getBsdxInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(group, sheathIndex)));
                }, sheathWeapon);

        append(mek, waz, "一威", "0005", "SWORD_KAITEN", 2, "一威・止刃", "DX_ICHII_BRAKE", 18,
                "一威の踏み込みを抑え、斬った後に相手を追い越しにくくする。近い間合いを保ち、袈裟や打突へ攻めをつなぐ。", copy -> {
                    replace(at(copy, 2, 18, CEventValRandom.class, 8, 10),
                            List.of(0, 64, 0, 0, 8, 0), List.of(0, 16, 0, 0, 4, 0));
                    replace(at(copy, 2, 18, CEventValRandom.class, 11, 24),
                            List.of(1, 0, 0, 0, 2, 0), List.of(1, 0, 0, 0, 0, 0));
                    gravity(at(copy, 2, 21, CEventValRandom.class, 20, 20), 0);
                    add(copy, 2, 21, constant(gravityTemplate, 2, 11, 50));
                    directHit(at(copy, 2, 41, CEventHit.class, 7, 10), 60, 60, 7, 10);
                    CEventVal originalGate = at(copy, 2, 47, CEventVal.class, 11, 87);
                    if (!List.of(0, 3, 0, 3).equals(List.of(originalGate.getInt1(), originalGate.getInt2(),
                            originalGate.getInt3(), originalGate.getInt4()))) throw invalid("一威の条件キャンセルが変更されています");
                    requireJump(at(copy, 2, 71, CEventChange.class, 87, 87), group, drawnIndex);
                });

        append(mek, waz, "逆風", "0003", "SWORD_TATE", 3, "逆風・返し", "DX_GYAKUFU_RETURN", 18,
                "斬り上げた後の攻めを選びやすくした逆風。空中では跳ね上がりを抑えて返し、低い位置の相手を狙う。", copy -> {
                    directHit(at(copy, 1, 41, CEventHit.class, 24, 27), 60, 60, 24, 27);
                    cancel(copy, 1, 28, gate);
                    gravity(at(copy, 1, 21, CEventValRandom.class, 0, 0), 10);
                    replace(at(copy, 2, 19, CEventValRandom.class, 14, 16),
                            List.of(0, 32, 0, 0, 8, 0), List.of(0, 12, 0, 0, 4, 0));
                    directHit(at(copy, 2, 41, CEventHit.class, 10, 11), 50, 50, 10, 11);
                    requireJump(at(copy, 3, 71, CEventChange.class, 35, 35), group, drawnIndex);
                });

        append(mek, waz, "銃撃", "G_LAUNCHER04", "0008", 3, "銃撃・離脱", "DX_GUNSHOT_RETREAT", 24,
                "一発撃った後に短く退き、刀を振る間合いを取り直す。射撃後に別の技へ移ることもできるが、背後の壁には注意。", copy -> {
                    if (realEffects(copy, 3, 17).size() != 1) throw invalid("銃撃の単発Effectが変更されています");
                    cancelDonor(copy, 3, 17, List.of(0, 2, 0, 1));
                    requireNormalEnd(at(copy, 3, 71, CEventChange.class, 50, 50));
                    gravity(at(copy, 3, 21, CEventValRandom.class, 18, 18), 0);
                    if (!events(copy, 3, 18).isEmpty()) throw invalid("銃撃の後退slotが既に使用されています");
                    CEventValRandom originalLift = at(copy, 2, 19, CEventValRandom.class, 18, 25);
                    if (!List.of(0, 16, 0, 0, 2, 0).equals(six(originalLift))) throw invalid("銃撃の回復供体が変更されています");
                    CEventValRandom retreat = constant(originalLift, 3, 18, 0);
                    retreat.setEndFrame(25);
                    retreat.setInt2(-6);
                    add(copy, 3, 18, retreat);
                });

        append(mek, waz, "切返", "0004", "KIRIKAESHI", 1, "切返・二連", "DX_KIRIKAESHI_DOUBLE", 26,
                "重い切り返しを二拍に分け、刃の届く位置を続けて攻める。一撃ごとの威力は抑えめで、撃ち終わりは射撃や別の斬撃へ移れる。", copy -> {
                    CEventHit hit = at(copy, 1, 41, CEventHit.class, 54, 65);
                    if (!Integer.valueOf(1).equals(hit.getHitCount()) || !Integer.valueOf(0).equals(hit.getHitInterval()))
                        throw invalid("切返の単発Hitが変更されています");
                    directHit(hit, 150, 65, 54, 65);
                    hit.setHitCount(2); hit.setHitInterval(7);
                    replace(at(copy, 1, 18, CEventValRandom.class, 54, 59),
                            List.of(0, 4, 0, 0, 16, 0), List.of(0, 4, 0, 0, 8, 0));
                    replace(at(copy, 1, 18, CEventValRandom.class, 60, 63),
                            List.of(0, 24, 0, 0, 16, 0), List.of(0, 24, 0, 0, 8, 0));
                    cancel(copy, 1, 66, gate);
                    requireJump(at(copy, 1, 71, CEventChange.class, 116, 116), group, drawnIndex);
                });

        // No new rows escape until every donor and every already-present variant has passed validation.
        publishAppended(targetMek, targetWaz, mek, waz);
    }

    private static void requireJump(CEventChange change, int group, int index) {
        if (!Integer.valueOf(1).equals(change.getFlag()) || !Integer.valueOf(0).equals(change.getInt1())
                || change.getBsdxInfoCollectionList1().size() != 1 || change.getBsdxInfoCollectionList2().size() != 1)
            throw invalid("納刀・抜刀の遷移構造が変更されています");
        var condition = change.getBsdxInfoCollectionList1().get(0);
        var jump = change.getBsdxInfoCollectionList2().get(0);
        if (!Integer.valueOf(0).equals(condition.getInt1()) || !List.of(0).equals(condition.getTypeList())
                || !condition.getParamList().isEmpty() || !Integer.valueOf(16).equals(jump.getInt1())
                || !List.of(1).equals(jump.getTypeList()) || !List.of(group, index).equals(jump.getParamList())
                || !condition.getIntList3().isEmpty() || !condition.getIntList4().isEmpty()
                || !jump.getIntList3().isEmpty() || !jump.getIntList4().isEmpty()
                || !Integer.valueOf(0).equals(condition.getInt2()) || !Integer.valueOf(0).equals(jump.getInt2()))
            throw invalid("納刀・抜刀の遷移先が変更されています");
    }

    private static void publishAppended(Mek targetMek, Waz targetWaz, Mek stagedMek, Waz stagedWaz) {
        int oldWeapons = targetMek.getMekWeaponInfoMap().size();
        int oldSkills = targetWaz.getSkillList().size();
        int appended = stagedMek.getMekWeaponInfoMap().size() - oldWeapons;
        if (appended < 0 || stagedWaz.getSkillList().size() - oldSkills != appended)
            throw invalid("追加された武装と主WAZの件数が一致しません");
        if (oldWeapons == stagedMek.getMekWeaponInfoMap().size()
                && oldSkills == stagedWaz.getSkillList().size()) return;
        Map<Integer, Mek.MekWeaponInfo> weapons = new LinkedHashMap<>(targetMek.getMekWeaponInfoMap());
        for (int i = oldWeapons; i < stagedMek.getMekWeaponInfoMap().size(); i++)
            weapons.put(i, stagedMek.getMekWeaponInfoMap().get(i));
        List<Waz.Skill> skills = new ArrayList<>(targetWaz.getSkillList());
        skills.addAll(stagedWaz.getSkillList().subList(oldSkills, stagedWaz.getSkillList().size()));
        var material = copy(targetMek.getMekMaterialBlock(), Mek.MekMaterialBlock.class);
        var regular = new ArrayList<>(targetMek.getMekMaterialBlock().getRegularEntries());
        var trailing = new ArrayList<>(targetMek.getMekMaterialBlock().getTrailingEntries());
        trailing.addAll(stagedMek.getMekMaterialBlock().getTrailingEntries()
                .subList(oldWeapons, stagedMek.getMekMaterialBlock().getTrailingEntries().size()));
        List<Mek.MekMaterialBlock.PluginEntry> entries = new ArrayList<>(regular);
        entries.addAll(trailing);
        material.setRegularEntries(regular);
        material.setTrailingEntries(trailing);
        material.setEntries(entries);
        targetMek.setMekWeaponInfoMap(weapons);
        targetMek.setMekMaterialBlock(material);
        targetWaz.setSkillList(skills);
    }

    private static void append(Mek mek, Waz waz, String sourceName, String sourceEnglish, String sequence,
                               int phases, String name, String english, int heat, String description,
                               Consumer<Waz.Skill> edit, Mek.MekWeaponInfo... materials) {
        Waz.Skill source = skill(waz, sourceName, sourceEnglish, phases);
        requireReviewedSuffix(source);
        var sourceWeapon = weapon(mek, waz, source, sourceName, sequence);
        CharacterWeaponVariants.append(mek, waz, sourceWeapon, source, name, english, heat, description, edit, materials);
    }

    private static void requireReviewedSuffix(Waz.Skill source) {
        List<List<Integer>> expected = switch (source.getSkillNameEnglish()) {
            case "PUNCH", "0001", "0004", "0008" -> List.of(List.of(0, 0));
            case "0005", "0009", "0011" -> List.of(List.of(0, 0), List.of(1, 0));
            case "0003" -> List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0));
            case "G_LAUNCHER04" -> List.of(List.of(2, 0));
            default -> throw invalid("未審査の供体suffixです: " + source.getSkillNameEnglish());
        };
        if (source.getSkillSuffixList() == null || source.getSkillSuffixList().size() != expected.size())
            throw invalid("供体suffixの件数が変更されています: " + source.getSkillNameEnglish());
        for (int i = 0; i < expected.size(); i++) {
            var entry = source.getSkillSuffixList().get(i);
            if (entry == null || !Objects.equals(expected.get(i).get(0), entry.getInt1())
                    || !Objects.equals(expected.get(i).get(1), entry.getInt2()))
                throw invalid("供体suffixの入口またはskip標識が変更されています: " + source.getSkillNameEnglish());
        }
    }

    private static <T> T copy(T source, Class<T> type) {
        try {
            return JSON.readValue(JSON.writeValueAsBytes(source), type);
        } catch (IOException error) {
            throw new IllegalStateException("Tsukuyomi追加武装の独立コピーに失敗しました", error);
        }
    }

    private static <T extends SkillInfoObject> T at(Waz.Skill skill, int phase, int slot, Class<T> type, int start, int end) {
        return event(skill, phase, slot, type, start, end, start, end);
    }

    private static List<SkillInfoObject> events(Waz.Skill skill, int phase, int slot) {
        var units = skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(unit -> Integer.valueOf(slot).equals(unit.getUnitQuantity())).toList();
        if (units.size() > 1) throw invalid("重複slot " + slot);
        return units.isEmpty() ? List.of() : units.get(0).getSkillInfoObjectList();
    }

    private static void add(Waz.Skill skill, int phase, int slot, SkillInfoObject event) {
        var units = skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection();
        var matches = units.stream().filter(unit -> Integer.valueOf(slot).equals(unit.getUnitQuantity())).toList();
        if (matches.size() > 1) throw invalid("重複slot " + slot);
        SkillUnit unit;
        if (matches.isEmpty()) {
            unit = new SkillUnit(slot, "追加武装の専用イベント");
            units.add(unit);
            units.sort(Comparator.comparing(SkillUnit::getUnitQuantity));
        } else unit = matches.get(0);
        event.setSlotNum(slot);
        unit.getSkillInfoObjectList().add(event);
        unit.getSkillInfoObjectList().sort(Comparator.comparing(SkillInfoObject::getStartFrame));
    }

    private static List<Integer> six(CEventValRandom e) {
        return List.of(e.getInt1(), e.getInt2(), e.getInt3(), e.getInt4(), e.getInt5(), e.getInt6());
    }

    private static void replace(CEventValRandom e, List<Integer> expected, List<Integer> target) {
        if (!Integer.valueOf(3).equals(e.getTypeId()) || !expected.equals(six(e)))
            throw invalid("速度・ジャンプの供体値または型が変更されています");
        e.setInt1(target.get(0)); e.setInt2(target.get(1)); e.setInt3(target.get(2));
        e.setInt4(target.get(3)); e.setInt5(target.get(4)); e.setInt6(target.get(5));
    }

    private static void gravity(CEventValRandom event, int oldValue) {
        if (!Integer.valueOf(2).equals(event.getTypeId()) || !List.of(0, 0, 0, 0, oldValue, 0).equals(six(event)))
            throw invalid("重力slotの値または型が変更されています");
        event.setInt2(50);
        event.setInt5(50);
    }

    private static CEventValRandom constant(CEventValRandom donor, int type, int frame, int value) {
        if (!Integer.valueOf(type).equals(donor.getTypeId())) throw invalid("定数供体の型が変更されています");
        CEventValRandom result = copy(donor, CEventValRandom.class);
        result.setStartFrame(frame); result.setEndFrame(frame);
        result.setInt1(0); result.setInt2(value); result.setInt3(0);
        result.setInt4(0); result.setInt5(value); result.setInt6(0);
        return result;
    }

    private static CEventVal cancelDonor(Waz.Skill skill, int phase, int frame, List<Integer> expected) {
        CEventVal gate = at(skill, phase, 47, CEventVal.class, frame, frame);
        if (!Integer.valueOf(0).equals(gate.getTypeId())
                || !expected.equals(List.of(gate.getInt1(), gate.getInt2(), gate.getInt3(), gate.getInt4()))) {
            throw invalid("通常キャンセル供体が変更されています");
        }
        return gate;
    }

    private static void cancel(Waz.Skill skill, int phase, int frame, CEventVal donor) {
        CEventVal result = copy(donor, CEventVal.class);
        result.setStartFrame(frame); result.setEndFrame(frame);
        add(skill, phase, 47, result);
    }

    private static void directHit(CEventHit hit, int expectedPower, int targetPower, int start, int end) {
        int oldStart = hit.getStartFrame(), oldEnd = hit.getEndFrame();
        attack(hit, oldStart, oldEnd, expectedPower, oldStart, oldEnd, expectedPower).run();
        attack(hit, oldStart, oldEnd, expectedPower, start, end, targetPower).run();
    }

    private static CEventWazaSelect reference(CEventEffect effect) {
        var values = effect.getCeventEffectUnitList().stream()
                .filter(unit -> Integer.valueOf(3).equals(unit.getUnitSlotNum()) && Integer.valueOf(1).equals(unit.getBuffer()))
                .map(CEventEffect.CEventEffectUnit::getData).toList();
        if (values.size() != 1 || !(values.get(0) instanceof CEventWazaSelect result)
                || result.getWazFileNo() == null || result.getWazSequenceNo() == null
                || result.getWazFileNo() < 0 || result.getWazSequenceNo() < 0) throw invalid("Effect参照が変更されています");
        return result;
    }

    private static CEventValRandom launchCount(CEventEffect effect) {
        var values = effect.getCeventEffectUnitList().stream()
                .filter(unit -> Integer.valueOf(4).equals(unit.getUnitSlotNum()) && Integer.valueOf(1).equals(unit.getBuffer()))
                .map(CEventEffect.CEventEffectUnit::getData).toList();
        if (values.size() != 1 || !(values.get(0) instanceof CEventValRandom result)
                || !Integer.valueOf(2).equals(result.getTypeId())
                || !List.of(0, 1, 0, 0, 1, 0).equals(six(result))) throw invalid("単発Effectの発射数が変更されています");
        return result;
    }

    private static List<CEventEffect> realEffects(Waz.Skill skill, int phase, int frame) {
        List<CEventEffect> result = new ArrayList<>();
        for (var event : events(skill, phase, 48)) {
            if (event instanceof CEventEffect effect && reference(effect).getWazFileNo() != 0) {
                if (!Integer.valueOf(frame).equals(effect.getStartFrame()) || !Integer.valueOf(frame).equals(effect.getEndFrame()))
                    throw invalid("実弾Effectの時刻が変更されています");
                launchCount(effect);
                result.add(effect);
            }
        }
        return result;
    }

    private static IllegalStateException invalid(String message) {
        return new IllegalStateException("Tsukuyomi追加武装拒否: " + message);
    }
}
