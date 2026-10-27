package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.map.parser.MapDataParser;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.spm.generator.SpmGenerator;
import com.giga.nexas.dto.bsdx.spm.parser.SpmParser;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityBlockingReason;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportBlockedMap;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportWrittenMap;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline.BheMapImportPipeline;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.spm.BheToBsdxSpmConverter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapResourceMaterializerTest {

    private static final String CHARSET = "windows-31j";
    private static final Path REAL_SPM = Paths.get("src/main/resources/game/bhe/spm/mapb6_es02_ob01.spm");
    private static final String TARGET_SPM = "bhe_mapB6_ES02_ob01.spm";
    private static final String TARGET_MAP = "bhe_mapB6_ES02_01.map";

    @TempDir
    Path outputRoot;

    @Test
    void realDirectSpriteMapConvertsB6SpmAndPreservesSourceBytes() throws Exception {
        byte[] sourceBytes = Files.readAllBytes(REAL_SPM);
        Spm parsed = materializeDirectAndRead(BheMapReferenceType.SPRITE_MAP);

        assertEquals("SPM VER-2.00", parsed.getSpmVersion());
        assertEquals(38, parsed.getPageData().size());
        assertEquals(53, parsed.getPageData().stream().mapToInt(page -> page.getChipData().size()).sum());
        assertEquals(14, parsed.getAnimData().size());
        assertEquals(List.of("mapb6_es02_ob01_0004_0001.png", "mapb6_es02_ob01_0003_0001.png",
                        "mapb6_es02_ob01_0002_0001.png", "mapb6_es02_ob01_0001_0001.png"),
                parsed.getImageData().stream().map(Spm.SPMImageData::getImageName).toList());
        assertEquals("岩01", parsed.getAnimData().get(0).getAnimName());
        assertEquals(List.of(0), parsed.getAnimData().get(0).getPatData().get(0).getPageNo());
        var page = parsed.getPageData().get(0);
        assertEquals(121, page.getPageWidth());
        assertEquals(150, page.getPageHeight());
        assertRect(page.getPageRect(), -64, -96, 57, 54);
        assertEquals(17L, page.getHitFlag());
        assertEquals(2, page.getHitRects().size());
        // BHE的id/shapeType及形状载荷必须重编成DX的七整数HitArea，不能按源字节直拷。
        assertEquals(2, page.getHitRects().get(0).getUnk0());
        assertRect(page.getHitRects().get(0).getHitRect(), -48, -48, 48, 48);
        assertRect(page.getHitRects().get(1).getHitRect(), 64, -72, 72, 0);
        var chip = page.getChipData().get(0);
        assertEquals(0, chip.getImageNo());
        assertEquals(121, chip.getChipWidth());
        assertEquals(150, chip.getChipHeight());
        assertRect(chip.getDstRect(), -64, -96, 57, 54);
        assertRect(chip.getSrcRect(), 343, 2, 464, 152);
        assertEquals(256L, chip.getDrawOptionValue());
        assertArrayEquals(sourceBytes, Files.readAllBytes(REAL_SPM));
    }

    @Test
    void singleSpriteMapListReferenceUsesDirectSpmConversion() throws Exception {
        materializeDirectAndRead(BheMapReferenceType.SPRITE_MAP_LIST);
    }

    @Test
    void resourceSlotSpmUsesDirectSpmConversion() throws Exception {
        materializeDirectAndRead(BheMapReferenceType.RESOURCE_SLOT);
    }

    @Test
    void repeatedDirectSpmReferencesDeduplicateConvertedBytes() throws Exception {
        byte[] before = Files.readAllBytes(REAL_SPM);
        byte[] expected = convertedSpmBytes();

        BheMapResourceMaterializationResult result = materialize(false,
                directReference(BheMapReferenceType.SPRITE_MAP), directReference(BheMapReferenceType.RESOURCE_SLOT));

        assertEquals(0, result.getOutputConflictCount());
        assertEquals(1, result.getDuplicateTargetResourceCount());
        assertEquals(2, result.getServicedResourceReferenceCount());
        assertEquals(1, result.getMaterializedResourceCount());
        assertEquals(1, result.getWrittenFileCount());
        assertArrayEquals(expected, Files.readAllBytes(resourceRoot().resolve(TARGET_SPM)));
        assertArrayEquals(before, Files.readAllBytes(REAL_SPM));
    }

    @Test
    void differentDirectSpmsInSameBatchBlockEveryCommitAndPreserveSources() throws Exception {
        Path otherRealSpm = Paths.get("src/main/resources/game/bhe/spm/mapb6_es01_ob01.spm");
        byte[] firstSourceBytes = Files.readAllBytes(REAL_SPM);
        byte[] secondSourceBytes = Files.readAllBytes(otherRealSpm);
        Path firstSource = outputRoot.resolve("incoming-a.spm");
        Path secondSource = outputRoot.resolve("incoming-b.spm");
        Files.write(firstSource, firstSourceBytes);
        Files.write(secondSource, secondSourceBytes);

        // 两个真实、可解析的BHE供体必须在转换后仍不同，避免把相同输出误当冲突。
        var parser = new com.giga.nexas.dto.bhe.spm.parser.SpmParser();
        var first = parser.parse(Files.readAllBytes(firstSource), "incoming-a.spm", CHARSET);
        var second = parser.parse(Files.readAllBytes(secondSource), "incoming-b.spm", CHARSET);
        Path firstConverted = outputRoot.resolve("converted-a.spm");
        Path secondConverted = outputRoot.resolve("converted-b.spm");
        new SpmGenerator().generate(firstConverted.toString(), new BheToBsdxSpmConverter().convert(first), CHARSET);
        new SpmGenerator().generate(secondConverted.toString(), new BheToBsdxSpmConverter().convert(second), CHARSET);
        assertFalse(Arrays.equals(Files.readAllBytes(firstConverted), Files.readAllBytes(secondConverted)));

        Files.createDirectories(resourceRoot());
        Path sentinel = resourceRoot().resolve("sentinel.bin");
        byte[] sentinelBytes = {11, 29, 47, (byte) 0xFE};
        Files.write(sentinel, sentinelBytes);

        BheMapResourceMaterializationResult result = materialize(false,
                reference(BheMapReferenceType.SPRITE_MAP, firstSource, TARGET_SPM),
                reference(BheMapReferenceType.RESOURCE_SLOT, secondSource, TARGET_SPM));

        List<String> outputNames;
        try (var files = Files.list(resourceRoot())) {
            outputNames = files.map(path -> path.getFileName().toString()).sorted().toList();
        }
        assertAll(
                () -> assertTrue(result.getOutputConflictCount() > 0),
                () -> assertEquals(0, result.getWrittenFileCount(), "同批资源冲突时不得提交首份pending"),
                () -> assertFalse(Files.exists(resourceRoot().resolve(TARGET_SPM))),
                () -> assertEquals(List.of("sentinel.bin"), outputNames),
                () -> assertArrayEquals(sentinelBytes, Files.readAllBytes(sentinel)),
                () -> assertArrayEquals(firstSourceBytes, Files.readAllBytes(firstSource)),
                () -> assertArrayEquals(secondSourceBytes, Files.readAllBytes(secondSource)),
                () -> assertArrayEquals(firstSourceBytes, Files.readAllBytes(REAL_SPM)),
                () -> assertArrayEquals(secondSourceBytes, Files.readAllBytes(otherRealSpm)));
    }

    @Test
    void existingConvertedDirectSpmIsReusedWithoutConflict() throws Exception {
        byte[] expected = convertedSpmBytes();
        Files.createDirectories(resourceRoot());
        Path existing = resourceRoot().resolve(TARGET_SPM);
        Files.write(existing, expected);

        BheMapResourceMaterializationResult result = materialize(false, directReference(BheMapReferenceType.SPRITE_MAP));

        assertEquals(0, result.getOutputConflictCount());
        assertEquals(0, result.getWrittenFileCount());
        assertEquals(1, result.getExistingIdenticalFileCount());
        assertArrayEquals(expected, Files.readAllBytes(existing));
    }

    @Test
    void existingUnconvertedBheSpmConflictsInsteadOfBeingReused() throws Exception {
        byte[] original = Files.readAllBytes(REAL_SPM);
        assertFalse(Arrays.equals(original, convertedSpmBytes()), "真实样本必须存在引擎格式差异");
        Files.createDirectories(resourceRoot());
        Path existing = resourceRoot().resolve(TARGET_SPM);
        Files.write(existing, original);

        BheMapResourceMaterializationResult result = materialize(false, directReference(BheMapReferenceType.SPRITE_MAP));

        assertEquals(1, result.getOutputConflictCount());
        assertEquals(0, result.getWrittenFileCount());
        assertEquals(0, result.getExistingIdenticalFileCount());
        assertArrayEquals(original, Files.readAllBytes(existing), "冲突时保留既有目标");
        assertArrayEquals(original, Files.readAllBytes(REAL_SPM));
    }

    @Test
    void nonSpmReferencesKeepOriginalBytes() throws Exception {
        Path source = outputRoot.resolve("opaque-source.bin");
        byte[] original = {0, 1, 2, (byte) 0xFF, 0, 47, 83, 80, 77};
        Files.write(source, original);

        BheMapResourceMaterializationResult result = materialize(false,
                reference(BheMapReferenceType.FOREGROUND, source, "bhe_foreground.bin"),
                reference(BheMapReferenceType.RESOURCE_SLOT, source, "bhe_slot.bin"));

        assertEquals(2, result.getWrittenFileCount());
        assertEquals(0, result.getOutputConflictCount());
        assertFalse(result.isSpmWritten());
        assertArrayEquals(original, Files.readAllBytes(resourceRoot().resolve("bhe_foreground.bin")));
        assertArrayEquals(original, Files.readAllBytes(resourceRoot().resolve("bhe_slot.bin")));
        assertArrayEquals(original, Files.readAllBytes(source));
    }

    @Test
    void compositionSpriteMapListIsNotMaterializedAgain() throws Exception {
        byte[] before = Files.readAllBytes(REAL_SPM);
        byte[] composed = convertedSpmBytes();
        Files.createDirectories(resourceRoot());
        Path existing = resourceRoot().resolve("bhe_composed.spm");
        Files.write(existing, composed);

        BheMapResourceMaterializationResult result = materialize(true,
                directReference(BheMapReferenceType.SPRITE_MAP_LIST));

        assertEquals(0, result.getMaterializedResourceCount());
        assertEquals(0, result.getServicedResourceReferenceCount());
        assertEquals(0, result.getWrittenFileCount());
        assertFalse(result.isSpmWritten());
        assertFalse(Files.exists(resourceRoot().resolve(TARGET_SPM)));
        assertArrayEquals(composed, Files.readAllBytes(existing));
        new SpmParser().parse(Files.readAllBytes(existing), existing.getFileName().toString(), CHARSET);
        assertArrayEquals(before, Files.readAllBytes(REAL_SPM));
    }

    @Test
    void completeStaticResourceRootMaterializesOnlyResourcesUsedByWrittenMaps() throws Exception {
        Path completeResourceRoot = resolveCompleteStaticResourceRoot();
        assumeTrue(
                completeResourceRoot != null && Files.isDirectory(completeResourceRoot),
                "complete BHE static resource root must be provided by -Dbhe.staticResourceRoot or BHE_STATIC_RESOURCE_ROOT"
        );

        BheMapAppendRequest request = new BheMapAppendRequest();
        request.setBheStaticResourceRoot(completeResourceRoot);
        request.setCurrentTargetMapGroupPath(request.getBsdxMapGroupPath());
        request.setOutputRoot(outputRoot);

        BheMapAppendResult result = new BheMapImportPipeline().importMapsIntoCurrentResourceTree(request);
        BheMapImportOutputResult output = result.getOutputResult();
        BheMapResourceMaterializationResult resources = result.getResourceMaterializationResult();

        assertEquals(142, result.getAppendPlan().size());
        assertEquals(137, output.getWrittenMapCount());
        assertEquals(33, output.getDirectWrittenMapCount());
        assertEquals(104, output.getCompositionWrittenMapCount());
        assertEquals(104, output.getComposedSpmWrittenCount());
        assertEquals(137, output.getAppendedMapGroupCount());
        assertEquals(5, output.getBlockedMaps().size());
        assertEquals(5, output.compositionBlockingReasonCount(MapCompatibilityBlockingReason.MISSING_SOURCE_SPM));
        assertEquals(97, resources.getMaterializedResourceCount());
        assertEquals(68, resources.getMaterializedForegroundCount());
        assertEquals(18, resources.getMaterializedResourceSlotCount());
        assertEquals(11, resources.getMaterializedSpriteMapCount());
        assertEquals(97, resources.getWrittenFileCount());
        assertEquals(0, resources.getExistingIdenticalFileCount());
        assertEquals(0, resources.getOutputConflictCount());
        assertEquals(137, resources.getDuplicateTargetResourceCount());
        assertEquals(0, resources.getSkippedBlockedResourceCount());
        assertEquals(0, resources.getMissingSourceResourceCount());
        assertFalse(resources.isComposedSpmWritten());
        assertFalse(resources.isPreviewWritten());
        assertFalse(resources.isPacked());
        assertTrue(resources.isSpmWritten());
        assertEquals(
                resources.getMaterializedForegroundCount()
                        + resources.getMaterializedResourceSlotCount()
                        + resources.getMaterializedSpriteMapCount(),
                resources.getMaterializedResourceCount()
        );

        Set<String> writtenMapNames = output.getWrittenMaps().stream()
                .map(BheMapImportWrittenMap::getTargetMapFileName)
                .collect(Collectors.toSet());
        Set<String> materializedFileNames = resources.getWrittenResourceFileList().stream()
                .map(path -> path.getFileName().toString())
                .collect(Collectors.toSet());
        Set<String> expectedWrittenResourceNames = new HashSet<>();
        Set<String> blockedOnlyResourceNames = new HashSet<>();
        for (BheMapEntryPlan entry : result.getAppendPlan().getEntries()) {
            boolean written = writtenMapNames.contains(entry.getTargetMapFileName());
            for (BheMapResourceReference reference : entry.getResourceReferences().getReferences()) {
                if (!isOutputReferenceForEntry(entry, reference) || reference.isMissing()) {
                    continue;
                }
                if (written && isOutputReferenceForWrittenMap(writtenMapFor(output, entry), reference)) {
                    expectedWrittenResourceNames.add(reference.getTargetFileName());
                }
            }
        }
        for (BheMapEntryPlan entry : result.getAppendPlan().getEntries()) {
            if (writtenMapNames.contains(entry.getTargetMapFileName())) {
                continue;
            }
            for (BheMapResourceReference reference : entry.getResourceReferences().getReferences()) {
                if (isOutputReferenceForEntry(entry, reference) && !reference.isMissing()) {
                    blockedOnlyResourceNames.add(reference.getTargetFileName());
                }
            }
        }
        blockedOnlyResourceNames.removeAll(expectedWrittenResourceNames);

        assertEquals(expectedWrittenResourceNames.size(), resources.getMaterializedResourceCount());
        assertEquals(expectedWrittenResourceNames, materializedFileNames);
        assertTrue(blockedOnlyResourceNames.stream().noneMatch(materializedFileNames::contains));

        for (Path writtenResource : resources.getWrittenResourceFileList()) {
            assertTrue(Files.isRegularFile(writtenResource));
        }
        for (BheMapImportBlockedMap blockedMap : output.getBlockedMaps()) {
            assertFalse(Files.exists(outputRoot.resolve(blockedMap.getTargetMapFileName())));
        }

        assertTrue(Files.isRegularFile(output.getOutputMapGroupPath()));
        MapGroupGrp outputMapGroup = parseMapGroup(output.getOutputMapGroupPath(), request.getCharset());
        assertEquals(output.getBaselineMapGroupCount() + 137, outputMapGroup.getGroupList().size());
        MapDataParser mapDataParser = new MapDataParser();
        for (BheMapImportWrittenMap writtenMap : output.getWrittenMaps()) {
            assertTrue(Files.isRegularFile(writtenMap.getMapPath()));
            mapDataParser.parse(Files.readAllBytes(writtenMap.getMapPath()), writtenMap.getTargetMapFileName(), request.getCharset());
        }

        assertEquals(0, output.getBlockedByMultipleSpriteMapListCount());
        assertEquals(0, output.getBlockedByUnsupportedScriptGroupCount());
        assertEquals(5, output.getBlockedByMissingSourceSpmCount());
        assertEquals(0, output.getBlockedByMissingFinalReferenceCount());
        assertEquals(
                resources.getMaterializedSpriteMapCount() + output.getComposedSpmWrittenCount(),
                countFilesByExtension(outputRoot, ".spm")
        );
        assertEquals(0, countFilesByExtension(outputRoot, ".pac"));
        assertFalse(Files.exists(Paths.get("tools/probe/bhe_map_spm_selector_probe.js")));
        assertFalse(Files.exists(Paths.get("tools/probe/bsdx_map_object_probe.js")));
    }

    private Spm materializeDirectAndRead(BheMapReferenceType type) throws Exception {
        byte[] before = Files.readAllBytes(REAL_SPM);
        byte[] expected = convertedSpmBytes();
        assertFalse(Arrays.equals(before, expected), "真实BHE HitArea与BSDX载荷不能字节相同");

        BheMapResourceMaterializationResult result = materialize(false, directReference(type));

        assertEquals(0, result.getOutputConflictCount());
        assertEquals(1, result.getWrittenFileCount());
        assertEquals(1, result.getMaterializedResourceCount());
        assertTrue(result.isSpmWritten());
        byte[] actual = Files.readAllBytes(resourceRoot().resolve(TARGET_SPM));
        assertArrayEquals(expected, actual, "直引SPM必须走已有转换器和BSDX生成器，不能复制BHE原字节");
        assertArrayEquals(before, Files.readAllBytes(REAL_SPM));
        return new SpmParser().parse(actual, TARGET_SPM, CHARSET);
    }

    private byte[] convertedSpmBytes() throws Exception {
        var source = (com.giga.nexas.dto.bhe.spm.Spm) new BheBinService().parse(REAL_SPM.toString(), CHARSET).getData();
        assertEquals("SPM VER-2.00", source.getSpmVersion());
        assertEquals(38, source.getPageData().size());
        assertEquals(1, source.getPageData().get(0).getHitRects().get(0).getShapeType().intValue());
        Spm converted = new BheToBsdxSpmConverter().convert(source);
        Path expected = outputRoot.resolve("oracle-bsdx.spm");
        new SpmGenerator().generate(expected.toString(), converted, CHARSET);
        return Files.readAllBytes(expected);
    }

    private BheMapResourceMaterializationResult materialize(boolean composition, BheMapResourceReference... references) {
        BheMapEntryPlan entry = new BheMapEntryPlan();
        entry.setSourceMapFileName("mapb6_es02_01.map");
        entry.setTargetMapFileName(TARGET_MAP);
        entry.getResourceReferences().getReferences().addAll(Arrays.asList(references));
        BheMapAppendPlan plan = new BheMapAppendPlan();
        plan.getEntries().add(entry);
        BheMapImportWrittenMap written = new BheMapImportWrittenMap();
        written.setTargetMapFileName(TARGET_MAP);
        written.setMapPath(resourceRoot().resolve(TARGET_MAP));
        written.setComposition(composition);
        if (composition) {
            written.setComposedSpmFileName("bhe_composed.spm");
            written.setComposedSpmPath(resourceRoot().resolve("bhe_composed.spm"));
        }
        BheMapImportOutputResult output = new BheMapImportOutputResult();
        output.getWrittenMaps().add(written);
        return new BheMapResourceMaterializer().materialize(plan, output, resourceRoot(), CHARSET);
    }

    private BheMapResourceReference directReference(BheMapReferenceType type) {
        return reference(type, REAL_SPM, TARGET_SPM);
    }

    private BheMapResourceReference reference(BheMapReferenceType type, Path source, String targetName) {
        BheMapResourceReference reference = new BheMapResourceReference();
        reference.setType(type);
        reference.setStatus(BheMapReferenceStatus.FOUND_DEFERRED);
        reference.setSourcePath(source);
        reference.setSourceText(source.getFileName().toString());
        reference.setSourceFileName(source.getFileName().toString());
        reference.setTargetText(targetName);
        reference.setTargetFileName(targetName);
        return reference;
    }

    private Path resourceRoot() {
        return outputRoot.resolve("resources");
    }

    private void assertRect(Spm.SPMRect rect, int left, int top, int right, int bottom) {
        assertEquals(List.of(left, top, right, bottom),
                List.of(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom()));
    }

    private boolean isOutputReferenceForEntry(BheMapEntryPlan entry, BheMapResourceReference reference) {
        if (reference == null || reference.getType() == null) {
            return false;
        }
        if (reference.getType() == BheMapReferenceType.FOREGROUND
                || reference.getType() == BheMapReferenceType.RESOURCE_SLOT
                || reference.getType() == BheMapReferenceType.SPRITE_MAP) {
            return true;
        }
        return reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST
                && entry.getResourceReferences().getReferences().stream()
                .filter(candidate -> candidate.getType() == BheMapReferenceType.SPRITE_MAP_LIST)
                .count() == 1;
    }

    private boolean isOutputReferenceForWrittenMap(
            BheMapImportWrittenMap writtenMap,
            BheMapResourceReference reference
    ) {
        if (writtenMap != null && writtenMap.isComposition()
                && reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST) {
            return false;
        }
        return true;
    }

    private BheMapImportWrittenMap writtenMapFor(BheMapImportOutputResult output, BheMapEntryPlan entry) {
        for (BheMapImportWrittenMap writtenMap : output.getWrittenMaps()) {
            if (writtenMap.getTargetMapFileName().equals(entry.getTargetMapFileName())) {
                return writtenMap;
            }
        }
        return null;
    }

    private MapGroupGrp parseMapGroup(Path mapGroupPath, String charset) throws Exception {
        return (MapGroupGrp) new BsdxBinService().parse(mapGroupPath.toString(), charset).getData();
    }

    private long countFilesByExtension(Path root, String extension) throws Exception {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension))
                    .count();
        }
    }

    private Path resolveCompleteStaticResourceRoot() {
        String propertyValue = System.getProperty("bhe.staticResourceRoot");
        if (!isBlank(propertyValue)) {
            return Paths.get(propertyValue).toAbsolutePath().normalize();
        }
        String environmentValue = System.getenv("BHE_STATIC_RESOURCE_ROOT");
        if (!isBlank(environmentValue)) {
            return Paths.get(environmentValue).toAbsolutePath().normalize();
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
