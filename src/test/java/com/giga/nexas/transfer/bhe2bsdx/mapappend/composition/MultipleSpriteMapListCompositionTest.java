package com.giga.nexas.transfer.bhe2bsdx.mapappend.composition;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.dto.bsdx.map.generator.MapDataGenerator;
import com.giga.nexas.dto.bsdx.map.parser.MapDataParser;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.spm.generator.SpmGenerator;
import com.giga.nexas.dto.bsdx.spm.parser.SpmParser;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BheMapCatalogLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.plan.BheMapAppendPlanBuilder;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceRewriteTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class MultipleSpriteMapListCompositionTest {

    @TempDir
    Path tempDir;

    @Test
    void syntheticCompositionRewritesSpmIndexesAndMapTypeIds() throws Exception {
        MultipleSpriteMapListCompositionPlan plan = syntheticPlan();

        Spm composed = new BheMapAppendSpmComposer().compose(plan);

        assertEquals(3, composed.getNumPageData());
        assertEquals(5, composed.getNumImageData());
        assertEquals(4, composed.getNumAnimData());
        assertEquals(2, composed.getPatPageNum());
        assertEquals(2, composed.getPageData().get(1).getChipData().get(0).getImageNo());
        assertEquals(List.of(1, -1), composed.getAnimData().get(2).getPatData().get(0).getPageNo());

        MapData sourceMapData = syntheticBheMapData();
        MultipleSpriteMapListMapDataConverter converter = new MultipleSpriteMapListMapDataConverter();
        MultipleSpriteMapListCompositionResult result = converter.convert(
                syntheticEntryPlan(),
                sourceMapData,
                syntheticRewriteTable(),
                plan
        );

        assertTrue(result.isConverted());
        assertEquals("bhe_synthetic.spm", result.getConvertedMapData().getSpriteMap());
        assertEquals(0, result.getConvertedMapData().getScriptEntryGroupBlocks().get(0).getEntries().get(0).getTypeId());
        assertEquals(3, result.getConvertedMapData().getScriptEntryGroupBlocks().get(0).getEntries().get(1).getTypeId());
    }

    @Test
    void completeStaticResourceRootPlansAndConvertsComposableMultipleSpriteMapListMaps() throws Exception {
        Path completeResourceRoot = resolveCompleteStaticResourceRoot();
        assumeTrue(
                completeResourceRoot != null && Files.isDirectory(completeResourceRoot),
                "complete BHE static resource root must be provided by -Dbhe.staticResourceRoot or BHE_STATIC_RESOURCE_ROOT"
        );

        BheMapAppendRequest request = new BheMapAppendRequest();
        request.setBheStaticResourceRoot(completeResourceRoot);
        BheMapCatalog sourceCatalog = new BheMapCatalogLoader().load(
                request.resolveBheMapGroupPath(),
                request.getCharset()
        );
        BheMapAppendPlan appendPlan = new BheMapAppendPlanBuilder().build(
                sourceCatalog,
                request.resolveBheMapDir(),
                request.resolveBheStaticResourceRoot(),
                new BsdxMapBaseline(),
                request.getCharset(),
                new BheMapAppendAudit()
        );

        MultipleSpriteMapListCompositionPlanner planner = new MultipleSpriteMapListCompositionPlanner();
        MultipleSpriteMapListMapDataConverter converter = new MultipleSpriteMapListMapDataConverter();
        BheBinService bheBinService = new BheBinService();
        CompositionStats stats = new CompositionStats();

        for (BheMapEntryPlan entry : appendPlan.getEntries()) {
            MapData sourceMapData = (MapData) bheBinService.parse(entry.getSourceMapPath().toString(), request.getCharset()).getData();
            if (spriteMapListSize(sourceMapData) <= 1) {
                continue;
            }
            stats.multipleSpriteMapListMapCount++;
            MultipleSpriteMapListCompositionPlan compositionPlan = planner.plan(entry, sourceMapData, request.getCharset());
            if (!compositionPlan.isComposable()) {
                stats.blockedMapCount++;
                for (MapCompatibilityIssue issue : compositionPlan.getCompatibilityAnalysis().getBlockingIssues()) {
                    stats.blockingReasonCounts.merge(issue.getReason(), 1, Integer::sum);
                }
                continue;
            }

            MultipleSpriteMapListCompositionResult result = converter.convert(
                    entry,
                    sourceMapData,
                    BheMapResourceRewriteTable.fromEntryPlan(entry),
                    compositionPlan
            );
            if (!result.isConverted()) {
                stats.blockedMapCount++;
                for (MapCompatibilityIssue issue : result.getBlockingIssues()) {
                    stats.blockingReasonCounts.merge(issue.getReason(), 1, Integer::sum);
                }
                continue;
            }
            stats.composableMapCount++;
            stats.totalRewrittenScriptEntryCount += compositionPlan.getRewrittenScriptEntryCount();
            if (stats.roundtripVerifiedMapCount < 3) {
                verifyRoundtrip(entry, result, request.getCharset());
                stats.roundtripVerifiedMapCount++;
            }
        }

        System.out.println("multiple spriteMapList composition stats: " + stats);

        assertEquals(109, stats.multipleSpriteMapListMapCount);
        assertTrue(stats.composableMapCount > 0);
        assertEquals(109, stats.composableMapCount + stats.blockedMapCount);
        assertTrue(stats.totalRewrittenScriptEntryCount > 0);
        assertTrue(stats.roundtripVerifiedMapCount > 0);
        assertFalse(stats.blockingReasonCounts.containsKey(MapCompatibilityBlockingReason.NOT_MULTIPLE_SPRITE_MAP_LIST));
    }

    private void verifyRoundtrip(
            BheMapEntryPlan entry,
            MultipleSpriteMapListCompositionResult result,
            String charset
    ) throws Exception {
        Path mapPath = tempDir.resolve(entry.getTargetMapFileName());
        new MapDataGenerator().generate(mapPath.toString(), result.getConvertedMapData(), charset);
        com.giga.nexas.dto.bsdx.map.MapData reparsedMap =
                new MapDataParser().parse(Files.readAllBytes(mapPath), entry.getTargetMapFileName(), charset);
        assertEquals(result.getConvertedMapData().getSpriteMap(), reparsedMap.getSpriteMap());

        Path spmPath = tempDir.resolve(result.getCompositionPlan().getTargetComposedSpriteMapFileName());
        new SpmGenerator().generate(spmPath.toString(), result.getComposedSpm(), charset);
        Spm reparsedSpm = new SpmParser().parse(
                Files.readAllBytes(spmPath),
                result.getCompositionPlan().getTargetComposedSpriteMapFileName(),
                charset
        );
        assertEquals(result.getComposedSpm().getNumAnimData(), reparsedSpm.getNumAnimData());
        assertEquals(result.getComposedSpm().getNumPageData(), reparsedSpm.getNumPageData());
        assertEquals(result.getComposedSpm().getNumImageData(), reparsedSpm.getNumImageData());
    }

    private MultipleSpriteMapListCompositionPlan syntheticPlan() {
        MultipleSpriteMapListCompositionPlan plan = new MultipleSpriteMapListCompositionPlan();
        plan.setSourceMapFileName("synthetic.map");
        plan.setTargetMapFileName("bhe_synthetic.map");
        plan.setTargetComposedSpriteMapFileName("bhe_synthetic.spm");
        plan.setTargetComposedSpriteMapText("bhe_synthetic.spm");
        plan.setSpriteMapListSize(2);

        SourceSpriteMapCompositionPlan first = syntheticSourcePlan(0, syntheticSpm(1, 2, 1, 0), 0, 0, 0);
        SourceSpriteMapCompositionPlan second = syntheticSourcePlan(1, syntheticSpm(2, 3, 3, 0), 1, 2, 1);
        plan.getSourceSpriteMaps().add(first);
        plan.getSourceSpriteMaps().add(second);
        plan.setComposedPageCount(3);
        plan.setComposedImageCount(5);
        plan.setComposedAnimCount(4);
        plan.setComposedPatPageNum(2);

        MapCompatibilityAnalysis analysis = new MapCompatibilityAnalysis();
        analysis.setSourceMapFileName("synthetic.map");
        analysis.setSpriteMapListSize(2);
        analysis.setInspectedScriptEntryCount(2);
        analysis.setRewrittenScriptEntryCount(2);
        plan.setCompatibilityAnalysis(analysis);
        plan.setRewrittenScriptEntryCount(2);
        return plan;
    }

    private SourceSpriteMapCompositionPlan syntheticSourcePlan(
            int index,
            Spm spm,
            int pageOffset,
            int imageOffset,
            int animOffset
    ) {
        SourceSpriteMapCompositionPlan sourcePlan = new SourceSpriteMapCompositionPlan();
        sourcePlan.setSpriteMapIndex(index);
        sourcePlan.setSourceText("source" + index + ".spm");
        sourcePlan.setSourceFileName("source" + index + ".spm");
        sourcePlan.setTargetText("bhe_source" + index + ".spm");
        sourcePlan.setTargetFileName("bhe_source" + index + ".spm");
        sourcePlan.setBsdxSpm(spm);
        sourcePlan.setPageCount(spm.getPageData().size());
        sourcePlan.setImageCount(spm.getImageData().size());
        sourcePlan.setAnimCount(spm.getAnimData().size());
        sourcePlan.setPatPageNum(spm.getPatPageNum());
        sourcePlan.setPageOffset(pageOffset);
        sourcePlan.setImageOffset(imageOffset);
        sourcePlan.setAnimOffset(animOffset);
        return sourcePlan;
    }

    private Spm syntheticSpm(int pageCount, int imageCount, int animCount, int localPageNo) {
        Spm spm = new Spm();
        spm.setExtensionName("spm");
        spm.setSpmVersion("SPM VER-2.00");
        spm.setPatPageNum(1);
        spm.setImageData(new ArrayList<>());
        for (int index = 0; index < imageCount; index++) {
            Spm.SPMImageData image = new Spm.SPMImageData();
            image.setImageName("image" + index + ".png");
            spm.getImageData().add(image);
        }
        spm.setPageData(new ArrayList<>());
        for (int index = 0; index < pageCount; index++) {
            spm.getPageData().add(page(index % imageCount));
        }
        spm.setAnimData(new ArrayList<>());
        for (int index = 0; index < animCount; index++) {
            spm.getAnimData().add(anim(localPageNo));
        }
        spm.setNumImageData(spm.getImageData().size());
        spm.setNumPageData(spm.getPageData().size());
        spm.setNumAnimData(spm.getAnimData().size());
        return spm;
    }

    private Spm.SPMPageData page(int imageNo) {
        Spm.SPMPageData page = new Spm.SPMPageData();
        page.setNumChipData(1);
        page.setPageWidth(16);
        page.setPageHeight(16);
        page.setPageRect(rect(0, 0, 16, 16));
        page.setPageOption(0L);
        page.setRotateCenterX(0);
        page.setRotateCenterY(0);
        page.setHitFlag(0L);
        page.setHitRects(new ArrayList<>());
        Spm.SPMChipData chip = new Spm.SPMChipData();
        chip.setImageNo(imageNo);
        chip.setDstRect(rect(0, 0, 16, 16));
        chip.setChipWidth(16);
        chip.setChipHeight(16);
        chip.setSrcRect(rect(0, 0, 16, 16));
        chip.setDrawOption(0L);
        chip.setDrawOptionValue(0L);
        chip.setOption(0);
        page.setChipData(new ArrayList<>(List.of(chip)));
        return page;
    }

    private Spm.SPMAnimData anim(int pageNo) {
        Spm.SPMAnimData anim = new Spm.SPMAnimData();
        anim.setAnimName("anim" + pageNo);
        anim.setNumPat(1);
        anim.setAnimRotateDirection(0);
        anim.setAnimReverseDirection(0);
        Spm.SPMPatData pat = new Spm.SPMPatData();
        pat.setWaitFrame(1);
        pat.setPageNo(new ArrayList<>(List.of(pageNo)));
        anim.setPatData(new ArrayList<>(List.of(pat)));
        return anim;
    }

    private Spm.SPMRect rect(int left, int top, int right, int bottom) {
        Spm.SPMRect rect = new Spm.SPMRect();
        rect.setLeft(left);
        rect.setTop(top);
        rect.setRight(right);
        rect.setBottom(bottom);
        return rect;
    }

    private MapData syntheticBheMapData() {
        MapData mapData = new MapData("synthetic.map");
        mapData.setMagic("MAPDATA VER-1.00");
        mapData.setWidth(1);
        mapData.setHeight(1);
        mapData.getTileRecords().add(tileRecord());
        mapData.setCameraRectBlock(cameraRectBlock());
        mapData.setRawPointGroup0(rawPointGroupBlock(1, 2));
        mapData.setRawPointGroup1(rawPointGroupBlock(3, 4));
        mapData.setRawRectGroup0(rawRectGroupBlock(1, 2, 3, 4));
        mapData.setRawRectGroup1(rawRectGroupBlock(5, 6, 7, 8));
        mapData.setRawRectGroup2(rawRectGroupBlock(9, 10, 11, 12));
        mapData.setNamedResourceSlotBlocks(resourceSlots());
        mapData.setScriptEntryGroupBlocks(emptyBheScriptGroups());
        mapData.getScriptEntryGroupBlocks().get(0).getEntries().add(scriptEntry(0, 0, 10, 20));
        mapData.getScriptEntryGroupBlocks().get(0).getEntries().add(scriptEntry(1, 2, 30, 40));
        mapData.getScriptEntryGroupBlocks().get(0).setCount(2);
        mapData.setForegroundImage("fg/source.png");
        mapData.setSpriteMapList(new ArrayList<>(List.of("spm/a.spm", "spm/b.spm")));
        return mapData;
    }

    private BheMapEntryPlan syntheticEntryPlan() {
        BheMapEntryPlan entryPlan = new BheMapEntryPlan();
        entryPlan.setSourceMapFileName("synthetic.map");
        entryPlan.setTargetMapFileName("bhe_synthetic.map");
        entryPlan.setTargetGroupResourceName("bhe_synthetic");
        return entryPlan;
    }

    private BheMapResourceRewriteTable syntheticRewriteTable() {
        BheMapResourceRewriteTable table = new BheMapResourceRewriteTable();
        table.put(found(BheMapReferenceType.FOREGROUND, "fg/source.png", "fg/bhe_source.png"));
        for (int slotIndex = 0; slotIndex < 8; slotIndex++) {
            table.put(found(BheMapReferenceType.RESOURCE_SLOT,
                    "slot/slot" + slotIndex + ".png",
                    "slot/bhe_slot" + slotIndex + ".png"));
        }
        return table;
    }

    private BheMapResourceReference found(BheMapReferenceType type, String sourceText, String targetText) {
        BheMapResourceReference reference = new BheMapResourceReference();
        reference.setType(type);
        reference.setSourceText(sourceText);
        reference.setTargetText(targetText);
        reference.setStatus(BheMapReferenceStatus.FOUND_DEFERRED);
        return reference;
    }

    private MapData.TileRecord tileRecord() {
        MapData.TileRecord record = new MapData.TileRecord();
        record.setPackedValue0(10);
        record.setPackedValue1(20);
        record.setByte0((byte) 1);
        record.setByte1((byte) 2);
        return record;
    }

    private MapData.CameraRectBlock cameraRectBlock() {
        MapData.CameraRectBlock block = new MapData.CameraRectBlock();
        block.setCount(1);
        MapData.CameraRectEntry entry = new MapData.CameraRectEntry();
        entry.setLeft(1);
        entry.setTop(2);
        entry.setRight(3);
        entry.setBottom(4);
        block.getEntries().add(entry);
        return block;
    }

    private MapData.RawPointGroupBlock rawPointGroupBlock(int value0, int value1) {
        MapData.RawPointGroupBlock block = new MapData.RawPointGroupBlock();
        block.setCount(1);
        MapData.RawPointGroupEntry entry = new MapData.RawPointGroupEntry();
        entry.setRawValue0(value0);
        entry.setRawValue1(value1);
        block.getEntries().add(entry);
        return block;
    }

    private MapData.RawRectGroupBlock rawRectGroupBlock(int value0, int value1, int value2, int value3) {
        MapData.RawRectGroupBlock block = new MapData.RawRectGroupBlock();
        block.setCount(1);
        MapData.RawRectGroupEntry entry = new MapData.RawRectGroupEntry();
        entry.setRawValue0(value0);
        entry.setRawValue1(value1);
        entry.setRawValue2(value2);
        entry.setRawValue3(value3);
        block.getEntries().add(entry);
        return block;
    }

    private List<MapData.NamedResourceSlotBlock> resourceSlots() {
        List<MapData.NamedResourceSlotBlock> slots = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < 8; slotIndex++) {
            MapData.NamedResourceSlotBlock slot = new MapData.NamedResourceSlotBlock();
            slot.setSlotNum(slotIndex);
            slot.setSlotText("slot/slot" + slotIndex + ".png");
            slot.setParam0(100 + slotIndex);
            slot.setParam1(101 + slotIndex);
            slot.setParam2(102 + slotIndex);
            slot.setParam3(103 + slotIndex);
            slot.setParam4(104 + slotIndex);
            slots.add(slot);
        }
        return slots;
    }

    private List<MapData.ScriptEntryGroupBlock> emptyBheScriptGroups() {
        List<MapData.ScriptEntryGroupBlock> groups = new ArrayList<>();
        for (int groupNum = 0; groupNum < 9; groupNum++) {
            MapData.ScriptEntryGroupBlock group = new MapData.ScriptEntryGroupBlock();
            group.setGroupNum(groupNum);
            group.setCount(0);
            groups.add(group);
        }
        return groups;
    }

    private MapData.ScriptEntry scriptEntry(int groupIndex, int typeId, int x, int y) {
        MapData.ScriptEntry entry = new MapData.ScriptEntry();
        entry.setGroupIndex(groupIndex);
        entry.setTypeId(typeId);
        entry.setX(x);
        entry.setY(y);
        return entry;
    }

    private int spriteMapListSize(MapData mapData) {
        if (mapData.getSpriteMapList() == null) {
            return 0;
        }
        return (int) mapData.getSpriteMapList().stream()
                .filter(value -> value != null && !value.isBlank())
                .count();
    }

    private Path resolveCompleteStaticResourceRoot() {
        String propertyValue = System.getProperty("bhe.staticResourceRoot");
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Paths.get(propertyValue).toAbsolutePath().normalize();
        }
        String environmentValue = System.getenv("BHE_STATIC_RESOURCE_ROOT");
        if (environmentValue != null && !environmentValue.isBlank()) {
            return Paths.get(environmentValue).toAbsolutePath().normalize();
        }
        return null;
    }

    private static class CompositionStats {

        private int multipleSpriteMapListMapCount;
        private int composableMapCount;
        private int blockedMapCount;
        private int totalRewrittenScriptEntryCount;
        private int roundtripVerifiedMapCount;
        private Map<MapCompatibilityBlockingReason, Integer> blockingReasonCounts =
                new EnumMap<>(MapCompatibilityBlockingReason.class);

        @Override
        public String toString() {
            return "multipleSpriteMapListMapCount=" + multipleSpriteMapListMapCount
                    + ", composableMapCount=" + composableMapCount
                    + ", blockedMapCount=" + blockedMapCount
                    + ", totalRewrittenScriptEntryCount=" + totalRewrittenScriptEntryCount
                    + ", roundtripVerifiedMapCount=" + roundtripVerifiedMapCount
                    + ", blockingReasonCounts=" + blockingReasonCounts;
        }
    }
}
