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
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVal;
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

/** Sou Cool Gain keeps charge in the scalar and uses parent phase changes for the private return protocol. */
final class SouCoolGainReturnProtocolFixer {
    private static final String BODY_JP = "★蒼：クールゲイン：本体・回収", BODY_EN = "DX_SOU_COOL_GAIN_BODY";
    private static final String ICE_JP = "★蒼：クールゲイン：氷・回収", ICE_EN = "DX_SOU_COOL_GAIN_ICE";
    // Complete original flag1/name/83-slot/phase/suffix records from the real BHE binary boundaries.
    private static final List<SourceSignature> MAIN = List.of(
            new SourceSignature("クールゲインLv1", "C_GAIN01", 2, 2749, "ffcbfed245bdcfceb3ea465a5ca6cec567259fa29a39a9f0f69169706e677c90"),
            new SourceSignature("クールゲインLv2", "C_GAIN02", 2, 2749, "818ec68875675e22327f272b8af7b6e4b127bcc803737d34b3960ecc505352de"),
            new SourceSignature("クールゲインLv3", "C_GAIN03", 2, 2725, "7f82a99970ab1fe6aaf24fad60b556691af3852cf71aa8c4413155c2f13c09d1"));
    private static final List<SourceSignature> PUBLIC = List.of(
            new SourceSignature("★蒼：クールゲイン：本体", "0344", 3, 11484, "98ac7ed2e7fd20abdf9ac198890a27df0903054c2346da578b5279e20e1735a7"),
            new SourceSignature("★蒼：クールゲイン：氷", "0345", 8, 12498, "5f6e3837b825abff9fcdb719087d5b221e02dcf9695b67e309cd0198128234e0"),
            new SourceSignature("★蒼：クールゲイン：熱冷却", "0346", 1, 1350, "f5849d84b11d3eaba2d0143d2440b8922d2ce818be67b3c6f3cec6fcf57fb26c"));
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SouCoolGainReturnProtocolFixer() { }

