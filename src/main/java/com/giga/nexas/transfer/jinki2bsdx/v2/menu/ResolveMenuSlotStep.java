package com.giga.nexas.transfer.jinki2bsdx.v2.menu;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;

import java.util.List;

/**
 * 从 BSDX 原菜单数据里推导可复用槽位。
 *
 * <p>菜单替换不能只拿一个“第 25 槽”的硬编码 anim index。
 * 真正的关系是：SelectMekaMenu.dat 行给出原 mekaIndex 和 select-menu anim，
 * MekaPilot.dat 再用 mekaIndex 反查 pilot anim row。</p>
 *
 * <p>这样做的价值是以后换 donor 槽时，不需要重新调试 SPM anim 槽位，
 * 只要 dat 关系仍成立，mapping 就能自动推导出来。</p>
 */
public class ResolveMenuSlotStep {

    public MenuSlotMapping resolve(MenuOverrideContext context) {
        if (context == null) {
            throw new IllegalArgumentException("MenuOverrideContext 不能为空");
        }

        BsdxBaselineBundle baseline = context.getBsdxBaseline();
        MenuOverrideSpec spec = context.getSpec();
        Dat selectMenuDat = baseline == null ? null : baseline.getSelectMekaMenuDat();

        List<Object> targetRow = readSelectMenuRow(selectMenuDat, spec.getVisibleSlotIndex());
        int sourceMekaIndex = readRequiredIntColumn(targetRow, 0, "SelectMekaMenu.dat mekaIndex");
        int selectMenuAnimIndex = readRequiredIntColumn(targetRow, 1, "SelectMekaMenu.dat animIndex");
        int selectMenuState = resolveSelectMenuState(
                selectMenuDat.getData(),
                spec.getStateDonorRowIndex(),
                targetRow
        );
        int pilotRowIndex = findPilotRowIndex(baseline.getMekaPilotDat(), sourceMekaIndex);
        int pilotAnimIndex = pilotRowIndex;

        // pilotAnimIndex 当前等于 pilotRowIndex，这是旧资源结构的事实：
        // MekaPilot.spm 的 anim 顺序和 MekaPilot.dat 行顺序对齐。
        // 这里显式落到 mapping 里，后续 SPM step 不再重复做这个推断。
        MenuSlotMapping mapping = new MenuSlotMapping(
                spec.getVisibleSlotIndex(),
                sourceMekaIndex,
                selectMenuAnimIndex,
                selectMenuState,
                pilotRowIndex,
                pilotAnimIndex
        );
        validateMapping(mapping, baseline);

        context.setSlotMapping(mapping);
        context.getAudit().setSlotMapping(mapping);
        context.getAudit().addSlotMappingNote(
                "resolved menu slot: selectRow=" + mapping.getSelectMenuRowIndex()
                        + ", sourceMeka=" + mapping.getSourceMekaIndex()
                        + ", selectAnim=" + mapping.getSelectMenuAnimIndex()
                        + ", pilotRow=" + mapping.getPilotRowIndex()
                        + ", pilotAnim=" + mapping.getPilotAnimIndex()
        );
        return mapping;
    }

    public List<Object> readSelectMenuRow(Dat dat, int rowIndex) {
        if (dat == null || dat.getData() == null) {
            throw new IllegalStateException("缺少 SelectMekaMenu.dat 基线，无法定位替换槽");
        }
        if (rowIndex < 0 || rowIndex >= dat.getData().size()) {
            throw new IllegalStateException("替换槽位越界: " + rowIndex);
        }
        List<Object> row = dat.getData().get(rowIndex);
        if (row == null) {
            throw new IllegalStateException("SelectMekaMenu.dat 行为空: " + rowIndex);
        }
        return row;
    }

    public int readRequiredIntColumn(List<Object> row, int columnIndex, String label) {
        if (row == null || columnIndex < 0 || columnIndex >= row.size()) {
            throw new IllegalStateException(label + " 列不存在: " + columnIndex);
        }
        Integer value = toInt(row.get(columnIndex));
        if (value == null) {
            throw new IllegalStateException(label + " 不是整数: " + row.get(columnIndex));
        }
        return value;
    }

    public int resolveSelectMenuState(List<List<Object>> rows, Integer donorRowIndex, List<Object> fallbackRow) {
        // SelectMekaMenu 第 3 列不是 mekaIndex，而是菜单状态/分支值。
        // AKAO 当前复用相邻 donor 行，避免目标槽原本可能处在移动/删除等特殊菜单分支。
        if (rows != null && donorRowIndex != null && donorRowIndex >= 0 && donorRowIndex < rows.size()) {
            List<Object> donorRow = rows.get(donorRowIndex);
            if (donorRow != null && donorRow.size() > 2) {
                Integer donorState = toInt(donorRow.get(2));
                if (donorState != null) {
                    return donorState;
                }
            }
        }
        return readRequiredIntColumn(fallbackRow, 2, "SelectMekaMenu.dat state");
    }

    public int findPilotRowIndex(Dat mekaPilotDat, int mekaIndex) {
        if (mekaPilotDat == null || mekaPilotDat.getData() == null) {
            throw new IllegalStateException("缺少 MekaPilot.dat 基线，无法反查 pilot 行");
        }
        for (int i = 0; i < mekaPilotDat.getData().size(); i++) {
            List<Object> row = mekaPilotDat.getData().get(i);
            if (row == null || row.isEmpty()) {
                continue;
            }
            Integer value = toInt(row.get(0));
            if (value != null && value == mekaIndex) {
                return i;
            }
        }
        throw new IllegalStateException("MekaPilot.dat 中找不到 SelectMekaMenu 槽位对应的 pilot 行: " + mekaIndex);
    }

    public void validateMapping(MenuSlotMapping mapping, BsdxBaselineBundle baseline) {
        if (mapping == null || baseline == null) {
            throw new IllegalStateException("菜单槽位映射或基线为空");
        }
        if (baseline.getSelectMekaMenuMekaSpm() == null
                || baseline.getSelectMekaMenuMekaSpm().getAnimData() == null
                || mapping.getSelectMenuAnimIndex() < 0
                || mapping.getSelectMenuAnimIndex() >= baseline.getSelectMekaMenuMekaSpm().getAnimData().size()) {
            throw new IllegalStateException("SelectMekaMenuMeka.spm animIndex 越界: " + mapping.getSelectMenuAnimIndex());
        }
        if (baseline.getMekaPilotSpm() == null
                || baseline.getMekaPilotSpm().getAnimData() == null
                || mapping.getPilotAnimIndex() < 0
                || mapping.getPilotAnimIndex() >= baseline.getMekaPilotSpm().getAnimData().size()) {
            throw new IllegalStateException("MekaPilot.spm animIndex 越界: " + mapping.getPilotAnimIndex());
        }
    }

    private Integer toInt(Object value) {
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
