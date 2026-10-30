package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;
import com.giga.nexas.io.BinaryWriter;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.cross.CrossRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Sou雪月花の多段MULTILOCK発射だけを、有限の単一対象準備・解放へ限定する。 */
final class SouSnowMoonSingleTargetFixer {
    private static final String PRIVATE_JP = "★蒼：雪月花・単一対象";
    private static final String PRIVATE_EN = "DX_SOU_SNOW_MOON_SINGLE_TARGET";
    private static final List<Integer> PREPARE_POINTS = List.of(34, 28, 22);
    // SHA256 covers complete original flag1/name/83-slot/phase/suffix records from the real BHE binaries.
    private static final List<SourceSignature> MAIN = List.of(
            new SourceSignature("雪月花Lv1", "SETUGEKKA01", 3, 11660, "a9e15656469f819a34d0f9a01fbe6f45973f871693f22778d63a49525fa821de"),
            new SourceSignature("雪月花Lv2", "SETUGEKKA02", 3, 11608, "401b988cbf59b3cf7cd1f7dd65c302bc8cb398a32ceabb0e675134482db60aeb"),
            new SourceSignature("雪月花Lv3", "SETUGEKKA03", 3, 11560, "f46cc24a0d7682e38158a42517c9485ef2f03bcfe19cbf558de6c4be35d06fdd"));
    private static final List<SourceSignature> PUBLIC = List.of(
            new SourceSignature("★蒼：雪月花凍結", "0338", 8, 9084, "d36019348022c29d7879c5d73a7b9447270d7c413ddf619f8945ca7aaa2be904"),
            new SourceSignature("★蒼：雪月花凍結2", "0340", 4, 13975, "4e22bd0ebc2d942dbffb03962359beb36ca5311f9c72bf18e2b16ffdd50ec8a2"),
            new SourceSignature("★蒼：雪月花フィールド", "0339", 1, 1114, "a4c2d31afb98d0521662ae08d348916f2e7e997e976a363d20395594083d6259"));
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SouSnowMoonSingleTargetFixer() { }

    static void fix(FollowupMekaContext context) {
        if (context == null || context.getReboundWaz() == null) return;
        Location location = resolve(context);
        List<RawSkill> rawMain = new ArrayList<>(), rawPublic = new ArrayList<>();
        for (SourceSignature signature : MAIN) rawMain.add(requireSource(location.rawMain(), signature));
        for (SourceSignature signature : PUBLIC) rawPublic.add(requireSource(location.rawTama(), signature));

        // Reuse the existing conversion/rebinding rules on independent DTOs and a private plan copy.
        List<Waz.Skill> expectedMain = expectedTargets(context, rawMain, true);
        List<Waz.Skill> expectedPublic = expectedTargets(context, rawPublic, false);
        List<LocatedSkill> publicSkills = new ArrayList<>();
        for (int i = 0; i < PUBLIC.size(); i++) {
            LocatedSkill actual = findTarget(location.host(), PUBLIC.get(i), location.sourceBase(), location.sourceEnd());
            if (actual.index() != Math.addExact(location.sourceBase(), rawPublic.get(i).index()))
                throw invalid("公共技能が元source sliceの正しい位置にありません");
            validateTargetShape(actual.skill(), PUBLIC.get(i));
            equalPayload(expectedPublic.get(i), actual.skill(), "公共" + PUBLIC.get(i).english());
            publicSkills.add(actual);
        }
        Waz.Skill prototype = publicSkills.get(1).skill();
        LocatedSkill installed = findPrivate(location);
        int privateIndex = installed == null ? location.host().getSkillList().size() : installed.index();
        Waz.Skill expectedChild = buildChild(prototype);
        if (installed != null) {
            validateTargetShape(installed.skill(), new SourceSignature(PRIVATE_JP, PRIVATE_EN, 4, 0, ""));
            equalPayload(expectedChild, installed.skill(), "既存私有515");
        }

        List<LocatedSkill> mainSkills = new ArrayList<>();
        List<Waz.Skill> patched = new ArrayList<>();
        for (int level = 0; level < MAIN.size(); level++) {
            SourceSignature signature = MAIN.get(level);
            LocatedSkill actual = findTarget(location.main(), signature, 0, location.main().getSkillList().size());
            validateTargetShape(actual.skill(), signature);
            Waz.Skill expectedInstalled = copy(expectedMain.get(level), Waz.Skill.class);
            patchMain(expectedInstalled, level, privateIndex);
            equalPayload(installed == null ? expectedMain.get(level) : expectedInstalled,
                    mainControlGraph(actual.skill()), signature.english());
            Waz.Skill candidate = copy(actual.skill(), Waz.Skill.class);
            if (installed == null) patchMain(candidate, level, privateIndex);
            equalPayload(expectedInstalled, mainControlGraph(candidate), "候補" + signature.english());
            mainSkills.add(actual);
            patched.add(candidate);
        }
        validateMek(context, mainSkills.get(2).index());
        // Later legitimate private appends may follow this child. Verified repeats never replace any objects.
        if (installed != null) return;

        List<Waz.Skill> newMain = new ArrayList<>(location.main().getSkillList());
        for (int i = 0; i < mainSkills.size(); i++) newMain.set(mainSkills.get(i).index(), patched.get(i));
        List<Waz.Skill> newHost = new ArrayList<>(location.host().getSkillList());
        newHost.add(expectedChild);
        int count = Math.addExact(location.host().getSkillList().size(), 1);
        if (newHost.size() != count || privateIndex != count - 1) throw invalid("候補の私有追加位置が一致しません");
        // All checks and independent construction precede these three non-throwing publications.
        location.main().setSkillList(newMain);
        location.host().setSkillList(newHost);
        location.group().setParam(count);
    }

