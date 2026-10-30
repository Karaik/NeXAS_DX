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

/** Souフレアシーカーの三callerを私有Lv3載体に結び、多鎖999の生成辺だけを一発へ限定する。 */
final class SouFlareSeekerSingleTargetFixer {
    private static final String PRIVATE_JP = "★蒼：フレアシーカー・単一対象";
    private static final String PRIVATE_EN = "DX_SOU_FLARE_SEEKER_SINGLE_TARGET";
    // Complete original flag1/name/83-slot/phase/suffix records, measured from real BHE binary boundaries.
    private static final List<SourceSignature> MAIN = List.of(
            new SourceSignature("フレアシーカーLv1", "E_MUSI01", 2, 3043, "eb1dd7f0303a8f9c4254a689568582f05c78927a105d0c82d8d008b0ec3f8f93"),
            new SourceSignature("フレアシーカーLv2", "E_MUSI02", 2, 3043, "4b9582191e32dfd31648a3e9a6e3aa5197fab046c953dade93113bc8ba4bf9e3"),
            new SourceSignature("フレアシーカーLv3", "E_MUSI03", 2, 3019, "22ce0aa4e444bd360880a60bb7b8c87d8d1eca1f28c36beba7b28fda50f4effa"));
    private static final List<SourceSignature> PUBLIC = List.of(
            new SourceSignature("★蒼：フレアシーカーLv1", "0348", 5, 11159, "eb7cb56c53301e97b334a1f50227d5fbd4865af675599b70019a45d5d579a899"),
            new SourceSignature("★蒼：フレアシーカーLv2", "0349", 5, 11087, "c060ba178c6dc0546fe58b351c3ed80d83917de6c8fab010b761ea3d5bd14e4d"),
            new SourceSignature("★蒼：フレアシーカーLv3", "0335", 5, 11087, "9eac6505b53345c7d36b73f49a9a7b8ada05e9de3e492ccc062e3faf71696c4c"),
            new SourceSignature("★蒼：フレアシーカー：虫", "0334", 8, 20464, "379bc901cd023cbe8ec2781bdaff0298e012f2c6988cb12fdfb1c743fd3a7bc7"));
    private static final SourceSignature BOMB = new SourceSignature("★蒼：フレアシーカー", "KINOKO83", 2, 11328, "c9a0173fe12211d5fd420940fbfce566f57956cd3404490c917f085abb6b7f7a");
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SouFlareSeekerSingleTargetFixer() { }

    static void fix(FollowupMekaContext context) {
        if (context == null || context.getReboundWaz() == null) return;
        Location location = resolve(context);
        List<RawSkill> rawMain = new ArrayList<>(), rawPublic = new ArrayList<>();
        for (SourceSignature signature : MAIN) rawMain.add(requireSource(location.rawMain(), signature));
        for (SourceSignature signature : PUBLIC) rawPublic.add(requireSource(location.rawTama(), signature));
        RawSkill rawBomb = requireSource(location.rawBomb(), BOMB);
        List<Waz.Skill> expectedMain = expectedTargets(context, rawMain, true);
        List<Waz.Skill> expectedPublic = expectedTargets(context, rawPublic, false);
        Waz.Skill expectedBomb = expectedTargets(context, List.of(rawBomb), false).get(0);
        List<LocatedSkill> publicSkills = new ArrayList<>();
        for (int i = 0; i < PUBLIC.size(); i++) {
            LocatedSkill actual = findTarget(location.host(), PUBLIC.get(i), location.sourceBase(), location.sourceEnd());
            if (actual.index() != Math.addExact(location.sourceBase(), rawPublic.get(i).index()))
                throw invalid("公共技能が元source sliceの正しい位置にありません");
            validateTargetShape(actual.skill(), PUBLIC.get(i));
            equalPayload(expectedPublic.get(i), actual.skill(), "公共" + PUBLIC.get(i).english());
            publicSkills.add(actual);
        }
        LocatedSkill actualBomb = findTarget(location.bomb().host(), BOMB, location.bomb().base(), location.bomb().end());
        if (actualBomb.index() != Math.addExact(location.bomb().base(), rawBomb.index())) throw invalid("Bomb後継のsource slice位置が不一致です");
        validateTargetShape(actualBomb.skill(), BOMB);
        equalPayload(expectedBomb, actualBomb.skill(), "Bomb正傷害/自然出口");
        Waz.Skill prototype = publicSkills.get(2).skill();
        LocatedSkill installed = findPrivate(location);
        int privateIndex = installed == null ? location.host().getSkillList().size() : installed.index();
        Waz.Skill expectedCarrier = buildCarrier(prototype);
        if (installed != null) {
            validateTargetShape(installed.skill(), new SourceSignature(PRIVATE_JP, PRIVATE_EN, 5, 0, ""));
            equalPayload(expectedCarrier, installed.skill(), "既存私有Flare");
        }
        List<LocatedSkill> callers = new ArrayList<>();
        List<Waz.Skill> candidates = new ArrayList<>();
        for (int level = 0; level < MAIN.size(); level++) {
            LocatedSkill actual = findTarget(location.main(), MAIN.get(level), 0, location.main().getSkillList().size());
            validateTargetShape(actual.skill(), MAIN.get(level));
            Waz.Skill expectedInstalled = copy(expectedMain.get(level), Waz.Skill.class);
            patchMain(expectedInstalled, privateIndex);
            equalPayload(installed == null ? expectedMain.get(level) : expectedInstalled, mainControlGraph(actual.skill()), MAIN.get(level).english());
            Waz.Skill candidate = copy(actual.skill(), Waz.Skill.class);
            if (installed == null) patchMain(candidate, privateIndex);
            equalPayload(expectedInstalled, mainControlGraph(candidate), "候補" + MAIN.get(level).english());
            callers.add(actual); candidates.add(candidate);
        }
        validateMek(context, callers.get(2).index());
        // Stable identity allows later legitimate private appends; verified repeats publish nothing.
        if (installed != null) return;
        List<Waz.Skill> newMain = new ArrayList<>(location.main().getSkillList());
        for (int i = 0; i < callers.size(); i++) newMain.set(callers.get(i).index(), candidates.get(i));
        List<Waz.Skill> newHost = new ArrayList<>(location.host().getSkillList());
        newHost.add(expectedCarrier);
        int count = Math.addExact(location.host().getSkillList().size(), 1);
        if (newHost.size() != count || privateIndex != count - 1) throw invalid("候補の私有追加位置が一致しません");
        // No reads, callbacks, or validation remain after this point.
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
        com.giga.nexas.dto.bhe.waz.Waz rawBomb = sourceWaz(raw.getCommonProjectileWazByFileName(), "bomb.waz");
        if (rawMain.getSkillList() == null || rawTama.getSkillList() == null || rawBomb.getSkillList() == null) throw invalid("元技能表がありません");
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
        return new Location(context.getReboundWaz(), host, group, sourceBase, sourceEnd, rawMain, rawTama, rawBomb, resolveBomb(context, rawBomb));
    }

