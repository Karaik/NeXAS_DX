package com.giga.nexas.controller.support;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.ResourceLayer;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.service.BsdxBinService;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hell 脚本编辑模式的数据装载器。
 *
 * <p>它把 `HellConfig.dat` 与 `Hell.bin` 字符串分发表拼成可直接展示的关卡列表，
 * 同时保留 `mod` 覆盖层来源信息，供 UI 标记当前读取的是原版还是覆盖版。
 */
public class HellScriptDataLoader {

    private static final String HELL_CONFIG_FILE = "HellConfig.dat";
    private static final String HELL_BIN_FILE = "Hell.bin";
    private static final int HELL_BIN_STRING_OFFSET = 3;

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    /**
     * 基于覆盖会话加载 Hell 关卡列表。
     */
    public List<HellStageDescriptor> loadStages(BsdxOverlayResourceSession session, String charset) throws Exception {
        Path hellConfigPath = session.resolveReadPath(HELL_CONFIG_FILE)
                .orElseThrow(() -> new IllegalStateException("Missing " + HELL_CONFIG_FILE));
        Path hellBinPath = session.resolveReadPath(HELL_BIN_FILE)
                .orElseThrow(() -> new IllegalStateException("Missing " + HELL_BIN_FILE));

        Dat hellConfig = (Dat) parse(hellConfigPath, charset).getData();
        Bin hellBin = (Bin) parse(hellBinPath, charset).getData();

        List<HellStageDescriptor> result = new ArrayList<>();
        List<List<Object>> rows = hellConfig.getData();
        List<String> strings = hellBin.getStringTable() == null ? List.of() : hellBin.getStringTable();

        for (int index = 0; index < rows.size(); index++) {
            List<Object> row = rows.get(index);
            String scriptFileName = resolveStageScriptFileName(strings, index);
            Path activeScriptPath = scriptFileName == null ? null : session.resolveReadPath(scriptFileName).orElse(null);

            result.add(HellStageDescriptor.builder()
                    .index(index)
                    .title(readString(row, HellConfigLayout.TITLE))
                    .description(readString(row, HellConfigLayout.DESCRIPTION))
                    .hellLevel(readInteger(row, HellConfigLayout.HELL_LEVEL))
                    .mapId(readInteger(row, HellConfigLayout.MAP_ID))
                    .shopId(readInteger(row, HellConfigLayout.SHOP_ID))
                    .portraitId(readInteger(row, HellConfigLayout.PORTRAIT_ID))
                    .balloonStyleId(readInteger(row, HellConfigLayout.BALLOON_STYLE_ID))
                    .enemyTypes(readIntegerSlice(row, HellConfigLayout.ENEMY_TYPE_START, HellConfigLayout.ENEMY_TYPE_END))
                    .enemyCounts(readIntegerSlice(row, HellConfigLayout.ENEMY_COUNT_START, HellConfigLayout.ENEMY_COUNT_END))
                    .scriptFileName(scriptFileName)
                    .activeScriptPath(activeScriptPath)
                    .scriptLayer(resolveLayer(session, scriptFileName))
                    .configLayer(resolveLayer(session, HELL_CONFIG_FILE))
                    .build());
        }

        return result;
    }

    /**
     * 统一解析入口。
     */
    private ResponseDTO<?> parse(Path path, String charset) throws Exception {
        return bsdxBinService.parse(path.toString(), charset);
    }

    /**
     * 从 `Hell.bin` 字符串表推导实际子脚本文件名。
     */
    private String resolveStageScriptFileName(List<String> strings, int stageIndex) {
        int stringIndex = stageIndex + HELL_BIN_STRING_OFFSET;
        if (stringIndex < 0 || stringIndex >= strings.size()) {
            return null;
        }
        String raw = strings.get(stringIndex);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return raw.toLowerCase(Locale.ROOT).endsWith(".bin") ? raw : raw + ".bin";
    }

    /**
     * 把当前命中的文件映射到来源层。
     */
    private ResourceLayer resolveLayer(BsdxOverlayResourceSession session, String fileName) {
        if (fileName == null) {
            return null;
        }
        return session.isOverridden(fileName) ? ResourceLayer.MOD : ResourceLayer.ROOT;
    }

    /**
     * 读取单列字符串。
     */
    private String readString(List<Object> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return null;
        }
        Object value = row.get(index);
        return value == null ? null : value.toString();
    }

    /**
     * 读取单列整数。
     */
    private Integer readInteger(List<Object> row, int index) {
        if (row == null || index < 0 || index >= row.size()) {
            return null;
        }
        Object value = row.get(index);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * 读取一段整数切片，保留原始槽位顺序。
     */
    private List<Integer> readIntegerSlice(List<Object> row, int fromInclusive, int toInclusive) {
        List<Integer> values = new ArrayList<>();
        for (int i = fromInclusive; i <= toInclusive; i++) {
            values.add(readInteger(row, i));
        }
        return values;
    }
}
