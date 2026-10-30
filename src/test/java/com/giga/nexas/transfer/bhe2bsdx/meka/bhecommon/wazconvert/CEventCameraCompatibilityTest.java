package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.wazconvert;

import com.giga.nexas.dto.bhe.BheInfoCollection;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventTerm;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventVal;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventCamera;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.convert.ConvertBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term.BheInfoCollectionObjectGraphRewriter;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Camera consumers require an object; ordinary POS/OBJECT_VAL still means a captured point. */
@Execution(ExecutionMode.SAME_THREAD)
class CEventCameraCompatibilityTest {

    private static final String CHARSET = "windows-31j";
    private static final Path BHE_WAZ = Path.of("src/main/resources/game/bhe/waz");
    private static final Path DX_WAZ = Path.of("src/main/resources/game/bsdx/waz");
    private static final String SOU_SHA256 = "4a06693a9bb33127aa60aa04f2460fd44beadc08a7725876793820f5a23bfc43";
    // Independent raw survey: 19 occurrences, identified by skill name and phase, not a production matcher.
    private static final Set<String> SOU_POINT_CAMERAS = Set.of(
            "138:KAWARA01/0/0", "138:KAWARA01/1/0", "139:KAWARA02/0/0", "139:KAWARA02/1/0",
            "140:KAWARA03/0/0", "140:KAWARA03/1/0", "141:KOUMORI01/1/0", "142:KOUMORI02/1/0",
            "143:KOUMORI03/1/0", "229:JUSOUTOU01/1/0", "230:JUSOUTOU02/1/0", "231:JUSOUTOU03/1/0",
            "459:KISS01/0/0", "459:KISS01/1/0", "460:KISS02/0/0", "460:KISS02/1/0", "461:KISS03/0/0",
            "461:KISS03/1/0", "611:EX_KAISERKICK/2/0");
    private static final Map<String, Integer> COMMON_POSITION_COUNTS = Map.of(
            "bomb.waz", 0, "effect.waz", 0, "laser.waz", 0, "tama01.waz", 6,
            "tama02.waz", 0, "tama03.waz", 0, "tama04.waz", 9, "tama05.waz", 4);

    @TempDir Path output;

    @Test
    void directCameraConversionRestoresPositionTypeBeforeAdaptingAndIsolatesSharedSource() throws Exception {
        var source = pointCamera((short) 1);
        BheInfoCollection shared = sourceTerm(source, 0).getBheInfoCollectionList().get(0);
        CEventTerm ordinaryPosition = term(7, shared);
        byte[] before = bytes(source);

        CEventCamera target = convertCamera(source);
        var inner = targetTerm(target, 0);
        BsdxInfoCollection actual = targetCollection(target);
        assertEquals(5, inner.getTypeId(), "BHE type7 must be restored to DX position type5");
        assertEquals(5, inner.getSlotNum());
        assertEquals(31, target.getTypeId());
        assertEquals(66, target.getSlotNum());
        assertObjectPosition(actual);
        assertEquals(20, target.getStartFrame());
        assertEquals(43, target.getEndFrame());
        assertEquals((short) 1, target.getShort1());
        assertEquals(2, inner.getStartFrame());
        assertEquals(23, inner.getEndFrame());

        assertNotSame(shared, actual);
        assertNotSame(shared.getTypeList(), actual.getTypeList());
        assertNotSame(shared.getParamList(), actual.getParamList());
        assertNotSame(shared.getIntList3(), actual.getIntList3());
        assertNotSame(shared.getIntList4(), actual.getIntList4());
        actual.getTypeList().set(0, 99);
        actual.getParamList().add(100);
        actual.getIntList3().set(0, 99);
        actual.getIntList4().add(100);
        assertEquals(List.of(3), shared.getTypeList());
        assertEquals(List.of(3), ordinaryPosition.getBheInfoCollectionList().get(0).getTypeList());
        assertArrayEquals(before, bytes(source));
    }

