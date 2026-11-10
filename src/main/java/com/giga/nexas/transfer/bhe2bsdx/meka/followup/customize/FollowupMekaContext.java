package com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import lombok.Data;

/**
 * 落盘前客制化上下文。
 *
 * <p>这里集中放“当前单机体这一轮已经算出来的结果”，避免客制化实现再去
 * 重新找 request / baseline / importPlan / rebind 结果。当前设计是只读+就地修正，
 * 不再额外长出第二套 pipeline 状态对象。</p>
 */
@Data
public class FollowupMekaContext {

    private FollowupGraftRequest request;
    private TsukuyomiBsdxBaselineBundle baseline;
    private TsukuyomiConvertedBundle convertedBundle;
    private TsukuyomiPackageBundle selectedPackage;
    private TsukuyomiImportPlan importPlan;
    private TsukuyomiGrpAppendPlan grpAppendPlan;
    private ProgramMaterialGrp syncedProgramMaterial;
    private Mek reboundMek;
    private Waz reboundWaz;
}
