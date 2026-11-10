package com.giga.nexas.transfer.bhe2bsdx.mapappend.mapgroup;

import lombok.Data;

/**
 * BHE MapGroup item 从 5 int 降到 BSDX 4 int 时丢弃的字段记录。
 */
@Data
public class BheMapGroupItemDowngrade {

    private String sourceMapFileName;
    private int itemIndex;
    private Integer droppedInt5;
}
