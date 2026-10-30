package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;

import java.io.IOException;
import java.util.*;

/** Sou人間砲弾だけのlevel/release二状態を、同宿主の私有制御弾へ保存する。 */
final class SouHumanCannonCounterFixer {
    private static final String PUBLIC_JP = "★蒼：人間砲弾";
    private static final String PUBLIC_EN = "HEATBOMB06";
    private static final String PRIVATE_JP = "★蒼：人間砲弾・制御";
    private static final String PRIVATE_EN = "DX_SOU_HUMAN_CANNON_CONTROL";
    private static final int RELEASE_OFFSET = 16;
    private static final List<Integer> NONE = List.of();
    private static final List<Integer> SELF = List.of(0, 0);
    private static final List<Integer> LAST = List.of(14, 0);
    private static final List<Integer> PARENT_MEKA = List.of(3, 0);
    private static final List<Integer> LEVEL_COMPARE = List.of(2, 0, 0, 0);
    private static final List<Integer> RELEASE_COMPARE = List.of(2, 0, 0, 5);
    private static final List<List<Integer>> CHILD_SUFFIX = List.of(
            List.of(0, 0), List.of(1, 0), List.of(2, 0), List.of(3, 0),
            List.of(4, 0), List.of(5, 0), List.of(6, 0), List.of(7, 0));
    private static final List<List<Integer>> MAIN_SUFFIX = List.of(
            List.of(0, 0), List.of(1, 0), List.of(2, 0), List.of(3, 0),
            List.of(4, 0), List.of(5, 0), List.of(6, 0), List.of(7, 0),
            List.of(8, 0), List.of(9, 0), List.of(10, 0), List.of(11, 0),
            List.of(12, 0), List.of(13, 0));
    private static final List<MainSignature> FAMILY = List.of(
            new MainSignature("人間砲弾Lv1", "N_HOUDAN01", 11, 41, 6, 6, 42, 134),
            new MainSignature("人間砲弾Lv2", "N_HOUDAN02", 7, 33, 7, 4, 34, 126),
            new MainSignature("人間砲弾Lv3", "N_HOUDAN03", 3, 21, 10, 2, 22, 114));
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SouHumanCannonCounterFixer() { }

    static void fix(FollowupMekaContext context) {
        // Existing MEK-only probes still exercise only the older Sou customization.
        if (context == null || context.getReboundWaz() == null) return;
        Location location = resolve(context);
        Waz main = context.getReboundWaz();
        List<LocatedSkill> callers = new ArrayList<>();
        for (MainSignature signature : FAMILY)
            callers.add(find(main.getSkillList(), signature.japanese(), signature.english(), 0, main.getSkillList().size()));

        LocatedSkill publicChild = find(location.host().getSkillList(), PUBLIC_JP, PUBLIC_EN,
                location.sourceBase(), location.sourceEnd());
        validateController(publicChild.skill(), false);
        LocatedSkill installed = findPrivate(location.host());
        boolean repeated = installed != null;
        int privateIndex = repeated ? installed.index() : location.host().getSkillList().size();
        if (repeated && privateIndex < location.sourceEnd())
            throw invalid("私有制御弾が元の公共追加区間内に存在します");
        for (int i = 0; i < FAMILY.size(); i++)
            validateCaller(callers.get(i).skill(), FAMILY.get(i), location,
                    repeated ? privateIndex : publicChild.index(), repeated);
        validateMekCaller(context, callers.get(2));

        // Every edit below is on an independent candidate. The public prototype is never edited.
        Waz.Skill privateChild = buildPrivateController(publicChild.skill());
        validateController(privateChild, true);
        if (repeated) {
            if (!payload(privateChild).equals(payload(installed.skill())))
                throw invalid("既存の私有制御弾が元供体と一致しません");
            // Validated repeats retain list, Skill, GRP entry, and inherited view identities.
            return;
        }
        List<Waz.Skill> patchedCallers = new ArrayList<>();
        for (int i = 0; i < callers.size(); i++) {
            Waz.Skill copy = copy(callers.get(i).skill(), Waz.Skill.class);
            CEventChange wait = changes(copy, 0, 1, 1).get(0);
            wait.getBsdxInfoCollectionList1().get(0).setParamList(new ArrayList<>(List.of(RELEASE_OFFSET - 1)));
            List<CEventChange> flight = changes(copy, 1, 5, 0);
            for (int level = 0; level < 4; level++)
                flight.get(level).getBsdxInfoCollectionList1().get(0)
                        .setParamList(new ArrayList<>(List.of(RELEASE_OFFSET + level)));
            controllerReference(copy).setWazSequenceNo(privateIndex);
            validateCaller(copy, FAMILY.get(i), location, privateIndex, true);
            patchedCallers.add(copy);
        }
        List<Waz.Skill> hostSkills = new ArrayList<>(location.host().getSkillList());
        hostSkills.add(privateChild);
        List<Waz.Skill> mainSkills = new ArrayList<>(main.getSkillList());
        for (int i = 0; i < callers.size(); i++) mainSkills.set(callers.get(i).index(), patchedCallers.get(i));
        int count = Math.addExact(location.host().getSkillList().size(), 1);
        if (hostSkills.size() != count || mainSkills.size() != main.getSkillList().size())
            throw invalid("公開前の主WAZ/公共WAZ件数が一致しません");

        // No reads, callbacks, or validation remain after this point.
        location.host().setSkillList(hostSkills);
        main.setSkillList(mainSkills);
        location.group().setParam(count);
    }

