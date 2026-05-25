package com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata;

/**
 * BHE MapData 转 BSDX MapData 时会阻止内存产物成立的问题类型。
 */
public enum BheMapDataConversionIssueType {
    UNSUPPORTED_SCRIPT_GROUP_INDEX,
    MISSING_RESOURCE_REWRITE,
    MISSING_FINAL_REFERENCE,
    MISSING_TARGET_TEXT,
    UNSUPPORTED_MULTIPLE_SPRITE_MAPS
}
