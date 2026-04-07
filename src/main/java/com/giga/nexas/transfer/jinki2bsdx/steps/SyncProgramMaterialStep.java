package com.giga.nexas.transfer.jinki2bsdx.steps;

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
 * 负责同步 ProgramMaterial。
 *
 * <p>当前策略分两层：</p>
 * <ul>
 *     <li>先保证 `array1/2/3` 外层长度和当前 grp 顶层长度一致</li>
 *     <li>再优先用外部真源 `ProgramMaterial.grp` 补能安全映射的 `values`</li>
 * </ul>
 *
 * <p>同时对 MapGroup.grp 每个 entry 的内部 array1/2/3 做长度 padding。</p>
 * <p>MapGroup 内部数组与 SpriteGroup/SeGroup/BatVoice 的顶层数量对齐：
 * array1 = SpriteGroup count, array2 = SeGroup count, array3 = BatVoice count。
 * 追加新条目到这些 grp 后，MapGroup 每个 entry 都需要同步 padding。</p>
 */
public class SyncProgramMaterialStep {

    private static final String CHARSET = "windows-31j";

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    public ProgramMaterialGrp syncOuterArrays(
            AkaoGraftRequest request,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        if (bsdxBaseline == null || bsdxBaseline.getProgramMaterialGrp() == null) {
            return null;
        }

        ProgramMaterialGrp programMaterialGrp = bsdxBaseline.getProgramMaterialGrp();

        // Step 5-1: 先把外层长度追平到当前 BSDX 基线追加后的真实大小。
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

        // Step 5-2: 如果外部真源里有 ProgramMaterial.grp，再把能安全映射的 values 补进来。
        ProgramMaterialGrp sourceProgramMaterial = loadExternalProgramMaterial(request);
        if (sourceProgramMaterial != null && grpAppendPlan != null) {
            mergeArray2Values(programMaterialGrp, sourceProgramMaterial, grpAppendPlan);
            mergeArray3Values(programMaterialGrp, sourceProgramMaterial, grpAppendPlan);
            mergeArray1ValuesIfSafe(programMaterialGrp, sourceProgramMaterial, grpAppendPlan);
        }

        // Step 5-3: MapGroup.grp 每个 entry 的 array1/2/3 也必须和 SpriteGroup/SeGroup/BatVoice 对齐。
        // 追加条目后（如 SpriteGroup 138→139, BatVoice 30→31），每个 MapGroup 的内部数组需要同步 padding。
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

    private void mergeArray2Values(
            ProgramMaterialGrp target,
            ProgramMaterialGrp source,
            GrpAppendPlan plan
    ) {
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

    private void mergeArray3Values(
            ProgramMaterialGrp target,
            ProgramMaterialGrp source,
            GrpAppendPlan plan
    ) {
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

    private void mergeArray1ValuesIfSafe(
            ProgramMaterialGrp target,
            ProgramMaterialGrp source,
            GrpAppendPlan plan
    ) {
        // array1 内部值绑的是 MapGroup 索引。
        // 当前 jinki2bsdx 还没有建立 MapGroup 映射，所以这里只做“明确安全”的同步：
        // 1. 源 values 为空，允许直接同步为空
        // 2. 或者源索引和目标索引相同，说明没有发生跨位置重定向
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

    /**
     * 对 MapGroup.grp 每个 entry 的内部 array1/2/3 做长度 padding，对齐到当前 grp 顶层数量。
     *
     * <p>对齐关系（每个 MapGroup entry）：</p>
     * <ul>
     *     <li>array1.size() == SpriteGroup.spriteList.size()</li>
     *     <li>array2.size() == SeGroup.seList.size()</li>
     *     <li>array3.size() == BatVoice.voiceList.size()</li>
     * </ul>
     *
     * <p>追加新条目到 SpriteGroup/BatVoice 等 grp 后，如果 MapGroup 的内部数组长度没有同步，
     * 引擎加载时会因长度不匹配而崩溃。</p>
     */
    private void padMapGroupInnerArrays(BsdxBaselineBundle bsdxBaseline) {
        if (bsdxBaseline == null || bsdxBaseline.getMapGroupGrp() == null
                || bsdxBaseline.getMapGroupGrp().getGroupList() == null) {
            return;
        }

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
}