    static void fix(FollowupMekaContext context) {
        if (context == null || context.getReboundWaz() == null) return;
        Location location = resolve(context);
        List<RawSkill> rawMain = new ArrayList<>(), rawPublic = new ArrayList<>();
        for (SourceSignature signature : MAIN) rawMain.add(requireSource(location.rawMain(), signature));
        for (SourceSignature signature : PUBLIC) rawPublic.add(requireSource(location.rawTama(), signature));
        List<Waz.Skill> expectedMain = expectedTargets(context, rawMain, true);
        List<Waz.Skill> expectedPublic = expectedTargets(context, rawPublic, false);
        List<LocatedSkill> publicSkills = new ArrayList<>();
        for (int i = 0; i < PUBLIC.size(); i++) {
            LocatedSkill actual = findTarget(location.host(), PUBLIC.get(i), location.sourceBase(), location.sourceEnd());
            if (actual.index() != Math.addExact(location.sourceBase(), rawPublic.get(i).index()))
                throw invalid("Public skill is outside its original source position");
            validateTargetShape(actual.skill(), PUBLIC.get(i));
            equalPayload(expectedPublic.get(i), actual.skill(), "public " + PUBLIC.get(i).english());
            publicSkills.add(actual);
        }
        LocatedSkill body = findPrivate(location, BODY_JP, BODY_EN), ice = findPrivate(location, ICE_JP, ICE_EN);
        if ((body == null) != (ice == null)) throw invalid("Partial private body/ice installation");
        int bodyIndex = body == null ? location.host().getSkillList().size() : body.index();
        int iceIndex = Math.addExact(bodyIndex, 1);
        if (ice != null && ice.index() != iceIndex) throw invalid("Private body/ice order changed");
        Waz.Skill expectedBody = buildBody(publicSkills.get(0).skill(), publicSkills.get(1).index(), iceIndex, location.hostGroup());
        Waz.Skill expectedIce = buildIce(publicSkills.get(1).skill());
        if (body != null) {
            validateTargetShape(body.skill(), new SourceSignature(BODY_JP, BODY_EN, 3, 0, ""));
            validateTargetShape(ice.skill(), new SourceSignature(ICE_JP, ICE_EN, 8, 0, ""));
            equalPayload(expectedBody, body.skill(), "installed private body");
            equalPayload(expectedIce, ice.skill(), "installed private ice");
        }
        List<LocatedSkill> callers = new ArrayList<>();
        List<Waz.Skill> candidates = new ArrayList<>();
        for (int level = 0; level < MAIN.size(); level++) {
            LocatedSkill actual = findTarget(location.main(), MAIN.get(level), 0, location.main().getSkillList().size());
            validateTargetShape(actual.skill(), MAIN.get(level));
            Waz.Skill expectedInstalled = copy(expectedMain.get(level), Waz.Skill.class);
            patchMain(expectedInstalled, bodyIndex);
            equalPayload(body == null ? expectedMain.get(level) : expectedInstalled, mainControlGraph(actual.skill()), MAIN.get(level).english());
            Waz.Skill candidate = copy(actual.skill(), Waz.Skill.class);
            if (body == null) patchMain(candidate, bodyIndex);
            equalPayload(expectedInstalled, mainControlGraph(candidate), "candidate " + MAIN.get(level).english());
            callers.add(actual); candidates.add(candidate);
        }
        validateMek(context, callers.get(2).index());
        // Later legitimate private appends are allowed. A verified repeat publishes nothing.
        if (body != null) return;
        List<Waz.Skill> newMain = new ArrayList<>(location.main().getSkillList());
        for (int i = 0; i < callers.size(); i++) newMain.set(callers.get(i).index(), candidates.get(i));
        List<Waz.Skill> newHost = new ArrayList<>(location.host().getSkillList());
        newHost.add(expectedBody); newHost.add(expectedIce);
        int count = Math.addExact(location.host().getSkillList().size(), 2);
        if (newHost.size() != count || bodyIndex != count - 2 || iceIndex != count - 1)
            throw invalid("Private append positions do not match the candidate host");
        // All validation and candidate construction finish before publishing the three shared views.
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
        return new Location(context.getReboundWaz(), host, group, hostGroup, sourceBase, sourceEnd, rawMain, rawTama);
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
        } catch (IOException error) { throw new IllegalStateException("Souクールゲイン元署名の読取に失敗", error); }
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
        independent.getCommonProjectileResourceBundle().getWazByFileName().put("sou-cool-gain-guard.waz", expected);
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

