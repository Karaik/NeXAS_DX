package com.giga.nexas.transfer.bhe2bsdx.meka.freja;

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

/** 既存の原技改造を保ち、この役割の追加六武装だけを一括公開する。 */
final class FrejaAdditionalVariants {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private FrejaAdditionalVariants() { }

    static void appendAll(Mek targetMek, Waz targetWaz) {
        Mek mek = copy(targetMek, Mek.class);
        Waz waz = copy(targetWaz, Waz.class);
        Waz.Skill javelin = skill(waz, "ジャベリン", "G_BIT03", 1);
        CEventVal gate = cancelDonor(javelin, 1, 13, List.of(0, 2, 0, 1));
        requireNormalEnd(at(javelin, 1, 71, CEventChange.class, 52, 52));
        Waz.Skill uppercut = skill(waz, "斬り上げ", "SCYTHE04", 2);
        CEventValRandom gravityTemplate = at(uppercut, 1, 21, CEventValRandom.class, 48, 48);
        if (!List.of(0, 0, 0, 0, 50, 0).equals(six(gravityTemplate))) throw invalid("重力供体が変更されています");
        Waz.Skill backkick = skill(waz, "バックキック", "0004", 2);
        CEventValRandom liftTemplate = at(backkick, 1, 19, CEventValRandom.class, 19, 26);
        if (!List.of(0, 3, 0, 0, 4, 0).equals(six(liftTemplate))) throw invalid("ジャンプ力供体が変更されています");

        append(mek, waz, "スピア", "0002", "0003", 5, "スピア・留め突き", "DX_SPEAR_ANCHOR", 22,
                "突き抜ける勢いを抑え、槍の届く間合いに留まるスピア。攻撃後は斬り上げやバックキックへ移り、距離を選び直せる。", copy -> {
                    replace(at(copy, 3, 18, CEventValRandom.class, 1, 8),
                            List.of(0, 36, 0, 0, 20, 0), List.of(0, 24, 0, 0, 8, 0));
                    replace(at(copy, 5, 18, CEventValRandom.class, 12, 15),
                            List.of(0, 36, 0, 0, 20, 0), List.of(0, 24, 0, 0, 8, 0));
                    directHit(at(copy, 3, 41, CEventHit.class, 3, 10), 180, 180, 3, 10);
                    directHit(at(copy, 5, 41, CEventHit.class, 16, 24), 180, 180, 16, 24);
                    requireNormalEnd(at(copy, 3, 71, CEventChange.class, 21, 21));
                    requireNormalEnd(at(copy, 5, 71, CEventChange.class, 64, 64));
                    cancel(copy, 3, 11, gate);
                    cancel(copy, 5, 25, gate);
                });

        append(mek, waz, "斬り上げ", "SCYTHE04", "0005", 2, "斬り上げ・低空", "DX_UPPERCUT_LOW", 20,
                "自機の跳ね上がりを抑えた斬り上げ。槍が届く高さを保ち、投擲や次の打撃へ攻めをつなぐ。高い相手を追い続ける技ではない。", copy -> {
                    replace(at(copy, 1, 19, CEventValRandom.class, 33, 39),
                            List.of(0, 24, 0, 0, 2, 0), List.of(0, 8, 0, 0, 0, 0));
                    replace(at(copy, 1, 18, CEventValRandom.class, 29, 32),
                            List.of(0, 0, 0, 0, 16, 0), List.of(0, 0, 0, 0, 8, 0));
                    replace(at(copy, 1, 18, CEventValRandom.class, 33, 51),
                            List.of(0, 4, 0, 0, 1, 0), List.of(0, 4, 0, 0, 0, 0));
                    directHit(at(copy, 1, 41, CEventHit.class, 29, 44), 170, 170, 29, 44);
                    add(copy, 1, 21, constant(gravityTemplate, 2, 0, 50));
                    CEventVal jumpEntry = at(copy, 1, 27, CEventVal.class, 0, 0);
                    CEventVal jumpRecovery = at(copy, 1, 27, CEventVal.class, 48, 48);
                    requireModifier(jumpEntry, List.of(0, 0, 0, 0));
                    requireModifier(jumpRecovery, List.of(0, 0, 0, 100));
                    var jumpSlot = copy.getPhasesInfo().get(0).getSkillUnitCollection().stream()
                            .filter(unit -> Integer.valueOf(27).equals(unit.getUnitQuantity())).toList();
                    if (jumpSlot.size() != 1 || jumpSlot.get(0).getSkillInfoObjectList().size() != 2
                            || !jumpSlot.get(0).getSkillInfoUnknownList().isEmpty())
                        throw invalid("斬り上げのjump modifier供体の件数またはFFが変更されています");
                    // 点イベントも開始値を100にする。48fの回復点でも一時的に0へ戻さない。
                    jumpEntry.setInt2(100); jumpEntry.setInt4(100);
                    jumpRecovery.setInt2(100);
                    if (!events(copy, 2, 19).isEmpty() || !events(copy, 2, 21).isEmpty())
                        throw invalid("斬り上げの回復入口が既に変更されています");
                    for (int slot : List.of(27, 28, 47)) {
                        boolean occupied = copy.getPhasesInfo().get(1).getSkillUnitCollection().stream()
                                .filter(unit -> Integer.valueOf(slot).equals(unit.getUnitQuantity()))
                                .anyMatch(unit -> !unit.getSkillInfoObjectList().isEmpty() || !unit.getSkillInfoUnknownList().isEmpty());
                        if (occupied) throw invalid("斬り上げの回復modifier・取消入口が既に変更されています");
                    }
                    add(copy, 2, 19, constant(liftTemplate, 3, 0, 0));
                    add(copy, 2, 21, constant(gravityTemplate, 2, 0, 50));
                    add(copy, 2, 27, modifier100(jumpEntry));
                    add(copy, 2, 28, modifier100(jumpEntry));
                    cancel(copy, 2, 0, gate);
                    openOrdinaryAfterSd(copy, 159, 44);
                    cancel(copy, 1, 45, gate);
                    at(copy, 1, 71, CEventChange.class, 37, 159);
                    at(copy, 2, 71, CEventChange.class, 28, 28);
                });

        append(mek, waz, "バックキック", "0004", "BLOW2", 2, "バックキック・退避", "DX_BACKKICK_RETREAT", 18,
                "低く蹴り返して間合いを外すバックキック。威力と攻撃時間を抑える代わりに、投擲や次の位置取りへ切り替えやすい。", copy -> {
                    replace(at(copy, 1, 19, CEventValRandom.class, 19, 26),
                            List.of(0, 3, 0, 0, 4, 0), List.of(0, 0, 0, 0, 0, 0));
                    gravity(at(copy, 1, 21, CEventValRandom.class, 3, 3), 0);
                    gravity(at(copy, 1, 21, CEventValRandom.class, 46, 46), 100);
                    replace(at(copy, 1, 18, CEventValRandom.class, 19, 36),
                            List.of(0, 24, 0, 0, 6, 0), List.of(0, 16, 0, 0, 4, 0));
                    replace(at(copy, 1, 18, CEventValRandom.class, 37, 55),
                            List.of(1, 0, 0, 0, 2, 0), List.of(1, 0, 0, 0, 0, 0));
                    directHit(at(copy, 1, 41, CEventHit.class, 23, 46), 300, 180, 23, 32);
                    openOrdinaryAfterSd(copy, 132, 32);
                    cancel(copy, 1, 33, gate);
                    at(copy, 1, 71, CEventChange.class, 33, 132);
                    requireNormalEnd(at(copy, 2, 71, CEventChange.class, 29, 29));
                });

        append(mek, waz, "スイング", "0003", "0004", 1, "スイング・双薙ぎ", "DX_SWING_DOUBLE", 28,
                "大きな薙ぎ払いを二拍に分けて放つ。一撃ごとの威力は低いが、届く位置を続けて攻め、撃ち終わりから次の武装へ移れる。", copy -> {
                    CEventHit hit = at(copy, 1, 41, CEventHit.class, 42, 51);
                    if (!Integer.valueOf(1).equals(hit.getHitCount()) || !Integer.valueOf(0).equals(hit.getHitInterval()))
                        throw invalid("スイングの単発Hitが変更されています");
                    directHit(hit, 250, 90, 42, 51);
                    hit.setHitCount(2); hit.setHitInterval(6);
                    replace(at(copy, 1, 18, CEventValRandom.class, 37, 41),
                            List.of(0, 8, 0, 0, 24, 0), List.of(0, 8, 0, 0, 12, 0));
                    cancel(copy, 1, 52, gate);
                    requireNormalEnd(at(copy, 1, 71, CEventChange.class, 113, 113));
                });

        append(mek, waz, "ジャベリン", "G_BIT03", "RIFLE", 1, "ジャベリン・二段投擲", "DX_JAVELIN_TWO", 16,
                "ジャベリンを二度に分けて投げ、相手の動きを牽制する。二投目までやや隙があるため、離れた間合いで使い、投げ終わりから接近したい。", copy -> {
                    List<CEventEffect> shots = realEffects(copy, 1, 13);
                    if (shots.size() != 1 || events(copy, 1, 48).size() != 1 || !events(copy, 1, 35).isEmpty())
                        throw invalid("ジャベリンの単発経路が変更されています");
                    CEventEffect second = copy(shots.get(0), CEventEffect.class);
                    second.setStartFrame(25); second.setEndFrame(25);
                    // The launch SE is inside this Effect; startup SE and voice remain single.
                    add(copy, 1, 48, second);
                    CEventVal exitGate = cancelDonor(copy, 1, 13, List.of(0, 2, 0, 1));
                    exitGate.setStartFrame(26); exitGate.setEndFrame(26);
                    requireNormalEnd(at(copy, 1, 71, CEventChange.class, 52, 52));
                });

        Waz.Skill walking = skill(waz, "歩行01", "WALK01", 1);
        requireReviewedSuffix(walking);
        CEventTerm center = position(at(walking, 1, 48, CEventEffect.class, 15, 15), List.of(0, 0));
        append(mek, waz, "火炎弾", "0005", "SINMISSILE", 1, "火炎弾・扇射", "DX_FLAME_FAN", 22,
                "左右へ広げた火炎弾に中央の一発を加え、前方を広く牽制する。離れた相手の移動を制限し、槍を振る間合いを作る。", copy -> {
                    List<CEventEffect> shots = realEffects(copy, 1, 14);
                    if (shots.size() != 2 || events(copy, 1, 48).size() != 4) throw invalid("火炎弾の実弾と装飾の組が変更されています");
                    CEventWazaSelect leftRef = reference(shots.get(0)), rightRef = reference(shots.get(1));
                    if (!Objects.equals(leftRef.getWazFileNo(), rightRef.getWazFileNo())
                            || !Objects.equals(leftRef.getWazSequenceNo(), rightRef.getWazSequenceNo()))
                        throw invalid("火炎弾の左右参照が一致しません");
                    CEventEffect left = null, right = null;
                    for (CEventEffect shot : shots) {
                        List<Integer> point = effectPosition(shot).getBsdxInfoCollectionList().get(0).getIntList3();
                        if (List.of(0, 1).equals(point) && left == null) left = shot;
                        else if (List.of(0, 2).equals(point) && right == null) right = shot;
                        else throw invalid("火炎弾の左右位置が変更されています");
                    }
                    if (left == null || right == null) throw invalid("火炎弾の左右位置が不足しています");
                    position(left, List.of(0, 1)); position(right, List.of(0, 2));
                    replace(direction(left), List.of(0, 0, 0, 0, 10, 0), List.of(0, 0, 0, 0, 30, 0));
                    replace(direction(right), List.of(0, 0, 0, 0, -10, 0), List.of(0, 0, 0, 0, -30, 0));
                    CEventEffect middle = copy(left, CEventEffect.class);
                    replace(direction(middle), List.of(0, 0, 0, 0, 30, 0), List.of(0, 0, 0, 0, 0, 0));
                    effectEntry(middle, 7).setData(copy(center, CEventTerm.class));
                    add(copy, 1, 48, middle);
                    cancelDonor(copy, 1, 14, List.of(0, 0, 0, 1));
                    requireNormalEnd(at(copy, 1, 71, CEventChange.class, 56, 56));
                });

        publishAppended(targetMek, targetWaz, mek, waz);
    }

