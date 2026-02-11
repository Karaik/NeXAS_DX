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
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static asset copy:
 * - Extract required names from spm/grp.
 * - Find matching files under asset root and copy to output.
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
            log.warn("static asset root not found: {}", assetRoot);
            return;
        }
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            log.warn("failed to create output dir: {}", outputDir, e);
            return;
        }

        Map<String, String> requiredNames = collectRequiredNames(spmMap, grpMap);
        if (requiredNames.isEmpty()) {
            log.info("static asset list is empty, skip copy");
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
                log.warn("static asset not found: {}", fileName);
                continue;
            }
            if (matches.size() > 1) {
                duplicated++;
                log.warn("multiple static assets matched: {} -> {}{}", fileName, matches.size(),
                        baseMatched ? " (base)" : "");
            }
            Path source = chooseBestMatch(fileName, matches);
            if (source == null) {
                missing++;
                log.warn("static asset not found(no preferred match): {}", fileName);
                continue;
            }
            String targetName = resolveTargetName(fileName, source);
            if (targetName == null || targetName.isEmpty()) {
                missing++;
                log.warn("static asset target name is empty: {}", fileName);
                continue;
            }
            Path target = outputDir.resolve(targetName);
            try {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                log.warn("copy static asset failed: {}", fileName, e);
            }
        }

        log.info("static asset copy done: total={}, copied={}, missing={}, duplicated={}",
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
        if (spm == null || spm.getImageData() == null || spm.getImageData().isEmpty()) {
            return;
        }
        List<Spm.SPMImageData> imageDataList = spm.getImageData();
        LinkedHashSet<Integer> referencedImageNo = collectReferencedImageNo(spm);
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

    private LinkedHashSet<Integer> collectReferencedImageNo(Spm spm) {
        LinkedHashSet<Integer> imageNoSet = new LinkedHashSet<>();
        if (spm == null || spm.getPageData() == null || spm.getPageData().isEmpty()) {
            return imageNoSet;
        }

        List<Spm.SPMPageData> pageData = spm.getPageData();
        LinkedHashSet<Integer> pageNoSet = new LinkedHashSet<>();
        collectReferencedPageNo(spm, pageNoSet);

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

    private void collectReferencedPageNo(Spm spm, LinkedHashSet<Integer> pageNoSet) {
        if (spm == null || pageNoSet == null || spm.getAnimData() == null) {
            return;
        }
        int patPageNum = spm.getPatPageNum() == null ? 0 : Math.max(spm.getPatPageNum(), 0);
        for (Spm.SPMAnimData animData : spm.getAnimData()) {
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
            log.warn("scan static asset root failed: {}", root, e);
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
