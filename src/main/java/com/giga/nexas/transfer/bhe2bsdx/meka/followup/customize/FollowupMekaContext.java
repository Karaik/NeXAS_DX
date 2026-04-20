package com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import lombok.Data;

@Data
public class FollowupMekaContext {

    private FollowupGraftRequest request;
    private TsukuyomiBsdxBaselineBundle baseline;
    private TsukuyomiConvertedBundle convertedBundle;
    private TsukuyomiPackageBundle selectedPackage;
    private TsukuyomiImportPlan importPlan;
    private TsukuyomiGrpAppendPlan grpAppendPlan;
    private ProgramMaterialGrp syncedProgramMaterial;
    private Mek reboundMek;
    private Waz reboundWaz;
}
