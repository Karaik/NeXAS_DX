package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert;

import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectilePrepareStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.grp.ConvertSelectedGrpFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.mek.ConvertSelectedMekFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.source.LoadRawBheSourceStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.spm.ConvertSelectedSpmFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz.ConvertSelectedWazFilesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

/**
 * 第 0 步资源转换层总览。
 *
 * <p>主流程只调用这个总览类。单机体私有资源留在 Tsukuyomi 子包内转换；
 * BHE 公共弹幕资源簇由同级 bhecommon 包统一转换，避免公共资源混入单机体 selected 转换步骤。</p>
 *
 * <p>公共弹幕资源簇的业务规约见：
 * src/main/resources/research/12-bhe-common-projectile-append-checklist.md。
 * Review 本类调用顺序时，需要同步核对该文档的 preparedBaseline / selected closure 边界。</p>
 */
public class TsukuyomiConvertOverviewStep {

    private final LoadRawBheSourceStep loadRawBheSourceStep = new LoadRawBheSourceStep();
    private final ConvertSelectedGrpFilesStep convertSelectedGrpFilesStep = new ConvertSelectedGrpFilesStep();
    private final ConvertSelectedMekFilesStep convertSelectedMekFilesStep = new ConvertSelectedMekFilesStep();
    private final ConvertSelectedWazFilesStep convertSelectedWazFilesStep = new ConvertSelectedWazFilesStep();
    private final ConvertSelectedSpmFilesStep convertSelectedSpmFilesStep = new ConvertSelectedSpmFilesStep();
    private final BheCommonProjectilePrepareStep bheCommonProjectilePrepareStep =
            new BheCommonProjectilePrepareStep();

    public TsukuyomiConvertedBundle convert(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle inheritedBaseline
    ) {
        TsukuyomiConvertedBundle convertedBundle = new TsukuyomiConvertedBundle();

        // 读取 BHE 原始资源：私有资源进入 selected 原料区，公共弹幕资源进入 common 原料区。
        TsukuyomiRawSourceBundle rawSourceBundle = loadRawBheSourceStep.load(request);
        convertedBundle.setRawSourceBundle(rawSourceBundle);

        // 第 0 步第二层：公共资源独立准备；本阶段只执行 convert，redirect/self/cross 保持 TODO。
        bheCommonProjectilePrepareStep.prepare(request, rawSourceBundle, inheritedBaseline, convertedBundle);

        // 单机体私有资源转换，只建立 Tsukuyomi 单机体源侧视图。
        convertSelectedGrpFilesStep.convert(request, rawSourceBundle, convertedBundle);
        convertSelectedMekFilesStep.convert(request, rawSourceBundle, convertedBundle);
        // raw source 的契约保证这里只包含私有 WAZ/SPM，因此 selected 转换无需 common 判断。
        convertSelectedWazFilesStep.convert(request, rawSourceBundle, convertedBundle);
        convertSelectedSpmFilesStep.convert(request, rawSourceBundle, convertedBundle);

        return convertedBundle;
    }
}