    private static BombLocation resolveBomb(FollowupMekaContext context, com.giga.nexas.dto.bhe.waz.Waz raw) {
        var plan = context.getConvertedBundle().getCommonProjectileAppendPlan();
        int source = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("bomb.waz");
        String name = plan.getSourceWazIndexToTargetFileName().get(source);
        Integer group = plan.getSourceWazIndexToTargetIndex().get(source), base = plan.getSourceWazIndexToTargetSkillBase().get(source);
        Integer count = plan.getSourceWazIndexToTargetSkillCount().get(source);
        if (name == null || group == null || base == null || base < 0 || count == null || count != raw.getSkillList().size()
                || plan.getCommonProjectileWazFiles().stream().noneMatch(name::equalsIgnoreCase)) throw invalid("Bomb後継の元source slice/出力planが不正です");
        var hosts = context.getBaseline().getWazByFileName().entrySet().stream().filter(e -> name.equalsIgnoreCase(e.getKey())).toList();
        if (hosts.size() != 1 || hosts.get(0).getValue() == null || hosts.get(0).getValue().getSkillList() == null) throw invalid("Bomb宿主が一意ではありません");
        Waz host = hosts.get(0).getValue();
        int end = Math.addExact(base, count);
        var groups = context.getBaseline().getWazaGroupGrp().getWazaList();
        if (group < 0 || group >= groups.size() || groups.get(group) == null || end > host.getSkillList().size()
                || !Objects.equals(groups.get(group).getParam(), host.getSkillList().size())) throw invalid("Bomb宿主source slice/GRP件数が不一致です");
        String stem = name.replaceAll("(?i)\\.waz$", "");
        if (!stem.equalsIgnoreCase(groups.get(group).getWazaCodeName()) && !stem.equalsIgnoreCase(groups.get(group).getWazaDisplayName()))
            throw invalid("Bomb宿主GRP身份が不一致です");
        return new BombLocation(host, base, end);
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
        } catch (IOException error) { throw new IllegalStateException("Souフレアシーカー元署名の読取に失敗", error); }
    }

    private static List<Waz.Skill> expectedTargets(FollowupMekaContext context, List<RawSkill> source, boolean mainControls) {
        com.giga.nexas.dto.bhe.waz.Waz shell = new com.giga.nexas.dto.bhe.waz.Waz();
        for (RawSkill skill : source) shell.getSkillList().add(copy(skill.skill(), com.giga.nexas.dto.bhe.waz.Waz.Skill.class));
        Waz expected = mainControls ? new BheToBsdxWazConverter().convert(shell)
                : new com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz.BheToBsdxWazConverter().convert(shell);
        BheCommonProjectileAppendPlan planCopy = copy(context.getConvertedBundle().getCommonProjectileAppendPlan(), BheCommonProjectileAppendPlan.class);
        if (mainControls) {
            for (Waz.Skill skill : expected.getSkillList()) retainMainControls(skill);
            // Main caller inner15 is private Sou SE16:4, so use the real followup resolver instead of the common-only redirect.
            var importCopy = context.getImportPlan() == null ? new com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan()
                    : copy(context.getImportPlan(), com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan.class);
            var rawMek = context.getConvertedBundle().getRawSourceBundle().getTsukuyomiMek();
            if (context.getRequest() == null || rawMek == null || rawMek.getMekBasicInfo() == null)
                throw invalid("元main WAZのMEK/group文脈がありません");
            importCopy.getSourceWazIndexByFileName().put(context.getRequest().getWazFileName(), rawMek.getMekBasicInfo().getWazFileSequence());
            var groupCopy = copy(context.getGrpAppendPlan(), com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan.class);
            Waz rebound = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.RebindWazStep()
                    .rebindAuxiliaryWaz(context.getRequest(), expected, importCopy, groupCopy, null, planCopy);
            if (rebound == null) throw invalid("独立main controlの再構築に失敗しました");
            return rebound.getSkillList();
        }
        TsukuyomiConvertedBundle independent = new TsukuyomiConvertedBundle();
        independent.getCommonProjectileResourceBundle().getWazByFileName().put("sou-flare-seeker-guard.waz", expected);
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
            if (!japanese || !english || found != null || i < location.sourceEnd()) throw invalid("私有Flareの双名/重複/source区間が不正です");
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

    private static Waz.Skill buildCarrier(Waz.Skill prototype) {
        Waz.Skill carrier = copy(prototype, Waz.Skill.class);
        carrier.setSkillNameJapanese(PRIVATE_JP); carrier.setSkillNameEnglish(PRIVATE_EN);
        CEventEffect dangerous = effect(carrier, 2, 1);
        if (!Integer.valueOf(1).equals(dangerous.getStartFrame()) || !Integer.valueOf(30).equals(dangerous.getEndFrame())
                || !Integer.valueOf(36).equals(dangerous.getInt3())) throw invalid("元多鎖発射窓/flagsが変更されています");
        for (var loop : slot(carrier, 2, 48).getSkillInfoUnknownList())
            if (loop.getStartFrame() <= 0 && loop.getEndFrame() >= 0) throw invalid("単発0f点が元Effect FF内です");
        var counts = dangerous.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(4).equals(u.getUnitSlotNum())).toList();
        if (counts.size() != 1 || !Integer.valueOf(1).equals(counts.get(0).getBuffer()) || !(counts.get(0).getData() instanceof CEventValRandom count))
            throw invalid("発射数の内側構造が不正です");
        dangerous.setStartFrame(0); dangerous.setEndFrame(0); dangerous.setInt3(4);
        count.setInt2(1); count.setInt5(1);
        return carrier;
    }

    private static void patchMain(Waz.Skill main, int privateIndex) {
        CEventEffect caller = effect(main, 0, 0);
        var references = caller.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(3).equals(u.getUnitSlotNum())).toList();
        if (references.size() != 1 || !Integer.valueOf(1).equals(references.get(0).getBuffer())
                || !(references.get(0).getData() instanceof com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect reference))
            throw invalid("主callerのWAZ参照が不正です");
        reference.setWazSequenceNo(privateIndex);
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
                .filter(w -> "E_MUSI".equals(w.getWeaponSequence())).toList();
        if (matches.size() != 1 || !Integer.valueOf(finalSkill).equals(matches.get(0).getWazSequence()))
            throw invalid("Sou MEKフレアシーカー参照が最終Lv3と一致しません");
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
        node.forEach(SouFlareSeekerSingleTargetFixer::removeMetadata);
    }

    private static <T> T copy(T source, Class<T> type) {
        try { return JSON.readValue(JSON.writeValueAsBytes(source), type); }
        catch (IOException error) { throw new IllegalStateException("Souフレアシーカーの独立コピーに失敗", error); }
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("Souフレアシーカーsingle-target拒否: " + reason);
    }

    private record SourceSignature(String japanese, String english, int phases, int bytes, String sha256) { }
    private record RawSkill(int index, com.giga.nexas.dto.bhe.waz.Waz.Skill skill) { }
    private record LocatedSkill(int index, Waz.Skill skill) { }
    private record BombLocation(Waz host, int base, int end) { }
    private record Location(Waz main, Waz host, WazaGroupGrp.WazaGroupEntry group, int sourceBase, int sourceEnd,
                            com.giga.nexas.dto.bhe.waz.Waz rawMain, com.giga.nexas.dto.bhe.waz.Waz rawTama,
                            com.giga.nexas.dto.bhe.waz.Waz rawBomb, BombLocation bomb) { }
}