    private static Location resolve(FollowupMekaContext context) {
        if (context.getBaseline() == null || context.getConvertedBundle() == null
                || context.getConvertedBundle().getPreparedBaselineBundle() != context.getBaseline()
                || context.getBaseline().getWazByFileName() == null
                || context.getBaseline().getWazaGroupGrp() == null
                || context.getReboundMek() == null || context.getReboundMek().getMekBasicInfo() == null
                || context.getReboundWaz().getSkillList() == null)
            throw invalid("Souの最終基線/主WAZ/MEK共有ビューが不足または不一致です");
        BheCommonProjectileAppendPlan plan = context.getConvertedBundle().getCommonProjectileAppendPlan();
        if (plan == null) throw invalid("継承した公共append planがありません");
        int sourceTama = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama05.waz");
        int sourceEffect = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("effect.waz");
        Integer targetGroup = plan.getSourceWazIndexToTargetIndex().get(sourceTama);
        String name = plan.getSourceWazIndexToTargetFileName().get(sourceTama);
        Integer base = plan.getSourceWazIndexToTargetSkillBase().get(sourceTama);
        Integer sourceCount = plan.getSourceWazIndexToTargetSkillCount().get(sourceTama);
        Integer effectGroup = plan.getSourceWazIndexToTargetIndex().get(sourceEffect);
        Integer effectBase = plan.getSourceWazIndexToTargetSkillBase().get(sourceEffect);
        if (sourceTama < 0 || sourceEffect < 0 || targetGroup == null || name == null || name.isBlank()
                || base == null || base < 0 || sourceCount == null || sourceCount <= 0
                || effectGroup == null || effectGroup < 0 || effectBase == null || effectBase < 0
                || plan.getCommonProjectileWazFiles().stream().noneMatch(name::equalsIgnoreCase))
            throw invalid("Tama05宿主/出力/源区間またはEffectの計画が不正です");
        var hosts = context.getBaseline().getWazByFileName().entrySet().stream()
                .filter(entry -> name.equalsIgnoreCase(entry.getKey())).toList();
        if (hosts.size() != 1 || hosts.get(0).getValue() == null || hosts.get(0).getValue().getSkillList() == null)
            throw invalid("Tama05の最終宿主が一意ではありません");
        Waz host = hosts.get(0).getValue();
        long sourceEnd = (long) base + sourceCount;
        var groups = context.getBaseline().getWazaGroupGrp().getWazaList();
        if (sourceEnd > host.getSkillList().size() || targetGroup < 0 || targetGroup >= groups.size()
                || groups.get(targetGroup) == null || !Objects.equals(groups.get(targetGroup).getParam(), host.getSkillList().size()))
            throw invalid("公共宿主の源区間またはWazaGroup件数が一致しません");
        WazaGroupGrp.WazaGroupEntry group = groups.get(targetGroup);
        String stem = name.toLowerCase(Locale.ROOT).replace(".waz", "");
        if (!stem.equalsIgnoreCase(group.getWazaCodeName()) && !stem.equalsIgnoreCase(group.getWazaDisplayName()))
            throw invalid("公共宿主のGRP身份が計画と一致しません");
        Integer mainGroup = context.getReboundMek().getMekBasicInfo().getWazFileSequence();
        if (mainGroup == null || mainGroup < 0 || mainGroup >= groups.size()
                || context.getGrpAppendPlan() == null || context.getGrpAppendPlan().getWazaGroupIndex() != mainGroup
                || groups.get(mainGroup) == null
                || !Objects.equals(groups.get(mainGroup).getParam(), context.getReboundWaz().getSkillList().size()))
            throw invalid("Sou主WAZの最終groupまたは件数が一致しません");
        return new Location(host, group, targetGroup, base, (int) sourceEnd, effectGroup, effectBase);
    }