    private static LocatedSkill findPrivate(Location location, String japaneseName, String englishName) {
        LocatedSkill found = null;
        for (int i = 0; i < location.host().getSkillList().size(); i++) {
            Waz.Skill skill = location.host().getSkillList().get(i);
            if (skill == null) continue;
            boolean japanese = japaneseName.equals(skill.getSkillNameJapanese()), english = englishName.equals(skill.getSkillNameEnglish());
            if (!japanese && !english) continue;
            if (!japanese || !english || found != null || i < location.sourceEnd())
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

    private static Waz.Skill buildBody(Waz.Skill prototype, int publicIceIndex, int privateIceIndex, int hostGroup) {
        Waz.Skill body = copy(prototype, Waz.Skill.class);
        body.setSkillNameJapanese(BODY_JP); body.setSkillNameEnglish(BODY_EN);
        var values = slot(body, 1, 35).getSkillInfoObjectList();
        if (values.size() != 2 || !(values.get(0) instanceof CEventVal reset)
                || !Integer.valueOf(0).equals(reset.getStartFrame()) || !Integer.valueOf(0).equals(reset.getEndFrame())
                || !Integer.valueOf(0).equals(reset.getInt1()) || !Integer.valueOf(0).equals(reset.getInt2())
                || !Integer.valueOf(0).equals(reset.getInt3()) || !Integer.valueOf(0).equals(reset.getInt4()))
            throw invalid("Phase1's original channel4-only downgrade is not the expected zero point");
        // BHE phase1 writes only channel4=1. Its downgraded scalar0 must not erase the charge accumulated by phase0.
        values.remove(0);
        if (slot(body, 0, 48).getSkillInfoObjectList().size() != 4) throw invalid("Body emission graph changed");
        for (int i = 0; i < 4; i++) {
            CEventWazaSelect reference = reference(effect(body, 0, i));
            if (!Integer.valueOf(hostGroup).equals(reference.getWazFileNo()) || !Integer.valueOf(publicIceIndex).equals(reference.getWazSequenceNo()))
                throw invalid("Body no longer emits the original public ice");
            reference.setWazSequenceNo(privateIceIndex);
        }
        // Phase0 +7, phase1 -1, phase2 scalar0 and its 20f exit remain the original payload.
        return body;
    }

    private static Waz.Skill buildIce(Waz.Skill prototype) {
        Waz.Skill ice = copy(prototype, Waz.Skill.class);
        ice.setSkillNameJapanese(ICE_JP); ice.setSkillNameEnglish(ICE_EN);
        for (int phase : List.of(5, 6)) {
            var events = slot(ice, phase, 71).getSkillInfoObjectList();
            if (events.size() != 2 || !(events.get(0) instanceof CEventChange signal) || !(events.get(1) instanceof CEventChange absent)
                    || signal.getBsdxInfoCollectionList1().size() != 1 || absent.getBsdxInfoCollectionList1().size() != 1)
                throw invalid("Ice return decision graph changed");
            var predicate = signal.getBsdxInfoCollectionList1().get(0);
            predicate.setTypeList(new ArrayList<>(phase == 5 ? List.of(2, 8, 7) : List.of(2, 0, 0, 2)));
            predicate.setParamList(new ArrayList<>(phase == 5 ? List.of() : List.of(0)));
            predicate.setIntList3(new ArrayList<>(phase == 5 ? List.of(0, 0) : List.of(2, 0)));
            absent.getBsdxInfoCollectionList1().get(0).setIntList3(new ArrayList<>(List.of(2, 0)));
            // Native PARENT_TURN is true even for NULL; test the direct parent NULL first to enter the existing phase7 fade.
            slot(ice, phase, 71).setSkillInfoObjectList(new ArrayList<>(List.of(absent, signal)));
        }
        return ice;
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
                .filter(w -> "C_GAIN".equals(w.getWeaponSequence())).toList();
        if (matches.size() != 1 || !Integer.valueOf(finalSkill).equals(matches.get(0).getWazSequence()))
            throw invalid("Sou MEKクールゲイン参照が最終Lv3と一致しません");
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
        node.forEach(SouCoolGainReturnProtocolFixer::removeMetadata);
    }

    private static <T> T copy(T source, Class<T> type) {
        try { return JSON.readValue(JSON.writeValueAsBytes(source), type); }
        catch (IOException error) { throw new IllegalStateException("Souクールゲインの独立コピーに失敗", error); }
    }

    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    private static IllegalStateException invalid(String reason) {
        return new IllegalStateException("Sou Cool Gain return protocol rejected: " + reason);
    }

    private record SourceSignature(String japanese, String english, int phases, int bytes, String sha256) { }
    private record RawSkill(int index, com.giga.nexas.dto.bhe.waz.Waz.Skill skill) { }
    private record LocatedSkill(int index, Waz.Skill skill) { }
    private record Location(Waz main, Waz host, WazaGroupGrp.WazaGroupEntry group, int hostGroup, int sourceBase, int sourceEnd,
                            com.giga.nexas.dto.bhe.waz.Waz rawMain, com.giga.nexas.dto.bhe.waz.Waz rawTama) { }
}
