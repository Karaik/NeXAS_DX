package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.grp.groupmap.*;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectilePrepareStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.CommonProjectileTestSupport;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term.BheInfoCollectionObjectGraphRewriter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek.BheToBsdxMekConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.resolve.BheResourceIndexResolver;
import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Real source/public-role regression; bounded carrier births do not prove public520 liveness or runtime hits. */
@Execution(ExecutionMode.SAME_THREAD)
class SouFlareSeekerSingleTargetTest {
    private static final ObjectMapper JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String CHARSET = "windows-31j";
    private static final Path BHE = Path.of("src/main/resources/game/bhe");
    private static final Path DX = Path.of("src/main/resources/game/bsdx");
    private static final List<String> JAPANESE = List.of("フレアシーカーLv1", "フレアシーカーLv2", "フレアシーカーLv3");
    private static final List<String> ENGLISH = List.of("E_MUSI01", "E_MUSI02", "E_MUSI03");
    private static final List<String> CARRIER_JP = List.of("★蒼：フレアシーカーLv1", "★蒼：フレアシーカーLv2", "★蒼：フレアシーカーLv3");
    private static final List<String> CARRIER_EN = List.of("0348", "0349", "0335");
    private static final List<Integer> FIRE_FRAME = List.of(18, 14, 10);
    private static final String CHILD_JP = "★蒼：フレアシーカー：虫";
    private static final String CHILD_EN = "0334";

    @TempDir Path output;
    private int sequence;

