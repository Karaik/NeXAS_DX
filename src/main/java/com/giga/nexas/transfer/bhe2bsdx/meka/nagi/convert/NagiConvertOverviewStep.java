package com.giga.nexas.transfer.bhe2bsdx.meka.nagi.convert;

import com.giga.nexas.transfer.bhe2bsdx.meka.nagi.convert.grp.ConvertSelectedGrpFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.nagi.convert.mek.ConvertSelectedMekFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.nagi.convert.source.LoadRawBheSourceStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.nagi.convert.spm.ConvertSelectedSpmFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.nagi.convert.waz.ConvertSelectedWazFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.nagi.NagiGraftRequest;

public class NagiConvertOverviewStep {

    private final LoadRawBheSourceStep loadRawBheSourceStep = new LoadRawBheSourceStep();
    private final ConvertSelectedGrpFilesStep convertSelectedGrpFilesStep = new ConvertSelectedGrpFilesStep();
    private final ConvertSelectedMekFilesStep convertSelectedMekFilesStep = new ConvertSelectedMekFilesStep();
    private final ConvertSelectedWazFilesStep convertSelectedWazFilesStep = new ConvertSelectedWazFilesStep();
    private final ConvertSelectedSpmFilesStep convertSelectedSpmFilesStep = new ConvertSelectedSpmFilesStep();

    public TsukuyomiConvertedBundle convert(
            NagiGraftRequest request,
            TsukuyomiBsdxBaselineBundle inheritedBaseline
    ) {
        TsukuyomiConvertedBundle convertedBundle = new TsukuyomiConvertedBundle();
        TsukuyomiRawSourceBundle rawSourceBundle = loadRawBheSourceStep.load(request);
        convertedBundle.setRawSourceBundle(rawSourceBundle);
        convertedBundle.setPreparedBaselineBundle(inheritedBaseline);
        if (request.getPreviousCharacterResult() != null) {
            convertedBundle.setCommonProjectileAppendPlan(request.getPreviousCharacterResult().getCommonProjectileAppendPlan());
        }
        convertSelectedGrpFilesStep.convert(request, rawSourceBundle, convertedBundle);
        convertSelectedMekFilesStep.convert(request, rawSourceBundle, convertedBundle);
        convertSelectedWazFilesStep.convert(request, rawSourceBundle, convertedBundle);
        convertSelectedSpmFilesStep.convert(request, rawSourceBundle, convertedBundle);
        return convertedBundle;
    }
}
