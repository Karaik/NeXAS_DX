package com.giga.nexas.transfer.jinki2bsdx.model;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import lombok.Data;

/**
 * AKAO graft 主流程的输出容器。
 */
@Data
public class AkaoGraftResult {

    private JinkiPackageBundle jinkiPackage;
    private BsdxBaselineBundle bsdxBaseline;
    private JinkiImportPlan importPlan;
    private GrpAppendPlan grpAppendPlan;
    private ProgramMaterialGrp syncedProgramMaterial;
    private Mek reboundAkaoMek;
    private Waz reboundAkaoWaz;
    private ImportedAssetSet importedAssetSet;
    private Dat patchedMekaDat;
    private Dat patchedMekaPilotDat;
    private Dat patchedSelectMekaMenuDat;
    private Spm patchedMekaPilotSpm;
    private Spm patchedSelectMekaMenuMekaSpm;
    private ExePatchPlan exePatchPlan;
    private PacPackPlan pacPackPlan;
}
