package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bhe.BheInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sou呪装刀を単一対象の一動作へ限定する。
 * 元のMULTILOCK条件が無条件へ降格したP1自己再入だけを除き、元の46f/49f回復を残す。
 */
final class SouCursedBladeSingleTargetFixer {
    private static final List<Integer> NONE = List.of();
    private static final List<Integer> SELF = List.of(0, 0);
    private static final List<Integer> ALWAYS = List.of(0);
    private static final List<Integer> MULTILOCK = List.of(2, 9, 8, 5);
    private static final List<Signature> FAMILY = List.of(
            new Signature("呪装刀Lv1", "JUSOUTOU01", 78, 41, 74, 11),
            new Signature("呪装刀Lv2", "JUSOUTOU02", 86, 44, 77, 9),
            new Signature("呪装刀Lv3", "JUSOUTOU03", 90, 50, 83, 7));
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SouCursedBladeSingleTargetFixer() { }

    static void fix(FollowupMekaContext context) {
        // Retain the existing MEK-only customization boundary.
        if (context == null || context.getReboundWaz() == null) return;
        com.giga.nexas.dto.bhe.waz.Waz source = sourceMain(context);
        Waz target = context.getReboundWaz();
        List<LocatedSkill> skills = new ArrayList<>();
        Boolean installed = null;
        for (Signature signature : FAMILY) {
            validateSource(findSource(source, signature), signature);
            LocatedSkill located = findTarget(target, signature);
            boolean currentInstalled = validateTarget(located.skill(), signature);
            if (installed != null && installed != currentInstalled)
                throw invalid("三等級が部分的に適用されています");
            installed = currentInstalled;
            skills.add(located);
        }
        // A verified repeat retains the complete installed object graph, including list identities.
        if (Boolean.TRUE.equals(installed)) return;

        List<Waz.Skill> candidates = new ArrayList<>(target.getSkillList());
        for (int i = 0; i < FAMILY.size(); i++) {
            Waz.Skill candidate = copy(skills.get(i).skill());
            targetSlot(candidate, 1).getSkillInfoObjectList().remove(0);
            if (!validateTarget(candidate, FAMILY.get(i))) throw invalid("候補の自己再入が残っています");
            candidates.set(skills.get(i).index(), candidate);
        }
        // Source/target preflight and all candidate construction complete before the single publication.
        target.setSkillList(candidates);
    }

    private static com.giga.nexas.dto.bhe.waz.Waz sourceMain(FollowupMekaContext context) {
        if (context.getConvertedBundle() == null || context.getConvertedBundle().getRawSourceBundle() == null)
            throw invalid("変換前Sou WAZがありません");
        Map<String, com.giga.nexas.dto.bhe.waz.Waz> sources = context.getConvertedBundle().getRawSourceBundle().getWazByFileName();
        if (sources == null) throw invalid("変換前WAZ表がありません");
        var matches = sources.entrySet().stream().filter(e -> "sou.waz".equalsIgnoreCase(e.getKey())).toList();
        if (matches.size() != 1 || matches.get(0).getValue() == null) throw invalid("変換前sou.wazが一意ではありません");
        return matches.get(0).getValue();
    }

    private static com.giga.nexas.dto.bhe.waz.Waz.Skill findSource(com.giga.nexas.dto.bhe.waz.Waz source, Signature signature) {
        if (source.getSkillList() == null) throw invalid("変換前技能表がありません");
        var matches = source.getSkillList().stream().filter(s -> s != null
                && signature.japanese().equals(s.getSkillNameJapanese()) && signature.english().equals(s.getSkillNameEnglish())).toList();
        if (matches.size() != 1) throw invalid("変換前技能双名が一意ではありません: " + signature.english());
        return matches.get(0);
    }

    private static LocatedSkill findTarget(Waz target, Signature signature) {
        if (target.getSkillList() == null) throw invalid("最終技能表がありません");
        LocatedSkill match = null;
        for (int i = 0; i < target.getSkillList().size(); i++) {
            Waz.Skill value = target.getSkillList().get(i);
            if (value == null || !signature.japanese().equals(value.getSkillNameJapanese()) || !signature.english().equals(value.getSkillNameEnglish())) continue;
            if (match != null) throw invalid("最終技能双名が重複しています: " + signature.english());
            match = new LocatedSkill(i, value);
        }
        if (match == null) throw invalid("最終技能双名がありません: " + signature.english());
        return match;
    }

