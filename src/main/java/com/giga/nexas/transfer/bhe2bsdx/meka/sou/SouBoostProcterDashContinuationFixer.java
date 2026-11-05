package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
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

/** Preserve each Boost Procter level's dash phase while removing only the downgraded dash-end OR branch. */
final class SouBoostProcterDashContinuationFixer {
    private static final List<String> PRIVATE_JP = List.of("◆蒼：ブースト・プロクターLv1・継続", "◆蒼：ブースト・プロクターLv2・継続", "◆蒼：ブースト・プロクターLv3・継続");
    private static final List<String> PRIVATE_EN = List.of("DX_SOU_BOOST_PROCTER_LV1", "DX_SOU_BOOST_PROCTER_LV2", "DX_SOU_BOOST_PROCTER_LV3");
    // Complete original flag1/name/83-slot/phase/suffix records, measured against real BHE binary boundaries.
    private static final List<SourceSignature> MAIN = List.of(
            new SourceSignature("ブースト・プロクターLv1", "B_PROCTER01", 1, 1828, "ecd5aa0935092d61bf3420ac146be4061c60d72b8fded7dbfd535b689541a724"),
            new SourceSignature("ブースト・プロクターLv2", "B_PROCTER02", 1, 1860, "83b7aca42db8125265b43b8e4832a003855f84b1c6025d27e3e1c4e796ec17e9"),
            new SourceSignature("ブースト・プロクターLv3", "B_PROCTER03", 1, 1860, "0ac86cccf50c20e430ca1ef503841e6f7b0d30f5f3d15b9d1c1bee6609316547"));
    private static final List<SourceSignature> PUBLIC = List.of(
            new SourceSignature("◆蒼：ブースト・プロクターLv1", "BIT61", 3, 9544, "5dd22b26588e462c94f611d898ae86050cdfeefc1b675ecce8be76c23a5c6d6d"),
            new SourceSignature("◆蒼：ブースト・プロクターLv2", "BIT62", 3, 9544, "9025d0525e4da418f23be0f7ac2cc93bc87b820b428e41dff6506826d4c1435d"),
            new SourceSignature("◆蒼：ブースト・プロクターLv3", "BIT48", 3, 9496, "5e39cc6df40a444d7ba2eb1eedfb91e2db8f675dd419228d8138f54011b4bb61"));
    private static final List<SourceSignature> CHILDREN = List.of(
            new SourceSignature("◆蒼：ブースト・プロクター：縦", "0119", 1, 4498, "a04c632cc0f2ecaef9b59a26b8819c2036c29ca52b2142f1bdf740d130d536dc"),
            new SourceSignature("◆蒼：ブースト・プロクター：横", "0183", 1, 3762, "8db500d3a492fd3af1db099ce48d5e3f81dfc3d9a1d20ffbb4ff343149600a76"));
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SouBoostProcterDashContinuationFixer() { }

