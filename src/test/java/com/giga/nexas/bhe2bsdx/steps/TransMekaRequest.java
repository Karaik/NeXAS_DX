package com.giga.nexas.bhe2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.dat.Dat;
import lombok.Data;

/**
 * 迁移输入聚合对象：把 BHE 源数据与 BSDX 目标注册表打包成一次请求。
 * 说明：字段允许为空，Pipeline 会按是否存在进行条件执行。
 */
@Data
public class TransMekaRequest {

    // ===== BHE 源资源 =====
    private com.giga.nexas.dto.bhe.mek.Mek bheMek;
    private com.giga.nexas.dto.bhe.waz.Waz bheWaz;
    private com.giga.nexas.dto.bhe.spm.Spm bheSpm;
    private com.giga.nexas.dto.bhe.spm.Spm bheCSpm;
    private com.giga.nexas.dto.bhe.spm.Spm bheSSpm;
    private com.giga.nexas.dto.bhe.spm.Spm bheGSpm;
    private com.giga.nexas.dto.bhe.spm.Spm bheMSpm;

    private com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup bheBatVoiceGroup;
    private com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup bheMekaGroup;
    private com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry bheWazaGroup;
    private com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry bheSpriteGroupEntry;
    private com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp bheSpriteGroup;

    // ===== BSDX 目标注册表/容器 =====
    private BatVoiceGrp bsdxBatVoice;
    private MekaGroupGrp bsdxMekaGroup;
    private WazaGroupGrp bsdxWazaGroup;
    private SpriteGroupGrp bsdxSpriteGroup;

    // ===== BSDX UI SPM =====
    // 用于 Step6 UI 资源替换（mekaPilot/selectMenu）
    private Spm mekaPilotSpm;
    private Spm selectMekaMenuMekaSpm;
    // UI 选择菜单映射表：mekaIndex -> selectMenuAnimIndex
    private Dat selectMekaMenuDat;

    /**
     * 兼容旧入口的参数列表，统一打包成 Request。
     */
    public static TransMekaRequest fromLegacy(
            com.giga.nexas.dto.bhe.mek.Mek bheMek,
            com.giga.nexas.dto.bhe.waz.Waz bheWaz,
            com.giga.nexas.dto.bhe.spm.Spm bheSpm,
            com.giga.nexas.dto.bhe.spm.Spm bheCSpm,
            com.giga.nexas.dto.bhe.spm.Spm bheSSpm,
            com.giga.nexas.dto.bhe.spm.Spm bheGSpm,
            com.giga.nexas.dto.bhe.spm.Spm bheMSpm,
            com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup bheBatVoiceGroup,
            BatVoiceGrp bsdxBatVoice,
            com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup bheMekaGroup,
            com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry bheWazaGroup,
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry bheSpriteGroupEntry,
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp bheSpriteGroup,
            MekaGroupGrp bsdxMekaGroup,
            WazaGroupGrp bsdxWazaGroup,
            SpriteGroupGrp bsdxSpriteGroup,
            Spm mekaPilotSpm,
            Spm selectMekaMenuMekaSpm,
            Dat selectMekaMenuDat
    ) {
        TransMekaRequest request = new TransMekaRequest();
        request.setBheMek(bheMek);
        request.setBheWaz(bheWaz);
        request.setBheSpm(bheSpm);
        request.setBheCSpm(bheCSpm);
        request.setBheSSpm(bheSSpm);
        request.setBheGSpm(bheGSpm);
        request.setBheMSpm(bheMSpm);
        request.setBheBatVoiceGroup(bheBatVoiceGroup);
        request.setBsdxBatVoice(bsdxBatVoice);
        request.setBheMekaGroup(bheMekaGroup);
        request.setBheWazaGroup(bheWazaGroup);
        request.setBheSpriteGroupEntry(bheSpriteGroupEntry);
        request.setBheSpriteGroup(bheSpriteGroup);
        request.setBsdxMekaGroup(bsdxMekaGroup);
        request.setBsdxWazaGroup(bsdxWazaGroup);
        request.setBsdxSpriteGroup(bsdxSpriteGroup);
        request.setMekaPilotSpm(mekaPilotSpm);
        request.setSelectMekaMenuMekaSpm(selectMekaMenuMekaSpm);
        request.setSelectMekaMenuDat(selectMekaMenuDat);
        return request;
    }
}
