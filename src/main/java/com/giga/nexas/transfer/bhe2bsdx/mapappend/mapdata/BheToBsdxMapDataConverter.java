package com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceRewriteTable;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.scriptgroup.BheScriptGroupCompatibilityPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE MapData 到 BSDX MapData 的最小内存转换器。
 *
 * <p>本转换器不写任何文件；只要存在阻塞问题，就不返回可落盘的 BSDX MapData。</p>
 */
public class BheToBsdxMapDataConverter {

    public static final String BSDX_MAP_MAGIC = "MAPDATA VER-0.90";

    private final BheScriptGroupCompatibilityPolicy scriptGroupCompatibilityPolicy;

    public BheToBsdxMapDataConverter() {
        this(new BheScriptGroupCompatibilityPolicy());
    }

    public BheToBsdxMapDataConverter(BheScriptGroupCompatibilityPolicy scriptGroupCompatibilityPolicy) {
        this.scriptGroupCompatibilityPolicy = scriptGroupCompatibilityPolicy == null
                ? new BheScriptGroupCompatibilityPolicy()
                : scriptGroupCompatibilityPolicy;
    }

    public BheMapDataConversionResult convert(
            BheMapEntryPlan entryPlan,
            com.giga.nexas.dto.bhe.map.MapData sourceMapData,
            BheMapResourceRewriteTable rewriteTable
    ) {
        if (entryPlan == null) {
            throw new IllegalArgumentException("entryPlan 不能为空");
        }
        if (sourceMapData == null) {
            throw new IllegalArgumentException("sourceMapData 不能为空");
        }
        if (rewriteTable == null) {
            throw new IllegalArgumentException("rewriteTable 不能为空");
        }

        BheMapDataConversionResult result = new BheMapDataConversionResult();
        BheMapDataConversionAudit audit = result.getAudit();
        ResourceRewrite foreground = requireRewrite(
                result,
                entryPlan,
                BheMapReferenceType.FOREGROUND,
                sourceMapData.getForegroundImage(),
                rewriteTable
        );
        List<ResourceSlotRewrite> resourceSlots = convertResourceSlots(result, entryPlan, sourceMapData, rewriteTable);
        ResourceRewrite spriteMap = resolveSpriteMap(result, entryPlan, sourceMapData, rewriteTable);
        BheScriptGroupCompatibilityPolicy.ScriptGroupConversion scriptGroupConversion =
                scriptGroupCompatibilityPolicy.convert(
                        entryPlan.getSourceMapFileName(),
                        sourceMapData.getScriptEntryGroupBlocks(),
                        audit
                );
        for (BheMapDataConversionIssue issue : scriptGroupConversion.getBlockingIssues()) {
            result.addBlockingIssue(issue);
        }

        if (!result.getBlockingIssues().isEmpty()) {
            return result;
        }

        com.giga.nexas.dto.bsdx.map.MapData target = new com.giga.nexas.dto.bsdx.map.MapData(entryPlan.getTargetMapFileName());
        target.setMagic(BSDX_MAP_MAGIC);
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
        target.setScriptEntryGroupBlocks(scriptGroupConversion.getTargetGroups());
        target.setForegroundImage(foreground.targetText());
        target.setSpriteMap(spriteMap == null ? null : spriteMap.targetText());
        result.setConvertedMapData(target);
        return result;
    }

