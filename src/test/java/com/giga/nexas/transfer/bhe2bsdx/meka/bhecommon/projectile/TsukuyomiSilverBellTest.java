package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.generator.WazGenerator;
import com.giga.nexas.dto.bsdx.waz.parser.WazParser;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.convert.ConvertBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.cross.CrossRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.self.SelfRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Actual BHE source -> production prepare/append -> temporary BSDX binary -> parse. */
@Execution(ExecutionMode.SAME_THREAD)
class TsukuyomiSilverBellTest {
    private static final String CHARSET = "windows-31j";
    private static final Path BHE_ROOT = Path.of("src/main/resources/game/bhe");
    private static final Path DX_ROOT = Path.of("src/main/resources/game/bsdx");
    private final ObjectMapper json = new ObjectMapper();
    private final BheBinService bhe = new BheBinService();
    private final BsdxBinService dx = new BsdxBinService();

    @TempDir
    Path output;

    @Test
    void publicSeRebindingPreservesAllRawWazPayloads() throws Exception {
        TsukuyomiRawSourceBundle raw = loadSource();
        JsonNode before = json.valueToTree(raw.getCommonProjectileWazByFileName());
        new BheCommonProjectilePrepareStep().prepare(new TsukuyomiGraftRequest(), raw,
                loadBaseline(), new TsukuyomiConvertedBundle());
        JsonNode after = json.valueToTree(raw.getCommonProjectileWazByFileName());
        assertTrue(before.equals(after), () -> "公共SE重绑不得写回BHE源数组; first delta "
                + firstTreeDifference(before, after, ""));
    }

    @Test
    void initialFiringEntryIsReachableInsideItsOwnLoopAfterRealAppendAndBinaryReadback() throws Exception {
        verifyFiringEntry(0, 159);
    }

    @Test
    void pursuitFiringEntryIsReachableInsideItsOwnLoopAfterRealAppendAndBinaryReadback() throws Exception {
        verifyFiringEntry(1, 165);
    }