    private static void validateMekCaller(FollowupMekaContext context, LocatedSkill level3) {
        var map = context.getReboundMek().getMekWeaponInfoMap();
        if (map == null) throw invalid("Sou武装表がありません");
        var weapons = map.values().stream().filter(Objects::nonNull)
                .filter(w -> "人間砲弾".equals(w.getWeaponName()) && "N_HOUDAN".equals(w.getWeaponSequence())).toList();
        if (weapons.size() != 1 || !Objects.equals(weapons.get(0).getWazSequence(), level3.index())
                || !Integer.valueOf(0).equals(weapons.get(0).getWeaponCategory())
                || !Integer.valueOf(0).equals(weapons.get(0).getWeaponType())
                || !Integer.valueOf(0).equals(weapons.get(0).getWeaponIdentifier()))
            throw invalid("Sou人間砲弾の既存MEK入口が変更されています");
    }

    private static LocatedSkill find(List<Waz.Skill> skills, String japanese, String english, int start, int end) {
        LocatedSkill match = null;
        for (int i = start; i < end; i++) {
            Waz.Skill value = skills.get(i);
            if (value != null && japanese.equals(value.getSkillNameJapanese()) && english.equals(value.getSkillNameEnglish())) {
                if (match != null) throw invalid("技能双名が重複しています: " + english);
                match = new LocatedSkill(i, value);
            }
        }
        if (match == null) throw invalid("技能双名が存在しません: " + japanese + "/" + english);
        return match;
    }

    private static LocatedSkill findPrivate(Waz host) {
        LocatedSkill match = null;
        for (int i = 0; i < host.getSkillList().size(); i++) {
            Waz.Skill skill = host.getSkillList().get(i);
            if (skill == null) continue;
            boolean jp = PRIVATE_JP.equals(skill.getSkillNameJapanese()), en = PRIVATE_EN.equals(skill.getSkillNameEnglish());
            if (!jp && !en) continue;
            if (!jp || !en || match != null) throw invalid("私有制御弾の双名が占有/重複しています");
            match = new LocatedSkill(i, skill);
        }
        return match;
    }