    private List<ResourceSlotRewrite> convertResourceSlots(
            BheMapDataConversionResult result,
            BheMapEntryPlan entryPlan,
            com.giga.nexas.dto.bhe.map.MapData sourceMapData,
            BheMapResourceRewriteTable rewriteTable
    ) {
        List<ResourceSlotRewrite> rewrites = new ArrayList<>();
        List<com.giga.nexas.dto.bhe.map.MapData.NamedResourceSlotBlock> sourceSlots =
                sourceMapData.getNamedResourceSlotBlocks() == null
                        ? List.of()
                        : sourceMapData.getNamedResourceSlotBlocks();
        for (int slotIndex = 0; slotIndex < com.giga.nexas.dto.bhe.map.MapData.RESOURCE_SLOT_COUNT; slotIndex++) {
            com.giga.nexas.dto.bhe.map.MapData.NamedResourceSlotBlock sourceSlot =
                    slotIndex < sourceSlots.size() ? sourceSlots.get(slotIndex) : null;
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

    private ResourceRewrite resolveSpriteMap(
            BheMapDataConversionResult result,
            BheMapEntryPlan entryPlan,
            com.giga.nexas.dto.bhe.map.MapData sourceMapData,
            BheMapResourceRewriteTable rewriteTable
    ) {
        List<String> spriteMapList = sourceMapData.getSpriteMapList() == null ? List.of() : sourceMapData.getSpriteMapList();
        if (spriteMapList.size() > 1) {
            BheMapDataConversionIssue issue = BheMapDataConversionIssue.blocking(
                    BheMapDataConversionIssueType.UNSUPPORTED_MULTIPLE_SPRITE_MAPS,
                    entryPlan.getSourceMapFileName(),
                    "BHE spriteMapList 多于一项，不能自动降级到 BSDX 单 spriteMap"
            );
            issue.setReferenceType(BheMapReferenceType.SPRITE_MAP_LIST);
            issue.setReferenceText(String.join(", ", spriteMapList));
            result.addBlockingIssue(issue);
            return null;
        }
        if (spriteMapList.size() == 1) {
            return requireRewrite(
                    result,
                    entryPlan,
                    BheMapReferenceType.SPRITE_MAP_LIST,
                    spriteMapList.get(0),
                    rewriteTable
            );
        }
        return requireRewrite(
                result,
                entryPlan,
                BheMapReferenceType.SPRITE_MAP,
                sourceMapData.getSpriteMap(),
                rewriteTable
        );
    }

    private ResourceRewrite requireRewrite(
            BheMapDataConversionResult result,
            BheMapEntryPlan entryPlan,
            BheMapReferenceType referenceType,
            String sourceText
    ) {
        return requireRewrite(
                result,
                entryPlan,
                referenceType,
                sourceText,
                BheMapResourceRewriteTable.fromEntryPlan(entryPlan)
        );
    }

    private ResourceRewrite requireRewrite(
            BheMapDataConversionResult result,
            BheMapEntryPlan entryPlan,
            BheMapReferenceType referenceType,
            String sourceText,
            BheMapResourceRewriteTable rewriteTable
    ) {
        if (isBlank(sourceText)) {
            return new ResourceRewrite(sourceText, null);
        }
        BheMapResourceReference reference = rewriteTable.find(referenceType, sourceText);
        if (reference == null) {
            BheMapDataConversionIssue issue = issue(
                    BheMapDataConversionIssueType.MISSING_RESOURCE_REWRITE,
                    entryPlan,
                    referenceType,
                    sourceText,
                    "资源引用没有进入 rewrite table"
            );
            result.addBlockingIssue(issue);
            return new ResourceRewrite(sourceText, null);
        }
        if (reference.isMissing()) {
            BheMapDataConversionIssue issue = issue(
                    BheMapDataConversionIssueType.MISSING_FINAL_REFERENCE,
                    entryPlan,
                    referenceType,
                    sourceText,
                    "最终写入的资源引用源文件缺失"
            );
            result.addBlockingIssue(issue);
            return new ResourceRewrite(sourceText, null);
        }
        if (isBlank(reference.getTargetText())) {
            BheMapDataConversionIssue issue = issue(
                    BheMapDataConversionIssueType.MISSING_TARGET_TEXT,
                    entryPlan,
                    referenceType,
                    sourceText,
                    "资源引用缺少 targetText"
            );
            result.addBlockingIssue(issue);
            return new ResourceRewrite(sourceText, null);
        }
        result.getAudit().recordResourceRewrite();
        return new ResourceRewrite(sourceText, reference.getTargetText());
    }

    private BheMapDataConversionIssue issue(
            BheMapDataConversionIssueType type,
            BheMapEntryPlan entryPlan,
            BheMapReferenceType referenceType,
            String sourceText,
            String message
    ) {
        BheMapDataConversionIssue issue = BheMapDataConversionIssue.blocking(
                type,
                entryPlan.getSourceMapFileName(),
                message
        );
        issue.setReferenceType(referenceType);
        issue.setReferenceText(sourceText);
        return issue;
    }

    private List<com.giga.nexas.dto.bsdx.map.MapData.NamedResourceSlotBlock> toTargetResourceSlots(
            List<ResourceSlotRewrite> rewrites
    ) {
        List<com.giga.nexas.dto.bsdx.map.MapData.NamedResourceSlotBlock> targetSlots = new ArrayList<>();
        for (int slotIndex = 0; slotIndex < com.giga.nexas.dto.bsdx.map.MapData.RESOURCE_SLOT_COUNT; slotIndex++) {
            ResourceSlotRewrite rewrite = slotIndex < rewrites.size() ? rewrites.get(slotIndex) : null;
            com.giga.nexas.dto.bhe.map.MapData.NamedResourceSlotBlock sourceSlot =
                    rewrite == null ? null : rewrite.sourceSlot();
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
            List<com.giga.nexas.dto.bhe.map.MapData.TileRecord> sourceRecords
    ) {
        List<com.giga.nexas.dto.bsdx.map.MapData.TileRecord> targetRecords = new ArrayList<>();
        if (sourceRecords == null) {
            return targetRecords;
        }
        for (com.giga.nexas.dto.bhe.map.MapData.TileRecord source : sourceRecords) {
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
            com.giga.nexas.dto.bhe.map.MapData.CameraRectBlock source
    ) {
        com.giga.nexas.dto.bsdx.map.MapData.CameraRectBlock target =
                new com.giga.nexas.dto.bsdx.map.MapData.CameraRectBlock();
        if (source == null) {
            target.setCount(0);
            return target;
        }
        target.setCount(source.getCount());
        for (com.giga.nexas.dto.bhe.map.MapData.CameraRectEntry sourceEntry : source.getEntries()) {
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
            com.giga.nexas.dto.bhe.map.MapData.RawPointGroupBlock source
    ) {
        com.giga.nexas.dto.bsdx.map.MapData.RawPointGroupBlock target =
                new com.giga.nexas.dto.bsdx.map.MapData.RawPointGroupBlock();
        if (source == null) {
            target.setCount(0);
            return target;
        }
        target.setCount(source.getCount());
        for (com.giga.nexas.dto.bhe.map.MapData.RawPointGroupEntry sourceEntry : source.getEntries()) {
            com.giga.nexas.dto.bsdx.map.MapData.RawPointGroupEntry targetEntry =
                    new com.giga.nexas.dto.bsdx.map.MapData.RawPointGroupEntry();
            targetEntry.setRawValue0(sourceEntry.getRawValue0());
            targetEntry.setRawValue1(sourceEntry.getRawValue1());
            target.getEntries().add(targetEntry);
        }
        return target;
    }

    private com.giga.nexas.dto.bsdx.map.MapData.RawRectGroupBlock copyRawRectGroupBlock(
            com.giga.nexas.dto.bhe.map.MapData.RawRectGroupBlock source
    ) {
        com.giga.nexas.dto.bsdx.map.MapData.RawRectGroupBlock target =
                new com.giga.nexas.dto.bsdx.map.MapData.RawRectGroupBlock();
        if (source == null) {
            target.setCount(0);
            return target;
        }
        target.setCount(source.getCount());
        for (com.giga.nexas.dto.bhe.map.MapData.RawRectGroupEntry sourceEntry : source.getEntries()) {
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

    private record ResourceSlotRewrite(
            com.giga.nexas.dto.bhe.map.MapData.NamedResourceSlotBlock sourceSlot,
            ResourceRewrite rewrite
    ) {
    }
}
