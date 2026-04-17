package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert;

import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.common.AppendSpecifiedCommonResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.common.BuildSpecifiedCommonResourceAppendPlanStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.grp.ConvertSelectedGrpFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.mek.ConvertSelectedMekFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.source.LoadRawBheSourceStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.spm.ConvertSelectedSpmFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz.ConvertSelectedWazFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiCommonResourceAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * 第 0 步资源转换层总览。
 *
 * <p>主流程只调用这个类。这个类内部再把工作拆给 source / grp / mek / waz / spm / common
 * 等子包，避免主流程编排层平铺多个转换步骤。</p>
 */
public class TsukuyomiConvertOverviewStep {

    private final LoadRawBheSourceStep loadRawBheSourceStep = new LoadRawBheSourceStep();
    private final ConvertSelectedGrpFilesStep convertSelectedGrpFilesStep = new ConvertSelectedGrpFilesStep();
    private final ConvertSelectedMekFilesStep convertSelectedMekFilesStep = new ConvertSelectedMekFilesStep();
    private final ConvertSelectedWazFilesStep convertSelectedWazFilesStep = new ConvertSelectedWazFilesStep();
    private final ConvertSelectedSpmFilesStep convertSelectedSpmFilesStep = new ConvertSelectedSpmFilesStep();
    private final BuildSpecifiedCommonResourceAppendPlanStep buildSpecifiedCommonResourceAppendPlanStep =
            new BuildSpecifiedCommonResourceAppendPlanStep();
    private final AppendSpecifiedCommonResourcesStep appendSpecifiedCommonResourcesStep =
            new AppendSpecifiedCommonResourcesStep();

    public TsukuyomiConvertedBundle convert(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle inheritedBaseline
    ) {
        TsukuyomiConvertedBundle convertedBundle = new TsukuyomiConvertedBundle();

        // TODO 20260417：读取 BHE 原始资源，包含选定资源和指定公共资源。
        TsukuyomiRawSourceBundle rawSourceBundle = loadRawBheSourceStep.load(request);
        convertedBundle.setRawSourceBundle(rawSourceBundle);

        // TODO 20260417：转换选定 grp 文件，建立源侧 group 视图。
        convertSelectedGrpFilesStep.convert(request, rawSourceBundle, convertedBundle);

        // TODO 20260417：转换选定 mek 文件，先保留空实现骨架。
        convertSelectedMekFilesStep.convert(request, rawSourceBundle, convertedBundle);

        // TODO 20260417：转换选定 waz 文件和指定公共弹幕 waz，先保留空实现骨架。
        convertSelectedWazFilesStep.convert(request, rawSourceBundle, convertedBundle);

        // TODO 20260417：转换选定 spm 文件以及公共资源推导出的 spm，先保留空实现骨架。
        convertSelectedSpmFilesStep.convert(request, rawSourceBundle, convertedBundle);

        // TODO 20260417：根据指定公共资源构建追加计划。
        TsukuyomiCommonResourceAppendPlan appendPlan =
                buildSpecifiedCommonResourceAppendPlanStep.buildAppendPlan(request, rawSourceBundle, convertedBundle);
        convertedBundle.setCommonResourceAppendPlan(appendPlan);

        // TODO 20260417：把指定公共资源追加到继承基线；当前空实现直接返回继承基线。
        TsukuyomiBsdxBaselineBundle preparedBaseline =
                appendSpecifiedCommonResourcesStep.append(request, inheritedBaseline, convertedBundle, appendPlan);
        convertedBundle.setPreparedBaselineBundle(preparedBaseline);

        return convertedBundle;
    }
}