    private static Location resolve(FollowupMekaContext context) {
        if (context.getConvertedBundle() == null || context.getConvertedBundle().getRawSourceBundle() == null
                || context.getBaseline() == null || context.getConvertedBundle().getPreparedBaselineBundle() != context.getBaseline()
                || context.getBaseline().getWazaGroupGrp() == null || context.getBaseline().getWazaGroupGrp().getWazaList() == null
                || context.getBaseline().getWazByFileName() == null || context.getReboundWaz().getSkillList() == null
                || context.getReboundMek() == null || context.getReboundMek().getMekBasicInfo() == null)
            throw invalid("Sou最終main/MEK/基線共有ビューが不足または不一致です");
        TsukuyomiRawSourceBundle raw = context.getConvertedBundle().getRawSourceBundle();
        com.giga.nexas.dto.bhe.waz.Waz rawMain = sourceWaz(raw.getWazByFileName(), "sou.waz");
        com.giga.nexas.dto.bhe.waz.Waz rawTama = sourceWaz(raw.getCommonProjectileWazByFileName(), "tama05.waz");
        if (rawMain.getSkillList() == null || rawTama.getSkillList() == null) throw invalid("元技能表がありません");
        BheCommonProjectileAppendPlan plan = context.getConvertedBundle().getCommonProjectileAppendPlan();
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama05.waz");
        if (plan == null || sourceGroup < 0) throw invalid("継承公共planがありません");
        String hostName = plan.getSourceWazIndexToTargetFileName().get(sourceGroup);
        Integer hostGroup = plan.getSourceWazIndexToTargetIndex().get(sourceGroup);
        Integer sourceBase = plan.getSourceWazIndexToTargetSkillBase().get(sourceGroup);
        Integer sourceCount = plan.getSourceWazIndexToTargetSkillCount().get(sourceGroup);
        if (hostName == null || hostGroup == null || sourceBase == null || sourceCount == null || sourceBase < 0
                || sourceCount != rawTama.getSkillList().size()
                || plan.getCommonProjectileWazFiles().stream().noneMatch(hostName::equalsIgnoreCase))
            throw invalid("Tama05の元source slice/出力planが不正です");
        var hosts = context.getBaseline().getWazByFileName().entrySet().stream().filter(e -> hostName.equalsIgnoreCase(e.getKey())).toList();
        if (hosts.size() != 1 || hosts.get(0).getValue() == null || hosts.get(0).getValue().getSkillList() == null)
            throw invalid("Tama05最終宿主が一意ではありません");
        Waz host = hosts.get(0).getValue();
        int sourceEnd = Math.addExact(sourceBase, sourceCount);
        var groups = context.getBaseline().getWazaGroupGrp().getWazaList();
        if (hostGroup < 0 || hostGroup >= groups.size() || groups.get(hostGroup) == null || sourceEnd > host.getSkillList().size()
                || !Objects.equals(groups.get(hostGroup).getParam(), host.getSkillList().size()))
            throw invalid("Tama05宿主source sliceまたはGRP件数が不一致です");
        WazaGroupGrp.WazaGroupEntry group = groups.get(hostGroup);
        String stem = hostName.replaceAll("(?i)\\.waz$", "");
        if (!stem.equalsIgnoreCase(group.getWazaCodeName()) && !stem.equalsIgnoreCase(group.getWazaDisplayName()))
            throw invalid("Tama05宿主のGRP身份が不一致です");
        Integer mainGroup = context.getReboundMek().getMekBasicInfo().getWazFileSequence();
        if (mainGroup == null || mainGroup < 0 || mainGroup >= groups.size() || groups.get(mainGroup) == null
                || context.getGrpAppendPlan() == null || context.getGrpAppendPlan().getWazaGroupIndex() != mainGroup
                || !Objects.equals(groups.get(mainGroup).getParam(), context.getReboundWaz().getSkillList().size()))
            throw invalid("Sou mainのgroup/件数が不一致です");
        return new Location(context.getReboundWaz(), host, group, sourceBase, sourceEnd, rawMain, rawTama);
    }

