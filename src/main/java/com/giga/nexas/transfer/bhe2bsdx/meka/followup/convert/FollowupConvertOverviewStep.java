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

/**
 * follow-up 单机体的 convert 总入口。
 *
 * <p>这层只负责把当前机体的 BHE 资源转换成 BSDX DTO 形状，并挂到
 * `TsukuyomiConvertedBundle` 里。它不负责：
 * `closure / grp append / rebind / 落盘 / 菜单 / exe / pack`。</p>
 */
public class FollowupConvertOverviewStep {

    private final LoadRawBheSourceStep loadRawBheSourceStep = new LoadRawBheSourceStep();
    private final ConvertSelectedGrpFilesStep convertSelectedGrpFilesStep = new ConvertSelectedGrpFilesStep();
    private final ConvertSelectedMekFilesStep convertSelectedMekFilesStep = new ConvertSelectedMekFilesStep();
    private final ConvertSelectedWazFilesStep convertSelectedWazFilesStep = new ConvertSelectedWazFilesStep();
    private final ConvertSelectedSpmFilesStep convertSelectedSpmFilesStep = new ConvertSelectedSpmFilesStep();

    /**
     * follow-up 机体都沿用同一个 convert 编排：
     * load raw -> grp -> mek -> waz -> spm。
     *
     * <p>公共弹幕资源 append plan 直接继承上一机体结果，不在 follow-up convert 里重复准备。</p>
     */
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