    @Test
    void directionZoomHeightAndFlagUnitsKeepTheirPayloadsWhenPositionIsAdapted() throws Exception {
        var source = pointCamera((short) 0);
        addUnit(source, 1, 1, term(11, pointCollection()));
        addUnit(source, 2, 1, value(0, 11, 22, 33, 44));
        addUnit(source, 3, 1, value(0, 55, 66, 77, 88));
        addUnit(source, 4, 1, term(-1, pointCollection()));
        byte[] before = bytes(source);

        CEventCamera target = convertCamera(source);
        assertEquals(5, target.getCeventCameraUnitList().size());
        assertObjectPosition(targetCollection(target));
        int[] types = {5, 9, 0, 0, -1};
        for (int slot = 0; slot < 5; slot++) {
            var unit = targetUnit(target, slot);
            assertEquals(types[slot], unit.getData().getTypeId(), "restored slot " + slot);
            assertEquals(slot, unit.getUnitSlotNum());
            assertEquals(1, unit.getBuffer());
            if (slot > 0) assertArrayEquals(bytes(sourceUnit(source, slot).getData()), bytes(unit.getData()));
        }
        assertEquals(List.of(3), targetTerm(target, 1).getBsdxInfoCollectionList().get(0).getTypeList());
        assertEquals(List.of(3), targetTerm(target, 4).getBsdxInfoCollectionList().get(0).getTypeList());
        assertArrayEquals(before, bytes(source));
    }

    @Test
    void collectionNearMissesKeepAllFieldsWithoutBroadPosRewriting() {
        Map<String, Consumer<BheInfoCollection>> changes = new LinkedHashMap<>();
        changes.put("different root", c -> c.setInt1(19));
        changes.put("already object", c -> c.setTypeList(new ArrayList<>(List.of(1))));
        changes.put("compound term", c -> c.setTypeList(new ArrayList<>(List.of(3, 0))));
        changes.put("empty mode", c -> c.getTypeList().clear());
        changes.put("parameters", c -> c.getParamList().add(7));
        changes.put("other entity", c -> c.getIntList3().set(0, 1));
        changes.put("anchor", c -> c.getIntList3().set(1, 1));
        changes.put("extra aux", c -> c.getIntList3().add(0));
        changes.put("missing aux", c -> c.getIntList3().clear());
        changes.put("aux4", c -> c.getIntList4().addAll(List.of(0, 0)));
        changes.put("int2", c -> c.setInt2(1));
        changes.put("null root", c -> c.setInt1(null));
        changes.put("null int2", c -> c.setInt2(null));
        changes.put("null modes", c -> c.setTypeList(null));
        changes.put("null params", c -> c.setParamList(null));
        changes.put("null aux", c -> c.setIntList3(null));
        changes.put("null aux4", c -> c.setIntList4(null));
        for (var change : changes.entrySet()) {
            var source = pointCamera((short) 1);
            BheInfoCollection collection = sourceTerm(source, 0).getBheInfoCollectionList().get(0);
            change.getValue().accept(collection);
            assertCollectionEqual(collection, targetCollection(convertCamera(source)), change.getKey());
        }
    }

    @Test
    void multipleSourceCollectionsDoNotPromoteTheFirstPointIntoAnObject() {
        var source = pointCamera((short) 0);
        sourceTerm(source, 0).getBheInfoCollectionList().add(pointCollection());
        CEventCamera target = convertCamera(source);
        // Existing CEventTerm mapper emits one collection; this malformed DTO is outside the narrow recipe.
        assertEquals(1, targetTerm(target, 0).getBsdxInfoCollectionList().size());
        assertEquals(List.of(3), targetCollection(target).getTypeList());
        assertEquals(2, sourceTerm(source, 0).getBheInfoCollectionList().size());
    }

    @Test
    void disabledAbsentOrUnidentifiedPositionCannotAdaptAnotherCameraUnit() throws Exception {
        for (int variant = 0; variant < 3; variant++) {
            var source = pointCamera((short) 1);
            if (variant == 0) sourceUnit(source, 0).setBuffer(0);
            if (variant == 1) source.getCeventCameraUnitList().clear();
            if (variant == 2) sourceUnit(source, 0).setUnitSlotNum(null);
            addUnit(source, 1, 1, term(11, pointCollection()));
            CEventCamera target = convertCamera(source);
            assertNull(target.getCeventCameraUnitList().stream()
                    .filter(u -> Integer.valueOf(0).equals(u.getUnitSlotNum())).findFirst().orElse(null));
            assertEquals(List.of(3), targetTerm(target, 1).getBsdxInfoCollectionList().get(0).getTypeList());
            assertArrayEquals(bytes(sourceTerm(source, 1)), bytes(targetTerm(target, 1)));
        }
    }