    private static void requireModifier(CEventVal value, List<Integer> expected) {
        if (!Integer.valueOf(0).equals(value.getTypeId())
                || !expected.equals(List.of(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4())))
            throw invalid("jump modifierの元載荷が変更されています");
    }

    private static CEventVal modifier100(CEventVal donor) {
        requireModifier(donor, List.of(0, 100, 0, 100));
        CEventVal result = copy(donor, CEventVal.class);
        result.setStartFrame(0); result.setEndFrame(0);
        return result;
    }

    private static void openOrdinaryAfterSd(Waz.Skill value, int originalEnd, int sdEnd) {
        var units = value.getPhasesInfo().get(0).getSkillUnitCollection().stream()
                .filter(unit -> Integer.valueOf(47).equals(unit.getUnitQuantity())).toList();
        if (units.size() != 1) throw invalid("取消slotが一意ではありません");
        SkillUnit slot = units.get(0);
        if (slot.getSkillInfoObjectList().size() != 1 || slot.getSkillInfoUnknownList().size() != 1)
            throw invalid("取消slotのSDまたはFF件数が変更されています");
        CEventVal sd = at(value, 1, 47, CEventVal.class, 3, originalEnd);
        if (!Integer.valueOf(0).equals(sd.getTypeId())
                || !List.of(0, 3, 0, 3).equals(List.of(sd.getInt1(), sd.getInt2(), sd.getInt3(), sd.getInt4())))
            throw invalid("取消slotのSD載荷が変更されています");
        SkillInfoUnknown loop = slot.getSkillInfoUnknownList().get(0);
        if (!Integer.valueOf(255).equals(loop.getTypeId()) || !frames(loop, 3, originalEnd)
                || !List.of(-1, 1, -2).equals(List.of(loop.getInt1(), loop.getInt2(), loop.getInt3())))
            throw invalid("取消slotの無限FF署名が変更されています");
        // 63ACF0はslot時計全体を起点へ戻す。終点短縮では新しい通常門へ到達できない。
        slot.getSkillInfoUnknownList().clear();
        sd.setEndFrame(sdEnd);
    }

