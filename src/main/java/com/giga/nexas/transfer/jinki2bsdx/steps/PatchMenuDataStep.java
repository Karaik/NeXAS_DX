package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 负责补当前阶段可执行的菜单层 dat。
 *
 * <p>当前只补两份：</p>
 * <ul>
 *     <li>`Meka.dat`</li>
 *     <li>`MekaPilot.dat`</li>
 * </ul>
 *
 * <p>`SelectMekaMenu.dat` 因为当前没有可靠的 JINKI 真源行和对应 UI spm 输入，
 * 这一步先不硬补。</p>
 */
public class PatchMenuDataStep {

    private static final String CHARSET = "windows-31j";

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    public void patchMenuData(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan,
            AkaoGraftResult result
    ) {
        if (request == null || !request.isPatchMenuData()) {
            return;
        }
        if (jinkiPackage == null || bsdxBaseline == null || grpAppendPlan == null || result == null) {
            return;
        }

        Dat patchedMekaDat = patchMekaDat(jinkiPackage, bsdxBaseline, grpAppendPlan);
        Dat patchedMekaPilotDat = patchMekaPilotDat(request, jinkiPackage, bsdxBaseline, grpAppendPlan);

        result.setPatchedMekaDat(patchedMekaDat);
        result.setPatchedMekaPilotDat(patchedMekaPilotDat);

        writePatchedDatOutputs(result.getImportedAssetSet(), patchedMekaDat, patchedMekaPilotDat);
    }

    private Dat patchMekaDat(
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        Dat source = jinkiPackage.getMekaDat();
        Dat baseline = bsdxBaseline.getMekaDat();
        if (baseline == null) {
            return null;
        }

        Dat patched = copyDat(baseline);
        Integer sourceMekaIndex = findMekaIndexByCode(jinkiPackage.getMekaGroupGrp(), "AKAO");
        if (source == null || sourceMekaIndex == null) {
            appendOrReplaceSingleIntRow(patched, grpAppendPlan.getMekaGroupIndex(), 0, 0);
            return patched;
        }

        List<Object> sourceRow = findFirstRowByFirstColumn(source, sourceMekaIndex);
        if (sourceRow == null) {
            appendOrReplaceSingleIntRow(patched, grpAppendPlan.getMekaGroupIndex(), 0, 0);
            return patched;
        }

        List<Object> targetRow = new ArrayList<>(sourceRow);
        if (!targetRow.isEmpty()) {
            targetRow.set(0, grpAppendPlan.getMekaGroupIndex());
        }
        appendOrReplaceRowByFirstColumn(patched, grpAppendPlan.getMekaGroupIndex(), targetRow);
        return patched;
    }

    private Dat patchMekaPilotDat(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        Dat baseline = bsdxBaseline.getMekaPilotDat();
        if (baseline == null) {
            return null;
        }

        Dat source = jinkiPackage.getMekaPilotDat();
        if (source == null) {
            source = loadExternalOptionalDat(request.getExternalStaticAssetRoot(), "MekaPilot.dat");
        }

        Dat patched = copyDat(baseline);
        Integer sourceMekaIndex = findMekaIndexByCode(jinkiPackage.getMekaGroupGrp(), "AKAO");

        // 如果没有源 MekaPilot.dat，就退化成“保证目标 meka 索引至少进列表一次”。
        if (source == null || sourceMekaIndex == null) {
            appendSingleColumnIndexIfAbsent(patched, grpAppendPlan.getMekaGroupIndex());
            return patched;
        }

        List<List<Object>> matchedRows = findAllRowsByFirstColumn(source, sourceMekaIndex);
        if (matchedRows.isEmpty()) {
            appendSingleColumnIndexIfAbsent(patched, grpAppendPlan.getMekaGroupIndex());
            return patched;
        }

        for (List<Object> row : matchedRows) {
            List<Object> targetRow = new ArrayList<>(row);
            if (!targetRow.isEmpty()) {
                targetRow.set(0, grpAppendPlan.getMekaGroupIndex());
            }
            appendOrReplaceRowByFirstColumn(patched, grpAppendPlan.getMekaGroupIndex(), targetRow);
        }
        return patched;
    }

    private Dat loadExternalOptionalDat(Path root, String fileName) {
        if (root == null || !Files.exists(root)) {
            return null;
        }

        Path path = root.resolve(fileName);
        if (!Files.exists(path)) {
            return null;
        }

        try {
            ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
            return (Dat) dto.getData();
        } catch (IOException e) {
            throw new IllegalStateException("解析外部 dat 失败: " + path, e);
        }
    }