    private static void validateCaller(Waz.Skill skill, MainSignature signature, Location location,
                                       int controller, boolean encoded) {
        phases(skill, 14, MAIN_SUFFIX);
        List<CEventChange> wait = changes(skill, 0, 1, 1);
        expect(wait.get(0), signature.waitStart(), signature.waitEnd(), RELEASE_COMPARE,
                List.of(encoded ? RELEASE_OFFSET - 1 : 0), LAST, List.of(0, 0), NONE);
        loop(slot(skill, 0, 71).getSkillInfoUnknownList().get(0), signature.waitStart(), signature.waitEnd());
        SkillUnit cancel = slot(skill, 0, 47);
        only(cancel, 1, 0);
        val(cancel.getSkillInfoObjectList().get(0), signature.cancelStart(), signature.fire(), List.of(0, 3, 0, 3));
        SkillUnit lift = slot(skill, 0, 19);
        only(lift, 1, 0);
        random(lift.getSkillInfoObjectList().get(0), 3, signature.liftStart(), signature.fire(),
                List.of(0, signature.lift(), 0, 0, 0, 0));
        SkillUnit gravity = slot(skill, 0, 21);
        only(gravity, 1, 1);
        random(gravity.getSkillInfoObjectList().get(0), 2, signature.fire(), signature.waitEnd(), List.of(0, 0, 0, 0, 0, 0));
        loop(gravity.getSkillInfoUnknownList().get(0), signature.fire(), signature.waitEnd());

        List<CEventChange> flight = changes(skill, 1, 5, 0);
        for (int level = 0; level < 4; level++)
            expect(flight.get(level), 0, 0, LEVEL_COMPARE, List.of((encoded ? RELEASE_OFFSET : 0) + level),
                    LAST, List.of(0, 1), List.of(6 - level));
        expect(flight.get(4), 0, 0, List.of(0), NONE, NONE, List.of(0, 1), List.of(2));
        expect(changes(skill, 12, 1, 0).get(0), 25, 25, List.of(0), NONE, NONE, List.of(0, 0), NONE);
        expect(changes(skill, 13, 1, 0).get(0), 38, 38, List.of(0), NONE, NONE, List.of(0, 2), NONE);

        SkillUnit effect = slot(skill, 0, 48);
        only(effect, 3, 0);
        int[][] references = {{location.targetGroup(), controller},
                {location.effectGroup(), location.effectBase() + 234},
                {location.effectGroup(), location.effectBase() + 47}};
        for (int i = 0; i < 3; i++) {
            SkillInfoObject object = effect.getSkillInfoObjectList().get(i);
            if (!(object instanceof CEventEffect event) || !Integer.valueOf(19).equals(object.getTypeId())
                    || !frames(object, signature.fire(), signature.fire())
                    || !Arrays.asList(i == 0 ? 1 : 0, 0, 0, 0, -1, -1, -1, 0, 1, 0, 0)
                    .equals(Arrays.asList(event.getInt1(), event.getInt2(), event.getInt3(), event.getInt4(),
                            event.getInt5(), event.getInt6(), event.getInt7(), event.getInt8(), event.getInt9(),
                            event.getInt10(), event.getInt11()))) throw invalid("人間砲弾の21/33/41f発射構造が変更されています");
            CEventWazaSelect ref = effectReference(event);
            if (!Objects.equals(references[i][0], ref.getWazFileNo()) || !Objects.equals(references[i][1], ref.getWazSequenceNo()))
                throw invalid("人間砲弾の制御弾/装飾参照が配対されていません");
        }
    }

    private static Waz.Skill buildPrivateController(Waz.Skill prototype) {
        Waz.Skill child = copy(prototype, Waz.Skill.class);
        child.setSkillNameJapanese(PRIVATE_JP);
        child.setSkillNameEnglish(PRIVATE_EN);
        SkillUnit initial = optionalSlot(child, 0, 35);
        if (initial == null) {
            initial = new SkillUnit(35, "Sou私有level初期化");
            child.getPhasesInfo().get(0).getSkillUnitCollection().add(initial);
            child.getPhasesInfo().get(0).getSkillUnitCollection().sort(Comparator.comparing(SkillUnit::getUnitQuantity));
        }
        initial.setSkillInfoObjectList(new ArrayList<>(List.of(point(0, 0))));
        // Replace the sole zero producer. Appending +16 behind it would be hidden by first-active.
        slot(child, 7, 35).setSkillInfoObjectList(new ArrayList<>(List.of(point(1, RELEASE_OFFSET))));
        return child;
    }

