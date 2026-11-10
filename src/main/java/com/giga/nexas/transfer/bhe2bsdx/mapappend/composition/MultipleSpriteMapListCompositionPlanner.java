package com.giga.nexas.transfer.bhe2bsdx.mapappend.composition;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MultipleSpriteMapListCompositionPlanner {

    private final BheBinService bheBinService;
    private final BheMapAppendSpmDtoConverter spmDtoConverter;
    private final MapCompatibilityAnalyzer compatibilityAnalyzer;

    public MultipleSpriteMapListCompositionPlanner() {
        this(new BheBinService(), new BheMapAppendSpmDtoConverter(), new MapCompatibilityAnalyzer());
    }

    public MultipleSpriteMapListCompositionPlanner(
            BheBinService bheBinService,
            BheMapAppendSpmDtoConverter spmDtoConverter,
            MapCompatibilityAnalyzer compatibilityAnalyzer
    ) {
        this.bheBinService = bheBinService == null ? new BheBinService() : bheBinService;
        this.spmDtoConverter = spmDtoConverter == null ? new BheMapAppendSpmDtoConverter() : spmDtoConverter;
        this.compatibilityAnalyzer = compatibilityAnalyzer == null ? new MapCompatibilityAnalyzer() : compatibilityAnalyzer;
    }

    public MultipleSpriteMapListCompositionPlan plan(
            BheMapEntryPlan entryPlan,
            MapData sourceMapData,
            String charset
    ) {
        MultipleSpriteMapListCompositionPlan compositionPlan = new MultipleSpriteMapListCompositionPlan();
        compositionPlan.setSourceMapFileName(entryPlan == null ? null : entryPlan.getSourceMapFileName());
        compositionPlan.setTargetMapFileName(entryPlan == null ? null : entryPlan.getTargetMapFileName());
        compositionPlan.setTargetComposedSpriteMapFileName(targetComposedSpriteMapFileName(entryPlan));
        compositionPlan.setTargetComposedSpriteMapText(targetComposedSpriteMapFileName(entryPlan));

        List<String> spriteMapList = normalizedSpriteMapList(sourceMapData);
        compositionPlan.setSpriteMapListSize(spriteMapList.size());
        Map<String, BheMapResourceReference> referencesBySourceText = spriteMapListReferences(entryPlan);

        int pageOffset = 0;
        int imageOffset = 0;
        int animOffset = 0;
        int chipCount = 0;
        int hitCount = 0;
        int composedPatPageNum = 0;
        for (int index = 0; index < spriteMapList.size(); index++) {
            SourceSpriteMapCompositionPlan sourcePlan = buildSourcePlan(
                    index,
                    spriteMapList.get(index),
                    referencesBySourceText.get(spriteMapList.get(index)),
                    charset
            );
            sourcePlan.setPageOffset(pageOffset);
            sourcePlan.setImageOffset(imageOffset);
            sourcePlan.setAnimOffset(animOffset);
            compositionPlan.getSourceSpriteMaps().add(sourcePlan);
            pageOffset += sourcePlan.getPageCount();
            imageOffset += sourcePlan.getImageCount();
            animOffset += sourcePlan.getAnimCount();
            chipCount += sourcePlan.getChipCount();
            hitCount += sourcePlan.getHitCount();
            composedPatPageNum = Math.max(composedPatPageNum, sourcePlan.getPatPageNum());
        }

        compositionPlan.setComposedPageCount(pageOffset);
        compositionPlan.setComposedImageCount(imageOffset);
        compositionPlan.setComposedAnimCount(animOffset);
        compositionPlan.setComposedChipCount(chipCount);
        compositionPlan.setComposedHitCount(hitCount);
        compositionPlan.setComposedPatPageNum(composedPatPageNum);
        MapCompatibilityAnalysis analysis = compatibilityAnalyzer.analyze(entryPlan, sourceMapData, compositionPlan);
        compositionPlan.setCompatibilityAnalysis(analysis);
        compositionPlan.setRewrittenScriptEntryCount(analysis.getRewrittenScriptEntryCount());
        return compositionPlan;
    }

    private SourceSpriteMapCompositionPlan buildSourcePlan(
            int spriteMapIndex,
            String sourceText,
            BheMapResourceReference reference,
            String charset
    ) {
        SourceSpriteMapCompositionPlan sourcePlan = new SourceSpriteMapCompositionPlan();
        sourcePlan.setSpriteMapIndex(spriteMapIndex);
        sourcePlan.setSourceText(sourceText);
        sourcePlan.setSourceFileName(reference == null ? sourceFileName(sourceText) : reference.getSourceFileName());
        sourcePlan.setTargetText(reference == null ? null : reference.getTargetText());
        sourcePlan.setTargetFileName(reference == null ? null : reference.getTargetFileName());
        sourcePlan.setSourcePath(reference == null ? null : reference.getSourcePath());
        if (reference == null) {
            return sourcePlan;
        }
        if (reference.getStatus() != BheMapReferenceStatus.FOUND_DEFERRED || reference.getSourcePath() == null) {
            return sourcePlan;
        }
        try {
            com.giga.nexas.dto.bhe.spm.Spm sourceSpm =
                    (com.giga.nexas.dto.bhe.spm.Spm) bheBinService.parse(reference.getSourcePath().toString(), charset).getData();
            com.giga.nexas.dto.bsdx.spm.Spm bsdxSpm = spmDtoConverter.convert(sourceSpm);
            sourcePlan.setSourceSpm(sourceSpm);
            sourcePlan.setBsdxSpm(bsdxSpm);
            applyCounts(sourcePlan, bsdxSpm);
        } catch (Exception e) {
            sourcePlan.setParseProblem("source SPM parse failed: " + reference.getSourcePath() + " -> " + e.getMessage());
        }
        return sourcePlan;
    }

    private void applyCounts(SourceSpriteMapCompositionPlan sourcePlan, com.giga.nexas.dto.bsdx.spm.Spm spm) {
        sourcePlan.setPageCount(safeSize(spm.getPageData()));
        sourcePlan.setImageCount(safeSize(spm.getImageData()));
        sourcePlan.setAnimCount(safeSize(spm.getAnimData()));
        sourcePlan.setPatPageNum(spm.getPatPageNum() == null ? 0 : Math.max(0, spm.getPatPageNum()));
        sourcePlan.setChipCount(countChips(spm));
        sourcePlan.setHitCount(countHits(spm));
    }

    private Map<String, BheMapResourceReference> spriteMapListReferences(BheMapEntryPlan entryPlan) {
        Map<String, BheMapResourceReference> references = new LinkedHashMap<>();
        if (entryPlan == null
                || entryPlan.getResourceReferences() == null
                || entryPlan.getResourceReferences().getReferences() == null) {
            return references;
        }
        for (BheMapResourceReference reference : entryPlan.getResourceReferences().getReferences()) {
            if (reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST && reference.getSourceText() != null) {
                references.put(reference.getSourceText().trim(), reference);
            }
        }
        return references;
    }

    private List<String> normalizedSpriteMapList(MapData sourceMapData) {
        if (sourceMapData == null || sourceMapData.getSpriteMapList() == null) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String value : sourceMapData.getSpriteMapList()) {
            if (value != null && !value.isBlank()) {
                values.add(value.trim());
            }
        }
        return values;
    }

    private String targetComposedSpriteMapFileName(BheMapEntryPlan entryPlan) {
        if (entryPlan == null || entryPlan.getTargetGroupResourceName() == null
                || entryPlan.getTargetGroupResourceName().isBlank()) {
            return null;
        }
        return entryPlan.getTargetGroupResourceName() + ".spm";
    }

    private String sourceFileName(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        return Path.of(reference.trim().replace('\\', '/')).getFileName().toString();
    }

    private int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private int countChips(com.giga.nexas.dto.bsdx.spm.Spm spm) {
        if (spm.getPageData() == null) {
            return 0;
        }
        int count = 0;
        for (com.giga.nexas.dto.bsdx.spm.Spm.SPMPageData page : spm.getPageData()) {
            count += page == null || page.getChipData() == null ? 0 : page.getChipData().size();
        }
        return count;
    }

    private int countHits(com.giga.nexas.dto.bsdx.spm.Spm spm) {
        if (spm.getPageData() == null) {
            return 0;
        }
        int count = 0;
        for (com.giga.nexas.dto.bsdx.spm.Spm.SPMPageData page : spm.getPageData()) {
            count += page == null || page.getHitRects() == null ? 0 : page.getHitRects().size();
        }
        return count;
    }
}
