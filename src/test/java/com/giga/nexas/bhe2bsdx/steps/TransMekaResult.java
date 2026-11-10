package com.giga.nexas.bhe2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 迁移输出：包含转换后的对象与关键索引信息，便于调试/写盘。
 * 可作为后续写回 PAC 或输出目录的上下文数据。
 */
@Data
public class TransMekaResult {

    // ===== 关键索引 =====
    private int batVoiceIndex = -1;
    private int mekaGroupIndex = -1;
    private int wazaGroupIndex = -1;
    private int spriteGroupIndex = -1;

    // ===== 索引映射 =====
    private Map<Integer, Integer> spriteIndexMap = new HashMap<>();

    // ===== 转换后的 BSDX 资源 =====
    private BatVoiceGrp.BatVoiceGroup bsdxBatVoiceGroup;
    private Mek bsdxMeka;
    private Waz bsdxWaz;
    private Spm bsdxSpm;
    private Spm bsdxCSpm;
    private Spm bsdxSSpm;
    private Spm bsdxGSpm;
    private Spm bsdxMSpm;
    private Spm bsdxMekaPilotSpm;
    private Spm bsdxSelectMekaMenuMekaSpm;
}
