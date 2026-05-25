package com.giga.nexas.transfer.bhe2bsdx.mapappend.composition;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheToBsdxMapDataConverter;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceRewriteTable;

import java.util.ArrayList;
import java.util.List;

public class MultipleSpriteMapListMapDataConverter {

    public MultipleSpriteMapListCompositionResult convert(
            BheMapEntryPlan entryPlan,
            MapData sourceMapData,
            BheMapResourceRewriteTable rewriteTable,
            MultipleSpriteMapListCompositionPlan compositionPlan
    ) {
        MultipleSpriteMapListCompositionResult result = new MultipleSpriteMapListCompositionResult();
        result.setCompositionPlan(compositionPlan);
        if (compositionPlan == null || compositionPlan.getCompatibilityAnalysis() == null) {
            result.addBlockingIssue(MapCompatibilityIssue.blocking(
                    MapCompatibilityBlockingReason.NOT_MULTIPLE_SPRITE_MAP_LIST,
                    entryPlan == null ? null : entryPlan.getSourceMapFileName(),
                    "composition plan is missing"
            ));
            return result;
        }
        for (MapCompatibilityIssue issue : compositionPlan.getCompatibilityAnalysis().getBlockingIssues()) {
            result.addBlockingIssue(issue);
        }
        if (!result.getBlockingIssues().isEmpty()) {
            return result;
        }

        ResourceRewrite foreground = requireRewrite(
                result,
                entryPlan,
                BheMapReferenceType.FOREGROUND,
                sourceMapData.getForegroundImage(),
                rewriteTable
        );
        List<ResourceSlotRewrite> resourceSlots = convertResourceSlots(result, entryPlan, sourceMapData, rewriteTable);
        if (!result.getBlockingIssues().isEmpty()) {
            return result;
        }

        com.giga.nexas.dto.bsdx.map.MapData target = new com.giga.nexas.dto.bsdx.map.MapData(
                entryPlan == null ? null : entryPlan.getTargetMapFileName()
        );
        target.setExtensionName("map");
        target.setMagic(BheToBsdxMapDataConverter.BSDX_MAP_MAGIC);
        target.setWidth(sourceMapData.getWidth());
        target.setHeight(sourceMapData.getHeight());
        target.setTileRecords(copyTileRecords(sourceMapData.getTileRecords()));
        target.setCameraRectBlock(copyCameraRectBlock(sourceMapData.getCameraRectBlock()));
        target.setRawPointGroup0(copyRawPointGroupBlock(sourceMapData.getRawPointGroup0()));
        target.setRawPointGroup1(copyRawPointGroupBlock(sourceMapData.getRawPointGroup1()));
        target.setRawRectGroup0(copyRawRectGroupBlock(sourceMapData.getRawRectGroup0()));
        target.setRawRectGroup1(copyRawRectGroupBlock(sourceMapData.getRawRectGroup1()));
        target.setRawRectGroup2(copyRawRectGroupBlock(sourceMapData.getRawRectGroup2()));
        target.setNamedResourceSlotBlocks(toTargetResourceSlots(resourceSlots));
        target.setScriptEntryGroupBlocks(toTargetScriptGroups(sourceMapData, compositionPlan));
        target.setForegroundImage(foreground.targetText());
        target.setSpriteMap(compositionPlan.getTargetComposedSpriteMapText());
        result.setConvertedMapData(target);
        result.setComposedSpm(new BheMapAppendSpmComposer().compose(compositionPlan));
        return result;
    }

    private List<ResourceSlotRewrite> convertResourceSlots(
            MultipleSpriteMapListCompositionResult result,
            BheMapEntryPlan entryPlan,
            MapData sourceMapData,
            BheMapResourceRewriteTable rewriteTable
    ) {
        List<ResourceSlotRewrite> rewrites = new ArrayList<>();
        List<MapData.NamedResourceSlotBlock> sourceSlots = sourceMapData.getNamedResourceSlotBlocks() == null
                ? List.of()
                : sourceMapData.getNamedResourceSlotBlocks();
        for (int slotIndex = 0; slotIndex < MapData.RESOURCE_SLOT_COUNT; slotIndex++) {
            MapData.NamedResourceSlotBlock sourceSlot = slotIndex < sourceSlots.size() ? sourceSlots.get(slotIndex) : null;
            ResourceRewrite rewrite = requireRewrite(
                    result,
                    entryPlan,
                    BheMapReferenceType.RESOURCE_SLOT,
                    sourceSlot == null ? null : sourceSlot.getSlotText(),
                    rewriteTable
            );
            rewrites.add(new ResourceSlotRewrite(sourceSlot, rewrite));
        }
        return rewrites;
    }