    private void verifyFiringEntry(int phase, int unreachableSourceFrame) throws Exception {
        Path sourcePath = resolve(BHE_ROOT.resolve("waz"), "tama03.waz");
        byte[] sourceBytes = Files.readAllBytes(sourcePath);
        TsukuyomiRawSourceBundle raw = loadSource();
        var sourceTama = raw.getCommonProjectileWazByFileName().get("tama03.waz");
        JsonNode sourceTree = json.valueToTree(sourceTama);
        int sourceIndex = findSourceSkill(sourceTama, "BIT44");
        assertEquals("◆月詠：銀鈴", sourceTama.getSkillList().get(sourceIndex).getSkillNameJapanese());
        var sourceSlot = sourceTama.getSkillList().get(sourceIndex).getPhasesInfo().get(phase)
                .getSkillUnitCollection().stream().filter(u -> Integer.valueOf(82).equals(u.getUnitQuantity()))
                .findFirst().orElseThrow();
        var sourceEntry = (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange)
                sourceSlot.getSkillInfoObjectList().get(1);
        var sourceLoop = sourceSlot.getSkillInfoUnknownList().get(0);
        assertEquals(unreachableSourceFrame, sourceEntry.getStartFrame());
        assertEquals(unreachableSourceFrame, sourceEntry.getEndFrame());
        assertEquals(List.of(0, 147, -1), List.of(sourceLoop.getStartFrame(), sourceLoop.getEndFrame(), sourceLoop.getInt1()));
        assertFalse(canEvaluate(sourceEntry.getStartFrame(), sourceEntry.getEndFrame(),
                sourceLoop.getStartFrame(), sourceLoop.getEndFrame()), "原始未完成銀鈴的入口在无限loop之外");

        TsukuyomiBsdxBaselineBundle baseline = loadBaseline();
        // Pure DX has no Tama03 host; the production append plan must resolve its Tama05 fallback.
        String originalHostName = BheCommonProjectileResources.commonProjectileHostWazFiles().get(3);
        Path originalHostPath = resolve(DX_ROOT.resolve("waz"), originalHostName);
        byte[] originalHostBytes = Files.readAllBytes(originalHostPath);
        Waz originalHost = findWaz(baseline, originalHostName);
        int originalSkillCount = originalHost.getSkillList().size();
        assertArrayEquals(originalHostBytes, write(originalHost, "original-host.waz"), "原DX夹具必须自身逐字节回环");

        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        TsukuyomiConvertedBundle converted = new TsukuyomiConvertedBundle();
        new BheCommonProjectilePrepareStep().prepare(request, raw, baseline, converted);
        var plan = converted.getCommonProjectileAppendPlan();
        String targetName = plan.getSourceWazIndexToTargetFileName().get(3);
        int base = plan.getSourceWazIndexToTargetSkillBase().get(3);
        assertEquals(originalSkillCount, base);
        Waz target = findWaz(baseline, targetName);
        assertEquals("BIT44", target.getSkillList().get(base + sourceIndex).getSkillNameEnglish());
        Waz convertedTama = converted.getCommonProjectileResourceBundle().getWazByFileName().get("tama03.waz");
        assertNotNull(convertedTama);
        Waz.Skill convertedSilver = convertedTama.getSkillList().get(sourceIndex);
        assertNotSame(convertedSilver, target.getSkillList().get(base + sourceIndex), "角色适配必须替换独立副本");
        SkillUnit convertedChange = convertedSilver.getPhasesInfo().get(phase).getSkillUnitCollection().stream()
                .filter(unit -> Integer.valueOf(71).equals(unit.getUnitQuantity())).findFirst().orElseThrow();
        assertEquals(unreachableSourceFrame, convertedChange.getSkillInfoObjectList().get(1).getStartFrame(),
                "已转换的源视图保留原窗口，不能与宿主定制共用Skill");
        assertEquals(unreachableSourceFrame, convertedChange.getSkillInfoObjectList().get(1).getEndFrame());
        byte[] generatedBytes = write(target, "appended-host.waz");
        com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.TsukuyomiSilverBellCustomizer.customizeAppended(baseline, plan);
        assertArrayEquals(generatedBytes, write(target, "repeated-host.waz"), "重复角色适配必须保持最终字节");
        Waz parsed = new WazParser().parse(generatedBytes, "appended-host.waz", CHARSET);

        // Independent raw/baseline fixtures run the full ordinary prepare sequence. SelfRedirect builds
        // the SE and append maps consumed by CrossRedirect; the wrapper's role adapter is not invoked.
        TsukuyomiRawSourceBundle controlRaw = loadSource();
        TsukuyomiBsdxBaselineBundle controlBaseline = loadBaseline();
        TsukuyomiConvertedBundle controls = new TsukuyomiConvertedBundle();
        new ConvertBheCommonProjectileResourcesStep().convert(controlRaw, controls);
        BheCommonProjectileAppendPlan controlPlan = new BheCommonProjectileAppendPlan();
        controls.setCommonProjectileAppendPlan(controlPlan);
        new SelfRedirectBheCommonProjectileResourcesStep().redirect(
                request, controlRaw, controlBaseline, controls, controlPlan);
        new CrossRedirectBheCommonProjectileResourcesStep().redirect(
                request, controlRaw, controlBaseline, controls, controlPlan);
        assertEquals(targetName, controlPlan.getSourceWazIndexToTargetFileName().get(3));
        assertEquals(base, controlPlan.getSourceWazIndexToTargetSkillBase().get(3));
        Waz ordinaryConversion = findWaz(controlBaseline, controlPlan.getSourceWazIndexToTargetFileName().get(3));
        for (String identity : List.of("BIT65", "BIT66", "BIT58")) {
            int controlIndex = findSourceSkill(sourceTama, identity);
            assertArrayEquals(writeSkill(ordinaryConversion.getSkillList().get(base + controlIndex), "expected-" + identity),
                    writeSkill(parsed.getSkillList().get(base + controlIndex), "actual-" + identity),
                    identity + "必须与未定制的正常转换逐字节一致");
        }
        assertArrayEquals(originalHostBytes, Arrays.copyOf(generatedBytes, originalHostBytes.length),
                "公共追加必须保留原DX整个WAZ前缀，不能按英文名修改原DX技能");
        assertArrayEquals(sourceBytes, Files.readAllBytes(sourcePath), "BHE原始文件只读");
        JsonNode sourceAfter = json.valueToTree(sourceTama);
        assertTrue(sourceTree.equals(sourceAfter), () -> "BHE源对象不能被转换或角色适配污染; first delta "
                + firstTreeDifference(sourceTree, sourceAfter, ""));

        Waz.Skill silverBell = parsed.getSkillList().get(base + sourceIndex);
        assertEquals(5, silverBell.getPhasesInfo().size());
        SkillUnit changeSlot = silverBell.getPhasesInfo().get(phase).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(71).equals(u.getUnitQuantity())).findFirst().orElseThrow();
        assertEquals(3, changeSlot.getSkillInfoObjectList().size());
        assertEquals(1, changeSlot.getSkillInfoUnknownList().size());
        SkillInfoUnknown loop = changeSlot.getSkillInfoUnknownList().get(0);
        assertEquals(List.of(0, 147, -1, 1, -2), List.of(loop.getStartFrame(), loop.getEndFrame(),
                loop.getInt1(), loop.getInt2(), loop.getInt3()), "原生CWorkLoop契约保持");
        CEventChange firing = (CEventChange) changeSlot.getSkillInfoObjectList().get(1);
        assertEquals(phase == 0 ? 1 : 2, firing.getFlag());
        assertEquals(0, firing.getInt1());
        assertEquals(phase == 0 ? 1 : 2, firing.getBsdxInfoCollectionList1().size());
        assertCondition(firing.getBsdxInfoCollectionList1().get(0), List.of(2, 3, 6, 2), List.of(300));
        if (phase == 1) assertCondition(firing.getBsdxInfoCollectionList1().get(1), List.of(2, 3, 4, 2), List.of(50));
        BsdxInfoCollection destination = firing.getBsdxInfoCollectionList2().get(0);
        assertEquals(16, destination.getInt1());
        assertEquals(phase == 0 ? List.of(0, 1) : List.of(0, 0), destination.getTypeList());
        assertEquals(phase == 0 ? List.of(2) : List.of(), destination.getParamList());
        assertTrue(destination.getIntList3().isEmpty());
        assertTrue(destination.getIntList4().isEmpty());
        int selectedPhase = destination.getTypeList().get(1) == 0 ? phase + 1 : destination.getParamList().get(0);
        assertEquals(2, selectedPhase, "两条已满足条件的射击入口都必须进入phase2");

