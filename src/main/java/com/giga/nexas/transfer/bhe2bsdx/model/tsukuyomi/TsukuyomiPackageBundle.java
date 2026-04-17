package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tsukuyomi 源资源包。
 */
@Data
public class TsukuyomiPackageBundle {

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
