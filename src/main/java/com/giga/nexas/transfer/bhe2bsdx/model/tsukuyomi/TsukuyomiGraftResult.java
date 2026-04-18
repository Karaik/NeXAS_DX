package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import lombok.Data;

/**
 * Tsukuyomi graft 结果容器。
 */
@Data
public class TsukuyomiGraftResult {

    private TsukuyomiRawSourceBundle rawSourceBundle;
    private TsukuyomiConvertedBundle convertedBundle;
    private BheCommonProjectileAppendPlan commonProjectileAppendPlan;
    private TsukuyomiPackageBundle tsukuyomiPackage;
    private TsukuyomiBsdxBaselineBundle bsdxBaseline;
    private TsukuyomiBsdxBaselineBundle preparedBaselineBundle;
    private TsukuyomiImportPlan importPlan;
    private TsukuyomiGrpAppendPlan grpAppendPlan;
    private ProgramMaterialGrp syncedProgramMaterial;
    private Mek reboundTsukuyomiMek;
    private Waz reboundTsukuyomiWaz;
    private TsukuyomiImportedAssetSet importedAssetSet;
    private Dat patchedMekaDat;
    private Dat patchedMekaPilotDat;
    private Dat patchedSelectMekaMenuDat;
    private Spm patchedMekaPilotSpm;
    private Spm patchedSelectMekaMenuMekaSpm;
    private TsukuyomiExePatchPlan exePatchPlan;
    private TsukuyomiPacPackPlan pacPackPlan;
}