    private static CEventVal point(int operation, int amount) {
        CEventVal value = new CEventVal(0);
        value.setSlotNum(35); value.setStartFrame(0); value.setEndFrame(0);
        value.setInt1(0); value.setInt2(amount); value.setInt3(operation); value.setInt4(amount);
        return value;
    }

    private static void validateController(Waz.Skill child, boolean encoded) {
        phases(child, 8, CHILD_SUFFIX);
        SkillUnit initial = optionalSlot(child, 0, 35);
        if (encoded) {
            if (initial == null) throw invalid("私有levelの初期化がありません");
            only(initial, 1, 0);
            val(initial.getSkillInfoObjectList().get(0), 0, 0, List.of(0, 0, 0, 0));
        } else if (initial != null) only(initial, 0, 0);
        for (int phase = 1; phase <= 4; phase++) {
            SkillUnit count = slot(child, phase, 35);
            only(count, 1, 0);
            val(count.getSkillInfoObjectList().get(0), 0, 0, List.of(0, 0, 1, 1));
            validateSimpleInputPhase(child, phase);
        }
        validateSimpleInputPhase(child, 0);
        for (int phase : List.of(5, 6)) {
            SkillUnit holdingCounter = optionalSlot(child, phase, 35);
            if (holdingCounter != null) only(holdingCounter, 0, 0);
        }
        List<CEventChange> hold = changes(child, 5, 9, 0);
        commonHold(hold, 147, false);
        for (int level = 0; level < 4; level++)
            expect(hold.get(4 + level), 11, 11, LEVEL_COMPARE, List.of(level), SELF, List.of(0, 1), List.of(level + 1));
        expect(hold.get(8), 147, 147, List.of(0), NONE, NONE, List.of(0, 0), NONE);
        List<CEventChange> direction = changes(child, 6, 5, 0);
        commonHold(direction, 150, true);
        expect(direction.get(4), 150, 150, List.of(0), NONE, NONE, List.of(0, 0), NONE);
        SkillUnit release = slot(child, 7, 35);
        only(release, 1, 0);
        val(release.getSkillInfoObjectList().get(0), 0, 0,
                encoded ? List.of(0, RELEASE_OFFSET, 1, RELEASE_OFFSET) : List.of(0, 0, 0, 0));
        expect(changes(child, 7, 1, 0).get(0), 65, 65, List.of(0), NONE, NONE, List.of(0, 0), NONE);
    }

    private static void validateSimpleInputPhase(Waz.Skill child, int phase) {
        List<CEventChange> change = changes(child, phase, 3, 0);
        expect(change.get(0), 0, 0, List.of(2, 1, 0), NONE, PARENT_MEKA, List.of(0, 2), NONE);
        expect(change.get(1), 0, 0, List.of(2, 4, 3), NONE, NONE, List.of(0, 1), List.of(7));
        expect(change.get(2), 0, 0, List.of(0), NONE, NONE, List.of(0, 1), List.of(5));
    }

    private static void commonHold(List<CEventChange> changes, int end, boolean directionHeld) {
        expect(changes.get(0), 0, end, List.of(2, 1, 0), NONE, PARENT_MEKA, List.of(0, 2), NONE);
        expect(changes.get(1), 0, end, List.of(2, 0, 1, 3), List.of(300), SELF, List.of(0, 1), List.of(7));
        expect(changes.get(2), 0, end, List.of(2, 4, directionHeld ? 5 : 7), NONE, NONE,
                directionHeld ? List.of(0, 1) : List.of(0, 0), directionHeld ? List.of(5) : NONE);
        expect(changes.get(3), 0, end, List.of(2, 4, 3), NONE, NONE, List.of(0, 1), List.of(7));
    }

