package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;

import java.util.List;

/**
 * 负责同步 ProgramMaterial 外层数组长度。
 *
 * <p>当前规则很直接：</p>
 * <ul>
 *     <li>`array1.size()` 必须追平 `SpriteGroup.spriteList.size()`</li>
 *     <li>`array2.size()` 必须追平 `SeGroup.seList.size()`</li>
 *     <li>`array3.size()` 必须追平 `BatVoice.voiceList.size()`</li>
 * </ul>
 *
 * <p>第一轮只补空槽，不尝试生成新的 values 内容。</p>
 */
public class SyncProgramMaterialStep {

    public ProgramMaterialGrp syncOuterArrays(
            AkaoGraftRequest request,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        if (bsdxBaseline == null || bsdxBaseline.getProgramMaterialGrp() == null) {
            return null;
        }

        ProgramMaterialGrp programMaterialGrp = bsdxBaseline.getProgramMaterialGrp();

        int requiredArray1Size = bsdxBaseline.getSpriteGroupGrp() != null
                && bsdxBaseline.getSpriteGroupGrp().getSpriteList() != null
                ? bsdxBaseline.getSpriteGroupGrp().getSpriteList().size()
                : 0;

        int requiredArray2Size = bsdxBaseline.getSeGroupGrp() != null
                && bsdxBaseline.getSeGroupGrp().getSeList() != null
                ? bsdxBaseline.getSeGroupGrp().getSeList().size()
                : 0;

        int requiredArray3Size = bsdxBaseline.getBatVoiceGrp() != null
                && bsdxBaseline.getBatVoiceGrp().getVoiceList() != null
                ? bsdxBaseline.getBatVoiceGrp().getVoiceList().size()
                : 0;

        ensureOuterArraySize(programMaterialGrp.getArray1(), requiredArray1Size);
        ensureOuterArraySize(programMaterialGrp.getArray2(), requiredArray2Size);
        ensureOuterArraySize(programMaterialGrp.getArray3(), requiredArray3Size);

        return programMaterialGrp;
    }

    private void ensureOuterArraySize(List<ProgramMaterialGrp.IntArray> values, int requiredSize) {
        if (values == null) {
            throw new IllegalStateException("ProgramMaterial 外层数组不能为空");
        }

        while (values.size() < requiredSize) {
            values.add(new ProgramMaterialGrp.IntArray());
        }
    }
}