    private static com.giga.nexas.dto.bhe.waz.Waz sourceWaz(Map<String, com.giga.nexas.dto.bhe.waz.Waz> files, String name) {
        if (files == null) throw invalid("元WAZ表がありません");
        var matches = files.entrySet().stream().filter(e -> name.equalsIgnoreCase(e.getKey())).toList();
        if (matches.size() != 1 || matches.get(0).getValue() == null) throw invalid("元WAZが一意ではありません: " + name);
        return matches.get(0).getValue();
    }

    private static RawSkill requireSource(com.giga.nexas.dto.bhe.waz.Waz source, SourceSignature signature) {
        RawSkill found = null;
        for (int i = 0; i < source.getSkillList().size(); i++) {
            var skill = source.getSkillList().get(i);
            if (skill == null || !signature.japanese().equals(skill.getSkillNameJapanese()) || !signature.english().equals(skill.getSkillNameEnglish())) continue;
            if (found != null) throw invalid("元技能双名が重複しています: " + signature.english());
            found = new RawSkill(i, skill);
        }
        if (found == null) throw invalid("元技能双名がありません: " + signature.english());
        byte[] originalRecord = sourceBytes(found.skill(), signature);
        if (originalRecord.length != signature.bytes() || !signature.sha256().equals(sha256(originalRecord)))
            throw invalid("元技能完全署名が変更されています: " + signature.english());
        return found;
    }

