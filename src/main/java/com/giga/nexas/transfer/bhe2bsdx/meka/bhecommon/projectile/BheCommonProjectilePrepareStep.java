package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile;

import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.convert.ConvertBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.RedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * BHE 公共弹幕资源簇准备入口。
 *
 * <p>Tsukuyomi 第 0 步只调用这一处公共资源入口，不直接展开公共资源内部的
 * convert / redirect / self redirect / cross redirect 细节。这样公共资源簇可以独立于
 * 单机体迁移流程演进，其他 BHE 机体也不会把公共资源逻辑复制到自己的包里。</p>
 *
 * <p>本入口必须与规约文档双向对齐：
 * src/main/resources/research/12-bhe-common-projectile-append-checklist.md。
 * 该文档固定了 bhe_* 命名空间、WAZ/SPM/SE 目标索引映射、term 独立 TODO、
 * Voice 特例，以及 preparedBaseline 与 mergedPackageBundle 的业务边界。</p>
 */
public class BheCommonProjectilePrepareStep {

    private final ConvertBheCommonProjectileResourcesStep convertStep =
            new ConvertBheCommonProjectileResourcesStep();
    private final RedirectBheCommonProjectileResourcesStep redirectStep =
            new RedirectBheCommonProjectileResourcesStep();

    public void prepare(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle
    ) {
        if (convertedBundle == null) {
            return;
        }

        // 公共 WAZ/SPM 的格式转换只在 bhecommon 内执行，不混入 selected。
        convertStep.convert(rawSourceBundle, convertedBundle);

        BheCommonProjectileAppendPlan appendPlan = new BheCommonProjectileAppendPlan();
        convertedBundle.setCommonProjectileAppendPlan(appendPlan);

        // TODO 20260417：在 redirect 内继续实现公共 WAZ 交叉重写、Voice 特例审计和 term 全量语义转换。
        redirectStep.redirect(request, rawSourceBundle, inheritedBaseline, convertedBundle, appendPlan);

        // TODO 20260417：redirect 接入公共资源前，preparedBaseline 等于继承基线。
        convertedBundle.setPreparedBaselineBundle(inheritedBaseline);
    }
}