    private static void validateSource(com.giga.nexas.dto.bhe.waz.Waz.Skill skill, Signature signature) {
        if (!Integer.valueOf(3).equals(skill.getPhaseQuantity()) || skill.getPhasesInfo() == null || skill.getPhasesInfo().size() != 3
                || skill.getSkillSuffixList() == null || skill.getSkillSuffixList().size() != 3)
            throw invalid("変換前phase/suffix数が変更されています");
        for (int i = 0; i < 3; i++) {
            var suffix = skill.getSkillSuffixList().get(i);
            if (suffix == null || !Integer.valueOf(i).equals(suffix.getInt1()) || !Integer.valueOf(0).equals(suffix.getInt2()))
                throw invalid("変換前suffixが変更されています");
        }
        var prep = sourceChanges(skill, 0, 3);
        expectSource(prep.get(0), signature.release(), List.of(2, 4, 3), NONE, NONE, SELF, NONE);
        expectSource(prep.get(1), signature.entry(), MULTILOCK, List.of(0), SELF, SELF, NONE);
        expectSource(prep.get(2), signature.prepEnd(), ALWAYS, NONE, NONE, List.of(0, 2), NONE);
        var attack = sourceChanges(skill, 1, 2);
        expectSource(attack.get(0), signature.reentry(), MULTILOCK, List.of(0), SELF, List.of(0, 1), List.of(1));
        expectSource(attack.get(1), 46, ALWAYS, NONE, NONE, SELF, NONE);
        expectSource(sourceChanges(skill, 2, 1).get(0), 49, ALWAYS, NONE, NONE, SELF, NONE);
    }

    private static boolean validateTarget(Waz.Skill skill, Signature signature) {
        if (!Integer.valueOf(3).equals(skill.getPhaseQuantity()) || skill.getPhasesInfo() == null || skill.getPhasesInfo().size() != 3
                || skill.getSkillSuffixList() == null || skill.getSkillSuffixList().size() != 3)
            throw invalid("最終phase/suffix数が変更されています");
        for (int i = 0; i < 3; i++) {
            var suffix = skill.getSkillSuffixList().get(i);
            if (suffix == null || !Integer.valueOf(i).equals(suffix.getInt1()) || !Integer.valueOf(0).equals(suffix.getInt2()))
                throw invalid("最終suffixが変更されています");
        }
        var prep = targetChanges(skill, 0);
        if (prep.size() != 3) throw invalid("P0 Change数が変更されています");
        expectTarget(prep.get(0), signature.release(), List.of(2, 4, 3), NONE, NONE, SELF, NONE);
        expectTarget(prep.get(1), signature.entry(), ALWAYS, NONE, SELF, SELF, NONE);
        expectTarget(prep.get(2), signature.prepEnd(), ALWAYS, NONE, NONE, List.of(0, 2), NONE);
        var attack = targetChanges(skill, 1);
        if (attack.size() != 1 && attack.size() != 2) throw invalid("P1 Change数が変更されています");
        boolean installed = attack.size() == 1;
        if (!installed) expectTarget(attack.get(0), signature.reentry(), ALWAYS, NONE, SELF, List.of(0, 1), List.of(1));
        expectTarget(attack.get(installed ? 0 : 1), 46, ALWAYS, NONE, NONE, SELF, NONE);
        var recovery = targetChanges(skill, 2);
        if (recovery.size() != 1) throw invalid("P2 Change数が変更されています");
        expectTarget(recovery.get(0), 49, ALWAYS, NONE, NONE, SELF, NONE);
        return installed;
    }

    private static List<com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange> sourceChanges(
            com.giga.nexas.dto.bhe.waz.Waz.Skill skill, int phase, int count) {
        var data = skill.getPhasesInfo().get(phase);
        if (data == null || data.getSkillUnitCollection() == null) throw invalid("変換前slot表がありません");
        com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit changeSlot = null;
        Set<Integer> slots = new HashSet<>();
        for (var unit : data.getSkillUnitCollection()) {
            if (unit == null || unit.getUnitQuantity() == null || !slots.add(unit.getUnitQuantity())
                    || unit.getSkillInfoObjectList() == null || unit.getSkillInfoUnknownList() == null)
                throw invalid("変換前slot重複またはnull一覧があります");
            if (Integer.valueOf(82).equals(unit.getUnitQuantity())) changeSlot = unit;
        }
        if (changeSlot == null || changeSlot.getSkillInfoObjectList().size() != count || !changeSlot.getSkillInfoUnknownList().isEmpty())
            throw invalid("変換前Change/FF数が変更されています");
        List<com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange> events = new ArrayList<>();
        for (var event : changeSlot.getSkillInfoObjectList()) {
            if (!(event instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange change)
                    || !Integer.valueOf(40).equals(change.getTypeId()) || !Integer.valueOf(1).equals(change.getFlag())
                    || !Integer.valueOf(0).equals(change.getInt1()) || change.getBheInfoCollectionList1() == null
                    || change.getBheInfoCollectionList1().size() != 1 || change.getBheInfoCollectionList2() == null
                    || change.getBheInfoCollectionList2().size() != 1) throw invalid("変換前Change完全署名が変更されています");
            events.add(change);
        }
        return events;
    }