    @Test
    void followupAndTsukuyomiPublicPathsAdaptCameraButPreserveOrdinaryPosition() throws Exception {
        var source = syntheticWaz(pointCamera((short) 1), true);
        byte[] before = bheWazBytes(source, "synthetic-before.waz");
        for (String route : List.of("followup", "tsukuyomi")) {
            Waz target = convert(source, route);
            assertObjectPosition(targetCollection(cameras(target).get("0:CAMERA_FIXTURE/0/0")));
            var ordinary = (com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm)
                    target.getSkillList().get(0).getPhasesInfo().get(0).getSkillUnitCollection().stream()
                            .filter(u -> Integer.valueOf(11).equals(u.getUnitQuantity())).findFirst().orElseThrow()
                            .getSkillInfoObjectList().get(0);
            assertEquals(List.of(3), ordinary.getBsdxInfoCollectionList().get(0).getTypeList());
            new BheInfoCollectionObjectGraphRewriter().rewrite(target, route);
            assertObjectPosition(targetCollection(cameras(target).get("0:CAMERA_FIXTURE/0/0")));
            assertEquals(List.of(3), ordinary.getBsdxInfoCollectionList().get(0).getTypeList());
            assertDxRoundTrip(target, route + "-synthetic");
        }
        assertArrayEquals(before, bheWazBytes(source, "synthetic-after.waz"));
    }

    @Test
    void actualCommonWrapperReachesTheSharedCameraAdapter() throws Exception {
        var source = syntheticWaz(pointCamera((short) 0), false);
        TsukuyomiRawSourceBundle raw = new TsukuyomiRawSourceBundle();
        raw.getCommonProjectileWazByFileName().put("tama05.waz", source);
        TsukuyomiConvertedBundle converted = new TsukuyomiConvertedBundle();
        new ConvertBheCommonProjectileResourcesStep().convert(raw, converted);
        Waz target = converted.getCommonProjectileResourceBundle().getWazByFileName().get("tama05.waz");
        assertNotNull(target);
        assertObjectPosition(targetCollection(cameras(target).get("0:CAMERA_FIXTURE/0/0")));
        new BheInfoCollectionObjectGraphRewriter().rewrite(target, "common wrapper");
        assertObjectPosition(targetCollection(cameras(target).get("0:CAMERA_FIXTURE/0/0")));
        assertDxRoundTrip(target, "common-synthetic");
        assertEquals(List.of(3), sourceTerm(sourceCameras(source).get("0:CAMERA_FIXTURE/0/0"), 0)
                .getBheInfoCollectionList().get(0).getTypeList());
    }

    @Test
    void eventBinaryRoundTripsChangeExactlyTheObjectModeWordAndKeepFrameAndShortFlags() throws Exception {
        for (short flag : new short[]{0, 1}) {
            var source = pointCamera(flag);
            byte[] original = bytes(source);
            assertEquals(74, original.length);
            var sourceParsed = new com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera(37);
            sourceParsed.readInfo(new BinaryReader(original, CHARSET));
            assertArrayEquals(original, bytes(sourceParsed));

            CEventCamera target = convertCamera(sourceParsed);
            byte[] expected = original.clone();
            ByteBuffer.wrap(expected).order(ByteOrder.LITTLE_ENDIAN).putInt(30, 1);
            assertArrayEquals(expected, bytes(target), "only inner POS mode at byte30 changes");
            new BheInfoCollectionObjectGraphRewriter().rewrite(target, "event roundtrip");
            assertArrayEquals(expected, bytes(target));
            CEventCamera targetParsed = new CEventCamera(31);
            targetParsed.readInfo(new BinaryReader(bytes(target), CHARSET));
            assertArrayEquals(expected, bytes(targetParsed));
            assertObjectPosition(targetCollection(targetParsed));
            assertEquals(flag, targetParsed.getShort1());
            assertEquals(20, targetParsed.getStartFrame());
            assertEquals(43, targetParsed.getEndFrame());
            assertEquals(2, targetTerm(targetParsed, 0).getStartFrame());
            assertEquals(23, targetTerm(targetParsed, 0).getEndFrame());
        }
    }

