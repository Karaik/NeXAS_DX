package com.giga.nexas.transfer.bhe2bsdx.mapappend.composition;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;

import java.util.List;

public class MapCompatibilityAnalyzer {

    public MapCompatibilityAnalysis analyze(
            BheMapEntryPlan entryPlan,
            MapData sourceMapData,
            MultipleSpriteMapListCompositionPlan compositionPlan
    ) {
        MapCompatibilityAnalysis analysis = new MapCompatibilityAnalysis();
        analysis.setSourceMapFileName(entryPlan == null ? null : entryPlan.getSourceMapFileName());
        List<String> spriteMapList = normalizedSpriteMapList(sourceMapData);
        analysis.setSpriteMapListSize(spriteMapList.size());
        if (spriteMapList.size() <= 1) {
            analysis.addIssue(issue(
                    MapCompatibilityBlockingReason.NOT_MULTIPLE_SPRITE_MAP_LIST,
                    entryPlan,
                    "map does not use multiple spriteMapList entries"
            ));
            return analysis;
        }
        if (compositionPlan == null) {
            analysis.addIssue(issue(
                    MapCompatibilityBlockingReason.MISSING_SOURCE_SPM,
                    entryPlan,
                    "composition plan is missing"
            ));
            return analysis;
        }

        validateSourceSpriteMaps(entryPlan, compositionPlan, analysis);
        validateScriptEntries(entryPlan, sourceMapData, compositionPlan, analysis);
        return analysis;
    }

    private void validateSourceSpriteMaps(
            BheMapEntryPlan entryPlan,
            MultipleSpriteMapListCompositionPlan compositionPlan,
            MapCompatibilityAnalysis analysis
    ) {
        for (SourceSpriteMapCompositionPlan sourcePlan : compositionPlan.getSourceSpriteMaps()) {
            if (sourcePlan.getSourcePath() == null) {
                MapCompatibilityIssue issue = issue(
                        MapCompatibilityBlockingReason.MISSING_SOURCE_SPM,
                        entryPlan,
                        "source SPM is missing: " + sourcePlan.getSourceText()
                );
                applySourcePlan(issue, sourcePlan);
                analysis.addIssue(issue);
                continue;
            }
            if (sourcePlan.getParseProblem() != null) {
                MapCompatibilityIssue issue = issue(
                        MapCompatibilityBlockingReason.SPM_PARSE_FAILURE,
                        entryPlan,
                        sourcePlan.getParseProblem()
                );
                applySourcePlan(issue, sourcePlan);
                analysis.addIssue(issue);
                continue;
            }
            validateSpmInternalReferences(entryPlan, sourcePlan, analysis);
        }
    }

    private void validateSpmInternalReferences(
            BheMapEntryPlan entryPlan,
            SourceSpriteMapCompositionPlan sourcePlan,
            MapCompatibilityAnalysis analysis
    ) {
        Spm spm = sourcePlan.getBsdxSpm();
        if (spm == null) {
            return;
        }
        List<Spm.SPMPageData> pages = spm.getPageData() == null ? List.of() : spm.getPageData();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            Spm.SPMPageData page = pages.get(pageIndex);
            if (page == null || page.getChipData() == null) {
                continue;
            }
            for (int chipIndex = 0; chipIndex < page.getChipData().size(); chipIndex++) {
                Spm.SPMChipData chip = page.getChipData().get(chipIndex);
                Integer imageNo = chip == null ? null : chip.getImageNo();
                if (imageNo == null || imageNo < 0 || imageNo >= sourcePlan.getImageCount()) {
                    MapCompatibilityIssue issue = issue(
                            MapCompatibilityBlockingReason.INVALID_CHIP_IMAGE_REFERENCE,
                            entryPlan,
                            "SPM chip imageNo is outside source imageData range"
                    );
                    applySourcePlan(issue, sourcePlan);
                    issue.setMessage(issue.getMessage()
                            + ": pageIndex=" + pageIndex
                            + ", chipIndex=" + chipIndex
                            + ", imageNo=" + imageNo
                            + ", imageCount=" + sourcePlan.getImageCount());
                    analysis.addIssue(issue);
                }
            }
        }