    private ResourceRewrite requireRewrite(
            MultipleSpriteMapListCompositionResult result,
            BheMapEntryPlan entryPlan,
            BheMapReferenceType referenceType,
            String sourceText,
            BheMapResourceRewriteTable rewriteTable
    ) {
        if (isBlank(sourceText)) {
            return new ResourceRewrite(sourceText, null);
        }
        BheMapResourceReference reference = rewriteTable == null ? null : rewriteTable.find(referenceType, sourceText);
        if (reference == null) {
            result.addBlockingIssue(referenceIssue(
                    MapCompatibilityBlockingReason.MISSING_RESOURCE_REWRITE,
                    entryPlan,
                    referenceType,
                    sourceText,
                    "resource reference is missing from rewrite table"
            ));
            return new ResourceRewrite(sourceText, null);
        }
        if (reference.getStatus() != BheMapReferenceStatus.FOUND_DEFERRED) {
            result.addBlockingIssue(referenceIssue(
                    MapCompatibilityBlockingReason.MISSING_FINAL_REFERENCE,
                    entryPlan,
                    referenceType,
                    sourceText,
                    "resource source file is missing"
            ));
            return new ResourceRewrite(sourceText, null);
        }
        if (isBlank(reference.getTargetText())) {
            result.addBlockingIssue(referenceIssue(
                    MapCompatibilityBlockingReason.MISSING_TARGET_TEXT,
                    entryPlan,
                    referenceType,
                    sourceText,
                    "resource targetText is missing"
            ));
            return new ResourceRewrite(sourceText, null);
        }
        return new ResourceRewrite(sourceText, reference.getTargetText());
    }

    private MapCompatibilityIssue referenceIssue(
            MapCompatibilityBlockingReason reason,
            BheMapEntryPlan entryPlan,
            BheMapReferenceType referenceType,
            String sourceText,
            String message
    ) {
        MapCompatibilityIssue issue = MapCompatibilityIssue.blocking(
                reason,
                entryPlan == null ? null : entryPlan.getSourceMapFileName(),
                message
        );
        issue.setReferenceType(referenceType);
        issue.setReferenceText(sourceText);
        return issue;
    }

    private List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> toTargetScriptGroups(
            MapData sourceMapData,
            MultipleSpriteMapListCompositionPlan compositionPlan
    ) {
        List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> targetGroups = emptyTargetGroups();
        if (sourceMapData.getScriptEntryGroupBlocks() == null) {
            return targetGroups;
        }
        for (int outerGroupIndex = 0; outerGroupIndex < sourceMapData.getScriptEntryGroupBlocks().size(); outerGroupIndex++) {
            if (outerGroupIndex >= targetGroups.size()) {
                continue;
            }
            MapData.ScriptEntryGroupBlock sourceGroup = sourceMapData.getScriptEntryGroupBlocks().get(outerGroupIndex);
            if (sourceGroup == null || sourceGroup.getEntries() == null) {
                continue;
            }
            com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock targetGroup = targetGroups.get(outerGroupIndex);
            for (MapData.ScriptEntry sourceEntry : sourceGroup.getEntries()) {
                SourceSpriteMapCompositionPlan sourcePlan =
                        compositionPlan.getSourceSpriteMaps().get(sourceEntry.getGroupIndex());
                com.giga.nexas.dto.bsdx.map.MapData.ScriptEntry targetEntry =
                        new com.giga.nexas.dto.bsdx.map.MapData.ScriptEntry();
                targetEntry.setTypeId(sourcePlan.getAnimOffset() + sourceEntry.getTypeId());
                targetEntry.setX(sourceEntry.getX());
                targetEntry.setY(sourceEntry.getY());
                targetGroup.getEntries().add(targetEntry);
            }
            targetGroup.setCount(targetGroup.getEntries().size());
        }
        return targetGroups;
    }

    private List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> emptyTargetGroups() {
        List<com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock> groups = new ArrayList<>();
        for (int groupNum = 0; groupNum < com.giga.nexas.dto.bsdx.map.MapData.SCRIPT_GROUP_COUNT; groupNum++) {
            com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock group =
                    new com.giga.nexas.dto.bsdx.map.MapData.ScriptEntryGroupBlock();
            group.setGroupNum(groupNum);
            group.setCount(0);
            groups.add(group);
        }
        return groups;
    }

