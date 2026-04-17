package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bhe.mek.Mek;
import com.giga.nexas.dto.bhe.spm.Spm;
import com.giga.nexas.dto.bhe.waz.Waz;
import com.giga.nexas.dto.bsdx.dat.Dat;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tsukuyomi 第 0 步读取到的 BHE 原始资源包。
 *
 * <p>这个对象只表示“尚未转换”的源侧数据，后续转换步骤会把它整理成 BSDX 形状的资源包。</p>
 */
@Data
public class TsukuyomiRawSourceBundle {

    private BatVoiceGrp batVoiceGrp;
    private MekaGroupGrp mekaGroupGrp;
    private SeGroupGrp seGroupGrp;
    private SpriteGroupGrp spriteGroupGrp;
    private WazaGroupGrp wazaGroupGrp;

    private Dat mekaDat;
    private Dat mekaPilotDat;
    private Dat weaponEquipDat;
    private Mek tsukuyomiMek;

    private Map<String, Spm> spmByFileName = new LinkedHashMap<>();
    private Map<String, Waz> wazByFileName = new LinkedHashMap<>();
}
