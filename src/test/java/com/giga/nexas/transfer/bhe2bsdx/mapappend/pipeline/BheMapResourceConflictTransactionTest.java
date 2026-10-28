package com.giga.nexas.transfer.bhe2bsdx.mapappend.pipeline;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.dto.bhe.map.generator.MapDataGenerator;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BheMapCatalogLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BsdxMapBaselineLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapImportOutputWriter;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapOutputConflictType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.output.BheMapSafeOutputWriter;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.plan.BheMapAppendPlanBuilder;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.preview.BheMapPreviewMaterializer;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceMaterializationResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceMaterializer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BheMapResourceConflictTransactionTest {

    private static final String CHARSET = "windows-31j";
    private static final String TARGET_MAP = "bhe_conflict.map";
    private static final String TARGET_SPM = "bhe_conflicting.spm";

    @TempDir
    Path temporary;

    @Test
    void resourcePreparationConflictBlocksMapMapGroupAndResourceCommit() throws Exception {
        Path firstRealSpm = Path.of("src/main/resources/game/bhe/spm/mapb6_es02_ob01.spm");
        Path secondRealSpm = Path.of("src/main/resources/game/bhe/spm/mapb6_es01_ob01.spm");
        Path sourceMapGroup = Path.of("src/main/resources/game/bhe/grp/mapgroup.grp");
        Path baselineMapGroup = Path.of("src/main/resources/game/bsdx/grp/MapGroup.grp");
        Path inputs = Files.createDirectory(temporary.resolve("inputs"));
        Path outputRoot = Files.createDirectory(temporary.resolve("output"));
        Path firstSource = inputs.resolve("incoming-a.spm");
        Path secondSource = inputs.resolve("incoming-b.spm");
        Files.copy(firstRealSpm, firstSource);
        Files.copy(secondRealSpm, secondSource);
        Path sourceMap = inputs.resolve("conflict.map");
        writeSourceMap(sourceMap);
        Path currentMapGroup = outputRoot.resolve("MapGroup.grp");
        Files.copy(baselineMapGroup, currentMapGroup);
        Path sentinel = outputRoot.resolve("sentinel.bin");
        Files.write(sentinel, new byte[]{11, 29, 47, (byte) 0xFE});
        Map<Path, byte[]> originalInputs = new LinkedHashMap<>();
        for (Path path : List.of(firstRealSpm, secondRealSpm, sourceMapGroup, baselineMapGroup,
                firstSource, secondSource, sourceMap, currentMapGroup, sentinel)) {
            originalInputs.put(path, Files.readAllBytes(path));
        }

        BheMapAppendPlan plan = plan(sourceMap, firstSource, secondSource);
        BheMapAppendPlanBuilder planBuilder = new BheMapAppendPlanBuilder() {
            @Override
            public BheMapAppendPlan build(BheMapCatalog catalog, Path mapDir, Path resourceRoot,
                                          BsdxMapBaseline baseline, String charset, BheMapAppendAudit audit) {
                return plan;
            }
        };
        BheMapResourceMaterializer resources = new BheMapResourceMaterializer() {
            @Override
            public BheMapResourceMaterializationResult prepare(BheMapAppendPlan appendPlan,
                    BheMapImportOutputResult output, Path root, String charset) {
                // 真实Writer已准备map及MapGroup，真实resource.prepare再从有效供体产生冲突和首份pending。
                assertEquals(1, output.getWrittenMaps().size(), "必须实际准备地图，避免零输出的空场景");
                assertEquals(2, output.getPendingOutputFiles().size());
                BheMapResourceMaterializationResult result = super.prepare(appendPlan, output, root, charset);
                assertEquals(1, result.getOutputConflictCount());
                assertEquals(1, result.getConflicts().size());
                assertEquals(1, result.getPendingOutputFiles().size());
                return result;
            }
        };
        BheMapImportPipeline pipeline = new BheMapImportPipeline(new BheMapCatalogLoader(),
                new BsdxMapBaselineLoader(), planBuilder, new BheMapPreviewMaterializer(),
                new BheMapImportOutputWriter(), resources, new BheMapSafeOutputWriter());
        BheMapAppendRequest request = new BheMapAppendRequest();
        request.setBheMapGroupPath(sourceMapGroup);
        request.setCurrentTargetMapGroupPath(currentMapGroup);
        request.setBheMapDir(inputs);
        request.setBheStaticResourceRoot(inputs);
        request.setOutputRoot(outputRoot);
        request.setCharset(CHARSET);
        request.setPreviewMaterializationEnabled(false);

        var result = pipeline.importMapsIntoCurrentResourceTree(request);
        var output = result.getOutputResult();
        var resourceResult = result.getResourceMaterializationResult();
        List<String> outputNames;
        try (var files = Files.list(outputRoot)) {
            outputNames = files.map(path -> path.getFileName().toString()).sorted().toList();
        }
        assertAll("所有原始输入、临时供体及既有输出均不变", originalInputs.entrySet().stream()
                .map(entry -> (org.junit.jupiter.api.function.Executable) () ->
                        assertArrayEquals(entry.getValue(), Files.readAllBytes(entry.getKey()), entry.getKey().toString())));
        assertAll(
                () -> assertFalse(result.isOutputWritten()),
                () -> assertFalse(result.isResourcesMaterialized()),
                () -> assertFalse(output.isOutputCompleted()),
                () -> assertEquals(0, output.getWrittenFileCount()),
                () -> assertEquals(0, resourceResult.getWrittenFileCount()),
                () -> assertEquals(1, resourceResult.getOutputConflictCount()),
                () -> assertEquals(1, resourceResult.getConflicts().size()),
                () -> assertEquals(BheMapOutputConflictType.RESOURCE_FILE_CONFLICT,
                        resourceResult.getConflicts().get(0).getType()),
                () -> assertEquals(1, result.getImportManifest().getOutputConflictCount(), "同一冲突不能重复计数"),
                () -> assertTrue(output.getPendingOutputFiles().isEmpty()),
                () -> assertTrue(resourceResult.getPendingOutputFiles().isEmpty()),
                () -> assertFalse(Files.exists(outputRoot.resolve(TARGET_MAP))),
                () -> assertFalse(Files.exists(outputRoot.resolve(TARGET_SPM))),
                () -> assertEquals(List.of("MapGroup.grp", "sentinel.bin"), outputNames));
    }

    private BheMapAppendPlan plan(Path sourceMap, Path firstSource, Path secondSource) {
        BheMapEntryPlan entry = new BheMapEntryPlan();
        entry.setSourceMapFileName(sourceMap.getFileName().toString());
        entry.setSourceMapPath(sourceMap);
        entry.setSourceMapFound(true);
        entry.setSourceMapParsed(true);
        entry.setTargetMapFileName(TARGET_MAP);
        entry.setTargetGroupResourceName("bhe_conflict");
        entry.getSourceMapGroupSnapshot().setExistFlag(1);
        entry.getSourceMapGroupSnapshot().setGroupName("conflict");
        entry.getSourceMapGroupSnapshot().setGroupCodeName("CONFLICT");
        entry.getResourceReferences().getReferences().add(reference(BheMapReferenceType.SPRITE_MAP_LIST, firstSource));
        entry.getResourceReferences().getReferences().add(reference(BheMapReferenceType.RESOURCE_SLOT, secondSource));
        BheMapAppendPlan plan = new BheMapAppendPlan();
        plan.setSourceMapGroupTotalCount(1);
        plan.setSourceMigratableEntryCount(1);
        plan.getEntries().add(entry);
        return plan;
    }

    private BheMapResourceReference reference(BheMapReferenceType type, Path source) {
        BheMapResourceReference reference = new BheMapResourceReference();
        reference.setType(type);
        reference.setStatus(BheMapReferenceStatus.FOUND_DEFERRED);
        reference.setSourcePath(source);
        reference.setSourceText(source.getFileName().toString());
        reference.setSourceFileName(source.getFileName().toString());
        reference.setTargetText(TARGET_SPM);
        reference.setTargetFileName(TARGET_SPM);
        return reference;
    }

    private void writeSourceMap(Path path) throws Exception {
        MapData map = new MapData("conflict.map");
        map.setMagic("MAPDATA VER-1.00");
        map.setWidth(1);
        map.setHeight(1);
        MapData.TileRecord tile = new MapData.TileRecord();
        tile.setPackedValue0(0);
        tile.setPackedValue1(0);
        tile.setByte0((byte) 0);
        tile.setByte1((byte) 0);
        map.getTileRecords().add(tile);
        map.getCameraRectBlock().setCount(0);
        map.getRawPointGroup0().setCount(0);
        map.getRawPointGroup1().setCount(0);
        map.getRawRectGroup0().setCount(0);
        map.getRawRectGroup1().setCount(0);
        map.getRawRectGroup2().setCount(0);
        for (int i = 0; i < MapData.RESOURCE_SLOT_COUNT; i++) {
            MapData.NamedResourceSlotBlock slot = new MapData.NamedResourceSlotBlock();
            slot.setSlotNum(i);
            slot.setSlotText(i == 0 ? "incoming-b.spm" : "");
            slot.setParam0(0);
            slot.setParam1(0);
            slot.setParam2(0);
            slot.setParam3(0);
            slot.setParam4(0);
            map.getNamedResourceSlotBlocks().add(slot);
        }
        for (int i = 0; i < MapData.SCRIPT_GROUP_COUNT; i++) {
            MapData.ScriptEntryGroupBlock group = new MapData.ScriptEntryGroupBlock();
            group.setGroupNum(i);
            group.setCount(0);
            map.getScriptEntryGroupBlocks().add(group);
        }
        map.setForegroundImage("");
        map.setSpriteMapCount((byte) 1);
        map.setSpriteMapList(List.of("incoming-a.spm"));
        new MapDataGenerator().generate(path.toString(), map, CHARSET);
    }
}
