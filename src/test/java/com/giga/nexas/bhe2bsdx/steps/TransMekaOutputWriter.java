package com.giga.nexas.bhe2bsdx.steps;

import com.giga.nexas.dto.bsdx.Bsdx;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * 迁移输出写盘：只输出传入的变更集合，避免一次性写出全部 SPM 造成目录膨胀。
 * 输出目录结构与游戏资源一致：grp/mek/waz/spm。
 */
@Slf4j
public class TransMekaOutputWriter {

    private static final String CHARSET = "windows-31j";
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    /**
     * 写出迁移后的 BSDX 资源。
     *
     * @param outputDir 输出根目录（会自动创建）
     * @param grpMap    需要写出的 grp（仅传入变更的条目即可）
     * @param mekMap    需要写出的 mek（仅传入变更的条目即可）
     * @param wazMap    需要写出的 waz（仅传入变更的条目即可）
     * @param spmMap    需要写出的 spm（仅传入变更的条目即可）
     */
    public void writeOutputs(
            Path outputDir,
            Map<String, Grp> grpMap,
            Map<String, Mek> mekMap,
            Map<String, Waz> wazMap,
            Map<String, Spm> spmMap
    ) throws IOException {
        if (outputDir == null) {
            log.warn("输出目录为空，跳过写盘。");
            return;
        }
        Files.createDirectories(outputDir);

        writeMap(outputDir, grpMap, "grp", "grp");
        writeMap(outputDir, mekMap, "mek", "mek");
        writeMap(outputDir, wazMap, "waz", "waz");
        writeMap(outputDir, spmMap, "spm", "spm");
    }

    private <T extends Bsdx> void writeMap(
            Path targetDir,
            Map<String, T> map,
            String defaultExt,
            String label
    ) throws IOException {
        if (map == null || map.isEmpty()) {
            return;
        }
        Files.createDirectories(targetDir);

        int count = 0;
        for (Map.Entry<String, T> entry : map.entrySet()) {
            String baseName = normalizeBaseName(entry.getKey());
            if (baseName == null) {
                log.warn("跳过 {} 输出：key 为空", label);
                continue;
            }
            T obj = entry.getValue();
            if (obj == null) {
                log.warn("跳过 {} 输出：{} 对象为空", label, baseName);
                continue;
            }
            String ext = normalizeExtension(obj, defaultExt);
            Path target = targetDir.resolve(baseName + "." + ext);
            bsdxBinService.generate(target.toString(), obj, CHARSET);
            count++;
            log.info("✅ 输出 {}: {}", label, target);
        }

        log.info("{} 输出数量: {}", label, count);
    }

    private String normalizeBaseName(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.toLowerCase();
    }

    private String normalizeExtension(Bsdx obj, String defaultExt) {
        String ext = obj.getExtensionName();
        if (ext == null || ext.isBlank()) {
            obj.setExtensionName(defaultExt);
            return defaultExt;
        }
        return ext.toLowerCase();
    }
}
