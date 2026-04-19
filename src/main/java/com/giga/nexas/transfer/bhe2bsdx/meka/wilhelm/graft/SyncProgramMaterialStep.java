package com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm.graft;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SyncProgramMaterialStep {

    
    private static final String CHARSET = "windows-31j";

    
    private final BsdxBinService bsdxBinService;

    public SyncProgramMaterialStep() {
        this(new BsdxBinService());
    }

    public SyncProgramMaterialStep(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public ProgramMaterialGrp syncOuterArrays(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiGrpAppendPlan grpAppendPlan
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

    private ProgramMaterialGrp loadExternalProgramMaterial(TsukuyomiGraftRequest request) {
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

    private void mergeArray2Values(ProgramMaterialGrp target, ProgramMaterialGrp source, TsukuyomiGrpAppendPlan plan) {
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

    private void mergeArray3Values(ProgramMaterialGrp target, ProgramMaterialGrp source, TsukuyomiGrpAppendPlan plan) {
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

    private void mergeArray1ValuesIfSafe(ProgramMaterialGrp target, ProgramMaterialGrp source, TsukuyomiGrpAppendPlan plan) {
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

    private void padMapGroupInnerArrays(TsukuyomiBsdxBaselineBundle bsdxBaseline) {
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

    private int spriteGroupSize(TsukuyomiBsdxBaselineBundle baseline) {
        return baseline.getSpriteGroupGrp() != null && baseline.getSpriteGroupGrp().getSpriteList() != null
                ? baseline.getSpriteGroupGrp().getSpriteList().size()
                : 0;
    }

    private int seGroupSize(TsukuyomiBsdxBaselineBundle baseline) {
        return baseline.getSeGroupGrp() != null && baseline.getSeGroupGrp().getSeList() != null
                ? baseline.getSeGroupGrp().getSeList().size()
                : 0;
    }

    private int batVoiceGroupSize(TsukuyomiBsdxBaselineBundle baseline) {
        return baseline.getBatVoiceGrp() != null && baseline.getBatVoiceGrp().getVoiceList() != null
                ? baseline.getBatVoiceGrp().getVoiceList().size()
                : 0;
    }
}