    @Test
    void realSouNineteenCamerasChangeOnlyTheirModeWordsThroughBothPublicConverters() throws Exception {
        Path path = resolve(BHE_WAZ, "sou.waz");
        byte[] rawBytes = Files.readAllBytes(path);
        assertEquals(SOU_SHA256, sha256(rawBytes));
        var source = parseBhe(path);
        Map<String, com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera> sourceEvents = sourceCameras(source);
        assertTrue(sourceEvents.keySet().containsAll(SOU_POINT_CAMERAS));
        assertArrayEquals(rawBytes, bheWazBytes(source, "sou-source-before.waz"));

        for (String route : List.of("followup", "tsukuyomi")) {
            Waz target = convert(source, route);
            Map<String, CEventCamera> targetEvents = cameras(target);
            assertEquals(sourceEvents.keySet(), targetEvents.keySet());
            int changed = 0;
            int shortZero = 0;
            for (var row : sourceEvents.entrySet()) {
                String key = row.getKey();
                var sourceCamera = row.getValue();
                CEventCamera targetCamera = targetEvents.get(key);
                byte[] original = bytes(sourceCamera);
                assertArrayEquals(original, Arrays.copyOfRange(rawBytes, sourceCamera.getOffset(),
                        sourceCamera.getOffset() + original.length), "source raw camera " + key);
                byte[] expected = original.clone();
                if (SOU_POINT_CAMERAS.contains(key)) {
                    assertPointPosition(sourceTerm(sourceCamera, 0).getBheInfoCollectionList().get(0));
                    int relativeMode = sourceTerm(sourceCamera, 0).getOffset() - sourceCamera.getOffset() + 16;
                    assertEquals(3, ByteBuffer.wrap(original).order(ByteOrder.LITTLE_ENDIAN).getInt(relativeMode));
                    ByteBuffer.wrap(expected).order(ByteOrder.LITTLE_ENDIAN).putInt(relativeMode, 1);
                    assertObjectPosition(targetCollection(targetCamera));
                    assertEquals(5, targetTerm(targetCamera, 0).getTypeId());
                    assertEquals(31, targetCamera.getTypeId());
                    assertEquals(66, targetCamera.getSlotNum());
                    changed++;
                    if (sourceCamera.getShort1() == 0) shortZero++;
                }
                assertArrayEquals(expected, bytes(targetCamera), route + " camera payload " + key);
                if (SOU_POINT_CAMERAS.contains(key)) {
                    new BheInfoCollectionObjectGraphRewriter().rewrite(targetCamera, route + "/" + key);
                    assertArrayEquals(expected, bytes(targetCamera), "TermRewrite must preserve camera recipe " + key);
                }
            }
            assertEquals(19, changed);
            assertEquals(7, shortZero);
            assertDxRoundTrip(target, route + "-real-sou");
            assertArrayEquals(rawBytes, bheWazBytes(source, route + "-source-after.waz"));
            assertArrayEquals(rawBytes, Files.readAllBytes(path));
        }
    }

    @Test
    void realBheSouGeneratorParserRoundTripKeepsTheCapturedPointSourceIntact() throws Exception {
        Path sourcePath = resolve(BHE_WAZ, "sou.waz");
        byte[] original = Files.readAllBytes(sourcePath);
        var source = parseBhe(sourcePath);
        byte[] generated = bheWazBytes(source, "bhe-sou-roundtrip.waz");
        assertArrayEquals(original, generated);
        var reparsed = parseBhe(output.resolve("bhe-sou-roundtrip.waz"));
        assertArrayEquals(original, bheWazBytes(reparsed, "bhe-sou-roundtrip-again.waz"));
        for (String key : SOU_POINT_CAMERAS) {
            assertPointPosition(sourceTerm(sourceCameras(reparsed).get(key), 0).getBheInfoCollectionList().get(0));
        }
        assertArrayEquals(original, Files.readAllBytes(sourcePath));
    }

