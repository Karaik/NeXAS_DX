package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

/**
 * grp 顶层追加方案，以及追加后得到的新目标索引。
 */
@Data
public class GrpAppendPlan {

    private int mekaGroupIndex = -1;
    private int wazaGroupIndex = -1;
    private int spriteGroupIndex = -1;
    private int batVoiceGroupIndex = -1;
}
