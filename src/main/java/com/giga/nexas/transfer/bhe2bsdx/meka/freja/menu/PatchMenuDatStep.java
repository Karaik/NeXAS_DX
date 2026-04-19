package com.giga.nexas.transfer.bhe2bsdx.meka.freja.menu;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;

import java.util.ArrayList;
import java.util.List;


public class PatchMenuDatStep {

    public void patch(MenuOverrideContext context) {
        if (context == null) {
            throw new IllegalArgumentException("MenuOverrideContext 不能为 null");
        }
        MenuSlotMapping mapping = context.getSlotMapping();
        if (mapping == null) {
            throw new IllegalStateException("缺少菜单槽位映射，无法 patch 菜单 DAT");
        }

        int targetMekaIndex = context.getGrpAppendPlan().getMekaGroupIndex();
        Dat patchedMekaDat = patchMekaDat(context.getBsdxBaseline(), targetMekaIndex);
        Dat patchedMekaPilotDat = patchMekaPilotDat(context.getBsdxBaseline(), mapping, targetMekaIndex);
        Dat patchedSelectMekaMenuDat = patchSelectMekaMenuDat(context.getBsdxBaseline(), mapping, targetMekaIndex);

        context.setPatchedMekaDat(patchedMekaDat);
        context.setPatchedMekaPilotDat(patchedMekaPilotDat);
        context.setPatchedSelectMekaMenuDat(patchedSelectMekaMenuDat);
        context.getAudit().addDatPatchNote("patched Meka.dat with target meka index " + targetMekaIndex);
        context.getAudit().addDatPatchNote("patched MekaPilot.dat row " + mapping.getPilotRowIndex());
        context.getAudit().addDatPatchNote("patched SelectMekaMenu.dat row " + mapping.getSelectMenuRowIndex());
    }

    public Dat patchMekaDat(TsukuyomiBsdxBaselineBundle baseline, int targetMekaIndex) {
        Dat baselineDat = baseline == null ? null : baseline.getMekaDat();
        if (baselineDat == null) {
            return null;
        }

        Dat patched = copyDat(baselineDat);
        List<Object> targetRow = buildMekaDatRowFromBaselineTemplate(patched, targetMekaIndex);
        appendOrReplaceRowByFirstColumn(patched, targetMekaIndex, targetRow);
        return patched;
    }

    private List<Object> buildMekaDatRowFromBaselineTemplate(Dat baselineDat, int targetMekaIndex) {
        List<Object> template = resolveMekaDatTemplateRow(baselineDat, targetMekaIndex);
        List<Object> targetRow = new ArrayList<>(template);
        if (targetRow.isEmpty()) {
            throw new IllegalStateException("Meka.dat 模板行为空，无法写入目标机体 id");
        }
        targetRow.set(0, targetMekaIndex);
        return targetRow;
    }

    private List<Object> resolveMekaDatTemplateRow(Dat baselineDat, int targetMekaIndex) {
        if (baselineDat.getData() == null || baselineDat.getData().isEmpty()) {
            throw new IllegalStateException("Meka.dat 基线为空，无法生成目标机体行");
        }

        List<Object> bestRow = null;
        Integer bestId = null;
        for (List<Object> row : baselineDat.getData()) {
            Integer id = readFirstColumnAsInt(row);
            if (id == null) {
                continue;
            }
            if (id == targetMekaIndex) {
                return row;
            }
            if (id < targetMekaIndex && (bestId == null || id > bestId)) {
                bestId = id;
                bestRow = row;
            }
        }
        if (bestRow != null) {
            return bestRow;
        }

        for (List<Object> row : baselineDat.getData()) {
            Integer id = readFirstColumnAsInt(row);
            if (id != null && (bestId == null || id > bestId)) {
                bestId = id;
                bestRow = row;
            }
        }
        if (bestRow == null) {
            throw new IllegalStateException("Meka.dat 找不到可用模板行");
        }
        return bestRow;
    }

    public Dat patchMekaPilotDat(TsukuyomiBsdxBaselineBundle baseline, MenuSlotMapping mapping, int targetMekaIndex) {
        Dat baselineDat = baseline == null ? null : baseline.getMekaPilotDat();
        if (baselineDat == null) {
            return null;
        }

        Dat patched = copyDat(baselineDat);
        List<List<Object>> rows = patched.getData();
        int rowIndex = mapping.getPilotRowIndex();
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IllegalStateException("MekaPilot.dat pilot row 越界: " + rowIndex);
        }
        List<Object> row = new ArrayList<>(rows.get(rowIndex));
        if (!row.isEmpty()) {
            row.set(0, targetMekaIndex);
        }
        rows.set(rowIndex, row);
        return patched;
    }

    public Dat patchSelectMekaMenuDat(TsukuyomiBsdxBaselineBundle baseline, MenuSlotMapping mapping, int targetMekaIndex) {
        Dat baselineDat = baseline == null ? null : baseline.getSelectMekaMenuDat();
        if (baselineDat == null) {
            return null;
        }

        Dat patched = copyDat(baselineDat);
        List<List<Object>> rows = patched.getData();
        int rowIndex = mapping.getSelectMenuRowIndex();
        if (rowIndex < 0 || rowIndex >= rows.size()) {
            throw new IllegalStateException("SelectMekaMenu.dat row 越界: " + rowIndex);
        }
        List<Object> row = new ArrayList<>(rows.get(rowIndex));
        row.set(0, targetMekaIndex);
        if (row.size() > 1) {
            row.set(1, mapping.getSelectMenuAnimIndex());
        }
        if (row.size() > 2) {
            row.set(2, mapping.getSelectMenuState());
        }
        rows.set(rowIndex, row);
        return patched;
    }

    public void appendOrReplaceRowByFirstColumn(Dat dat, int firstColumn, List<Object> row) {
        if (dat.getData() == null) {
            dat.setData(new ArrayList<>());
        }
        for (int i = 0; i < dat.getData().size(); i++) {
            Integer value = readFirstColumnAsInt(dat.getData().get(i));
            if (value != null && value == firstColumn) {
                dat.getData().set(i, row);
                return;
            }
        }
        dat.getData().add(row);
    }

    public Integer readFirstColumnAsInt(List<Object> row) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        return toInteger(row.get(0));
    }

    public int toInt(Object value) {
        Integer converted = toInteger(value);
        if (converted == null) {
            throw new IllegalStateException("值不是整数: " + value);
        }
        return converted;
    }

    public Dat copyDat(Dat source) {
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

    private Integer toInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue) {
            return longValue.intValue();
        }
        if (value instanceof Short shortValue) {
            return shortValue.intValue();
        }
        if (value instanceof String stringValue) {
            try {
                return Integer.parseInt(stringValue.trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
