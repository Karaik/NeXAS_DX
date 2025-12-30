package com.giga.nexas.bhe2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.spm.Spm;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 静态资源复制：
 * - 从 spm/grp 里抽取资源文件名（图片/语音）
 * - 从指定资源目录中查找并复制到 output 根目录
 */
@Slf4j
public class StaticAssetCopier {

    public void copyAssets(Path outputDir, Path assetRoot,
                           Map<String, Spm> spmMap,
                           Map<String, Grp> grpMap) {
        if (outputDir == null || assetRoot == null) {
            return;
        }
        if (!Files.exists(assetRoot)) {
            log.warn("静态资源目录不存在: {}", assetRoot);
            return;
        }
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            log.warn("无法创建输出目录: {}", outputDir, e);
            return;
        }

        Map<String, String> requiredNames = collectRequiredNames(spmMap, grpMap);
        if (requiredNames.isEmpty()) {
            log.info("静态资源列表为空，跳过复制");
            return;
        }

        Map<String, List<Path>> index = buildFileIndex(assetRoot);
        int copied = 0;
        int missing = 0;
        int duplicated = 0;

        for (Map.Entry<String, String> entry : requiredNames.entrySet()) {
            String key = entry.getKey();
            String fileName = entry.getValue();
            List<Path> matches = index.get(key);
            if (matches == null || matches.isEmpty()) {
                missing++;
                log.warn("静态资源未找到: {}", fileName);
                continue;
            }
            if (matches.size() > 1) {
                duplicated++;
                log.warn("静态资源存在多个同名文件: {} -> {}", fileName, matches.size());
            }
            Path target = outputDir.resolve(fileName);
            try {
                Files.copy(matches.get(0), target, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                log.warn("复制静态资源失败: {}", fileName, e);
            }
        }

        log.info("静态资源复制完成: 总计={}, 复制={}, 缺失={}, 重名={}",
                requiredNames.size(), copied, missing, duplicated);
    }

    private Map<String, String> collectRequiredNames(Map<String, Spm> spmMap, Map<String, Grp> grpMap) {
        Map<String, String> required = new LinkedHashMap<>();
        if (spmMap != null) {
            for (Spm spm : spmMap.values()) {
                collectFromSpm(required, spm);
            }
        }
        if (grpMap != null) {
            for (Grp grp : grpMap.values()) {
                collectFromGrp(required, grp);
            }
        }
        return required;
    }

    private void collectFromSpm(Map<String, String> required, Spm spm) {
        if (spm == null || spm.getImageData() == null) {
            return;
        }
        for (Spm.SPMImageData image : spm.getImageData()) {
            if (image == null) {
                continue;
            }
            String name = normalizeFileName(image.getImageName());
            putRequired(required, name);
        }
    }

    private void collectFromGrp(Map<String, String> required, Grp grp) {
        if (grp instanceof BatVoiceGrp) {
            collectFromBatVoice(required, (BatVoiceGrp) grp);
        } else if (grp instanceof SeGroupGrp) {
            collectFromSeGroup(required, (SeGroupGrp) grp);
        }
    }

    private void collectFromBatVoice(Map<String, String> required, BatVoiceGrp grp) {
        if (grp.getVoiceList() == null) {
            return;
        }
        for (BatVoiceGrp.BatVoiceGroup group : grp.getVoiceList()) {
            if (group == null || group.getVoices() == null) {
                continue;
            }
            for (BatVoiceGrp.BatVoice voice : group.getVoices()) {
                if (voice == null) {
                    continue;
                }
                String name = normalizeFileName(voice.getVoiceFileName());
                putRequired(required, name);
            }
        }
    }

    private void collectFromSeGroup(Map<String, String> required, SeGroupGrp grp) {
        if (grp.getSeList() == null) {
            return;
        }
        for (SeGroupGrp.SeGroupGroup group : grp.getSeList()) {
            if (group == null || group.getSeItems() == null) {
                continue;
            }
            for (SeGroupGrp.SeGroupItem item : group.getSeItems()) {
                if (item == null) {
                    continue;
                }
                String name = normalizeFileName(item.getSeFileName());
                putRequired(required, name);
            }
        }
    }

    private void putRequired(Map<String, String> required, String name) {
        if (name == null || name.isEmpty()) {
            return;
        }
        String key = name.toLowerCase(Locale.ROOT);
        required.putIfAbsent(key, name);
    }

    private String normalizeFileName(String name) {
        if (name == null) {
            return "";
        }
        String trimmed = name.trim().replace("\\", "/");
        int slash = trimmed.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < trimmed.length()) {
            return trimmed.substring(slash + 1);
        }
        return trimmed;
    }

    private Map<String, List<Path>> buildFileIndex(Path root) {
        Map<String, List<Path>> index = new HashMap<>();
        try {
            Files.walk(root)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        String fileName = path.getFileName().toString();
                        String key = fileName.toLowerCase(Locale.ROOT);
                        index.computeIfAbsent(key, k -> new ArrayList<>()).add(path);
                    });
        } catch (IOException e) {
            log.warn("扫描静态资源目录失败: {}", root, e);
        }
        return index;
    }
}
