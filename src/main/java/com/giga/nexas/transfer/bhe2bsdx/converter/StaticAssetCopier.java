package com.giga.nexas.transfer.bhe2bsdx.converter;

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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
        copyAssets(outputDir, assetRoot, spmMap, grpMap, null);
    }

    public void copyAssets(Path outputDir, Path assetRoot,
                           Map<String, Spm> spmMap,
                           Map<String, Grp> grpMap,
                           Map<String, Set<Integer>> spmActionGroups) {
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

        Map<String, String> requiredNames = collectRequiredNames(spmMap, grpMap, spmActionGroups);
        if (requiredNames.isEmpty()) {
            log.info("静态资源列表为空，跳过复制");
            return;
        }

        AssetIndex index = buildFileIndex(assetRoot);
        int copied = 0;
        int missing = 0;
        int duplicated = 0;

        for (Map.Entry<String, String> entry : requiredNames.entrySet()) {
            String key = entry.getKey();
            String fileName = entry.getValue();
            boolean baseMatched = false;
            List<Path> matches = index.findByFileName(key);
            if (matches == null || matches.isEmpty()) {
                matches = index.findByBaseName(fileName);
                baseMatched = true;
            }
            if (matches == null || matches.isEmpty()) {
                missing++;
                log.warn("静态资源未找到: {}", fileName);
                continue;
            }
            if (matches.size() > 1) {
                duplicated++;
                log.warn("静态资源存在多个同名文件: {} -> {}{}", fileName, matches.size(),
                        baseMatched ? " (base)" : "");
            }
            Path source = chooseBestMatch(fileName, matches);
            if (source == null) {
                missing++;
                log.warn("静态资源未找到(无法选择匹配项): {}", fileName);
                continue;
            }
            String targetName = resolveTargetName(fileName, source);
            if (targetName == null || targetName.isEmpty()) {
                missing++;
                log.warn("静态资源目标名为空: {}", fileName);
                continue;
            }
            Path target = outputDir.resolve(targetName);
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                log.warn("复制静态资源失败: {}", fileName, e);
            }
        }

        log.info("静态资源复制完成: 总计={}, 复制={}, 缺失={}, 重名={}",
                requiredNames.size(), copied, missing, duplicated);
    }

    private Map<String, String> collectRequiredNames(
            Map<String, Spm> spmMap,
            Map<String, Grp> grpMap,
            Map<String, Set<Integer>> spmActionGroups
    ) {
        Map<String, String> required = new LinkedHashMap<>();
        if (spmMap != null) {
            for (Map.Entry<String, Spm> entry : spmMap.entrySet()) {
                collectFromSpm(required, normalizeSpmKey(entry.getKey()), entry.getValue(), spmActionGroups);
            }
        }
        if (grpMap != null) {
            for (Grp grp : grpMap.values()) {
                collectFromGrp(required, grp);
            }
        }
        return required;
    }

    private void collectFromSpm(
            Map<String, String> required,
            String spmKey,
            Spm spm,
            Map<String, Set<Integer>> spmActionGroups
    ) {
        if (spm == null || spm.getImageData() == null || spm.getImageData().isEmpty()) {
            return;
        }
        List<Spm.SPMImageData> imageDataList = spm.getImageData();
        Set<Integer> actionGroupSet = resolveActionGroups(spmKey, spmActionGroups);
        LinkedHashSet<Integer> referencedImageNo = collectReferencedImageNo(spm, actionGroupSet);
        if (referencedImageNo.isEmpty()) {
            for (Spm.SPMImageData image : imageDataList) {
                if (image == null) {
                    continue;
                }
                String name = normalizeFileName(image.getImageName());
                putRequired(required, name);
            }
            return;
        }

        boolean hasOutOfRange = false;
        for (Integer imageNo : referencedImageNo) {
            if (imageNo == null || imageNo < 0) {
                continue;
            }
            if (imageNo >= imageDataList.size()) {
                hasOutOfRange = true;
                continue;
            }
            Spm.SPMImageData image = imageDataList.get(imageNo);
            if (image == null) {
                continue;
            }
            String name = normalizeFileName(image.getImageName());
            putRequired(required, name);
        }
        if (hasOutOfRange) {
            log.warn("SPM imageNo out of range, fallback to full imageData set");
            for (Spm.SPMImageData image : imageDataList) {
                if (image == null) {
                    continue;
                }
                String name = normalizeFileName(image.getImageName());
                putRequired(required, name);
            }
        }
    }

    private LinkedHashSet<Integer> collectReferencedImageNo(Spm spm, Set<Integer> actionGroupSet) {
        LinkedHashSet<Integer> imageNoSet = new LinkedHashSet<>();
        if (spm == null || spm.getPageData() == null || spm.getPageData().isEmpty()) {
            return imageNoSet;
        }

        List<Spm.SPMPageData> pageData = spm.getPageData();
        LinkedHashSet<Integer> pageNoSet = new LinkedHashSet<>();
        collectReferencedPageNo(spm, pageNoSet, actionGroupSet);

        if (pageNoSet.isEmpty()) {
            for (int i = 0; i < pageData.size(); i++) {
                pageNoSet.add(i);
            }
        }

        for (Integer pageNo : pageNoSet) {
            if (pageNo == null || pageNo < 0 || pageNo >= pageData.size()) {
                continue;
            }
            Spm.SPMPageData page = pageData.get(pageNo);
            collectImageNoFromPage(page, imageNoSet);
        }
        return imageNoSet;
    }

    private void collectReferencedPageNo(Spm spm, LinkedHashSet<Integer> pageNoSet, Set<Integer> actionGroupSet) {
        if (spm == null || pageNoSet == null || spm.getAnimData() == null) {
            return;
        }
        int patPageNum = spm.getPatPageNum() == null ? 0 : Math.max(spm.getPatPageNum(), 0);
        List<Spm.SPMAnimData> animDataList = spm.getAnimData();
        for (int animIndex = 0; animIndex < animDataList.size(); animIndex++) {
            if (actionGroupSet != null && !actionGroupSet.isEmpty() && !actionGroupSet.contains(animIndex)) {
                continue;
            }
            Spm.SPMAnimData animData = animDataList.get(animIndex);
            if (animData == null || animData.getPatData() == null) {
                continue;
            }
            for (Spm.SPMPatData patData : animData.getPatData()) {
                if (patData == null || patData.getPageNo() == null) {
                    continue;
                }
                List<Integer> pageNoList = patData.getPageNo();
                int max = patPageNum > 0 ? Math.min(patPageNum, pageNoList.size()) : pageNoList.size();
                for (int i = 0; i < max; i++) {
                    Integer pageNo = pageNoList.get(i);
                    if (pageNo != null && pageNo >= 0) {
                        pageNoSet.add(pageNo);
                    }
                }
            }
        }
    }

    private void collectImageNoFromPage(Spm.SPMPageData page, LinkedHashSet<Integer> imageNoSet) {
        if (page == null || page.getChipData() == null || imageNoSet == null) {
            return;
        }
        for (Spm.SPMChipData chipData : page.getChipData()) {
            if (chipData == null || chipData.getImageNo() == null) {
                continue;
            }
            Integer imageNo = chipData.getImageNo();
            if (imageNo >= 0) {
                imageNoSet.add(imageNo);
            }
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

    private Set<Integer> resolveActionGroups(String spmKey, Map<String, Set<Integer>> spmActionGroups) {
        if (spmKey == null || spmActionGroups == null || spmActionGroups.isEmpty()) {
            return null;
        }
        return spmActionGroups.get(spmKey);
    }

    private String normalizeSpmKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        return key.trim().toLowerCase(Locale.ROOT);
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

    private AssetIndex buildFileIndex(Path root) {
        AssetIndex index = new AssetIndex();
        try {
            Files.walk(root)
                    .filter(Files::isRegularFile)
                    .forEach(index::add);
        } catch (IOException e) {
            log.warn("扫描静态资源目录失败: {}", root, e);
        }
        return index;
    }

    private Path chooseBestMatch(String targetName, List<Path> matches) {
        if (matches == null || matches.isEmpty()) {
            return null;
        }
        if (matches.size() == 1) {
            return matches.get(0);
        }
        String targetExt = extensionOf(targetName);
        if (!targetExt.isEmpty()) {
            for (Path path : matches) {
                if (targetExt.equalsIgnoreCase(extensionOf(path.getFileName().toString()))) {
                    return path;
                }
            }
        }
        for (Path path : matches) {
            if (extensionOf(path.getFileName().toString()).isEmpty()) {
                return path;
            }
        }
        return matches.get(0);
    }

    private String resolveTargetName(String requestedName, Path source) {
        String normalized = normalizeFileName(requestedName);
        if (!extensionOf(normalized).isEmpty()) {
            return normalized;
        }
        if (source != null) {
            String sourceName = normalizeFileName(source.getFileName().toString());
            if (!sourceName.isEmpty()) {
                return sourceName;
            }
        }
        return normalized;
    }

    private String extensionOf(String name) {
        if (name == null) {
            return "";
        }
        int dot = name.lastIndexOf('.');
        if (dot > 0 && dot + 1 < name.length()) {
            return name.substring(dot + 1);
        }
        return "";
    }

    private static final class AssetIndex {
        private final Map<String, List<Path>> byFileName = new HashMap<>();
        private final Map<String, List<Path>> byBaseName = new HashMap<>();

        void add(Path path) {
            if (path == null) {
                return;
            }
            String fileName = path.getFileName().toString();
            if (fileName.isEmpty()) {
                return;
            }
            String fileKey = fileName.toLowerCase(Locale.ROOT);
            byFileName.computeIfAbsent(fileKey, k -> new ArrayList<>()).add(path);

            int dot = fileName.lastIndexOf('.');
            String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
            if (!baseName.isEmpty()) {
                String baseKey = baseName.toLowerCase(Locale.ROOT);
                byBaseName.computeIfAbsent(baseKey, k -> new ArrayList<>()).add(path);
            }
        }

        List<Path> findByFileName(String nameKey) {
            if (nameKey == null || nameKey.isEmpty()) {
                return null;
            }
            return byFileName.get(nameKey);
        }

        List<Path> findByBaseName(String fileName) {
            if (fileName == null || fileName.isEmpty()) {
                return null;
            }
            int dot = fileName.lastIndexOf('.');
            String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
            String key = baseName.toLowerCase(Locale.ROOT);
            return byBaseName.get(key);
        }
    }
}
