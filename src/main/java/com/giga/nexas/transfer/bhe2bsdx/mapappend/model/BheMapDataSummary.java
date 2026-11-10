package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

/**
 * BHE .map 成功解析后的轻量摘要。
 *
 * <p>摘要只记录 import plan 已经解析出来的稳定元信息，帮助 MapData conversion 快速判断地图规模和脚本/资源段结构。</p>
 */
@Data
public class BheMapDataSummary {

    private String magic;
    private int width;
    private int height;
    private int tileRecordCount;
    private int cameraRectCount;
    private int rawPointGroup0Count;
    private int rawPointGroup1Count;
    private int rawRectGroup0Count;
    private int rawRectGroup1Count;
    private int rawRectGroup2Count;
    private int resourceSlotBlockCount;
    private int scriptEntryGroupCount;
    private int totalScriptEntryCount;
    private int spriteMapCount;
}
