package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.self;

import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * BHE 公共弹幕资源簇的自重定向步骤。
 *
 * <p>自重定向只处理“公共资源自身如何成为目标侧资源”的问题，不处理 WAZ 指向其他资源的交叉引用。
 * 典型内容包括：新增 8 个 bhe_* WazaGroup entry、12 个 bhe_* SpriteGroup entry、
 * 1 个 bhe_common_projectile_se SeGroup entry、同步 GRP/ProgramMaterial/MEK material 容量。</p>
 */
public class SelfRedirectBheCommonProjectileResourcesStep {

    public void redirect(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        // TODO 20260417：实现 bhe_* WAZ/SPM 目标 entry、公共 SE 聚合组和全局容量同步。
    }
}
