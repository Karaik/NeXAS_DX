package com.giga.nexas.transfer.bhe2bsdx.meka.yuri.convert;

import com.giga.nexas.transfer.bhe2bsdx.meka.yuri.convert.grp.ConvertSelectedGrpFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.yuri.convert.mek.ConvertSelectedMekFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.yuri.convert.source.LoadRawBheSourceStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.yuri.convert.spm.ConvertSelectedSpmFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.yuri.convert.waz.ConvertSelectedWazFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.yuri.YuriGraftRequest;

public class YuriConvertOverviewStep {

    private final LoadRawBheSourceStep loadRawBheSourceStep = new LoadRawBheSourceStep();
    private final ConvertSelectedGrpFilesStep convertSelectedGrpFilesStep = new ConvertSelectedGrpFilesStep();
    private final ConvertSelectedMekFilesStep convertSelectedMekFilesStep = new ConvertSelectedMekFilesStep();
    private final ConvertSelectedWazFilesStep convertSelectedWazFilesStep = new ConvertSelectedWazFilesStep();
    private final ConvertSelectedSpmFilesStep convertSelectedSpmFilesStep = new ConvertSelectedSpmFilesStep();

    public TsukuyomiConvertedBundle convert(
            YuriGraftRequest request,
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
