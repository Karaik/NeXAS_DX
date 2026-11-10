package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tsukuyomi graft 结果容器。
 */
@Data
public class TsukuyomiGraftResult {

    private TsukuyomiRawSourceBundle rawSourceBundle;
    private TsukuyomiConvertedBundle convertedBundle;
    private BheCommonProjectileAppendPlan commonProjectileAppendPlan;
    private TsukuyomiPackageBundle tsukuyomiPackage;
    private TsukuyomiBsdxBaselineBundle bsdxBaseline;
    private TsukuyomiBsdxBaselineBundle preparedBaselineBundle;
    private TsukuyomiImportPlan importPlan;
    private TsukuyomiGrpAppendPlan grpAppendPlan;
    /**
     * 已经进入整条 graft 链的 BHE 源 BatVoice group -> BSDX 目标 BatVoice group。
     *
     * <p>公共 WAZ 会被每一轮 follow-up 重新落盘，它里面可能同时引用多个 BHE 机体的语音。
     * 因此不能只拿“当前机体”的映射，否则前面机体在公共 WAZ 里的语音 group 会漏改。
     * 这个字段随结果向后传递，每一轮把上一轮累计映射和当前 {@link TsukuyomiGrpAppendPlan} 合并。</p>
     */
    private Map<Integer, Integer> cumulativeSourceBatVoiceGroupIndexToTargetIndex = new LinkedHashMap<>();
    private ProgramMaterialGrp syncedProgramMaterial;
    private Mek reboundTsukuyomiMek;
    private Waz reboundTsukuyomiWaz;
    private TsukuyomiImportedAssetSet importedAssetSet;
    private Dat patchedMekaDat;
    private Dat patchedMekaPilotDat;
    private Dat patchedSelectMekaMenuDat;
    private Spm patchedMekaPilotSpm;
    private Spm patchedSelectMekaMenuMekaSpm;
    private TsukuyomiExePatchPlan exePatchPlan;
    private TsukuyomiPacPackPlan pacPackPlan;
}