    static void fix(FollowupMekaContext context) {
        if (context == null || context.getReboundWaz() == null) return;
        Location location = resolve(context);
        List<RawSkill> rawMain = new ArrayList<>();
        for (SourceSignature signature : MAIN) rawMain.add(requireSource(location.rawMain(), signature));
        List<Waz.Skill> expectedMain = expectedTargets(context, rawMain, true);
        List<LocatedSkill> publicBodies = validatePublic(context, location.body(), location.rawBody(), PUBLIC);
        validatePublic(context, location.child(), location.rawChild(), CHILDREN);
        List<LocatedSkill> installed = new ArrayList<>();
        for (int level = 0; level < 3; level++) installed.add(findPrivate(location.body(), PRIVATE_JP.get(level), PRIVATE_EN.get(level)));
        long installedCount = installed.stream().filter(Objects::nonNull).count();
        if (installedCount != 0 && installedCount != 3) throw invalid("Partial private level installation");
        boolean present = installedCount == 3;
        int firstPrivate = present ? installed.get(0).index() : location.body().host().getSkillList().size();
        List<Waz.Skill> privateBodies = new ArrayList<>();
        List<LocatedSkill> callers = new ArrayList<>();
        List<Waz.Skill> candidates = new ArrayList<>();
        for (int level = 0; level < 3; level++) {
            int privateIndex = Math.addExact(firstPrivate, level);
            Waz.Skill expectedBody = buildBody(publicBodies.get(level).skill(), level);
            if (present) {
                if (installed.get(level).index() != privateIndex) throw invalid("Private level order changed");
                validateTargetShape(installed.get(level).skill(), new SourceSignature(PRIVATE_JP.get(level), PRIVATE_EN.get(level), 3, 0, ""));
                equalPayload(expectedBody, installed.get(level).skill(), "installed private level " + (level + 1));
            }
            privateBodies.add(expectedBody);
            LocatedSkill actual = findTarget(location.main(), MAIN.get(level), 0, location.main().getSkillList().size());
            validateTargetShape(actual.skill(), MAIN.get(level));
            Waz.Skill expectedInstalled = copy(expectedMain.get(level), Waz.Skill.class);
            patchMain(expectedInstalled, privateIndex);
            equalPayload(present ? expectedInstalled : expectedMain.get(level), mainControlGraph(actual.skill()), MAIN.get(level).english());
            Waz.Skill candidate = copy(actual.skill(), Waz.Skill.class);
            if (!present) patchMain(candidate, privateIndex);
            equalPayload(expectedInstalled, mainControlGraph(candidate), "candidate " + MAIN.get(level).english());
            callers.add(actual); candidates.add(candidate);
        }
        validateMek(context, callers.get(2).index());
        // Verified repeats preserve object identity and allow later legitimate private appends.
        if (present) return;
        List<Waz.Skill> newMain = new ArrayList<>(location.main().getSkillList());
        for (int level = 0; level < 3; level++) newMain.set(callers.get(level).index(), candidates.get(level));
        List<Waz.Skill> newHost = new ArrayList<>(location.body().host().getSkillList());
        newHost.addAll(privateBodies);
        int count = Math.addExact(location.body().host().getSkillList().size(), 3);
        if (newHost.size() != count || firstPrivate != count - 3) throw invalid("Private append positions changed");
        // Candidate construction and every guard finish before publishing the three shared views.
        location.main().setSkillList(newMain);
        location.body().host().setSkillList(newHost);
        location.body().group().setParam(count);
    }

    private static List<LocatedSkill> validatePublic(FollowupMekaContext context, HostLocation host,
                                                    com.giga.nexas.dto.bhe.waz.Waz raw, List<SourceSignature> signatures) {
        List<RawSkill> sources = new ArrayList<>();
        for (SourceSignature signature : signatures) sources.add(requireSource(raw, signature));
        List<Waz.Skill> expected = expectedTargets(context, sources, false);
        List<LocatedSkill> actual = new ArrayList<>();
        for (int i = 0; i < signatures.size(); i++) {
            LocatedSkill located = findTarget(host.host(), signatures.get(i), host.sourceBase(), host.sourceEnd());
            if (located.index() != Math.addExact(host.sourceBase(), sources.get(i).index()))
                throw invalid("Public skill is outside its original source position");
            validateTargetShape(located.skill(), signatures.get(i));
            equalPayload(expected.get(i), located.skill(), "public " + signatures.get(i).english());
            actual.add(located);
        }
        return actual;
    }

    private static Location resolve(FollowupMekaContext context) {
        if (context.getConvertedBundle() == null || context.getConvertedBundle().getRawSourceBundle() == null
                || context.getBaseline() == null || context.getConvertedBundle().getPreparedBaselineBundle() != context.getBaseline()
                || context.getBaseline().getWazaGroupGrp() == null || context.getBaseline().getWazaGroupGrp().getWazaList() == null
                || context.getBaseline().getWazByFileName() == null || context.getReboundWaz().getSkillList() == null
                || context.getReboundMek() == null || context.getReboundMek().getMekBasicInfo() == null)
            throw invalid("Sou final main/MEK/shared baseline context is missing or inconsistent");
        TsukuyomiRawSourceBundle raw = context.getConvertedBundle().getRawSourceBundle();
        var rawMain = sourceWaz(raw.getWazByFileName(), "sou.waz");
        var rawBody = sourceWaz(raw.getCommonProjectileWazByFileName(), "tama03.waz");
        var rawChild = sourceWaz(raw.getCommonProjectileWazByFileName(), "tama04.waz");
        if (rawMain.getSkillList() == null || rawBody.getSkillList() == null || rawChild.getSkillList() == null)
            throw invalid("Original skill tables are missing");
        var groups = context.getBaseline().getWazaGroupGrp().getWazaList();
        Integer mainGroup = context.getReboundMek().getMekBasicInfo().getWazFileSequence();
        if (mainGroup == null || mainGroup < 0 || mainGroup >= groups.size() || groups.get(mainGroup) == null
                || context.getGrpAppendPlan() == null || context.getGrpAppendPlan().getWazaGroupIndex() != mainGroup
                || !Objects.equals(groups.get(mainGroup).getParam(), context.getReboundWaz().getSkillList().size()))
            throw invalid("Sou main group/count is inconsistent");
        return new Location(context.getReboundWaz(), resolveHost(context, "tama03.waz", rawBody),
                resolveHost(context, "tama04.waz", rawChild), rawMain, rawBody, rawChild);
    }

