package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 生成菜单相关 DAT 产物。
 *
 * <p>这里复刻旧 pipeline 的三类 DAT 行为：
 * Meka.dat 取源侧相对 BSDX 多出的 source-only 行，改成目标 meka index；
 * MekaPilot.dat 在 donor pilot 行原地替换第一列；
 * SelectMekaMenu.dat 在可见菜单行原地替换 meka index，并保持推导出的 anim/state。</p>
 *
 * <p>这一步不写文件，只写入 MenuOverrideContext。实际落盘由 MenuOverridePipeline 统一处理。</p>
 */
public class PatchMenuDatStep {

    /**
     * 解析外部 fallback DAT 时使用的字符集。
     *
     * <p>正常路径会从已加载的 TsukuyomiPackageBundle 读取 DAT；只有源包缺失时才走外部目录 fallback。
     * 这里仍必须使用 windows-31j，避免 fallback 解析出的字符串 bytes 和旧 pipeline 不一致。</p>
     */
    private static final String CHARSET = "windows-31j";

    /**
     * 外部 fallback DAT 的解析服务。
     *
     * <p>只用于 `Meka.dat` 源侧缺失时的旧 pipeline 兼容路径；
     * 常规 DAT patch 不应反复读写磁盘。</p>
     */
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    public void patch(MenuOverrideContext context) {
        if (context == null) {
            throw new IllegalArgumentException("MenuOverrideContext 不能为空");
        }
        MenuSlotMapping mapping = context.getSlotMapping();
        if (mapping == null) {
            throw new IllegalStateException("缺少菜单槽位映射，无法 patch 菜单 DAT");
        }

        int targetMekaIndex = context.getGrpAppendPlan().getMekaGroupIndex();
        Dat patchedMekaDat = patchMekaDat(
                context.getTsukuyomiPackage(),
                context.getBsdxBaseline(),
                context.getRequest() == null ? null : context.getRequest().getExternalStaticAssetRoot(),
                targetMekaIndex
        );
        Dat patchedMekaPilotDat = patchMekaPilotDat(context.getBsdxBaseline(), mapping, targetMekaIndex);
        Dat patchedSelectMekaMenuDat = patchSelectMekaMenuDat(context.getBsdxBaseline(), mapping, targetMekaIndex);

        context.setPatchedMekaDat(patchedMekaDat);
        context.setPatchedMekaPilotDat(patchedMekaPilotDat);
        context.setPatchedSelectMekaMenuDat(patchedSelectMekaMenuDat);
        context.getAudit().addDatPatchNote("patched Meka.dat with target meka index " + targetMekaIndex);
        context.getAudit().addDatPatchNote("patched MekaPilot.dat row " + mapping.getPilotRowIndex());
        context.getAudit().addDatPatchNote("patched SelectMekaMenu.dat row " + mapping.getSelectMenuRowIndex());
    }

    public Dat patchMekaDat(
            TsukuyomiPackageBundle sourcePackage,
            TsukuyomiBsdxBaselineBundle baseline,
            Path externalRoot,
            int targetMekaIndex
    ) {
        Dat baselineDat = baseline == null ? null : baseline.getMekaDat();
        if (baselineDat == null) {
            return null;
        }

        Dat patched = copyDat(baselineDat);
        Dat source = sourcePackage == null ? null : sourcePackage.getMekaDat();
        if (source == null) {
            // TSUKUYOMI 包正常应有 Meka.dat；保留 external fallback 是为了复刻旧 pipeline 的容错路径。
            source = loadExternalOptionalDat(externalRoot, "Meka.dat");
        }
        if (source == null) {
            return patched;
        }

        List<Object> sourceRow = resolveSourceOnlyMekaDatRow(source, baselineDat);
        if (sourceRow == null) {
            return patched;
        }

        List<Object> targetRow = new ArrayList<>(sourceRow);
        if (!targetRow.isEmpty()) {
            targetRow.set(0, targetMekaIndex);
        }
        // TODO 客制化入口：如果某台机体需要手动写 DAT 内部魔法值，优先把字段做成明确 profile/spec，
        // 再在这里按列写入 targetRow，并补 audit note 说明“哪一列、为什么、来自哪个 donor/逆向结论”。
        // 不要直接在 source DAT 或旧 pipeline 里改二进制；最终判断仍然是新旧输出 byte parity。
        appendOrReplaceRowByFirstColumn(patched, targetMekaIndex, targetRow);
        return patched;
    }

    public List<Object> resolveSourceOnlyMekaDatRow(Dat source, Dat baseline) {
        if (source == null || source.getData() == null) {
            return null;
        }

        Set<Integer> baselineIds = new LinkedHashSet<>();
        if (baseline != null && baseline.getData() != null) {
            for (List<Object> row : baseline.getData()) {
                Integer id = readFirstColumnAsInt(row);
                if (id != null) {
                    baselineIds.add(id);
                }
            }
        }

        List<List<Object>> sourceOnlyRows = new ArrayList<>();
        for (List<Object> row : source.getData()) {
            Integer id = readFirstColumnAsInt(row);
            if (id != null && !baselineIds.contains(id)) {
                sourceOnlyRows.add(row);
            }
        }

        if (sourceOnlyRows.size() == 1) {
            return sourceOnlyRows.get(0);
        }

        // 如果未来源侧一次多出多台机体，当前阶段先沿用旧行为：取第一列 id 最大的新增行。
        // 这不是通用策略，只是为了保持 Tsukuyomi 迁移和旧 pipeline parity。
        List<Object> bestRow = null;
        Integer bestId = null;
        for (List<Object> row : sourceOnlyRows) {
            Integer id = readFirstColumnAsInt(row);
            if (id != null && (bestId == null || id > bestId)) {
                bestId = id;
                bestRow = row;
            }
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
        // TODO 客制化入口：如果 pilot 行还有该角色专属列需要覆写，在这里集中处理。
        // 建议先给 MenuOverrideSpec 增加“列号 -> 值”的显式结构，避免把无名魔法列散在多个 step 里。
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
        // TODO 客制化入口：SelectMekaMenu.dat 的额外列如果以后需要手动调值，应在这里写入。
        // 写之前先确认列语义，并把魔法值来源写进 audit；不要让 ResolveMenuSlotStep 同时承担“推导”和“覆写”两种职责。
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
        return toInt(row.get(0));
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
