package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect;

import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.cross.CrossRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.self.SelfRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * BHE 公共弹幕资源簇的重定向总入口。
 *
 * <p>redirect 是公共资源准备层的第二步：第一步 convert 只负责格式转换；
 * 这里负责把公共资源接入继承基线，并完成目标侧索引自洽。本类只保留调用骨架，
 * 具体规则由 self redirect、cross redirect 和 term 包分别承接。</p>
 *
 * <p>公共资源接入以 bhe_* 目标命名空间隔离 BHE 文件，并以 append plan 记录所有
 * sourceIndex 到 targetIndex 的映射。这里不能复用 JINKI 的 key-based merge 思路，
 * 否则单机体 graft 会把公共资源当成私有闭包重复合并和重定向。</p>
 */
public class RedirectBheCommonProjectileResourcesStep {

    private final SelfRedirectBheCommonProjectileResourcesStep selfRedirectStep =
            new SelfRedirectBheCommonProjectileResourcesStep();
    private final CrossRedirectBheCommonProjectileResourcesStep crossRedirectStep =
            new CrossRedirectBheCommonProjectileResourcesStep();

    public void redirect(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        // self redirect 先追加公共资源目标 entry 并建立 source -> target 映射。
        selfRedirectStep.redirect(request, rawSourceBundle, inheritedBaseline, convertedBundle, appendPlan);
        // cross redirect 再消费映射结果，重写公共 WAZ 内部引用和 term 语义。
        crossRedirectStep.redirect(request, rawSourceBundle, inheritedBaseline, convertedBundle, appendPlan);
    }
}