    private static SkillUnit targetSlot(Waz.Skill skill, int phase) {
        var data = skill.getPhasesInfo().get(phase);
        if (data == null || data.getSkillUnitCollection() == null) throw invalid("最終slot表がありません");
        SkillUnit changeSlot = null;
        Set<Integer> slots = new HashSet<>();
        for (var unit : data.getSkillUnitCollection()) {
            if (unit == null || unit.getUnitQuantity() == null || !slots.add(unit.getUnitQuantity())
                    || unit.getSkillInfoObjectList() == null || unit.getSkillInfoUnknownList() == null)
                throw invalid("最終slot重複またはnull一覧があります");
            if (Integer.valueOf(71).equals(unit.getUnitQuantity())) changeSlot = unit;
        }
        if (changeSlot == null || !changeSlot.getSkillInfoUnknownList().isEmpty()) throw invalid("最終Change slot欠落またはFF追加があります");
        return changeSlot;
    }

    private static List<CEventChange> targetChanges(Waz.Skill skill, int phase) {
        List<CEventChange> events = new ArrayList<>();
        for (var event : targetSlot(skill, phase).getSkillInfoObjectList()) {
            if (!(event instanceof CEventChange change) || !Integer.valueOf(34).equals(change.getTypeId())
                    || !Integer.valueOf(1).equals(change.getFlag()) || !Integer.valueOf(0).equals(change.getInt1())
                    || change.getBsdxInfoCollectionList1() == null || change.getBsdxInfoCollectionList1().size() != 1
                    || change.getBsdxInfoCollectionList2() == null || change.getBsdxInfoCollectionList2().size() != 1)
                throw invalid("最終Change完全署名が変更されています");
            events.add(change);
        }
        return events;
    }

    private static void expectSource(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange event,
                                     int frame, List<Integer> types, List<Integer> params, List<Integer> objects,
                                     List<Integer> targetTypes, List<Integer> targetParams) {
        if (!Integer.valueOf(frame).equals(event.getStartFrame()) || !Integer.valueOf(frame).equals(event.getEndFrame()))
            throw invalid("変換前Change時刻が変更されています");
        collection(event.getBheInfoCollectionList1().get(0), 0, types, params, objects);
        collection(event.getBheInfoCollectionList2().get(0), 16, targetTypes, targetParams, NONE);
    }

    private static void expectTarget(CEventChange event, int frame, List<Integer> types, List<Integer> params,
                                     List<Integer> objects, List<Integer> targetTypes, List<Integer> targetParams) {
        if (!Integer.valueOf(frame).equals(event.getStartFrame()) || !Integer.valueOf(frame).equals(event.getEndFrame()))
            throw invalid("最終Change時刻が変更されています");
        collection(event.getBsdxInfoCollectionList1().get(0), 0, types, params, objects);
        collection(event.getBsdxInfoCollectionList2().get(0), 16, targetTypes, targetParams, NONE);
    }

    private static void collection(BheInfoCollection value, int root, List<Integer> types, List<Integer> params, List<Integer> objects) {
        if (value == null || !Integer.valueOf(root).equals(value.getInt1()) || !types.equals(value.getTypeList())
                || !params.equals(value.getParamList()) || !objects.equals(value.getIntList3())
                || !NONE.equals(value.getIntList4()) || !Integer.valueOf(0).equals(value.getInt2()))
            throw invalid("変換前条件/宛先の完全署名が変更されています");
    }

    private static void collection(BsdxInfoCollection value, int root, List<Integer> types, List<Integer> params, List<Integer> objects) {
        if (value == null || !Integer.valueOf(root).equals(value.getInt1()) || !types.equals(value.getTypeList())
                || !params.equals(value.getParamList()) || !objects.equals(value.getIntList3())
                || !NONE.equals(value.getIntList4()) || !Integer.valueOf(0).equals(value.getInt2()))
            throw invalid("最終条件/宛先の完全署名が変更されています");
    }

    private static Waz.Skill copy(Waz.Skill source) {
        try { return JSON.readValue(JSON.writeValueAsBytes(source), Waz.Skill.class); }
        catch (IOException error) { throw new IllegalStateException("Sou呪装刀の独立コピーに失敗", error); }
    }

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("Sou呪装刀single-target拒否: " + reason);
    }

    private record Signature(String japanese, String english, int release, int entry, int prepEnd, int reentry) { }
    private record LocatedSkill(int index, Waz.Skill skill) { }
}
