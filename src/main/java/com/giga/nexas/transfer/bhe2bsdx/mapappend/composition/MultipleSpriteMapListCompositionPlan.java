package com.giga.nexas.transfer.bhe2bsdx.mapappend.composition;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MultipleSpriteMapListCompositionPlan {

    private String sourceMapFileName;
    private String targetMapFileName;
    private String targetComposedSpriteMapFileName;
    private String targetComposedSpriteMapText;
    private int spriteMapListSize;
    private int composedPageCount;
    private int composedImageCount;
    private int composedAnimCount;
    private int composedChipCount;
    private int composedHitCount;
    private int composedPatPageNum;
    private int rewrittenScriptEntryCount;
    private MapCompatibilityAnalysis compatibilityAnalysis = new MapCompatibilityAnalysis();
    private List<SourceSpriteMapCompositionPlan> sourceSpriteMaps = new ArrayList<>();

    public boolean isComposable() {
        return compatibilityAnalysis != null && compatibilityAnalysis.isComposable();
    }
}