    @Test
    void allThreeOriginalMainPointsSelectOnePrivateLv3CarrierWithoutChangingAnyOtherPayload() throws Exception {
        Fixture f = fixture();
        List<JsonNode> originalPackets = IntStream.range(0, 3).mapToObj(level -> payload(effect(family(f.main(), level), 0, 0))).toList();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill carrier = privateCarrier(f);
        for (int level = 0; level < 3; level++) {
            CEventEffect packet = effect(family(f.main(), level), 0, 0);
            assertEquals(List.of(FIRE_FRAME.get(level), FIRE_FRAME.get(level)), frames(packet));
            assertEquals(16, packet.getInt3());
            assertEquals(List.of(0, 1, 0, 0, 1, 0), six(inner(packet, 4, CEventValRandom.class)));
            assertEquals(List.of(0, 0, 0, 0), four(inner(packet, 36, CEventVal.class)), "retain the existing lowered caller scalar, including hidden Lv1/Lv2");
            ObjectNode expected = (ObjectNode) originalPackets.get(level).deepCopy();
            for (JsonNode unit : expected.path("ceventEffectUnitList"))
                if (unit.path("unitSlotNum").asInt() == 3) ((ObjectNode) unit.path("data")).put("wazSequenceNo", privateIndex(f));
            assertPayloadEquals(expected, packet, "only main caller WAZ sequence changes at level " + level);
        }
        assertEquals(5, carrier.getPhaseQuantity());
        assertEquals(List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0), List.of(3, 0), List.of(4, 0)), suffix(carrier));
        assertSourceFilesUnchanged(f);
    }

    @Test
    void completeMainGraphDiffersOnlyAtTheThreeExplicitCarrierReferences() throws Exception {
        Fixture f = fixture();
        Waz expected = expectedMain(f, f.host().getSkillList().size());
        Map<String, String> preserved = preservedGraphs(f);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertPayloadEquals(expected, f.main(), "complete main graph with exactly three private carrier reference deltas");
        assertEquals(preserved, preservedGraphs(f));
        assertSourceFilesUnchanged(f);
    }

    @Test
    void oneIndependentFullLv3CarrierCloneKeepsEveryExistingHostByteAndOnlyChangesItsDangerousPacket() throws Exception {
        Fixture f = fixture();
        int beforeCount = f.host().getSkillList().size();
        JsonNode prefix = payload(f.host().getSkillList()), groups = payload(f.context().getBaseline().getWazaGroupGrp());
        Waz.Skill prototype = copy(publicCarrier(f), Waz.Skill.class);
        Map<String, String> preserved = preservedGraphs(f);
        byte[] beforeBytes = writeWaz(f.host(), "flare-prefix-before");
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill carrier = privateCarrier(f);
        assertEquals(beforeCount + 1, f.host().getSkillList().size());
        assertEquals(beforeCount, privateIndex(f));
        assertPayloadEquals(prefix, f.host().getSkillList().subList(0, beforeCount), "all native, public517..520 and old private Counter/Snow prefix skills");
        assertArrayEquals(beforeBytes, Arrays.copyOf(writeWaz(f.host(), "flare-prefix-after"), beforeBytes.length));
        assertPayloadEquals(prototype, publicCarrier(f), "public Lv3 carrier stays unchanged");
        assertPayloadEquals(expectedCarrier(prototype, carrier), carrier, "independent full Lv3 clone with only identity and P2 dangerous packet deltas");
        assertNotSame(publicCarrier(f), carrier);
        for (int phase = 0; phase < 5; phase++) {
            assertNotSame(publicCarrier(f).getPhasesInfo().get(phase), carrier.getPhasesInfo().get(phase));
            assertNotSame(publicCarrier(f).getPhasesInfo().get(phase).getSkillUnitCollection(), carrier.getPhasesInfo().get(phase).getSkillUnitCollection());
        }
        assertFalse(carrier.getSkillNameJapanese().isBlank()); assertFalse(carrier.getSkillNameEnglish().isBlank());
        assertFalse(CARRIER_JP.contains(carrier.getSkillNameJapanese())); assertFalse(CARRIER_EN.contains(carrier.getSkillNameEnglish()));
        for (int i = 0; i < groups.path("wazaList").size(); i++) {
            JsonNode expected = groups.path("wazaList").get(i).deepCopy();
            if (i == f.hostGroup()) ((ObjectNode) expected).put("param", beforeCount + 1);
            assertPayloadEquals(expected, f.context().getBaseline().getWazaGroupGrp().getWazaList().get(i), "WazaGroup entry " + i);
        }
        assertEquals(groups.path("wazaList").size(), f.context().getBaseline().getWazaGroupGrp().getWazaList().size());
        assertEquals(preserved, preservedGraphs(f));
        assertEquals(211, f.context().getReboundMek().getMekWeaponInfoMap().size());
        assertEquals(211, f.context().getReboundMek().getMekMaterialBlock().getTrailingEntries().size());
        assertSourceFilesUnchanged(f);
    }

    @Test
    void dangerousChildShotIsOnePointBeforeTheOriginalLoopAndRetainsScalarOneAndTheNaturalCarrierExit() throws Exception {
        Fixture f = fixture();
        assertOriginalExitBytes(f);
        JsonNode publicChild = payload(f.host().getSkillList().get(f.publicChild()));
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill carrier = privateCarrier(f);
        CEventEffect packet = effect(carrier, 2, 1);
        assertEquals(List.of(0, 0), frames(packet));
        assertEquals(4, packet.getInt3(), "clear only 0x20 and retain 0x4");
        assertEquals(List.of(0, 1, 0, 0, 1, 0), six(inner(packet, 4, CEventValRandom.class)));
        assertEquals(List.of(0, 29), frames(inner(packet, 4, CEventValRandom.class)), "retain all original interpolation/marker inner windows");
        assertEquals(List.of(0, 0, 0, 1), four(inner(packet, 36, CEventVal.class)), "the child still receives original scalar1");
        assertEquals(List.of(0, 29), frames(inner(packet, 36, CEventVal.class)));
        assertEquals(List.of(f.hostGroup(), f.publicChild()), reference(packet));
        assertEquals(List.of(List.of(1, 30, -1, 1, -2)), loops(unit(carrier, 2, 48)));
        for (SkillInfoUnknown loop : unit(carrier, 2, 48).getSkillInfoUnknownList())
            assertTrue(packet.getEndFrame() < loop.getStartFrame(), "0f never overlaps the original 1..30 Effect FF");
        SkillUnit counter = unit(carrier, 2, 35);
        assertEquals(List.of(0, 0), frames(counter.getSkillInfoObjectList().get(0)));
        assertEquals(List.of(0, 0, 0, 0), four(assertInstanceOf(CEventVal.class, counter.getSkillInfoObjectList().get(0))));
        assertEquals(List.of(1, 1), frames(counter.getSkillInfoObjectList().get(1)));
        assertEquals(List.of(0, 0, 1, -1), four(assertInstanceOf(CEventVal.class, counter.getSkillInfoObjectList().get(1))));
        assertEquals(List.of(List.of(1, 1, -1, 1, -2)), loops(counter));
        CEventChange exit = change(carrier, 2, 0);
        assertEquals(List.of(1, 30), frames(exit));
        assertEquals(List.of(2, 0, 0, 2), exit.getBsdxInfoCollectionList1().get(0).getTypeList());
        assertEquals(List.of(0), exit.getBsdxInfoCollectionList1().get(0).getParamList());
        assertEquals(List.of(4), exit.getBsdxInfoCollectionList2().get(0).getParamList());
        CEventChange finish = change(carrier, 4, 0);
        assertEquals(List.of(50, 50), frames(finish));
        assertEquals(List.of(0), finish.getBsdxInfoCollectionList1().get(0).getTypeList());
        assertEquals(List.of(0, 0), finish.getBsdxInfoCollectionList2().get(0).getTypeList());
        assertPayloadEquals(publicChild, f.host().getSkillList().get(f.publicChild()), "520 lifecycle and conditional exits remain an unchanged public dependency, not a new bounded-lifetime claim");
        assertSourceFilesUnchanged(f);
    }

    @Test
    void firstInstallUsesTheDynamicTailAndVerifiedRepeatAllowsLaterPrivateAppends() throws Exception {
        Fixture f = fixture();
        Waz.Skill earlier = copy(publicCarrier(f), Waz.Skill.class);
        earlier.setSkillNameJapanese("試験：先行私有弾"); earlier.setSkillNameEnglish("TEST_EARLIER_FLARE_TAIL");
        f.host().getSkillList().add(earlier); syncHostCount(f);
        int expectedIndex = f.host().getSkillList().size();
        Waz expected = expectedMain(f, expectedIndex);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill carrier = privateCarrier(f);
        assertEquals(expectedIndex, privateIndex(f));
        assertPayloadEquals(expected, f.main(), "dynamic main references");
        Waz.Skill later = copy(publicCarrier(f), Waz.Skill.class);
        later.setSkillNameJapanese("試験：後続私有弾"); later.setSkillNameEnglish("TEST_LATER_FLARE_TAIL");
        f.host().getSkillList().add(later); syncHostCount(f);
        List<Waz.Skill> mainList = f.main().getSkillList(), hostList = f.host().getSkillList();
        List<Waz.Skill> mainSkills = new ArrayList<>(mainList), hostSkills = new ArrayList<>(hostList);
        Set<Object> identities = Collections.newSetFromMap(new IdentityHashMap<>()); collectIdentities(carrier, identities);
        Snapshot before = snapshot(f);
        byte[] mainBytes = writeWaz(f.main(), "flare-repeat-main"), hostBytes = writeWaz(f.host(), "flare-repeat-host");
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertSame(mainList, f.main().getSkillList()); assertSame(hostList, f.host().getSkillList());
        for (int i = 0; i < mainSkills.size(); i++) assertSame(mainSkills.get(i), mainList.get(i));
        for (int i = 0; i < hostSkills.size(); i++) assertSame(hostSkills.get(i), hostList.get(i));
        assertSame(carrier, privateCarrier(f)); assertEquals(expectedIndex, privateIndex(f));
        Set<Object> after = Collections.newSetFromMap(new IdentityHashMap<>()); collectIdentities(carrier, after);
        for (Object identity : identities) assertTrue(after.contains(identity));
        assertEquals(before, snapshot(f));
        assertArrayEquals(mainBytes, writeWaz(f.main(), "flare-repeat-main-again"));
        assertArrayEquals(hostBytes, writeWaz(f.host(), "flare-repeat-host-again"));
        assertSourceFilesUnchanged(f);
    }

    @Test
    void sourceOrTargetDriftRejectsWithoutPublishingAnyCallerCarrierHostOrGroupChange() throws Exception {
        for (Consumer<Fixture> corrupt : List.<Consumer<Fixture>>of(
                f -> rawFamily(f, 2).setSkillNameJapanese("別名：フレアシーカーLv3"),
                f -> rawEffect(rawFamily(f, 2), 0, 0).setStartFrame(11),
                f -> rawCount(rawEffect(rawCarrier(f), 2, 1)).setInt5(998),
                f -> rawEffect(rawCarrier(f), 2, 1).setInt3(4),
                f -> rawChange(rawCarrier(f), 4, 0).setStartFrame(49),
                f -> referenceObject(effect(family(f.main(), 2), 0, 0)).setWazSequenceNo(f.publicCarriers().get(1)),
                f -> effect(family(f.main(), 2), 0, 0).setInt3(0),
                f -> {
                    CEventSe se = inner(effect(family(f.main(), 2), 0, 0), 15, CEventSe.class);
                    byte[] wrongItem = se.getByteDataList().get(0).clone();
                    ByteBuffer.wrap(wrongItem).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 12);
                    se.setByteDataList(new ArrayList<>(List.of(wrongItem)));
                },
                f -> f.context().getGrpAppendPlan().getSourceSeItemIndexToTargetIndexByGroup().get(16).remove(4),
                f -> inner(effect(publicCarrier(f), 2, 1), 4, CEventValRandom.class).setInt2(998),
                f -> inner(effect(publicCarrier(f), 2, 1), 36, CEventVal.class).setInt4(0),
                f -> unit(publicCarrier(f), 2, 48).getSkillInfoUnknownList().get(0).setStartFrame(0),
                f -> change(publicCarrier(f), 2, 0).getBsdxInfoCollectionList1().get(0).setParamList(new ArrayList<>(List.of(1))),
                f -> change(publicCarrier(f), 4, 0).setStartFrame(49),
                f -> family(f.main(), 2).getSkillSuffixList().get(1).setInt1(0),
                f -> unit(publicCarrier(f), 2, 48).setSkillInfoUnknownList(null))) {
            Fixture f = fixture(); corrupt.accept(f); assertRejectedWithoutExtraWrites(f);
        }
    }

    @Test
    void halfInstallDuplicateFamilyOrTamperedInstalledCarrierIsRejectedAtomically() throws Exception {
        Fixture first = fixture();
        referenceObject(effect(family(first.main(), 2), 0, 0)).setWazSequenceNo(first.host().getSkillList().size());
        assertRejectedWithoutExtraWrites(first);
        Fixture duplicate = fixture();
        duplicate.main().getSkillList().add(copy(family(duplicate.main(), 2), Waz.Skill.class)); syncMainCount(duplicate);
        assertRejectedWithoutExtraWrites(duplicate);
        Fixture installed = fixture();
        SouCustomizer.INSTANCE.customizeBeforeWrite(installed.context());
        Waz.Skill carrier = privateCarrier(installed);
        inner(effect(carrier, 2, 1), 4, CEventValRandom.class).setInt5(2);
        assertRejectedWithoutExtraWrites(installed);
        assertSame(carrier, privateCarrier(installed));
    }

    @Test
    void completeMainHostAndGroupSurviveTwoBinaryPassesAndParsedIdentityRepeat() throws Exception {
        Fixture f = fixture();
        Waz expected = expectedMain(f, f.host().getSkillList().size());
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill carrier = privateCarrier(f);
        assertPayloadEquals(expected, f.main(), "missing flare main repair fails before binary work");
        assertPayloadEquals(expectedCarrier(publicCarrier(f), carrier), carrier, "complete independent private carrier");
        BsdxBinService service = new BsdxBinService();
        Waz[] original = {f.main(), f.host()}, parsed = new Waz[2];
        for (int index = 0; index < 2; index++) {
            Waz expectedReadback = expectedHeaderReadback(original[index]);
            Path first = output.resolve("flare-" + index + "-pass1.waz"), second = output.resolve("flare-" + index + "-pass2.waz");
            service.generate(first.toString(), original[index], CHARSET);
            Waz parsed1 = (Waz) service.parse(first.toString(), CHARSET).getData();
            assertPayloadEquals(expectedReadback, parsed1, "first full binary readback graph " + index);
            service.generate(second.toString(), parsed1, CHARSET);
            parsed[index] = (Waz) service.parse(second.toString(), CHARSET).getData();
            assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second));
            assertPayloadEquals(expectedReadback, parsed[index], "second full binary readback graph " + index);
        }
        Path group1 = output.resolve("flare-group-pass1/WazaGroup.grp");
        Path group2 = output.resolve("flare-group-pass2/WazaGroup.grp");
        service.generate(group1.toString(), f.context().getBaseline().getWazaGroupGrp(), CHARSET);
        WazaGroupGrp parsedGroup = (WazaGroupGrp) service.parse(group1.toString(), CHARSET).getData();
        service.generate(group2.toString(), parsedGroup, CHARSET);
        assertArrayEquals(Files.readAllBytes(group1), Files.readAllBytes(group2));
        assertEquals(parsed[1].getSkillList().size(), parsedGroup.getWazaList().get(f.hostGroup()).getParam());
        f.context().setReboundWaz(parsed[0]); f.context().getBaseline().getWazByFileName().put("sou.waz", parsed[0]);
        String hostKey = f.context().getBaseline().getWazByFileName().keySet().stream().filter(f.hostName()::equalsIgnoreCase).findFirst().orElseThrow();
        f.context().getBaseline().getWazByFileName().put(hostKey, parsed[1]);
        f.context().getBaseline().setWazaGroupGrp(parsedGroup);
        List<Waz.Skill> sameMain = f.main().getSkillList(), sameHost = f.host().getSkillList();
        Waz.Skill sameCarrier = privateCarrier(f); Snapshot before = snapshot(f);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertSame(sameMain, f.main().getSkillList()); assertSame(sameHost, f.host().getSkillList()); assertSame(sameCarrier, privateCarrier(f));
        assertEquals(before, snapshot(f));
        assertOriginalExitBytes(f); assertSourceFilesUnchanged(f);
    }

    private Fixture fixture() throws Exception {
        BheBinService bhe = new BheBinService();
        Path mekPath = BHE.resolve("mek/sou/sou.mek"), mainPath = BHE.resolve("waz/sou/sou.waz");
        Map<Path, byte[]> sourceFiles = new LinkedHashMap<>();
        for (Path p : List.of(mekPath, mainPath, resolve(BHE.resolve("waz"), "tama05.waz"), resolve(DX.resolve("waz"), "Tama05.waz")))
            sourceFiles.put(p, Files.readAllBytes(p));
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) bhe.parse(mekPath.toString(), CHARSET).getData();
        var rawMain = (com.giga.nexas.dto.bhe.waz.Waz) bhe.parse(mainPath.toString(), CHARSET).getData();
        TsukuyomiRawSourceBundle raw = loadCommonSource(bhe);
        raw.setTsukuyomiMek(rawMek); raw.getWazByFileName().put("sou.waz", rawMain);
        TsukuyomiBsdxBaselineBundle baseline = loadBaseline();
        TsukuyomiConvertedBundle prepared = new TsukuyomiConvertedBundle();
        new BheCommonProjectilePrepareStep().prepare(new TsukuyomiGraftRequest(), raw, baseline, prepared);
        var plan = prepared.getCommonProjectileAppendPlan();
        Mek mek = new BheToBsdxMekConverter().convert(rawMek);
        Waz main = new BheToBsdxWazConverter().convert(rawMain);
        TsukuyomiPackageBundle selected = new TsukuyomiPackageBundle();
        selected.setTsukuyomiMek(mek); selected.getWazByFileName().put("sou.waz", copy(main, Waz.class));
        TsukuyomiGrpAppendPlan privatePlan = new TsukuyomiGrpAppendPlan();
        int mainGroup = baseline.getWazaGroupGrp().getWazaList().size();
        privatePlan.setWazaGroupIndex(mainGroup);
        privatePlan.getSourceWazGroupIndexToTargetIndex().put(rawMek.getMekBasicInfo().getWazFileSequence(), mainGroup);
        // Declare the actual nonidentity private-SE reference boundary, without pretending this fixture delivers audio assets.
        privatePlan.getSourceSeGroupIndexToTargetIndex().put(16, 46);
        privatePlan.getSourceSeItemIndexToTargetIndexByGroup().put(16, new LinkedHashMap<>(Map.of(4, 11)));
        rebindWazaSelects(main, new BheResourceIndexResolver(plan, privatePlan), Collections.newSetFromMap(new IdentityHashMap<>()));
        new BheInfoCollectionObjectGraphRewriter().rewrite(main, "SouFlareSeekerSingleTargetTest main");
        mek.getMekBasicInfo().setWazFileSequence(mainGroup);
        WazaGroupGrp.WazaGroupEntry entry = new WazaGroupGrp.WazaGroupEntry();
        entry.setExistFlag(1); entry.setWazaName("sou"); entry.setWazaCodeName("SOU"); entry.setWazaDisplayName("蒼");
        entry.setParam(main.getSkillList().size()); baseline.getWazaGroupGrp().getWazaList().add(entry);
        baseline.getWazByFileName().put("sou.waz", main);
        privatePlan.getTargetWazSkillCountByGroupIndex().put(mainGroup, main.getSkillList().size());
        FollowupGraftRequest request = new FollowupGraftRequest();
        request.setMekaCodeName("SOU"); request.setWazCodeName("SOU"); request.setWazFileName("sou.waz");
        request.setMekFileName("Sou.mek"); request.setSpriteFileName("sou.spm");
        TsukuyomiConvertedBundle currentSou = new TsukuyomiConvertedBundle();
        currentSou.setRawSourceBundle(raw); currentSou.setCommonProjectileAppendPlan(plan);
        currentSou.setPreparedBaselineBundle(baseline); currentSou.setSelectedResourceBundle(selected);
        FollowupMekaContext context = new FollowupMekaContext();
        context.setRequest(request); context.setBaseline(baseline); context.setConvertedBundle(currentSou);
        context.setSelectedPackage(selected); context.setGrpAppendPlan(privatePlan); context.setReboundMek(mek); context.setReboundWaz(main);
        // Isolate the other repairs; Flare Seeker first runs through the public entry in each test.
        SouSkillGlitchFixer.INSTANCE.fix(context);
        SouHumanCannonCounterFixer.fix(context);
        SouCursedBladeSingleTargetFixer.fix(context);
        SouSnowMoonSingleTargetFixer.fix(context);
        SouCoolGainReturnProtocolFixer.fix(context);
        SouBoostProcterDashContinuationFixer.fix(context);
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama05.waz");
        var rawTama = raw.getCommonProjectileWazByFileName().get("tama05.waz");
        String hostName = plan.getSourceWazIndexToTargetFileName().get(sourceGroup);
        int hostGroup = plan.getSourceWazIndexToTargetIndex().get(sourceGroup), base = plan.getSourceWazIndexToTargetSkillBase().get(sourceGroup);
        List<Integer> carriers = IntStream.range(0, 3).mapToObj(level -> base + sourceIndex(rawTama, CARRIER_JP.get(level), CARRIER_EN.get(level))).toList();
        int sourceCarrier = sourceIndex(rawTama, CARRIER_JP.get(2), CARRIER_EN.get(2));
        int child = base + sourceIndex(rawTama, CHILD_JP, CHILD_EN);
        Fixture f = new Fixture(context, raw, mainGroup, hostName, hostGroup, carriers, child, sourceCarrier, sourceFiles);
        for (int level = 0; level < 3; level++) {
            Waz.Skill skill = family(main, level);
            assertEquals(2, skill.getPhaseQuantity()); assertEquals(List.of(List.of(0, 0), List.of(1, 0)), suffix(skill));
            assertEquals(1, unit(skill, 0, 48).getSkillInfoObjectList().size());
            CEventEffect packet = effect(skill, 0, 0);
            assertEquals(List.of(FIRE_FRAME.get(level), FIRE_FRAME.get(level)), frames(packet));
            assertEquals(List.of(1, 0, 16, 0, -1, -1, -1, 0, 1, 0, 0), eleven(packet));
            assertEquals(List.of(f.hostGroup(), carriers.get(level)), reference(packet));
            assertEquals(List.of(0, 1, 0, 0, 1, 0), six(inner(packet, 4, CEventValRandom.class)));
            CEventSe targetSe = inner(packet, 15, CEventSe.class);
            assertEquals(1, targetSe.getByteDataList().size());
            var targetPair = ByteBuffer.wrap(targetSe.getByteDataList().get(0)).order(ByteOrder.LITTLE_ENDIAN);
            assertEquals(46, targetPair.getInt(0)); assertEquals(11, targetPair.getInt(4));
            var rawSe = (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventSe) rawEffect(rawFamily(f, level), 0, 0)
                    .getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(15).equals(u.getUnitSlotNum())).findFirst().orElseThrow().getData();
            var sourcePair = ByteBuffer.wrap(rawSe.getByteDataList().get(0)).order(ByteOrder.LITTLE_ENDIAN);
            assertEquals(16, sourcePair.getInt(0)); assertEquals(4, sourcePair.getInt(4));
        }
        assertEquals(5, publicCarrier(f).getPhaseQuantity());
        assertEquals(8, f.host().getSkillList().get(child).getPhaseQuantity());
        assertEquals(CHILD_EN, f.host().getSkillList().get(child).getSkillNameEnglish());
        CEventEffect dangerous = effect(publicCarrier(f), 2, 1);
        assertEquals(List.of(1, 30), frames(dangerous));
        assertEquals(List.of(1, 0, 36, 15, -1, -1, -1, 0, 1, 0, 0), eleven(dangerous));
        assertEquals(List.of(0, 999, 0, 0, 999, 0), six(inner(dangerous, 4, CEventValRandom.class)));
        assertEquals(List.of(0, 0, 0, 1), four(inner(dangerous, 36, CEventVal.class)));
        assertOriginalExitBytes(f); assertSourceFilesUnchanged(f);
        return f;
    }

    private static Waz expectedMain(Fixture f, int privateIndex) {
        Waz expected = copy(f.main(), Waz.class);
        for (int level = 0; level < 3; level++) referenceObject(effect(family(expected, level), 0, 0)).setWazSequenceNo(privateIndex);
        return expected;
    }

    private static Waz.Skill expectedCarrier(Waz.Skill prototype, Waz.Skill actual) {
        Waz.Skill expected = copy(prototype, Waz.Skill.class);
        expected.setSkillNameJapanese(actual.getSkillNameJapanese()); expected.setSkillNameEnglish(actual.getSkillNameEnglish());
        CEventEffect dangerous = effect(expected, 2, 1);
        dangerous.setStartFrame(0); dangerous.setEndFrame(0); dangerous.setInt3(4);
        CEventValRandom count = inner(dangerous, 4, CEventValRandom.class); count.setInt2(1); count.setInt5(1);
        return expected;
    }

    private static Waz expectedHeaderReadback(Waz original) {
        Waz expected = copy(original, Waz.class);
        for (Waz.Skill skill : expected.getSkillList()) {
            // Only the independently audited flag1 zero-phase shell NUL names change on binary readback.
            if (Integer.valueOf(0).equals(skill.getPhaseQuantity()) && skill.getPhasesInfo().isEmpty() && skill.getSkillSuffixList().isEmpty()
                    && !skill.isEmpty() && (skill.getSkillNameJapanese() == null || skill.getSkillNameJapanese().isEmpty())
                    && (skill.getSkillNameEnglish() == null || skill.getSkillNameEnglish().isEmpty())) {
                if (skill.getSkillNameJapanese() == null) skill.setSkillNameJapanese("");
                if (skill.getSkillNameEnglish() == null) skill.setSkillNameEnglish("");
            }
        }
        return expected;
    }

    private static int privateIndex(Fixture f) {
        Set<Integer> indices = new HashSet<>();
        for (int level = 0; level < 3; level++) {
            var reference = referenceObject(effect(family(f.main(), level), 0, 0));
            assertEquals(f.hostGroup(), reference.getWazFileNo()); indices.add(reference.getWazSequenceNo());
        }
        assertEquals(1, indices.size(), "all three main callers must use one private Lv3 carrier");
        int index = indices.iterator().next(), group = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama05.waz");
        var plan = f.context().getConvertedBundle().getCommonProjectileAppendPlan();
        assertTrue(index >= plan.getSourceWazIndexToTargetSkillBase().get(group) + plan.getSourceWazIndexToTargetSkillCount().get(group),
                "private carrier remains outside the original append-plan source slice");
        assertTrue(index < f.host().getSkillList().size()); return index;
    }

    private static Waz.Skill publicCarrier(Fixture f) { return f.host().getSkillList().get(f.publicCarriers().get(2)); }
    private static Waz.Skill privateCarrier(Fixture f) { return f.host().getSkillList().get(privateIndex(f)); }

    private static Waz.Skill family(Waz main, int level) {
        var matches = main.getSkillList().stream().filter(Objects::nonNull)
                .filter(s -> JAPANESE.get(level).equals(s.getSkillNameJapanese()) && ENGLISH.get(level).equals(s.getSkillNameEnglish())).toList();
        assertEquals(1, matches.size(), "unique exact Flare Seeker identity " + ENGLISH.get(level)); return matches.get(0);
    }

    private static SkillUnit unit(Waz.Skill skill, int phase, int number) {
        var matches = skill.getPhasesInfo().get(phase).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(number).equals(u.getUnitQuantity())).toList();
        assertEquals(1, matches.size()); return matches.get(0);
    }

    private static CEventEffect effect(Waz.Skill skill, int phase, int ordinal) { return assertInstanceOf(CEventEffect.class, unit(skill, phase, 48).getSkillInfoObjectList().get(ordinal)); }
    private static CEventChange change(Waz.Skill skill, int phase, int ordinal) { return assertInstanceOf(CEventChange.class, unit(skill, phase, 71).getSkillInfoObjectList().get(ordinal)); }
    private static <T> T inner(CEventEffect packet, int number, Class<T> type) {
        var units = packet.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(number).equals(u.getUnitSlotNum())).toList();
        assertEquals(1, units.size()); assertEquals(1, units.get(0).getBuffer()); return assertInstanceOf(type, units.get(0).getData());
    }
    private static CEventWazaSelect referenceObject(CEventEffect packet) { return inner(packet, 3, CEventWazaSelect.class); }
    private static List<Integer> reference(CEventEffect packet) { var v = referenceObject(packet); return List.of(v.getWazFileNo(), v.getWazSequenceNo()); }
    private static List<Integer> frames(SkillInfoObject event) { return List.of(event.getStartFrame(), event.getEndFrame()); }
    private static List<Integer> four(CEventVal v) { return List.of(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4()); }
    private static List<Integer> six(CEventValRandom v) { return List.of(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6()); }
    private static List<Integer> eleven(CEventEffect v) { return List.of(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6(), v.getInt7(), v.getInt8(), v.getInt9(), v.getInt10(), v.getInt11()); }
    private static List<List<Integer>> suffix(Waz.Skill s) { return s.getSkillSuffixList().stream().map(v -> List.of(v.getInt1(), v.getInt2())).toList(); }
    private static List<List<Integer>> loops(SkillUnit slot) { return slot.getSkillInfoUnknownList().stream().map(v -> List.of(v.getStartFrame(), v.getEndFrame(), v.getInt1(), v.getInt2(), v.getInt3())).toList(); }

    private static int sourceIndex(com.giga.nexas.dto.bhe.waz.Waz source, String japanese, String english) {
        var matches = IntStream.range(0, source.getSkillList().size()).filter(i -> {
            var s = source.getSkillList().get(i); return s != null && japanese.equals(s.getSkillNameJapanese()) && english.equals(s.getSkillNameEnglish());
        }).boxed().toList(); assertEquals(1, matches.size()); return matches.get(0);
    }
    private static com.giga.nexas.dto.bhe.waz.Waz.Skill rawFamily(Fixture f, int level) {
        var raw = f.raw().getWazByFileName().get("sou.waz"); return raw.getSkillList().get(sourceIndex(raw, JAPANESE.get(level), ENGLISH.get(level)));
    }
    private static com.giga.nexas.dto.bhe.waz.Waz.Skill rawCarrier(Fixture f) { return f.raw().getCommonProjectileWazByFileName().get("tama05.waz").getSkillList().get(f.sourceCarrier()); }
    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect rawEffect(com.giga.nexas.dto.bhe.waz.Waz.Skill s, int phase, int ordinal) {
        return (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect) s.getPhasesInfo().get(phase).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(54).equals(u.getUnitQuantity())).findFirst().orElseThrow().getSkillInfoObjectList().get(ordinal);
    }
    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventValRandom rawCount(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect e) {
        return (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventValRandom) e.getCeventEffectUnitList().stream()
                .filter(u -> Integer.valueOf(4).equals(u.getUnitSlotNum())).findFirst().orElseThrow().getData();
    }
    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange rawChange(com.giga.nexas.dto.bhe.waz.Waz.Skill s, int phase, int ordinal) {
        return (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange) s.getPhasesInfo().get(phase).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(82).equals(u.getUnitQuantity())).findFirst().orElseThrow().getSkillInfoObjectList().get(ordinal);
    }

    private static void assertOriginalExitBytes(Fixture f) {
        byte[] bytes = f.sourceFiles().entrySet().stream().filter(e -> e.getKey().startsWith(BHE) && e.getKey().getFileName().toString().equalsIgnoreCase("tama05.waz")).findFirst().orElseThrow().getValue();
        var carrier = rawCarrier(f);
        byte[] exit = encodedExit(1, 30, List.of(2, 0, 0, 2), List.of(0), List.of(0, 0), List.of(0, 1), List.of(4));
        int offset = rawChange(carrier, 2, 0).getOffset();
        assertArrayEquals(exit, Arrays.copyOfRange(bytes, offset, offset + exit.length));
        byte[] finish = encodedExit(50, 50, List.of(0), List.of(), List.of(), List.of(0, 0), List.of());
        offset = rawChange(carrier, 4, 0).getOffset();
        assertArrayEquals(finish, Arrays.copyOfRange(bytes, offset, offset + finish.length));
    }
    private static byte[] encodedExit(int start, int end, List<Integer> types, List<Integer> params, List<Integer> objects, List<Integer> targetTypes, List<Integer> targetParams) {
        List<Integer> words = new ArrayList<>(List.of(start, end, 1)); appendCollection(words, 0, types, params, objects); appendCollection(words, 16, targetTypes, targetParams, List.of()); words.add(0);
        ByteBuffer bytes = ByteBuffer.allocate(words.size() * 4).order(ByteOrder.LITTLE_ENDIAN); words.forEach(bytes::putInt); return bytes.array();
    }
    private static void appendCollection(List<Integer> w, int root, List<Integer> t, List<Integer> p, List<Integer> o) {
        w.add(root); w.add(t.size()); w.addAll(t); w.add(p.size()); w.addAll(p); w.add(o.size()); w.addAll(o); w.add(0); w.add(0);
    }
    private static void syncHostCount(Fixture f) { f.context().getBaseline().getWazaGroupGrp().getWazaList().get(f.hostGroup()).setParam(f.host().getSkillList().size()); }
    private static void syncMainCount(Fixture f) {
        f.context().getBaseline().getWazaGroupGrp().getWazaList().get(f.mainGroup()).setParam(f.main().getSkillList().size());
        f.context().getGrpAppendPlan().getTargetWazSkillCountByGroupIndex().put(f.mainGroup(), f.main().getSkillList().size());
    }
    private static Map<String, String> preservedGraphs(Fixture f) throws Exception {
        Map<String, String> result = new LinkedHashMap<>(); result.put("source", digest(f.raw())); result.put("mek", digest(f.context().getReboundMek()));
        result.put("selected", digest(f.context().getSelectedPackage())); result.put("common-plan", digest(f.context().getConvertedBundle().getCommonProjectileAppendPlan()));
        result.put("private-plan", digest(f.context().getGrpAppendPlan()));
        for (var e : f.context().getBaseline().getWazByFileName().entrySet())
            if (!"sou.waz".equalsIgnoreCase(e.getKey()) && !f.hostName().equalsIgnoreCase(e.getKey())) result.put("public:" + e.getKey(), digest(e.getValue()));
        return result;
    }
    private static Snapshot snapshot(Fixture f) throws Exception { return new Snapshot(digest(f.main()), digest(f.host()), digest(f.context().getBaseline().getWazaGroupGrp()), preservedGraphs(f)); }
    private static void assertRejectedWithoutExtraWrites(Fixture f) throws Exception {
        Snapshot before = snapshot(f); List<Waz.Skill> main = f.main().getSkillList(), host = f.host().getSkillList();
        assertThrows(IllegalStateException.class, () -> SouCustomizer.INSTANCE.customizeBeforeWrite(f.context()), "drift rejects before publishing any caller/carrier/count");
        assertEquals(before, snapshot(f)); assertSame(main, f.main().getSkillList()); assertSame(host, f.host().getSkillList()); assertSourceFilesUnchanged(f);
    }
    private static void assertSourceFilesUnchanged(Fixture f) throws Exception {
        for (var e : f.sourceFiles().entrySet()) assertArrayEquals(e.getValue(), Files.readAllBytes(e.getKey()), "read-only source " + e.getKey());
    }
    private byte[] writeWaz(Waz waz, String label) throws Exception {
        Path path = output.resolve(label + "-" + sequence++ + ".waz"); new BsdxBinService().generate(path.toString(), waz, CHARSET); return Files.readAllBytes(path);
    }

    private static TsukuyomiRawSourceBundle loadCommonSource(BheBinService service) throws Exception {
        TsukuyomiRawSourceBundle raw = new TsukuyomiRawSourceBundle();
        raw.setWazaGroupGrp((com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp) service.parse(resolve(BHE.resolve("grp"), "WazaGroup.grp").toString(), CHARSET).getData());
        raw.setSpriteGroupGrp((com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp) service.parse(resolve(BHE.resolve("grp"), "SpriteGroup.grp").toString(), CHARSET).getData());
        raw.setSeGroupGrp((com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp) service.parse(resolve(BHE.resolve("grp"), "SeGroup.grp").toString(), CHARSET).getData());
        for (String name : BheCommonProjectileResources.commonProjectileWazFiles())
            raw.getCommonProjectileWazByFileName().put(name, (com.giga.nexas.dto.bhe.waz.Waz) service.parse(resolve(BHE.resolve("waz"), name).toString(), CHARSET).getData());
        for (String name : BheCommonProjectileResources.commonProjectileSpmFiles())
            raw.getCommonProjectileSpmByFileName().put(name, (com.giga.nexas.dto.bhe.spm.Spm) service.parse(resolve(BHE.resolve("spm"), name).toString(), CHARSET).getData());
        return raw;
    }

    private static TsukuyomiBsdxBaselineBundle loadBaseline() throws Exception {
        BsdxBinService service = new BsdxBinService();
        TsukuyomiBsdxBaselineBundle b = new TsukuyomiBsdxBaselineBundle();
        b.setWazaGroupGrp((WazaGroupGrp) service.parse(resolve(DX.resolve("grp"), "WazaGroup.grp").toString(), CHARSET).getData());
        b.setSpriteGroupGrp((SpriteGroupGrp) service.parse(resolve(DX.resolve("grp"), "SpriteGroup.grp").toString(), CHARSET).getData());
        b.setSeGroupGrp((SeGroupGrp) service.parse(resolve(DX.resolve("grp"), "SeGroup.grp").toString(), CHARSET).getData());
        b.setBatVoiceGrp((BatVoiceGrp) service.parse(resolve(DX.resolve("grp"), "BatVoice.grp").toString(), CHARSET).getData());
        b.setProgramMaterialGrp((ProgramMaterialGrp) service.parse(resolve(DX.resolve("grp"), "ProgramMaterial.grp").toString(), CHARSET).getData());
        b.setMapGroupGrp((MapGroupGrp) service.parse(resolve(DX.resolve("grp"), "MapGroup.grp").toString(), CHARSET).getData());
        CommonProjectileTestSupport.loadBsdxHostCommonResources(b);
        return b;
    }

    private static void rebindWazaSelects(Object node, BheResourceIndexResolver resolver, Set<Object> seen) throws Exception {
        if (node == null || node instanceof Number || node instanceof String || node instanceof Boolean || !seen.add(node)) return;
        if (node instanceof CEventSe se && se.getByteDataList() != null) {
            List<byte[]> rebound = null;
            for (int i = 0; i < se.getByteDataList().size(); i++) {
                byte[] source = se.getByteDataList().get(i);
                if (source == null || source.length < 8) continue;
                var pair = java.nio.ByteBuffer.wrap(source).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                if (pair.getInt(0) != 16 || pair.getInt(4) != 4) continue;
                var target = resolver.resolveSe(16, 4);
                byte[] independent = source.clone();
                var bytes = java.nio.ByteBuffer.wrap(independent).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                bytes.putInt(0, target.targetGroupIndex()); bytes.putInt(4, target.targetItemIndex());
                if (rebound == null) rebound = new ArrayList<>(se.getByteDataList());
                rebound.set(i, independent);
            }
            if (rebound != null) se.setByteDataList(rebound);
            return;
        }
        if (node instanceof CEventWazaSelect ref && ref.getWazFileNo() != null && ref.getWazFileNo() >= 0) {
            int group = ref.getWazFileNo();
            ref.setWazSequenceNo(resolver.resolveWazSkillIndex(group, ref.getWazSequenceNo()));
            ref.setWazFileNo(resolver.resolveWazGroupIndex(group));
            return;
        }
        if (node instanceof Collection<?> values) { for (Object value : values) rebindWazaSelects(value, resolver, seen); return; }
        if (node instanceof Map<?, ?> values) { for (Object value : values.values()) rebindWazaSelects(value, resolver, seen); return; }
        if (!node.getClass().getName().startsWith("com.giga.nexas.dto.")) return;
        for (Class<?> type = node.getClass(); type != null; type = type.getSuperclass()) for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            field.setAccessible(true); rebindWazaSelects(field.get(node), resolver, seen);
        }
    }

    private static void collectIdentities(Object node, Set<Object> seen) throws Exception {
        if (node == null || node instanceof Number || node instanceof String || node instanceof Boolean || !seen.add(node)) return;
        if (node instanceof Collection<?> values) { for (Object value : values) collectIdentities(value, seen); return; }
        if (!node.getClass().getName().startsWith("com.giga.nexas.dto.")) return;
        for (Class<?> type = node.getClass(); type != null; type = type.getSuperclass()) for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            field.setAccessible(true); collectIdentities(field.get(node), seen);
        }
    }

    private static Path resolve(Path directory, String name) throws Exception {
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).filter(p -> name.equalsIgnoreCase(p.getFileName().toString())).findFirst().orElseThrow();
        }
    }

    private static <T> T copy(T value, Class<T> type) {
        try { return JSON.readValue(JSON.writeValueAsBytes(value), type); }
        catch (java.io.IOException error) { throw new IllegalStateException("independent test graph copy failed", error); }
    }

    private static String digest(Object value) throws Exception {
        // Preserved views must retain even source parser metadata; only expected-vs-readback WAZ comparisons normalize annotations.
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(value)));
    }

    private static void assertPayloadEquals(Object expected, Object actual, String label) throws Exception {
        JsonNode expectedGraph = payload(expected), actualGraph = payload(actual);
        // Keep exact complete-graph equality, but never attach the full WAZ as JUnit expected/actual diagnostic values.
        if (!expectedGraph.equals(actualGraph)) {
            List<String> differences = new ArrayList<>();
            collectDifferences(expectedGraph, actualGraph, "$", differences);
            Path directory = Path.of("target/sou134-single-target-design-20261030/graph-diff");
            Files.createDirectories(directory);
            String stem = label.replaceAll("[^A-Za-z0-9]+", "-") + "-" + System.currentTimeMillis();
            Path expectedFile = directory.resolve(stem + ".expected.json");
            Path actualFile = directory.resolve(stem + ".actual.json");
            Path differenceFile = directory.resolve(stem + ".diff.txt");
            String summary = label + "; expected SHA-256=" + digest(expectedGraph) + "; actual SHA-256=" + digest(actualGraph)
                    + "\nfirst differences (at most 10):\n" + String.join("\n", differences);
            // The deliberate missing-feature RED needs only bounded diagnostics. Save full graphs
            // for an unexpected binary readback mismatch, where independent serialization audit needs them.
            if (label.contains("binary readback")) {
                writeDiagnostic(expectedFile, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(expectedGraph));
                writeDiagnostic(actualFile, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(actualGraph));
                summary += "\nexpected graph: " + expectedFile + "\nactual graph: " + actualFile;
            }
            writeDiagnostic(differenceFile, summary);
            fail(summary + "\ndifference report: " + differenceFile);
        }
    }

    private static void collectDifferences(JsonNode expected, JsonNode actual, String path, List<String> differences) {
        if (differences.size() >= 10 || Objects.equals(expected, actual)) return;
        if (expected == null || actual == null || expected.getNodeType() != actual.getNodeType()) {
            differences.add(path + ": expected " + shortNode(expected) + ", actual " + shortNode(actual));
            return;
        }
        if (expected.isObject()) {
            Set<String> fields = new TreeSet<>();
            expected.fieldNames().forEachRemaining(fields::add);
            actual.fieldNames().forEachRemaining(fields::add);
            for (String field : fields) {
                collectDifferences(expected.get(field), actual.get(field), path + "." + field, differences);
                if (differences.size() >= 10) return;
            }
        } else if (expected.isArray()) {
            if (expected.size() != actual.size())
                differences.add(path + ".length: expected " + expected.size() + ", actual " + actual.size());
            for (int i = 0; i < Math.min(expected.size(), actual.size()) && differences.size() < 10; i++)
                collectDifferences(expected.get(i), actual.get(i), path + "[" + i + "]", differences);
        } else {
            differences.add(path + ": expected " + shortNode(expected) + ", actual " + shortNode(actual));
        }
    }

    private static String shortNode(JsonNode node) {
        if (node == null) return "<missing>";
        if (node.isObject()) return "<object fields=" + node.size() + ">";
        if (node.isArray()) return "<array size=" + node.size() + ">";
        String value = node.toString();
        return value.length() <= 120 ? value : value.substring(0, 117) + "...";
    }

    private static void writeDiagnostic(Path path, String text) throws Exception {
        Files.writeString(path, text.replace("\r\n", "\n").replace("\r", "\n").stripTrailing().replace("\n", "\r\n") + "\r\n");
    }

    private static JsonNode payload(Object value) {
        JsonNode tree = JSON.valueToTree(value);
        stripMetadata(tree);
        return tree;
    }

    private static void stripMetadata(JsonNode node) {
        // File labels and parser annotations are not serialized WAZ behavior.
        if (node instanceof ObjectNode object) object.remove(List.of("offset", "length", "slotNum", "unitDescription", "description", "fileName", "extensionName"));
        node.forEach(SouFlareSeekerSingleTargetTest::stripMetadata);
    }

    private record Snapshot(String main, String host, String groups, Map<String, String> preserved) { }
    private record Fixture(FollowupMekaContext context, TsukuyomiRawSourceBundle raw, int mainGroup, String hostName, int hostGroup,
                           List<Integer> publicCarriers, int publicChild, int sourceCarrier, Map<Path, byte[]> sourceFiles) {
        Waz main() { return context.getReboundWaz(); }
        Waz host() { return context.getBaseline().getWazByFileName().entrySet().stream().filter(e -> hostName.equalsIgnoreCase(e.getKey())).findFirst().orElseThrow().getValue(); }
    }
}