    @Test
    void originalDxKouKeepsAll232LegalObjectCameraTargetsAndExactBinaryRoundTrip() throws Exception {
        Path path = resolve(DX_WAZ, "kou.waz");
        byte[] original = Files.readAllBytes(path);
        Waz kou = (Waz) new BsdxBinService().parse(path.toString(), CHARSET).getData();
        int positions = 0;
        int directObject = 0;
        int compoundObject = 0;
        for (var row : cameras(kou).entrySet()) {
            CEventCamera camera = row.getValue();
            byte[] payload = bytes(camera);
            assertArrayEquals(payload, Arrays.copyOfRange(original, camera.getOffset(), camera.getOffset() + payload.length));
            var position = camera.getCeventCameraUnitList().stream()
                    .filter(u -> Integer.valueOf(0).equals(u.getUnitSlotNum())).findFirst().orElse(null);
            if (position == null) continue;
            BsdxInfoCollection collection = targetCollection(camera);
            assertEquals(20, collection.getInt1());
            assertEquals(1, collection.getTypeList().get(0), row.getKey());
            if (collection.getTypeList().equals(List.of(1))) directObject++;
            else if (collection.getTypeList().equals(List.of(1, 0))) compoundObject++;
            else fail("unreviewed original DX object camera " + row.getKey());
            positions++;
        }
        assertEquals(232, positions);
        assertEquals(229, directObject);
        assertEquals(3, compoundObject);
        assertDxRoundTrip(kou, "original-kou");
        assertArrayEquals(original, Files.readAllBytes(output.resolve("original-kou.waz")));
        assertArrayEquals(original, Files.readAllBytes(path));
    }

    @Test
    void realEightCommonResourcesHaveNoMatchingPointCamerasAndKeepEveryCameraPayload() throws Exception {
        TsukuyomiRawSourceBundle raw = new TsukuyomiRawSourceBundle();
        Map<String, byte[]> original = new LinkedHashMap<>();
        for (String name : COMMON_POSITION_COUNTS.keySet()) {
            Path path = resolve(BHE_WAZ, name);
            original.put(name, Files.readAllBytes(path));
            raw.getCommonProjectileWazByFileName().put(name, parseBhe(path));
        }
        TsukuyomiConvertedBundle converted = new TsukuyomiConvertedBundle();
        new ConvertBheCommonProjectileResourcesStep().convert(raw, converted);
        assertEquals(8, converted.getCommonProjectileResourceBundle().getWazByFileName().size());
        for (var file : raw.getCommonProjectileWazByFileName().entrySet()) {
            int positions = 0;
            var targetEvents = cameras(converted.getCommonProjectileResourceBundle().getWazByFileName().get(file.getKey()));
            var sourceEvents = sourceCameras(file.getValue());
            assertEquals(sourceEvents.keySet(), targetEvents.keySet());
            for (var row : sourceEvents.entrySet()) {
                var camera = row.getValue();
                var position = camera.getCeventCameraUnitList().stream()
                        .filter(u -> Integer.valueOf(0).equals(u.getUnitSlotNum())).findFirst().orElse(null);
                if (position != null) {
                    positions++;
                    assertNotEquals(List.of(3), sourceTerm(camera, 0).getBheInfoCollectionList().get(0).getTypeList());
                }
                byte[] payload = bytes(camera);
                assertArrayEquals(payload, Arrays.copyOfRange(original.get(file.getKey()), camera.getOffset(), camera.getOffset() + payload.length));
                assertArrayEquals(payload, bytes(targetEvents.get(row.getKey())), file.getKey() + "/" + row.getKey());
            }
            assertEquals(COMMON_POSITION_COUNTS.get(file.getKey()), positions, file.getKey());
            assertArrayEquals(original.get(file.getKey()), Files.readAllBytes(resolve(BHE_WAZ, file.getKey())));
        }
    }

    private CEventCamera convertCamera(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera source) {
        CEventCamera target = new CEventCamera(31);
        target.setSlotNum(66);
        target.setStartFrame(source.getStartFrame());
        target.setEndFrame(source.getEndFrame());
        target.setShort1(source.getShort1());
        target.transBheCEventCameraToBsdx(source, target);
        return target;
    }

    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera pointCamera(short flag) {
        var camera = new com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera(37);
        camera.setSlotNum(77);
        camera.setStartFrame(20);
        camera.setEndFrame(43);
        camera.setShort1(flag);
        addUnit(camera, 0, 1, term(7, pointCollection()));
        return camera;
    }

    private static BheInfoCollection pointCollection() {
        BheInfoCollection c = new BheInfoCollection();
        c.setInt1(20);
        c.getTypeList().add(3);
        c.getIntList3().addAll(List.of(0, 0));
        c.setInt2(0);
        return c;
    }

