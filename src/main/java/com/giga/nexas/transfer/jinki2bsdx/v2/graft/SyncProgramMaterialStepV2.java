package com.giga.nexas.transfer.jinki2bsdx.v2.graft;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 同步 ProgramMaterial.grp 和 MapGroup.grp 的数组边界。
 *
 * <p>GRP append 后，SpriteGroup / SeGroup / BatVoice 的顶层长度会增长。
 * ProgramMaterial 的三段外层数组，以及 MapGroup 每个 entry 内部的三段数组，都必须追平这些长度。
 * 否则游戏运行时按新 group 数量访问旧长度数组，会在加载或 confirm 过程中崩溃。</p>
 *
 * <p>外部 ProgramMaterial.grp 只在能安全映射时合并 values：
 * SE 有明确 group/item 映射，BatVoice 可整组复制，Sprite 的 values 绑定 MapGroup，
 * 当前没有 MapGroup 映射时只允许空值或源/目标同 index 的安全同步。</p>
 */
public class SyncProgramMaterialStepV2 {

    /**
     * 外部 ProgramMaterial.grp fallback 解析时使用的字符集。
     *
     * <p>ProgramMaterial 本身以数值结构为主，但统一走 BinService 解析，
     * 字符集仍保持和 BSDX 资源生成链一致。</p>
     */
    private static final String CHARSET = "windows-31j";

    /**
     * ProgramMaterial / MapGroup 解析服务。
     *
     * <p>当前只在存在外部 ProgramMaterial.grp 时使用；
     * 主体同步逻辑直接操作已经加载的 baseline DTO。</p>
     */
    private final BsdxBinService bsdxBinService;

    public SyncProgramMaterialStepV2() {
        this(new BsdxBinService());
    }