    private static CEventEffect.CEventEffectUnit effectEntry(CEventEffect effect, int slot) {
        var values = effect.getCeventEffectUnitList().stream().filter(unit -> Integer.valueOf(slot).equals(unit.getUnitSlotNum())
                && Integer.valueOf(1).equals(unit.getBuffer())).toList();
        if (values.size() != 1) throw invalid("Effect内部slotが不正です: " + slot);
        return values.get(0);
    }

    private static CEventTerm effectPosition(CEventEffect effect) {
        if (!(effectEntry(effect, 7).getData() instanceof CEventTerm term)
                || !Integer.valueOf(5).equals(term.getTypeId()) || !frames(term, 0, 0)
                || term.getBsdxInfoCollectionList().size() != 1) throw invalid("発射位置式が変更されています");
        return term;
    }

    private static CEventTerm position(CEventEffect effect, List<Integer> expected) {
        CEventTerm term = effectPosition(effect);
        var expression = term.getBsdxInfoCollectionList().get(0);
        if (!Integer.valueOf(20).equals(expression.getInt1()) || !List.of(1).equals(expression.getTypeList())
                || !List.of().equals(expression.getParamList()) || !expected.equals(expression.getIntList3())
                || !List.of().equals(expression.getIntList4()) || !Integer.valueOf(0).equals(expression.getInt2()))
            throw invalid("自身の発射位置署名が変更されています");
        return term;
    }

    private static CEventValRandom direction(CEventEffect effect) {
        if (!(effectEntry(effect, 29).getData() instanceof CEventValRandom result)
                || !Integer.valueOf(3).equals(result.getTypeId()) || !frames(result, 0, 0))
            throw invalid("火炎弾の方向補正式が変更されています");
        return result;
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
            case "0002" -> List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0), List.of(3, 0), List.of(4, 0));
            case "SCYTHE04", "0004" -> List.of(List.of(0, 0), List.of(1, 0));
            case "0003", "G_BIT03", "0005", "WALK01" -> List.of(List.of(0, 0));
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
            throw new IllegalStateException("Freja追加武装の独立コピーに失敗しました", error);
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
        return new IllegalStateException("Freja追加武装拒否: " + message);
    }
}