    private static void phases(Waz.Skill skill, int count, List<List<Integer>> suffix) {
        if (skill == null || !Integer.valueOf(count).equals(skill.getPhaseQuantity()) || skill.getPhasesInfo() == null
                || skill.getPhasesInfo().size() != count || skill.getSkillSuffixList() == null
                || skill.getSkillSuffixList().size() != suffix.size()) throw invalid("phase/suffix件数が変更されています");
        for (int i = 0; i < suffix.size(); i++) {
            var value = skill.getSkillSuffixList().get(i);
            if (value == null || !Objects.equals(suffix.get(i).get(0), value.getInt1())
                    || !Objects.equals(suffix.get(i).get(1), value.getInt2()))
                throw invalid("審査済みsuffix入口/skip標識が変更されています");
        }
    }

    private static List<CEventChange> changes(Waz.Skill skill, int phase, int count, int loops) {
        SkillUnit slot = slot(skill, phase, 71);
        only(slot, count, loops);
        List<CEventChange> values = new ArrayList<>();
        for (SkillInfoObject value : slot.getSkillInfoObjectList()) {
            if (!(value instanceof CEventChange change) || !Integer.valueOf(34).equals(value.getTypeId())
                    || !Integer.valueOf(1).equals(change.getFlag()) || !Integer.valueOf(0).equals(change.getInt1())
                    || change.getBsdxInfoCollectionList1() == null || change.getBsdxInfoCollectionList1().size() != 1
                    || change.getBsdxInfoCollectionList2() == null || change.getBsdxInfoCollectionList2().size() != 1)
                throw invalid("Changeの型/条件数/operationが変更されています");
            values.add(change);
        }
        return values;
    }

    private static void expect(CEventChange value, int start, int end, List<Integer> types, List<Integer> params,
                               List<Integer> objects, List<Integer> targetTypes, List<Integer> targetParams) {
        if (!frames(value, start, end)) throw invalid("Changeの時刻が変更されています");
        collection(value.getBsdxInfoCollectionList1().get(0), 0, types, params, objects);
        collection(value.getBsdxInfoCollectionList2().get(0), 16, targetTypes, targetParams, NONE);
    }

    private static void collection(BsdxInfoCollection value, int root, List<Integer> types,
                                   List<Integer> params, List<Integer> objects) {
        if (value == null || !Integer.valueOf(root).equals(value.getInt1()) || !types.equals(value.getTypeList())
                || !params.equals(value.getParamList()) || !objects.equals(value.getIntList3())
                || !NONE.equals(value.getIntList4()) || !Integer.valueOf(0).equals(value.getInt2()))
            throw invalid("比較/対象/宛先の完全署名が変更されています");
    }

    private static CEventWazaSelect controllerReference(Waz.Skill caller) {
        SkillInfoObject first = slot(caller, 0, 48).getSkillInfoObjectList().get(0);
        if (!(first instanceof CEventEffect effect)) throw invalid("最初の発射がEffectではありません");
        return effectReference(effect);
    }

    private static CEventWazaSelect effectReference(CEventEffect effect) {
        if (effect.getCeventEffectUnitList() == null) throw invalid("Effect内部参照がありません");
        var refs = effect.getCeventEffectUnitList().stream().filter(Objects::nonNull)
                .filter(unit -> Integer.valueOf(3).equals(unit.getUnitSlotNum())).toList();
        if (refs.size() != 1 || !Integer.valueOf(1).equals(refs.get(0).getBuffer())
                || !(refs.get(0).getData() instanceof CEventWazaSelect ref)
                || !Integer.valueOf(4).equals(ref.getTypeId()) || !frames(ref, 0, 0))
            throw invalid("EffectのWAZ選択署名が変更されています");
        return ref;
    }

