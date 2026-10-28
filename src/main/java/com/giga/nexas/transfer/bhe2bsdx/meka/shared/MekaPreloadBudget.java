package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 扩容最终机体的预加载调度费用表，保留继承行及原生100MiB调度阈值。 */
public final class MekaPreloadBudget {
    private static final String FILE_NAME = "BQMekaMemory.dat";
    private static final String CHARSET = "windows-31j";
    private static final int ORIGINAL_MEKA_COUNT = 103;
    // 原版有效表已使用的最大正权重。新ID保守占用调度预算，不表示heap或纹理的精确字节数。
    // 此策略仍须通过原生force/release/queue运行验收，不能由数据覆盖测试代替。
    private static final int NEW_ID_SCHEDULING_WEIGHT = 100_000_000;

    private MekaPreloadBudget() { }

    public static void writeIfExpanded(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle baseline,
            Path outputRoot,
            TsukuyomiImportedAssetSet assets
    ) throws IOException {
        if (baseline == null || baseline.getMekaGroupGrp() == null
                || baseline.getMekaGroupGrp().getMekaList() == null) return;
        int targetRows = baseline.getMekaGroupGrp().getMekaList().size();
        if (targetRows <= ORIGINAL_MEKA_COUNT) return;

        Path output = outputRoot.resolve(FILE_NAME);
        Path source = Files.isRegularFile(output) ? output
                : request == null || request.getBsdxDatDir() == null ? null : request.getBsdxDatDir().resolve(FILE_NAME);
        if (source == null || !Files.isRegularFile(source)) {
            throw new IllegalStateException("机体扩容至" + targetRows + "行时缺少预加载费用源: " + FILE_NAME);
        }

        byte[] sourceBytes = Files.readAllBytes(source);
        if (sourceBytes.length < 8 || (sourceBytes.length - 8) % Integer.BYTES != 0) {
            throw new IllegalStateException(FILE_NAME + "必须是完整的单列int表: " + source);
        }
        ByteBuffer header = ByteBuffer.wrap(sourceBytes).order(ByteOrder.LITTLE_ENDIAN);
        int columns = header.getInt();
        int columnType = header.getInt();
        if (columns != 1 || (columnType != 0 && columnType != 2)) {
            throw new IllegalStateException(FILE_NAME + "必须是单列整数schema: " + source);
        }
        int sourceRows = (sourceBytes.length - 8) / Integer.BYTES;
        if (sourceRows < ORIGINAL_MEKA_COUNT || sourceRows > targetRows) {
            throw new IllegalStateException(FILE_NAME + "继承行数" + sourceRows
                    + "不在原版" + ORIGINAL_MEKA_COUNT + "行至目标" + targetRows + "行范围内，不能补造旧行或截断");
        }

        BsdxBinService service = new BsdxBinService();
        Dat inherited = (Dat) service.parse(source.toString(), CHARSET).getData();
        if (inherited.getData() == null || inherited.getData().size() != sourceRows) {
            throw new IllegalStateException(FILE_NAME + "解析行数与输入字节不一致: " + source);
        }
        Dat expanded = new Dat();
        expanded.setFileName(inherited.getFileName());
        expanded.setExtensionName("dat");
        expanded.setColumnCount(inherited.getColumnCount());
        expanded.setColumnTypes(new ArrayList<>(inherited.getColumnTypes()));
        for (int id = 0; id < sourceRows; id++) {
            List<Object> row = inherited.getData().get(id);
            if (row == null || row.size() != 1 || !(row.get(0) instanceof Integer value) || value < 0) {
                throw new IllegalStateException(FILE_NAME + "第" + id + "行必须是非负整数调度权重");
            }
            expanded.addRow(new ArrayList<>(row));
        }
        while (expanded.getData().size() < targetRows) {
            expanded.addRow(List.of(NEW_ID_SCHEDULING_WEIGHT));
        }
        service.generate(output.toString(), expanded, CHARSET);
        Path identity = output.toAbsolutePath().normalize();
        assets.getGeneratedDatFiles().removeIf(path -> path != null
                && path.toAbsolutePath().normalize().equals(identity));
        assets.getGeneratedDatFiles().add(output);
    }
}
