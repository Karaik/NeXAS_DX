package com.giga.nexas.transfer.bhe2bsdx.mapappend.composition;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MultipleSpriteMapListCompositionResult {

    private MultipleSpriteMapListCompositionPlan compositionPlan;
    private com.giga.nexas.dto.bsdx.map.MapData convertedMapData;
    private com.giga.nexas.dto.bsdx.spm.Spm composedSpm;
    private List<MapCompatibilityIssue> blockingIssues = new ArrayList<>();

    public boolean isConverted() {
        return convertedMapData != null && composedSpm != null && blockingIssues.isEmpty();
    }

    public void addBlockingIssue(MapCompatibilityIssue issue) {
        if (issue != null) {
            blockingIssues.add(issue);
        }
    }
}
