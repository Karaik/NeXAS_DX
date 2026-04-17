package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Tsukuyomi 第 0 步转换后的 BSDX 形状资源包。
 *
 * <p>当前只是骨架容器：总览转换步骤会把 raw 读取结果、选定资源转换结果、
 * 指定公共资源追加计划、以及追加后的基线视图都挂在这里，供主流程统一取用。</p>
 */
@Data
public class TsukuyomiConvertedBundle {

    private TsukuyomiRawSourceBundle rawSourceBundle = new TsukuyomiRawSourceBundle();
    private TsukuyomiPackageBundle selectedResourceBundle = new TsukuyomiPackageBundle();
    private TsukuyomiPackageBundle specifiedCommonResourceBundle = new TsukuyomiPackageBundle();
    private TsukuyomiPackageBundle mergedPackageBundle = new TsukuyomiPackageBundle();
    private TsukuyomiCommonResourceAppendPlan commonResourceAppendPlan = new TsukuyomiCommonResourceAppendPlan();
    private TsukuyomiBsdxBaselineBundle preparedBaselineBundle = new TsukuyomiBsdxBaselineBundle();
    private List<String> notes = new ArrayList<>();
}