        List<Spm.SPMAnimData> anims = spm.getAnimData() == null ? List.of() : spm.getAnimData();
        for (int animIndex = 0; animIndex < anims.size(); animIndex++) {
            Spm.SPMAnimData anim = anims.get(animIndex);
            if (anim == null || anim.getPatData() == null) {
                continue;
            }
            for (int patIndex = 0; patIndex < anim.getPatData().size(); patIndex++) {
                Spm.SPMPatData pat = anim.getPatData().get(patIndex);
                if (pat == null || pat.getPageNo() == null) {
                    continue;
                }
                for (int pageSlot = 0; pageSlot < pat.getPageNo().size(); pageSlot++) {
                    Integer pageNo = pat.getPageNo().get(pageSlot);
                    if (pageNo != null && pageNo >= 0 && pageNo >= sourcePlan.getPageCount()) {
                        MapCompatibilityIssue issue = issue(
                                MapCompatibilityBlockingReason.INVALID_PAT_PAGE_REFERENCE,
                                entryPlan,
                                "SPM pat pageNo is outside source pageData range"
                        );
                        applySourcePlan(issue, sourcePlan);
                        issue.setTypeId(animIndex);
                        issue.setMessage(issue.getMessage()
                                + ": animIndex=" + animIndex
                                + ", patIndex=" + patIndex
                                + ", pageSlot=" + pageSlot
                                + ", pageNo=" + pageNo
                                + ", pageCount=" + sourcePlan.getPageCount());
                        analysis.addIssue(issue);
                    }
                }
            }
        }
    }

    private void validateScriptEntries(
            BheMapEntryPlan entryPlan,
            MapData sourceMapData,
            MultipleSpriteMapListCompositionPlan compositionPlan,
            MapCompatibilityAnalysis analysis
    ) {
        if (sourceMapData == null || sourceMapData.getScriptEntryGroupBlocks() == null) {
            return;
        }
        for (int outerGroupIndex = 0; outerGroupIndex < sourceMapData.getScriptEntryGroupBlocks().size(); outerGroupIndex++) {
            MapData.ScriptEntryGroupBlock groupBlock = sourceMapData.getScriptEntryGroupBlocks().get(outerGroupIndex);
            if (groupBlock == null || groupBlock.getEntries() == null) {
                continue;
            }
            for (int entryIndex = 0; entryIndex < groupBlock.getEntries().size(); entryIndex++) {
                MapData.ScriptEntry scriptEntry = groupBlock.getEntries().get(entryIndex);
                analysis.setInspectedScriptEntryCount(analysis.getInspectedScriptEntryCount() + 1);
                validateScriptEntry(entryPlan, scriptEntry, outerGroupIndex, entryIndex, compositionPlan, analysis);
            }
        }
    }

    private void validateScriptEntry(
            BheMapEntryPlan entryPlan,
            MapData.ScriptEntry scriptEntry,
            int outerGroupIndex,
            int entryIndex,
            MultipleSpriteMapListCompositionPlan compositionPlan,
            MapCompatibilityAnalysis analysis
    ) {
        if (scriptEntry == null || scriptEntry.getGroupIndex() == null) {
            MapCompatibilityIssue issue = scriptIssue(
                    MapCompatibilityBlockingReason.MISSING_GROUP_INDEX,
                    entryPlan,
                    scriptEntry,
                    outerGroupIndex,
                    entryIndex,
                    "script entry groupIndex is missing"
            );
            analysis.addIssue(issue);
            return;
        }
        int groupIndex = scriptEntry.getGroupIndex();
        if (groupIndex < 0 || groupIndex >= compositionPlan.getSourceSpriteMaps().size()) {
            MapCompatibilityIssue issue = scriptIssue(
                    MapCompatibilityBlockingReason.GROUP_INDEX_OUT_OF_SPRITE_MAP_LIST_RANGE,
                    entryPlan,
                    scriptEntry,
                    outerGroupIndex,
                    entryIndex,
                    "script entry groupIndex does not resolve to spriteMapList index"
            );
            analysis.addIssue(issue);
            return;
        }
        if (scriptEntry.getTypeId() == null) {
            MapCompatibilityIssue issue = scriptIssue(
                    MapCompatibilityBlockingReason.MISSING_TYPE_ID,
                    entryPlan,
                    scriptEntry,
                    outerGroupIndex,
                    entryIndex,
                    "script entry typeId is missing"
            );
            analysis.addIssue(issue);
            return;
        }
        SourceSpriteMapCompositionPlan sourcePlan = compositionPlan.getSourceSpriteMaps().get(groupIndex);
        if (scriptEntry.getTypeId() < 0 || scriptEntry.getTypeId() >= sourcePlan.getAnimCount()) {
            MapCompatibilityIssue issue = scriptIssue(
                    MapCompatibilityBlockingReason.TYPE_ID_OUT_OF_SOURCE_SPM_RANGE,
                    entryPlan,
                    scriptEntry,
                    outerGroupIndex,
                    entryIndex,
                    "script entry typeId is outside selected source SPM animData range"
            );
            applySourcePlan(issue, sourcePlan);
            analysis.addIssue(issue);
            return;
        }
        analysis.setRewrittenScriptEntryCount(analysis.getRewrittenScriptEntryCount() + 1);
    }

    private List<String> normalizedSpriteMapList(MapData sourceMapData) {
        if (sourceMapData == null || sourceMapData.getSpriteMapList() == null) {
            return List.of();
        }
        return sourceMapData.getSpriteMapList().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private MapCompatibilityIssue scriptIssue(
            MapCompatibilityBlockingReason reason,
            BheMapEntryPlan entryPlan,
            MapData.ScriptEntry scriptEntry,
            int outerGroupIndex,
            int entryIndex,
            String message
    ) {
        MapCompatibilityIssue issue = issue(reason, entryPlan, message);
        issue.setOuterGroupIndex(outerGroupIndex);
        issue.setEntryIndex(entryIndex);
        if (scriptEntry != null) {
            issue.setGroupIndex(scriptEntry.getGroupIndex());
            issue.setTypeId(scriptEntry.getTypeId());
            issue.setX(scriptEntry.getX());
            issue.setY(scriptEntry.getY());
        }
        return issue;
    }

    private MapCompatibilityIssue issue(
            MapCompatibilityBlockingReason reason,
            BheMapEntryPlan entryPlan,
            String message
    ) {
        return MapCompatibilityIssue.blocking(
                reason,
                entryPlan == null ? null : entryPlan.getSourceMapFileName(),
                message
        );
    }

    private void applySourcePlan(MapCompatibilityIssue issue, SourceSpriteMapCompositionPlan sourcePlan) {
        if (issue == null || sourcePlan == null) {
            return;
        }
        issue.setSpriteMapIndex(sourcePlan.getSpriteMapIndex());
        issue.setSpriteMapName(sourcePlan.getSourceFileName());
        issue.setReferenceText(sourcePlan.getSourceText());
    }
}
