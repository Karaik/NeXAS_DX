package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.common;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiCommonResourceAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;

/**
 * 把指定公共资源追加到继承基线。
 */
public class AppendSpecifiedCommonResourcesStep {

    public TsukuyomiBsdxBaselineBundle append(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle,
            TsukuyomiCommonResourceAppendPlan commonResourceAppendPlan
    ) {
        // TODO 20260417：把转换后的指定公共资源追加到继承基线，暂时直接返回原基线。
        return inheritedBaseline;
    }
}