    private static SkillUnit optionalSlot(Waz.Skill skill, int phase, int number) {
        var data = skill.getPhasesInfo().get(phase);
        if (data == null || data.getSkillUnitCollection() == null) throw invalid("phaseのslot表がありません");
        var values = data.getSkillUnitCollection().stream().filter(Objects::nonNull)
                .filter(unit -> Integer.valueOf(number).equals(unit.getUnitQuantity())).toList();
        if (values.size() > 1) throw invalid("slotが重複しています: " + number);
        return values.isEmpty() ? null : values.get(0);
    }

    private static SkillUnit slot(Waz.Skill skill, int phase, int number) {
        SkillUnit value = optionalSlot(skill, phase, number);
        if (value == null) throw invalid("必要なslotがありません: " + number);
        return value;
    }

    private static void only(SkillUnit slot, int events, int loops) {
        if (slot.getSkillInfoObjectList() == null || slot.getSkillInfoObjectList().size() != events
                || slot.getSkillInfoUnknownList() == null || slot.getSkillInfoUnknownList().size() != loops)
            throw invalid("slotのイベント/FF件数が変更されています: " + slot.getUnitQuantity());
    }

    private static void val(SkillInfoObject value, int start, int end, List<Integer> four) {
        if (!(value instanceof CEventVal val) || !Integer.valueOf(0).equals(value.getTypeId()) || !frames(value, start, end)
                || !four.equals(Arrays.asList(val.getInt1(), val.getInt2(), val.getInt3(), val.getInt4())))
            throw invalid("単一counter/取消の四整数署名が変更されています");
    }

    private static void random(SkillInfoObject value, int type, int start, int end, List<Integer> six) {
        if (!(value instanceof CEventValRandom val) || !Integer.valueOf(type).equals(value.getTypeId()) || !frames(value, start, end)
                || !six.equals(Arrays.asList(val.getInt1(), val.getInt2(), val.getInt3(), val.getInt4(), val.getInt5(), val.getInt6())))
            throw invalid("保持用物理の署名が変更されています");
    }

    private static void loop(SkillInfoUnknown value, int start, int end) {
        if (value == null || !Integer.valueOf(255).equals(value.getTypeId()) || !frames(value, start, end)
                || !List.of(-1, 1, -2).equals(Arrays.asList(value.getInt1(), value.getInt2(), value.getInt3())))
            throw invalid("元の無限FF署名が変更されています");
    }

    private static boolean frames(SkillInfoObject value, int start, int end) {
        return value != null && Integer.valueOf(start).equals(value.getStartFrame()) && Integer.valueOf(end).equals(value.getEndFrame());
    }

    private static <T> T copy(T source, Class<T> type) {
        try {
            return JSON.readValue(JSON.writeValueAsBytes(source), type);
        } catch (IOException error) {
            throw new IllegalStateException("Sou人間砲弾の独立コピーに失敗", error);
        }
    }

    private static JsonNode payload(Object value) {
        JsonNode tree = JSON.valueToTree(value);
        removeMetadata(tree);
        return tree;
    }

    private static void removeMetadata(JsonNode node) {
        if (node instanceof ObjectNode object) object.remove(List.of("offset", "length", "slotNum", "unitDescription", "description"));
        node.forEach(SouHumanCannonCounterFixer::removeMetadata);
    }

    private static IllegalStateException invalid(String message) {
        return new IllegalStateException("Sou人間砲弾counter拒否: " + message);
    }

    private record MainSignature(String japanese, String english, int liftStart, int fire, int lift,
                                 int cancelStart, int waitStart, int waitEnd) { }
    private record LocatedSkill(int index, Waz.Skill skill) { }
    private record Location(Waz host, WazaGroupGrp.WazaGroupEntry group, int targetGroup,
                            int sourceBase, int sourceEnd, int effectGroup, int effectBase) { }
}