    private static HostLocation resolveHost(FollowupMekaContext context, String sourceName, com.giga.nexas.dto.bhe.waz.Waz raw) {
        BheCommonProjectileAppendPlan plan = context.getConvertedBundle().getCommonProjectileAppendPlan();
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf(sourceName);
        if (plan == null || sourceGroup < 0) throw invalid("Common projectile plan is missing");
        String hostName = plan.getSourceWazIndexToTargetFileName().get(sourceGroup);
        Integer hostGroup = plan.getSourceWazIndexToTargetIndex().get(sourceGroup);
        Integer sourceBase = plan.getSourceWazIndexToTargetSkillBase().get(sourceGroup);
        Integer sourceCount = plan.getSourceWazIndexToTargetSkillCount().get(sourceGroup);
        if (hostName == null || hostGroup == null || sourceBase == null || sourceCount == null || sourceBase < 0
                || sourceCount != raw.getSkillList().size()
                || plan.getCommonProjectileWazFiles().stream().noneMatch(hostName::equalsIgnoreCase))
            throw invalid("Source slice/output plan is invalid: " + sourceName);
        var hosts = context.getBaseline().getWazByFileName().entrySet().stream().filter(e -> hostName.equalsIgnoreCase(e.getKey())).toList();
        if (hosts.size() != 1 || hosts.get(0).getValue() == null || hosts.get(0).getValue().getSkillList() == null)
            throw invalid("Final host is missing or ambiguous: " + sourceName);
        Waz host = hosts.get(0).getValue();
        int sourceEnd = Math.addExact(sourceBase, sourceCount);
        var groups = context.getBaseline().getWazaGroupGrp().getWazaList();
        if (hostGroup < 0 || hostGroup >= groups.size() || groups.get(hostGroup) == null || sourceEnd > host.getSkillList().size()
                || !Objects.equals(groups.get(hostGroup).getParam(), host.getSkillList().size()))
            throw invalid("Host source slice/group count is inconsistent: " + sourceName);
        WazaGroupGrp.WazaGroupEntry group = groups.get(hostGroup);
        String stem = hostName.replaceAll("(?i)\\.waz$", "");
        if (!stem.equalsIgnoreCase(group.getWazaCodeName()) && !stem.equalsIgnoreCase(group.getWazaDisplayName()))
            throw invalid("Host group identity is inconsistent: " + sourceName);
        // Several source files can share this host (pure-DX Tama03/Tama05 fallback).
        // An installed private family must be after every registered public source slice.
        int publicEnd = sourceEnd;
        for (var entry : plan.getSourceWazIndexToTargetFileName().entrySet()) {
            if (!hostName.equalsIgnoreCase(entry.getValue())) continue;
            Integer base = plan.getSourceWazIndexToTargetSkillBase().get(entry.getKey());
            Integer count = plan.getSourceWazIndexToTargetSkillCount().get(entry.getKey());
            if (base == null || count == null || base < 0 || count < 0
                    || !hostGroup.equals(plan.getSourceWazIndexToTargetIndex().get(entry.getKey())))
                throw invalid("Shared-host source slice is inconsistent: " + sourceName);
            int end = Math.addExact(base, count);
            if (end > host.getSkillList().size()) throw invalid("Shared-host source slice exceeds its host");
            publicEnd = Math.max(publicEnd, end);
        }
        return new HostLocation(host, group, sourceBase, sourceEnd, publicEnd);
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
        } catch (IOException error) { throw new IllegalStateException("Souブースト・プロクター元署名の読取に失敗", error); }
    }

    private static List<Waz.Skill> expectedTargets(FollowupMekaContext context, List<RawSkill> source, boolean mainControls) {
        com.giga.nexas.dto.bhe.waz.Waz shell = new com.giga.nexas.dto.bhe.waz.Waz();
        for (RawSkill skill : source) shell.getSkillList().add(copy(skill.skill(), com.giga.nexas.dto.bhe.waz.Waz.Skill.class));
        Waz expected = mainControls ? new BheToBsdxWazConverter().convert(shell)
                : new com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz.BheToBsdxWazConverter().convert(shell);
        BheCommonProjectileAppendPlan planCopy = copy(context.getConvertedBundle().getCommonProjectileAppendPlan(), BheCommonProjectileAppendPlan.class);
        if (mainControls) {
            for (Waz.Skill skill : expected.getSkillList()) retainMainControls(skill);
            // Rebuild main controls with the actual private and common mappings; leave presentation slots outside this guard.
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
        independent.getCommonProjectileResourceBundle().getWazByFileName().put("sou-boost-procter-guard.waz", expected);
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

    private static LocatedSkill findPrivate(HostLocation location, String japaneseName, String englishName) {
        LocatedSkill found = null;
        for (int i = 0; i < location.host().getSkillList().size(); i++) {
            Waz.Skill skill = location.host().getSkillList().get(i);
            if (skill == null) continue;
            boolean japanese = japaneseName.equals(skill.getSkillNameJapanese()), english = englishName.equals(skill.getSkillNameEnglish());
            if (!japanese && !english) continue;
            if (!japanese || !english || found != null || i < location.publicEnd())
                throw invalid("Private identity is partial, duplicated, or inside the public source slice: " + englishName);
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

    private static Waz.Skill buildBody(Waz.Skill prototype, int level) {
        Waz.Skill body = copy(prototype, Waz.Skill.class);
        body.setSkillNameJapanese(PRIVATE_JP.get(level)); body.setSkillNameEnglish(PRIVATE_EN.get(level));
        var events = slot(body, 1, 71).getSkillInfoObjectList();
        if (events.size() != 3 || !(events.get(1) instanceof CEventChange wrongEnd)
                || !Integer.valueOf(0).equals(wrongEnd.getStartFrame()) || !Integer.valueOf(149).equals(wrongEnd.getEndFrame())
                || !Integer.valueOf(2).equals(wrongEnd.getFlag()) || !Integer.valueOf(1).equals(wrongEnd.getInt1())
                || wrongEnd.getBsdxInfoCollectionList1().size() != 2)
            throw invalid("Original downgraded dash-end branch changed");
        for (int i = 0; i < 2; i++) {
            var condition = wrongEnd.getBsdxInfoCollectionList1().get(i);
            if (!List.of(2, 1, 4 + i).equals(condition.getTypeList()) || !List.of(3, 0).equals(condition.getIntList3())
                    || !Integer.valueOf(0).equals(condition.getInt2()))
                throw invalid("Original root dash-end predicates changed");
        }
        // The existing NOT SDASH AND NOT BDASH branch already expresses stopping the dash.
        // Preserve that branch, the original 149f cap, P2's 40f END, and this level's emission payloads.
        events.remove(1);
        return body;
    }

    private static void patchMain(Waz.Skill main, int privateIndex) {
        reference(effect(main, 0, 0)).setWazSequenceNo(privateIndex);
    }

    private static CEventWazaSelect reference(CEventEffect event) {
        var references = event.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(3).equals(u.getUnitSlotNum())).toList();
        if (references.size() != 1 || !Integer.valueOf(1).equals(references.get(0).getBuffer())
                || !(references.get(0).getData() instanceof CEventWazaSelect reference))
            throw invalid("WAZ reference is missing or ambiguous");
        return reference;
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
                .filter(w -> "B_PROCTER".equals(w.getWeaponSequence())).toList();
        if (matches.size() != 1 || !Integer.valueOf(finalSkill).equals(matches.get(0).getWazSequence()))
            throw invalid("Sou MEKブースト・プロクター参照が最終Lv3と一致しません");
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
        node.forEach(SouBoostProcterDashContinuationFixer::removeMetadata);
    }

    private static <T> T copy(T source, Class<T> type) {
        try { return JSON.readValue(JSON.writeValueAsBytes(source), type); }
        catch (IOException error) { throw new IllegalStateException("Souブースト・プロクターの独立コピーに失敗", error); }
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("Sou Boost Procter dash continuation rejected: " + reason);
    }

    private record SourceSignature(String japanese, String english, int phases, int bytes, String sha256) { }
    private record RawSkill(int index, com.giga.nexas.dto.bhe.waz.Waz.Skill skill) { }
    private record LocatedSkill(int index, Waz.Skill skill) { }
    private record HostLocation(Waz host, WazaGroupGrp.WazaGroupEntry group, int sourceBase, int sourceEnd, int publicEnd) { }
    private record Location(Waz main, HostLocation body, HostLocation child, com.giga.nexas.dto.bhe.waz.Waz rawMain,
                            com.giga.nexas.dto.bhe.waz.Waz rawBody, com.giga.nexas.dto.bhe.waz.Waz rawChild) { }
}