    public SyncProgramMaterialStepV2(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public ProgramMaterialGrp syncOuterArrays(
            AkaoGraftRequest request,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        if (bsdxBaseline == null || bsdxBaseline.getProgramMaterialGrp() == null) {
            return null;
        }

        ProgramMaterialGrp programMaterialGrp = bsdxBaseline.getProgramMaterialGrp();
        ensureOuterArraySize(programMaterialGrp.getArray1(), spriteGroupSize(bsdxBaseline));
        ensureOuterArraySize(programMaterialGrp.getArray2(), seGroupSize(bsdxBaseline));
        ensureOuterArraySize(programMaterialGrp.getArray3(), batVoiceGroupSize(bsdxBaseline));

        ProgramMaterialGrp sourceProgramMaterial = loadExternalProgramMaterial(request);
        if (sourceProgramMaterial != null && grpAppendPlan != null) {
            mergeArray2Values(programMaterialGrp, sourceProgramMaterial, grpAppendPlan);
            mergeArray3Values(programMaterialGrp, sourceProgramMaterial, grpAppendPlan);
            mergeArray1ValuesIfSafe(programMaterialGrp, sourceProgramMaterial, grpAppendPlan);
        }

        padMapGroupInnerArrays(bsdxBaseline);
        return programMaterialGrp;
    }

    private ProgramMaterialGrp loadExternalProgramMaterial(AkaoGraftRequest request) {
        if (request == null || request.getExternalStaticAssetRoot() == null) {
            return null;
        }
        Path path = request.getExternalStaticAssetRoot().resolve("ProgramMaterial.grp");
        if (!Files.exists(path)) {
            return null;
        }
        try {
            ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
            return (ProgramMaterialGrp) dto.getData();
        } catch (IOException e) {
            throw new IllegalStateException("解析外部 ProgramMaterial.grp 失败: " + path, e);
        }
    }

    private void mergeArray2Values(ProgramMaterialGrp target, ProgramMaterialGrp source, GrpAppendPlan plan) {
        for (Map.Entry<Integer, Integer> entry : plan.getSourceSeGroupIndexToTargetIndex().entrySet()) {
            Integer sourceGroupIndex = entry.getKey();
            Integer targetGroupIndex = entry.getValue();
            if (!isValidIndex(source.getArray2(), sourceGroupIndex) || !isValidIndex(target.getArray2(), targetGroupIndex)) {
                continue;
            }

            List<Integer> sourceValues = source.getArray2().get(sourceGroupIndex).getValues();
            Map<Integer, Integer> itemMap = plan.getSourceSeItemIndexToTargetIndexByGroup().get(sourceGroupIndex);
            if (itemMap == null || itemMap.isEmpty()) {
                continue;
            }

            Set<Integer> merged = new LinkedHashSet<>(target.getArray2().get(targetGroupIndex).getValues());
            for (Integer sourceItemIndex : sourceValues) {
                Integer targetItemIndex = itemMap.get(sourceItemIndex);
                if (targetItemIndex != null) {
                    merged.add(targetItemIndex);
                }
            }
            target.getArray2().get(targetGroupIndex).setValues(new ArrayList<>(merged));
        }
    }

    private void mergeArray3Values(ProgramMaterialGrp target, ProgramMaterialGrp source, GrpAppendPlan plan) {
        for (Map.Entry<Integer, Integer> entry : plan.getSourceBatVoiceGroupIndexToTargetIndex().entrySet()) {
            Integer sourceGroupIndex = entry.getKey();
            Integer targetGroupIndex = entry.getValue();
            if (!isValidIndex(source.getArray3(), sourceGroupIndex) || !isValidIndex(target.getArray3(), targetGroupIndex)) {
                continue;
            }
            List<Integer> sourceValues = source.getArray3().get(sourceGroupIndex).getValues();
            target.getArray3().get(targetGroupIndex).setValues(new ArrayList<>(sourceValues));
        }
    }

    private void mergeArray1ValuesIfSafe(ProgramMaterialGrp target, ProgramMaterialGrp source, GrpAppendPlan plan) {
        for (Map.Entry<Integer, Integer> entry : plan.getSourceSpriteGroupIndexToTargetIndex().entrySet()) {
            Integer sourceSpriteIndex = entry.getKey();
            Integer targetSpriteIndex = entry.getValue();
            if (!isValidIndex(source.getArray1(), sourceSpriteIndex) || !isValidIndex(target.getArray1(), targetSpriteIndex)) {
                continue;
            }

            List<Integer> sourceValues = source.getArray1().get(sourceSpriteIndex).getValues();
            if (sourceValues.isEmpty() || sourceSpriteIndex.equals(targetSpriteIndex)) {
                target.getArray1().get(targetSpriteIndex).setValues(new ArrayList<>(sourceValues));
            }
        }
    }

    private boolean isValidIndex(List<?> list, Integer index) {
        return list != null && index != null && index >= 0 && index < list.size();
    }

    private void ensureOuterArraySize(List<ProgramMaterialGrp.IntArray> values, int requiredSize) {
        if (values == null) {
            throw new IllegalStateException("ProgramMaterial 外层数组不能为空");
        }
        while (values.size() < requiredSize) {
            values.add(new ProgramMaterialGrp.IntArray());
        }
    }

    private void padMapGroupInnerArrays(BsdxBaselineBundle bsdxBaseline) {
        if (bsdxBaseline == null || bsdxBaseline.getMapGroupGrp() == null
                || bsdxBaseline.getMapGroupGrp().getGroupList() == null) {
            return;
        }

        int requiredArray1Size = spriteGroupSize(bsdxBaseline);
        int requiredArray2Size = seGroupSize(bsdxBaseline);
        int requiredArray3Size = batVoiceGroupSize(bsdxBaseline);

        for (MapGroupGrp.MapGroup entry : bsdxBaseline.getMapGroupGrp().getGroupList()) {
            ensureMapGroupArraySize(entry.getArray1(), requiredArray1Size);
            ensureMapGroupArraySize(entry.getArray2(), requiredArray2Size);
            ensureMapGroupArraySize(entry.getArray3(), requiredArray3Size);
        }
    }

    private void ensureMapGroupArraySize(List<MapGroupGrp.IntArray> array, int requiredSize) {
        if (array == null) {
            return;
        }
        while (array.size() < requiredSize) {
            array.add(new MapGroupGrp.IntArray());
        }
    }

    private int spriteGroupSize(BsdxBaselineBundle baseline) {
        return baseline.getSpriteGroupGrp() != null && baseline.getSpriteGroupGrp().getSpriteList() != null
                ? baseline.getSpriteGroupGrp().getSpriteList().size()
                : 0;
    }

    private int seGroupSize(BsdxBaselineBundle baseline) {
        return baseline.getSeGroupGrp() != null && baseline.getSeGroupGrp().getSeList() != null
                ? baseline.getSeGroupGrp().getSeList().size()
                : 0;
    }

    private int batVoiceGroupSize(BsdxBaselineBundle baseline) {
        return baseline.getBatVoiceGrp() != null && baseline.getBatVoiceGrp().getVoiceList() != null
                ? baseline.getBatVoiceGrp().getVoiceList().size()
                : 0;
    }
}