        // Native 63A1A0/63ACF0 rewind this slot's clock; actor+15EC cannot make 159/165 reachable.
        // This finite domain check is independent of the converter and requires no surrogate binary parser.
        assertAll("BIT44 phase " + phase + " firing entry must be evaluable while its own 0..147 loop repeats",
                () -> assertEquals(0, firing.getStartFrame()),
                () -> assertEquals(147, firing.getEndFrame()),
                () -> assertTrue(canEvaluate(firing.getStartFrame(), firing.getEndFrame(),
                        loop.getStartFrame(), loop.getEndFrame()), "射击入口不能在slot71的无限loop时钟之外"));
    }

    private static void assertCondition(BsdxInfoCollection condition, List<Integer> types, List<Integer> values) {
        assertEquals(0, condition.getInt1());
        assertEquals(types, condition.getTypeList());
        assertEquals(values, condition.getParamList());
        assertEquals(List.of(0, 0), condition.getIntList3());
        assertEquals(List.of(11, 0), condition.getIntList4(), "保留OBJECT/PARENTROCK，不能误用OBJECTPOS索引");
        assertEquals(0, condition.getInt2());
    }

    private static boolean canEvaluate(int eventStart, int eventEnd, int loopStart, int loopEnd) {
        return IntStream.rangeClosed(loopStart, loopEnd).anyMatch(clock -> clock >= eventStart && clock <= eventEnd);
    }

    /** Keep the complete source-tree equality assertion, but never dump megabytes into Surefire. */
    private static String firstTreeDifference(JsonNode expected, JsonNode actual, String pointer) {
        if (expected.equals(actual)) return null;
        if (expected.isObject() && actual.isObject()) {
            var fields = expected.fieldNames();
            while (fields.hasNext()) {
                String name = fields.next();
                String child = pointer + "/" + name.replace("~", "~0").replace("/", "~1");
                if (!actual.has(name)) return child + " expected=present actual=missing";
                String difference = firstTreeDifference(expected.get(name), actual.get(name), child);
                if (difference != null) return difference;
            }
            var added = actual.fieldNames();
            while (added.hasNext()) {
                String name = added.next();
                if (!expected.has(name)) return pointer + "/" + name.replace("~", "~0").replace("/", "~1")
                        + " expected=missing actual=present";
            }
        } else if (expected.isArray() && actual.isArray()) {
            if (expected.size() != actual.size()) return pointer + " expectedSize=" + expected.size()
                    + " actualSize=" + actual.size();
            for (int i = 0; i < expected.size(); i++) {
                String difference = firstTreeDifference(expected.get(i), actual.get(i), pointer + "/" + i);
                if (difference != null) return difference;
            }
        }
        String before = expected.toString();
        String after = actual.toString();
        return pointer + " expected=" + before.substring(0, Math.min(before.length(), 160))
                + " actual=" + after.substring(0, Math.min(after.length(), 160));
    }

    private TsukuyomiRawSourceBundle loadSource() throws Exception {
        var raw = new TsukuyomiRawSourceBundle();
        raw.setWazaGroupGrp(readBhe("grp", "wazagroup.grp", com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.class));
        raw.setSpriteGroupGrp(readBhe("grp", "spritegroup.grp", com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.class));
        raw.setSeGroupGrp(readBhe("grp", "segroup.grp", com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.class));
        for (String name : BheCommonProjectileResources.commonProjectileWazFiles()) {
            raw.getCommonProjectileWazByFileName().put(name,
                    readBhe("waz", name, com.giga.nexas.dto.bhe.waz.Waz.class));
        }
        for (String name : BheCommonProjectileResources.commonProjectileSpmFiles()) {
            raw.getCommonProjectileSpmByFileName().put(name,
                    readBhe("spm", name, com.giga.nexas.dto.bhe.spm.Spm.class));
        }
        return raw;
    }

    private TsukuyomiBsdxBaselineBundle loadBaseline() throws Exception {
        var baseline = new TsukuyomiBsdxBaselineBundle();
        baseline.setWazaGroupGrp(readDx("grp", "WazaGroup.grp", WazaGroupGrp.class));
        baseline.setSpriteGroupGrp(readDx("grp", "SpriteGroup.grp", SpriteGroupGrp.class));
        baseline.setSeGroupGrp(readDx("grp", "SeGroup.grp", SeGroupGrp.class));
        baseline.setBatVoiceGrp(readDx("grp", "BatVoice.grp", BatVoiceGrp.class));
        baseline.setProgramMaterialGrp(readDx("grp", "ProgramMaterial.grp", ProgramMaterialGrp.class));
        baseline.setMapGroupGrp(readDx("grp", "MapGroup.grp", MapGroupGrp.class));
        CommonProjectileTestSupport.loadBsdxHostCommonResources(baseline);
        return baseline;
    }

    private <T> T readBhe(String dir, String name, Class<T> type) throws Exception {
        return type.cast(bhe.parse(resolve(BHE_ROOT.resolve(dir), name).toString(), CHARSET).getData());
    }

    private <T> T readDx(String dir, String name, Class<T> type) throws Exception {
        return type.cast(dx.parse(resolve(DX_ROOT.resolve(dir), name).toString(), CHARSET).getData());
    }

    private byte[] write(Waz waz, String name) throws Exception {
        Path target = output.resolve(name);
        new WazGenerator().generate(target.toString(), waz, CHARSET);
        return Files.readAllBytes(target);
    }

    private byte[] writeSkill(Waz.Skill skill, String name) throws Exception {
        Waz single = new Waz(name);
        single.getSkillList().add(skill);
        return write(single, name + ".waz");
    }

    private static Waz findWaz(TsukuyomiBsdxBaselineBundle baseline, String name) {
        return baseline.getWazByFileName().entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name)).findFirst().orElseThrow().getValue();
    }

    private static int findSourceSkill(com.giga.nexas.dto.bhe.waz.Waz waz, String english) {
        List<Integer> matches = IntStream.range(0, waz.getSkillList().size())
                .filter(i -> english.equals(waz.getSkillList().get(i).getSkillNameEnglish())).boxed().toList();
        assertEquals(1, matches.size(), "源技能英文身份必须唯一: " + english);
        return matches.get(0);
    }

    private static Path resolve(Path directory, String name) throws Exception {
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Missing binary fixture: " + directory.resolve(name)));
        }
    }
}
