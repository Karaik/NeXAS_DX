package com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert;

import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.grp.ConvertSelectedGrpFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek.ConvertSelectedMekFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.source.LoadRawBheSourceStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.spm.ConvertSelectedSpmFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.ConvertSelectedWazFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

public class FollowupConvertOverviewStep {

    private final LoadRawBheSourceStep loadRawBheSourceStep = new LoadRawBheSourceStep();
    private final ConvertSelectedGrpFilesStep convertSelectedGrpFilesStep = new ConvertSelectedGrpFilesStep();
    private final ConvertSelectedMekFilesStep convertSelectedMekFilesStep = new ConvertSelectedMekFilesStep();
    private final ConvertSelectedWazFilesStep convertSelectedWazFilesStep = new ConvertSelectedWazFilesStep();
    private final ConvertSelectedSpmFilesStep convertSelectedSpmFilesStep = new ConvertSelectedSpmFilesStep();

    public TsukuyomiConvertedBundle convert(
            FollowupGraftRequest request,
            TsukuyomiGraftResult previousCharacterResult,
            TsukuyomiBsdxBaselineBundle inheritedBaseline
    ) {
        TsukuyomiConvertedBundle convertedBundle = new TsukuyomiConvertedBundle();
        TsukuyomiRawSourceBundle rawSourceBundle = loadRawBheSourceStep.load(request);
        convertedBundle.setRawSourceBundle(rawSourceBundle);
        convertedBundle.setPreparedBaselineBundle(inheritedBaseline);
        if (previousCharacterResult != null) {
            convertedBundle.setCommonProjectileAppendPlan(previousCharacterResult.getCommonProjectileAppendPlan());
        }
        convertSelectedGrpFilesStep.convert(request, rawSourceBundle, convertedBundle);
        convertSelectedMekFilesStep.convert(request, rawSourceBundle, convertedBundle);
        convertSelectedWazFilesStep.convert(request, rawSourceBundle, convertedBundle);
        convertSelectedSpmFilesStep.convert(request, rawSourceBundle, convertedBundle);
        return convertedBundle;
    }
}
