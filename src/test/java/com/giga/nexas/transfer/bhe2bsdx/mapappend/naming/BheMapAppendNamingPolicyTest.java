package com.giga.nexas.transfer.bhe2bsdx.mapappend.naming;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapSourceEntry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BheMapAppendNamingPolicyTest {

    private final BheMapAppendNamingPolicy namingPolicy = new BheMapAppendNamingPolicy();

    @Test
    void buildNormalMapNamesFromOneResourceStem() {
        BheMapSourceEntry sourceEntry = sourceEntry("mapb06_amagahara01");

        BheMapEntryPlan plan = namingPolicy.buildEntryPlan(sourceEntry);

        assertEquals("mapb06_amagahara01", plan.getSourceGroupResourceName());
        assertEquals("bhe_mapb06_amagahara01", plan.getTargetGroupResourceName());
        assertEquals("mapb06_amagahara01.map", plan.getSourceMapFileName());
        assertEquals("bhe_mapb06_amagahara01.map", plan.getTargetMapFileName());
        assertEquals("T_mapb06_amagahara01.bmp", plan.getSourcePreviewFileName());
        assertEquals("T_bhe_mapb06_amagahara01.bmp", plan.getTargetPreviewFileName());
        assertNull(plan.getBsdxPreviewFallbackFileName());
    }

    @Test
    void mapBlackUsesBsdxPreviewFallbackButKeepsMapGroupResourceName() {
        BheMapSourceEntry sourceEntry = sourceEntry("mapBlack_S01");

        BheMapEntryPlan plan = namingPolicy.buildEntryPlan(sourceEntry);

        assertEquals("bhe_mapBlack_S01", plan.getTargetGroupResourceName());
        assertEquals("T_mapBlack_S01.bmp", plan.getSourcePreviewFileName());
        assertEquals("T_bhe_mapBlack_S01.bmp", plan.getTargetPreviewFileName());
        assertEquals("T_mapBlack_S01.bmp", plan.getBsdxPreviewFallbackFileName());
    }

    private BheMapSourceEntry sourceEntry(String groupResourceName) {
        BheMapSourceEntry entry = new BheMapSourceEntry();
        entry.setSourceMapIndex(7);
        entry.setGroupResourceName(groupResourceName);
        return entry;
    }
}