    private List<com.giga.nexas.dto.bsdx.map.MapData.NamedResourceSlotBlock> toTargetResourceSlots(
            List<ResourceSlotRewrite> rewrites
    ) {
        List<com.giga.nexas.dto.bsdx.map.MapData.NamedResourceSlotBlock> targetSlots = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < com.giga.nexas.dto.bsdx.map.MapData.RESOURCE_SLOT_COUNT; slotIndex++) {
            ResourceSlotRewrite rewrite = slotIndex < rewrites.size() ? rewrites.get(slotIndex) : null;
            MapData.NamedResourceSlotBlock sourceSlot = rewrite == null ? null : rewrite.sourceSlot();
            com.giga.nexas.dto.bsdx.map.MapData.NamedResourceSlotBlock targetSlot =
                    new com.giga.nexas.dto.bsdx.map.MapData.NamedResourceSlotBlock();
            targetSlot.setSlotNum(sourceSlot == null ? slotIndex : sourceSlot.getSlotNum());
            targetSlot.setSlotText(rewrite == null || rewrite.rewrite() == null ? null : rewrite.rewrite().targetText());
            targetSlot.setParam0(sourceSlot == null ? 0 : sourceSlot.getParam0());
            targetSlot.setParam1(sourceSlot == null ? 0 : sourceSlot.getParam1());
            targetSlot.setParam2(sourceSlot == null ? 0 : sourceSlot.getParam2());
            targetSlot.setParam3(sourceSlot == null ? 0 : sourceSlot.getParam3());
            targetSlot.setParam4(sourceSlot == null ? 0 : sourceSlot.getParam4());
            targetSlots.add(targetSlot);
        }
        return targetSlots;
    }

    private List<com.giga.nexas.dto.bsdx.map.MapData.TileRecord> copyTileRecords(
            List<MapData.TileRecord> sourceRecords
    ) {
        List<com.giga.nexas.dto.bsdx.map.MapData.TileRecord> targetRecords = new ArrayList<>();
        if (sourceRecords == null) {
            return targetRecords;
        }
        for (MapData.TileRecord source : sourceRecords) {
            com.giga.nexas.dto.bsdx.map.MapData.TileRecord target =
                    new com.giga.nexas.dto.bsdx.map.MapData.TileRecord();
            target.setPackedValue0(source.getPackedValue0());
            target.setPackedValue1(source.getPackedValue1());
            target.setByte0(source.getByte0());
            target.setByte1(source.getByte1());
            targetRecords.add(target);
        }
        return targetRecords;
    }

    private com.giga.nexas.dto.bsdx.map.MapData.CameraRectBlock copyCameraRectBlock(
            MapData.CameraRectBlock source
    ) {
        com.giga.nexas.dto.bsdx.map.MapData.CameraRectBlock target =
                new com.giga.nexas.dto.bsdx.map.MapData.CameraRectBlock();
        if (source == null) {
            target.setCount(0);
            return target;
        }
        target.setCount(source.getCount());
        for (MapData.CameraRectEntry sourceEntry : source.getEntries()) {
            com.giga.nexas.dto.bsdx.map.MapData.CameraRectEntry targetEntry =
                    new com.giga.nexas.dto.bsdx.map.MapData.CameraRectEntry();
            targetEntry.setLeft(sourceEntry.getLeft());
            targetEntry.setTop(sourceEntry.getTop());
            targetEntry.setRight(sourceEntry.getRight());
            targetEntry.setBottom(sourceEntry.getBottom());
            target.getEntries().add(targetEntry);
        }
        return target;
    }

    private com.giga.nexas.dto.bsdx.map.MapData.RawPointGroupBlock copyRawPointGroupBlock(
            MapData.RawPointGroupBlock source
    ) {
        com.giga.nexas.dto.bsdx.map.MapData.RawPointGroupBlock target =
                new com.giga.nexas.dto.bsdx.map.MapData.RawPointGroupBlock();
        if (source == null) {
            target.setCount(0);
            return target;
        }
        target.setCount(source.getCount());
        for (MapData.RawPointGroupEntry sourceEntry : source.getEntries()) {
            com.giga.nexas.dto.bsdx.map.MapData.RawPointGroupEntry targetEntry =
                    new com.giga.nexas.dto.bsdx.map.MapData.RawPointGroupEntry();
            targetEntry.setRawValue0(sourceEntry.getRawValue0());
            targetEntry.setRawValue1(sourceEntry.getRawValue1());
            target.getEntries().add(targetEntry);
        }
        return target;
    }

    private com.giga.nexas.dto.bsdx.map.MapData.RawRectGroupBlock copyRawRectGroupBlock(
            MapData.RawRectGroupBlock source
    ) {
        com.giga.nexas.dto.bsdx.map.MapData.RawRectGroupBlock target =
                new com.giga.nexas.dto.bsdx.map.MapData.RawRectGroupBlock();
        if (source == null) {
            target.setCount(0);
            return target;
        }
        target.setCount(source.getCount());
        for (MapData.RawRectGroupEntry sourceEntry : source.getEntries()) {
            com.giga.nexas.dto.bsdx.map.MapData.RawRectGroupEntry targetEntry =
                    new com.giga.nexas.dto.bsdx.map.MapData.RawRectGroupEntry();
            targetEntry.setRawValue0(sourceEntry.getRawValue0());
            targetEntry.setRawValue1(sourceEntry.getRawValue1());
            targetEntry.setRawValue2(sourceEntry.getRawValue2());
            targetEntry.setRawValue3(sourceEntry.getRawValue3());
            target.getEntries().add(targetEntry);
        }
        return target;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record ResourceRewrite(String sourceText, String targetText) {
    }

    private record ResourceSlotRewrite(MapData.NamedResourceSlotBlock sourceSlot, ResourceRewrite rewrite) {
    }
}
