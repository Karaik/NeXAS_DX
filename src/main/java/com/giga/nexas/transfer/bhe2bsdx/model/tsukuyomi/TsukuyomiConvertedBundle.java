package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Tsukuyomi 第 0 步转换后的 BSDX 形状资源包。
 *
 * <p>总览转换步骤会把 raw 读取结果、单机体私有资源转换结果、bhecommon 公共资源转换结果、
 * 公共资源计划、以及接入后的基线视图都挂在这里，供主流程统一取用。</p>
 *
 * <p>{@code mergedPackageBundle} 只表示单机体 selected 资源转换视图；公共 WAZ/SPM/SE
 * 接入结果属于 {@code preparedBaselineBundle}，不能通过 merged 视图进入 selected closure。</p>
 */
@Data
public class TsukuyomiConvertedBundle {

    private TsukuyomiRawSourceBundle rawSourceBundle = new TsukuyomiRawSourceBundle();
    private TsukuyomiPackageBundle selectedResourceBundle = new TsukuyomiPackageBundle();
    private TsukuyomiPackageBundle commonProjectileResourceBundle = new TsukuyomiPackageBundle();
    private TsukuyomiPackageBundle mergedPackageBundle = new TsukuyomiPackageBundle();
    private BheCommonProjectileAppendPlan commonProjectileAppendPlan = new BheCommonProjectileAppendPlan();
    private TsukuyomiBsdxBaselineBundle preparedBaselineBundle = new TsukuyomiBsdxBaselineBundle();
    private List<String> notes = new ArrayList<>();
}