    private void writePatchedDatOutputs(ImportedAssetSet importedAssetSet, Dat patchedMekaDat, Dat patchedMekaPilotDat) {
        if (importedAssetSet == null || importedAssetSet.getOutputRootDir() == null) {
            return;
        }

        try {
            Path datDir = importedAssetSet.getOutputRootDir().resolve("dat");
            Files.createDirectories(datDir);

            if (patchedMekaDat != null) {
                Path output = datDir.resolve("Meka.dat");
                bsdxBinService.generate(output.toString(), patchedMekaDat, CHARSET);
                importedAssetSet.getGeneratedDatFiles().add(output);
            }

            if (patchedMekaPilotDat != null) {
                Path output = datDir.resolve("MekaPilot.dat");
                bsdxBinService.generate(output.toString(), patchedMekaPilotDat, CHARSET);
                importedAssetSet.getGeneratedDatFiles().add(output);
            }
        } catch (IOException e) {
            throw new IllegalStateException("写出 step9 dat 产物失败", e);
        }
    }

    private Dat copyDat(Dat source) {
        Dat target = new Dat();
        target.setFileName(source.getFileName());
        target.setExtensionName(source.getExtensionName());
        target.setColumnCount(source.getColumnCount());
        target.setColumnTypes(source.getColumnTypes() == null ? new ArrayList<>() : new ArrayList<>(source.getColumnTypes()));

        List<List<Object>> rows = new ArrayList<>();
        if (source.getData() != null) {
            for (List<Object> row : source.getData()) {
                rows.add(row == null ? null : new ArrayList<>(row));
            }
        }
        target.setData(rows);
        return target;
    }

    private Integer findMekaIndexByCode(MekaGroupGrp group, String codeName) {
        if (group == null || group.getMekaList() == null || codeName == null) {
            return null;
        }
        for (int i = 0; i < group.getMekaList().size(); i++) {
            MekaGroupGrp.MekaGroup entry = group.getMekaList().get(i);
            if (entry != null
                    && (entry.getExistFlag() == null || entry.getExistFlag() != 0)
                    && codeName.equalsIgnoreCase(entry.getMekaCodeName())) {
                return i;
            }
        }
        return null;
    }

    private List<Object> findFirstRowByFirstColumn(Dat dat, Integer index) {
        if (dat == null || dat.getData() == null || index == null) {
            return null;
        }
        for (List<Object> row : dat.getData()) {
            Integer value = readFirstColumnAsInt(row);
            if (value != null && value.equals(index)) {
                return row;
            }
        }
        return null;
    }

    private List<List<Object>> findAllRowsByFirstColumn(Dat dat, Integer index) {
        List<List<Object>> rows = new ArrayList<>();
        if (dat == null || dat.getData() == null || index == null) {
            return rows;
        }
        for (List<Object> row : dat.getData()) {
            Integer value = readFirstColumnAsInt(row);
            if (value != null && value.equals(index)) {
                rows.add(row);
            }
        }
        return rows;
    }

    private Integer readFirstColumnAsInt(List<Object> row) {
        if (row == null || row.isEmpty() || row.get(0) == null) {
            return null;
        }
        Object value = row.get(0);
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void appendOrReplaceSingleIntRow(Dat dat, int first, int second, int minColumns) {
        List<Object> row = new ArrayList<>();
        row.add(first);
        if (dat.getColumnCount() > 1 || minColumns > 1) {
            row.add(second);
        }
        appendOrReplaceRowByFirstColumn(dat, first, row);
    }

    private void appendSingleColumnIndexIfAbsent(Dat dat, int index) {
        if (findFirstRowByFirstColumn(dat, index) != null) {
            return;
        }
        List<Object> row = new ArrayList<>();
        row.add(index);
        dat.getData().add(row);
    }

    private void appendOrReplaceRowByFirstColumn(Dat dat, int index, List<Object> targetRow) {
        List<List<Object>> data = dat.getData();
        if (data == null) {
            data = new ArrayList<>();
            dat.setData(data);
        }

        for (int i = 0; i < data.size(); i++) {
            Integer value = readFirstColumnAsInt(data.get(i));
            if (value != null && value == index) {
                data.set(i, targetRow);
                return;
            }
        }

        data.add(targetRow);
    }
}