    private static byte[] sourceBytes(com.giga.nexas.dto.bhe.waz.Waz.Skill skill, SourceSignature signature) {
        if (!Integer.valueOf(signature.phases()).equals(skill.getPhaseQuantity()) || skill.getPhasesInfo() == null
                || skill.getPhasesInfo().size() != signature.phases() || skill.getSkillSuffixList() == null
                || skill.getSkillSuffixList().size() != signature.phases()) throw invalid("元phase/suffix数が変更されています");
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (BinaryWriter writer = new BinaryWriter(bytes, "windows-31j")) {
                writer.writeInt(1);
                writer.writeNullTerminatedString(skill.getSkillNameJapanese()); writer.writeNullTerminatedString(skill.getSkillNameEnglish());
                writer.writeInt(skill.getPhaseQuantity());
                for (var phase : skill.getPhasesInfo()) {
                    if (phase == null || phase.getSkillUnitCollection() == null) throw invalid("元slot表がありません");
                    Map<Integer, com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit> units = new java.util.HashMap<>();
                    for (var unit : phase.getSkillUnitCollection()) {
                        if (unit == null || unit.getUnitQuantity() == null || unit.getUnitQuantity() < 0 || unit.getUnitQuantity() >= 83
                                || unit.getSkillInfoObjectList() == null || unit.getSkillInfoUnknownList() == null
                                || units.putIfAbsent(unit.getUnitQuantity(), unit) != null) throw invalid("元slot重複/nullがあります");
                    }
                    for (int slot = 0; slot < 83; slot++) {
                        var unit = units.get(slot);
                        if (unit == null) { writer.writeInt(0); writer.writeInt(0); continue; }
                        writer.writeInt(unit.getSkillInfoObjectList().size());
                        for (var event : unit.getSkillInfoObjectList()) { if (event == null) throw invalid("元eventがnullです"); event.writeInfo(writer); }
                        writer.writeInt(unit.getSkillInfoUnknownList().size());
                        for (var loop : unit.getSkillInfoUnknownList()) { if (loop == null) throw invalid("元FFがnullです"); loop.writeInfo(writer); }
                    }
                }
                writer.writeInt(skill.getSkillSuffixList().size());
                for (int i = 0; i < skill.getSkillSuffixList().size(); i++) {
                    var suffix = skill.getSkillSuffixList().get(i);
                    if (suffix == null || !Integer.valueOf(i).equals(suffix.getInt1()) || !Integer.valueOf(0).equals(suffix.getInt2()))
                        throw invalid("元suffixが変更されています");
                    writer.writeInt(suffix.getInt1()); writer.writeInt(suffix.getInt2());
                }
            }
            return bytes.toByteArray();
        } catch (IOException error) { throw new IllegalStateException("Sou雪月花元署名の読取に失敗", error); }
    }

    private static List<Waz.Skill> expectedTargets(FollowupMekaContext context, List<RawSkill> source, boolean mainControls) {
        com.giga.nexas.dto.bhe.waz.Waz shell = new com.giga.nexas.dto.bhe.waz.Waz();
        for (RawSkill skill : source) shell.getSkillList().add(copy(skill.skill(), com.giga.nexas.dto.bhe.waz.Waz.Skill.class));
        Waz expected = mainControls ? new BheToBsdxWazConverter().convert(shell)
                : new com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz.BheToBsdxWazConverter().convert(shell);
        if (mainControls) for (Waz.Skill skill : expected.getSkillList()) retainMainControls(skill);
        TsukuyomiConvertedBundle independent = new TsukuyomiConvertedBundle();
        independent.getCommonProjectileResourceBundle().getWazByFileName().put("sou-snow-moon-guard.waz", expected);
        BheCommonProjectileAppendPlan planCopy = copy(context.getConvertedBundle().getCommonProjectileAppendPlan(), BheCommonProjectileAppendPlan.class);
        new CrossRedirectBheCommonProjectileResourcesStep().redirect(context.getRequest(), context.getConvertedBundle().getRawSourceBundle(), null, independent, planCopy);
        return expected.getSkillList();
    }

    private static Waz.Skill mainControlGraph(Waz.Skill skill) {
        Waz.Skill independent = copy(skill, Waz.Skill.class);
        retainMainControls(independent);
        return independent;
    }

    private static void retainMainControls(Waz.Skill skill) {
        for (var phase : skill.getPhasesInfo())
            phase.getSkillUnitCollection().removeIf(unit -> !Integer.valueOf(48).equals(unit.getUnitQuantity()) && !Integer.valueOf(71).equals(unit.getUnitQuantity()));
    }

    private static LocatedSkill findTarget(Waz waz, SourceSignature signature, int start, int end) {
        LocatedSkill found = null;
        for (int i = start; i < end; i++) {
            Waz.Skill skill = waz.getSkillList().get(i);
            if (skill == null || !signature.japanese().equals(skill.getSkillNameJapanese()) || !signature.english().equals(skill.getSkillNameEnglish())) continue;
            if (found != null) throw invalid("最終技能双名が重複しています: " + signature.english());
            found = new LocatedSkill(i, skill);
        }
        if (found == null) throw invalid("最終技能双名がありません: " + signature.english());
        return found;
    }

    private static LocatedSkill findPrivate(Location location) {
        LocatedSkill found = null;
        for (int i = 0; i < location.host().getSkillList().size(); i++) {
            Waz.Skill skill = location.host().getSkillList().get(i);
            if (skill == null) continue;
            boolean japanese = PRIVATE_JP.equals(skill.getSkillNameJapanese()), english = PRIVATE_EN.equals(skill.getSkillNameEnglish());
            if (!japanese && !english) continue;
            if (!japanese || !english || found != null || i < location.sourceEnd()) throw invalid("私有515の双名/重複/source区間が不正です");
            found = new LocatedSkill(i, skill);
        }
        return found;
    }

    private static void validateTargetShape(Waz.Skill skill, SourceSignature signature) {
        if (!Integer.valueOf(signature.phases()).equals(skill.getPhaseQuantity()) || skill.getPhasesInfo() == null
                || skill.getPhasesInfo().size() != signature.phases() || skill.getSkillSuffixList() == null
                || skill.getSkillSuffixList().size() != signature.phases()) throw invalid("最終phase/suffix数が変更されています");
        for (int i = 0; i < signature.phases(); i++) {
            var suffix = skill.getSkillSuffixList().get(i);
            if (suffix == null || !Integer.valueOf(i).equals(suffix.getInt1()) || !Integer.valueOf(0).equals(suffix.getInt2()))
                throw invalid("最終suffixが変更されています");
            var phase = skill.getPhasesInfo().get(i);
            if (phase == null || phase.getSkillUnitCollection() == null) throw invalid("最終slot表がありません");
            Set<Integer> slots = new HashSet<>();
            for (var unit : phase.getSkillUnitCollection()) {
                if (unit == null || unit.getUnitQuantity() == null || unit.getUnitQuantity() < 0 || unit.getUnitQuantity() >= 72
                        || !slots.add(unit.getUnitQuantity()) || unit.getSkillInfoObjectList() == null || unit.getSkillInfoUnknownList() == null
                        || unit.getSkillInfoObjectList().stream().anyMatch(Objects::isNull) || unit.getSkillInfoUnknownList().stream().anyMatch(Objects::isNull))
                    throw invalid("最終slot/event/FFが重複またはnullです");
            }
        }
    }

    private static Waz.Skill buildChild(Waz.Skill prototype) {
        Waz.Skill child = copy(prototype, Waz.Skill.class);
        child.setSkillNameJapanese(PRIVATE_JP); child.setSkillNameEnglish(PRIVATE_EN);
        CEventEffect effect = effect(child, 1, 2);
        patchEmitter(effect, 0, 0, 36, 4);
        return child;
    }

    private static void patchMain(Waz.Skill main, int level, int privateIndex) {
        int point = PREPARE_POINTS.get(level);
        if (!Integer.valueOf(point).equals(effect(main, 0, 1).getStartFrame())
                || !Integer.valueOf(point).equals(effect(main, 0, 1).getEndFrame())) throw invalid("元field建立点が変更されています");
        for (var loop : slot(main, 0, 48).getSkillInfoUnknownList())
            if (point >= loop.getStartFrame() && point <= loop.getEndFrame()) throw invalid("準備点が元FF区間内です");
        patchEmitter(effect(main, 0, 2), point, point, 36, 4);
        CEventEffect release = effect(main, 1, 0);
        patchEmitter(release, 3, 3, 32, 0);
        var references = release.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(3).equals(u.getUnitSlotNum())).toList();
        if (references.size() != 1 || !(references.get(0).getData() instanceof com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect ref))
            throw invalid("解放WAZ参照が不正です");
        ref.setWazSequenceNo(privateIndex);
    }

    private static void patchEmitter(CEventEffect effect, int start, int end, int oldFlags, int newFlags) {
        if (!Integer.valueOf(oldFlags).equals(effect.getInt3())) throw invalid("発射フラグが元署名と違います");
        var counts = effect.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(4).equals(u.getUnitSlotNum())).toList();
        if (counts.size() != 1 || !Integer.valueOf(1).equals(counts.get(0).getBuffer()) || !(counts.get(0).getData() instanceof CEventValRandom count))
            throw invalid("発射数の内側構造が不正です");
        effect.setStartFrame(start); effect.setEndFrame(end); effect.setInt3(newFlags);
        count.setInt2(1); count.setInt5(1);
    }

    private static SkillUnit slot(Waz.Skill skill, int phase, int slot) {
        var matches = skill.getPhasesInfo().get(phase).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList();
        if (matches.size() != 1) throw invalid("必要なslotが一意ではありません: " + slot);
        return matches.get(0);
    }

    private static CEventEffect effect(Waz.Skill skill, int phase, int ordinal) {
        var events = slot(skill, phase, 48).getSkillInfoObjectList();
        if (events.size() <= ordinal || !(events.get(ordinal) instanceof CEventEffect effect) || !Integer.valueOf(19).equals(effect.getTypeId()))
            throw invalid("必要なEffectがありません");
        return effect;
    }

    private static void validateMek(FollowupMekaContext context, int finalSkill) {
        if (context.getReboundMek().getMekWeaponInfoMap() == null) throw invalid("Sou MEK武装表がありません");
        var matches = context.getReboundMek().getMekWeaponInfoMap().values().stream().filter(Objects::nonNull)
                .filter(w -> "SETUGEKKA".equals(w.getWeaponSequence())).toList();
        if (matches.size() != 1 || !Integer.valueOf(finalSkill).equals(matches.get(0).getWazSequence()))
            throw invalid("Sou MEK雪月花参照が最終Lv3と一致しません");
    }

    private static void equalPayload(Object expected, Object actual, String label) {
        if (!payload(expected).equals(payload(actual))) throw invalid(label + "の完全載荷が変更されています");
    }

    private static JsonNode payload(Object value) {
        JsonNode tree = JSON.valueToTree(value);
        removeMetadata(tree);
        return tree;
    }

    private static void removeMetadata(JsonNode node) {
        if (node instanceof ObjectNode object) object.remove(List.of("offset", "length", "slotNum", "unitDescription", "description", "fileName", "extensionName"));
        node.forEach(SouSnowMoonSingleTargetFixer::removeMetadata);
    }

    private static <T> T copy(T source, Class<T> type) {
        try { return JSON.readValue(JSON.writeValueAsBytes(source), type); }
        catch (IOException error) { throw new IllegalStateException("Sou雪月花の独立コピーに失敗", error); }
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("Sou雪月花single-target拒否: " + reason);
    }

    private record SourceSignature(String japanese, String english, int phases, int bytes, String sha256) { }
    private record RawSkill(int index, com.giga.nexas.dto.bhe.waz.Waz.Skill skill) { }
    private record LocatedSkill(int index, Waz.Skill skill) { }
    private record Location(Waz main, Waz host, WazaGroupGrp.WazaGroupEntry group, int sourceBase, int sourceEnd,
                            com.giga.nexas.dto.bhe.waz.Waz rawMain, com.giga.nexas.dto.bhe.waz.Waz rawTama) { }
}
