package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.cross;

import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * BHE 公共弹幕资源簇的交叉重定向步骤。
 *
 * <p>交叉重定向只处理“公共 WAZ 指向其他资源”的引用重写，不处理公共资源目标 entry 的创建。
 * 典型内容包括：WAZ -> WAZ、WAZ -> SPM、WAZ -> SeGroup/SeItem。</p>
 *
 * <p>CEventVoice 只记录 SOU/MISAKI 外部角色语音依赖，不在公共资源阶段重建 BatVoice。
 * term / InfoCollection 属于公共资源阶段的全量语义转换分支，但重建规则独立且复杂；
 * 这里保留单独 TODO，避免和 WAZ/SPM/SE 的 index 重写混成一团。</p>
 */
public class CrossRedirectBheCommonProjectileResourcesStep {

    public void redirect(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        // TODO 20260417：实现 CEventWazaSelect、CEventSprite 和 CEventSe 的目标侧重写。
        // TODO 20260417：记录 CEventVoice 中 SOU/MISAKI 外部角色语音依赖，不重建 BatVoice。
        // TODO 20260417：接入全量 BHE InfoCollection/term 语义转换，具体方法和策略单独解决。
    }
}
