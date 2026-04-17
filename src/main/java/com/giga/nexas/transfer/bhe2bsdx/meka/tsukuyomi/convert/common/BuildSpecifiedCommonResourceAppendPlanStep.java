package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.common;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiCommonResourceAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * 生成指定公共资源追加计划。
 */
public class BuildSpecifiedCommonResourceAppendPlanStep {

    public TsukuyomiCommonResourceAppendPlan buildAppendPlan(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiConvertedBundle convertedBundle
    ) {
        // TODO 20260417：根据指定公共弹幕 WAZ 生成追加计划，暂时只保留空实现骨架。
        return new TsukuyomiCommonResourceAppendPlan();
    }
}