    private static CEventTerm term(int type, BheInfoCollection collection) {
        CEventTerm term = new CEventTerm(type);
        term.setStartFrame(2);
        term.setEndFrame(23);
        term.getBheInfoCollectionList().add(collection);
        return term;
    }

    private static CEventVal value(int type, int a, int b, int c, int d) {
        CEventVal value = new CEventVal(type);
        value.setStartFrame(3);
        value.setEndFrame(17);
        value.setInt1(a); value.setInt2(b); value.setInt3(c); value.setInt4(d);
        return value;
    }

    private static void addUnit(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera camera,
                                int slot, int buffer, SkillInfoObject data) {
        var unit = new com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera.CEventCameraUnit();
        unit.setUnitSlotNum(slot);
        unit.setCeventCameraUnitQuantity(slot);
        unit.setBuffer(buffer);
        unit.setData(data);
        camera.getCeventCameraUnitList().add(unit);
    }

    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera.CEventCameraUnit sourceUnit(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera camera, int slot) {
        return camera.getCeventCameraUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum()))
                .findFirst().orElseThrow();
    }

    private static CEventCamera.CEventCameraUnit targetUnit(CEventCamera camera, int slot) {
        return camera.getCeventCameraUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum()))
                .findFirst().orElseThrow();
    }

    private static CEventTerm sourceTerm(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera camera, int slot) {
        return (CEventTerm) sourceUnit(camera, slot).getData();
    }

    private static com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm targetTerm(CEventCamera camera, int slot) {
        return (com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm) targetUnit(camera, slot).getData();
    }

    private static BsdxInfoCollection targetCollection(CEventCamera camera) {
        return targetTerm(camera, 0).getBsdxInfoCollectionList().get(0);
    }

    private static void assertPointPosition(BheInfoCollection c) {
        assertEquals(20, c.getInt1()); assertEquals(List.of(3), c.getTypeList());
        assertEquals(List.of(), c.getParamList()); assertEquals(List.of(0, 0), c.getIntList3());
        assertEquals(List.of(), c.getIntList4()); assertEquals(0, c.getInt2());
    }

    private static void assertObjectPosition(BsdxInfoCollection c) {
        assertEquals(20, c.getInt1()); assertEquals(List.of(1), c.getTypeList());
        assertEquals(List.of(), c.getParamList()); assertEquals(List.of(0, 0), c.getIntList3());
        assertEquals(List.of(), c.getIntList4()); assertEquals(0, c.getInt2());
    }

    private static void assertCollectionEqual(BheInfoCollection expected, BsdxInfoCollection actual, String label) {
        // The existing mapper normalizes null scalars/lists; compatibility must not broaden that into mode1.
        assertEquals(expected.getInt1() == null ? 0 : expected.getInt1(), actual.getInt1(), label + "/root");
        assertEquals(expected.getTypeList() == null ? List.of() : expected.getTypeList(), actual.getTypeList(), label + "/types");
        assertEquals(expected.getParamList() == null ? List.of() : expected.getParamList(), actual.getParamList(), label + "/params");
        assertEquals(expected.getIntList3() == null ? List.of() : expected.getIntList3(), actual.getIntList3(), label + "/aux3");
        assertEquals(expected.getIntList4() == null ? List.of() : expected.getIntList4(), actual.getIntList4(), label + "/aux4");
        assertEquals(expected.getInt2() == null ? 0 : expected.getInt2(), actual.getInt2(), label + "/int2");
    }

    private static com.giga.nexas.dto.bhe.waz.Waz syntheticWaz(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera camera, boolean ordinaryPosition) {
        var waz = new com.giga.nexas.dto.bhe.waz.Waz("camera-fixture");
        waz.setExtensionName("waz");
        var skill = new com.giga.nexas.dto.bhe.waz.Waz.Skill();
        skill.setSkillNameJapanese("camera fixture"); skill.setSkillNameEnglish("CAMERA_FIXTURE"); skill.setPhaseQuantity(1);
        var phase = new com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase();
        var unit = new com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit(77, "camera");
        unit.getSkillInfoObjectList().add(camera); phase.getSkillUnitCollection().add(unit);
        if (ordinaryPosition) {
            var positionUnit = new com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit(11, "position");
            CEventTerm position = term(7, sourceTerm(camera, 0).getBheInfoCollectionList().get(0));
            position.setSlotNum(11); positionUnit.getSkillInfoObjectList().add(position);
            phase.getSkillUnitCollection().add(positionUnit);
        }
        skill.getPhasesInfo().add(phase);
        var suffix = new com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillSuffix(); suffix.setInt1(0); suffix.setInt2(0);
        skill.getSkillSuffixList().add(suffix); waz.getSkillList().add(skill);
        return waz;
    }

    private static Waz convert(com.giga.nexas.dto.bhe.waz.Waz source, String route) {
        return route.equals("followup")
                ? new com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter().convert(source)
                : new com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz.BheToBsdxWazConverter().convert(source);
    }

    private static Map<String, com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera> sourceCameras(
            com.giga.nexas.dto.bhe.waz.Waz waz) {
        Map<String, com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera> result = new LinkedHashMap<>();
        for (int skillIndex = 0; skillIndex < waz.getSkillList().size(); skillIndex++) {
            var skill = waz.getSkillList().get(skillIndex);
            for (int p = 0; p < skill.getPhasesInfo().size(); p++) {
                for (var unit : skill.getPhasesInfo().get(p).getSkillUnitCollection()) {
                    if (!Integer.valueOf(77).equals(unit.getUnitQuantity())) continue;
                    int ordinal = 0;
                    for (var event : unit.getSkillInfoObjectList()) {
                        String key = skillIndex + ":" + skill.getSkillNameEnglish() + "/" + p + "/" + ordinal++;
                        assertNull(result.put(key, (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventCamera) event));
                    }
                }
            }
        }
        return result;
    }

    private static Map<String, CEventCamera> cameras(Waz waz) {
        Map<String, CEventCamera> result = new LinkedHashMap<>();
        for (int skillIndex = 0; skillIndex < waz.getSkillList().size(); skillIndex++) {
            var skill = waz.getSkillList().get(skillIndex);
            for (int p = 0; p < skill.getPhasesInfo().size(); p++) {
                for (var unit : skill.getPhasesInfo().get(p).getSkillUnitCollection()) {
                    if (!Integer.valueOf(66).equals(unit.getUnitQuantity())) continue;
                    int ordinal = 0;
                    for (var event : unit.getSkillInfoObjectList()) {
                        String key = skillIndex + ":" + skill.getSkillNameEnglish() + "/" + p + "/" + ordinal++;
                        assertNull(result.put(key, (CEventCamera) event));
                    }
                }
            }
        }
        return result;
    }

    private static byte[] bytes(SkillInfoObject event) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (BinaryWriter writer = new BinaryWriter(stream, CHARSET)) { event.writeInfo(writer); }
        return stream.toByteArray();
    }

    private static byte[] bytes(com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject event) throws Exception {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try (BinaryWriter writer = new BinaryWriter(stream, CHARSET)) { event.writeInfo(writer); }
        return stream.toByteArray();
    }

    private byte[] bheWazBytes(com.giga.nexas.dto.bhe.waz.Waz waz, String name) throws Exception {
        Path path = output.resolve(name);
        new BheBinService().generate(path.toString(), waz, CHARSET);
        return Files.readAllBytes(path);
    }

    private void assertDxRoundTrip(Waz waz, String name) throws Exception {
        var service = new BsdxBinService();
        Path first = output.resolve(name + ".waz"), second = output.resolve(name + "-again.waz");
        service.generate(first.toString(), waz, CHARSET);
        Waz parsed = (Waz) service.parse(first.toString(), CHARSET).getData();
        var before = cameras(waz); var after = cameras(parsed);
        assertEquals(before.keySet(), after.keySet());
        for (String key : before.keySet()) assertArrayEquals(bytes(before.get(key)), bytes(after.get(key)), name + "/" + key);
        service.generate(second.toString(), parsed, CHARSET);
        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second));
    }

    private static com.giga.nexas.dto.bhe.waz.Waz parseBhe(Path path) throws Exception {
        return (com.giga.nexas.dto.bhe.waz.Waz) new BheBinService().parse(path.toString(), CHARSET).getData();
    }

    private static Path resolve(Path directory, String name) throws Exception {
        try (var files = Files.list(directory)) {
            return files.filter(p -> p.getFileName().toString().equalsIgnoreCase(name)).findFirst().orElseThrow();
        }
    }

    private static String sha256(byte[] bytes) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    }
}
