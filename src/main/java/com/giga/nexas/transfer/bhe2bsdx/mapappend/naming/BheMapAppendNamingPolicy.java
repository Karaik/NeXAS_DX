package com.giga.nexas.transfer.bhe2bsdx.mapappend.naming;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapPreviewPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapSourceEntry;

/**
 * BHE 地图追加命名策略。
 *
 * <p>所有跨 step 的字符串拼接都集中在这里，禁止 preview/map/grp 各自散落一套命名规则。</p>
 */
public class BheMapAppendNamingPolicy {

    public static final String BHE_RESOURCE_PREFIX = "bhe_";
    public static final String PREVIEW_FILE_PREFIX = "T_";
    public static final String PREVIEW_FILE_EXTENSION = ".bmp";
    public static final String MAP_FILE_EXTENSION = ".map";

    /**
     * BHE 中缺少预览图、但与 BSDX 空黑图字节一致的地图名。
     */
    public static final String BHE_EMPTY_BLACK_MAP_RESOURCE_NAME = "mapBlack_S01";

    /**
     * BSDX 侧可作为空黑图预览 fallback 的原生地图名。
     */
    public static final String BSDX_EMPTY_BLACK_MAP_RESOURCE_NAME = "mapBlack_S01";

    public BheMapEntryPlan buildEntryPlan(BheMapSourceEntry sourceEntry) {
        BheMapEntryPlan plan = new BheMapEntryPlan();
        plan.setSourceMapIndex(sourceEntry.getSourceMapIndex());
        plan.setSourceGroupResourceName(sourceEntry.getGroupResourceName());
        plan.setTargetGroupResourceName(toTargetGroupResourceName(sourceEntry.getGroupResourceName()));
        plan.setSourceMapFileName(toMapFileName(sourceEntry.getGroupResourceName()));
        plan.setTargetMapFileName(toMapFileName(plan.getTargetGroupResourceName()));
        plan.setSourcePreviewFileName(toPreviewFileName(sourceEntry.getGroupResourceName()));
        plan.setTargetPreviewFileName(toPreviewFileName(plan.getTargetGroupResourceName()));
        plan.setBsdxPreviewFallbackFileName(resolveBsdxPreviewFallbackFileName(sourceEntry.getGroupResourceName()));
        plan.setPreviewPlan(buildPreviewPlan(plan));
        return plan;
    }

    public String toTargetGroupResourceName(String sourceGroupResourceName) {
        return BHE_RESOURCE_PREFIX + sourceGroupResourceName;
    }

    public String toMapFileName(String groupResourceName) {
        return groupResourceName + MAP_FILE_EXTENSION;
    }

    public String toPreviewFileName(String groupResourceName) {
        return PREVIEW_FILE_PREFIX + groupResourceName + PREVIEW_FILE_EXTENSION;
    }

    public String toTargetResourceFileName(String sourceResourceFileName) {
        if (sourceResourceFileName == null || sourceResourceFileName.isBlank()) {
            return null;
        }
        return BHE_RESOURCE_PREFIX + sourceResourceFileName.trim();
    }

    private String resolveBsdxPreviewFallbackFileName(String sourceGroupResourceName) {
        if (BHE_EMPTY_BLACK_MAP_RESOURCE_NAME.equalsIgnoreCase(sourceGroupResourceName)) {
            return toPreviewFileName(BSDX_EMPTY_BLACK_MAP_RESOURCE_NAME);
        }
        return null;
    }

    private BheMapPreviewPlan buildPreviewPlan(BheMapEntryPlan entryPlan) {
        BheMapPreviewPlan previewPlan = new BheMapPreviewPlan();
        previewPlan.setSourcePreviewFileName(entryPlan.getSourcePreviewFileName());
        previewPlan.setTargetPreviewFileName(entryPlan.getTargetPreviewFileName());
        previewPlan.setBsdxPreviewFallbackFileName(entryPlan.getBsdxPreviewFallbackFileName());
        return previewPlan;
    }
}
