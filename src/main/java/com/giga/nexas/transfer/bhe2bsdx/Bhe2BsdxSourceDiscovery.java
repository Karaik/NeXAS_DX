package com.giga.nexas.transfer.bhe2bsdx;

import com.giga.nexas.transfer.bhe2bsdx.model.MekaSource;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BHE→BSDX 源机体发现与查找。
 * <p>
 * 负责从 mekBheJson 目录自动发现可用源机体，
 * 并提供按 codeName 在各类 GRP 中查找条目的工具方法。
 */
@Slf4j
public class Bhe2BsdxSourceDiscovery {

    private final Bhe2BsdxConfig config;
    private final Bhe2BsdxResourceLoader resourceLoader;

    public Bhe2BsdxSourceDiscovery(Bhe2BsdxConfig config, Bhe2BsdxResourceLoader resourceLoader) {
        this.config = config;
        this.resourceLoader = resourceLoader;
    }

    // ===== 源机体发现 =====

    /**
     * 发现全部可用源机体（自动扫描 + 运行时过滤）。
     * <p>
     * 先从 mekBheJson 目录扫描 *.mek.json 文件，
     * 再根据系统属性 transfer.sources / transfer.limit 进行过滤和截断。
     *
     * @return 排序后的源机体列表
     */
    public List<MekaSource> discoverSources() throws IOException {
        List<MekaSource> sources = discoverSourcesFromMekJson();
        sources = applyRuntimeFilters(sources);
        return sources;
    }

    /**
     * 从 mekBheJson 目录扫描 *.mek.json 文件，自动发现源机体。
     * <p>
     * 通过 BHE MekaGroup GRP 建立 baseKey→codeName 映射，
     * 只有在 GRP 中存在且 existFlag!=0 的机体才会被收录。
     *
     * @return 按 baseKey 排序的源机体列表
     */
    private List<MekaSource> discoverSourcesFromMekJson() throws IOException {
        List<MekaSource> sources = new ArrayList<>();
        Path bheMekJsonDir = config.getBheMekJsonDir();

        if (!Files.isDirectory(bheMekJsonDir)) {
            log.warn("mekBheJson 目录不存在: {}", bheMekJsonDir.toAbsolutePath());
            return sources;
        }

        // 从 BHE MekaGroup GRP 构建 codeName 映射
        Map<String, String> codeNameMap = loadBheCodeNameMap();
        final String suffix = ".mek.json";

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(bheMekJsonDir, "*" + suffix)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (!fileName.endsWith(suffix)) {
                    continue;
                }
                String baseKey = normalizeKey(fileName.substring(0, fileName.length() - suffix.length()));
                if (baseKey.isEmpty()) {
                    continue;
                }

                String codeName = codeNameMap.get(baseKey);
                if (codeName == null || codeName.isBlank()) {
                    log.warn("跳过未匹配 codeName 的机体: baseKey={}", baseKey);
                    continue;
                }
                sources.add(new MekaSource(baseKey, codeName));
            }
        }

        sources.sort(Comparator.comparing(MekaSource::getBaseKey));
        log.info("自动发现源机体: {} 个 (来自 {})", sources.size(), bheMekJsonDir);
        return sources;
    }

    /**
     * 根据系统属性对源机体列表进行运行时过滤。
     * <p>
     * 支持两个系统属性：
     * <ul>
     *   <li>transfer.sources — 逗号分隔的 baseKey 白名单，只保留命中的机体</li>
     *   <li>transfer.limit — 最大数量限制，超出部分截断</li>
     * </ul>
     *
     * @param sources 原始源机体列表
     * @return 过滤后的源机体列表
     */
    private List<MekaSource> applyRuntimeFilters(List<MekaSource> sources) {
        if (sources == null || sources.isEmpty()) {
            return sources;
        }

        List<MekaSource> filtered = sources;

        // 按 baseKey 白名单过滤
        String filterProp = System.getProperty("transfer.sources");
        if (filterProp != null && !filterProp.isBlank()) {
            Set<String> allowList = new LinkedHashSet<>();
            for (String token : filterProp.split(",")) {
                String key = normalizeKey(token);
                if (!key.isEmpty()) {
                    allowList.add(key);
                }
            }
            if (!allowList.isEmpty()) {
                filtered = new ArrayList<>();
                for (MekaSource source : sources) {
                    if (source != null && allowList.contains(source.getBaseKey())) {
                        filtered.add(source);
                    }
                }
                log.info("运行时过滤生效 transfer.sources={}, 命中 {} 个",
                        allowList, filtered.size());
            }
        }

        // 按数量限制截断
        String limitProp = System.getProperty("transfer.limit");
        if (limitProp != null && !limitProp.isBlank()) {
            try {
                int limit = Integer.parseInt(limitProp.trim());
                if (limit > 0 && filtered.size() > limit) {
                    filtered = new ArrayList<>(filtered.subList(0, limit));
                    log.info("运行时限制生效 transfer.limit={}, 截断后 {} 个", limit, filtered.size());
                }
            } catch (NumberFormatException e) {
                log.warn("transfer.limit 不是有效整数: {}", limitProp);
            }
        }

        return filtered;
    }

    /**
     * 从 BHE MekaGroup GRP 加载 baseKey→codeName 映射。
     * <p>
     * 遍历 BHE mekagroup 中所有 existFlag!=0 的条目，
     * 以 mekaName（小写）为 key、mekaCodeName（大写）为 value 构建映射。
     *
     * @return baseKey→codeName 映射表
     */
    private Map<String, String> loadBheCodeNameMap() throws IOException {
        Map<String, String> codeNameMap = new HashMap<>();

        Map<String, com.giga.nexas.dto.bhe.grp.Grp> bheGrp = resourceLoader.registerBheGrp();
        com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp bheMekaGroup =
                (com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp) bheGrp.get("mekagroup");
        if (bheMekaGroup == null || bheMekaGroup.getMekaList() == null) {
            log.warn("无法载入 BHE MekaGroup，自动发现将返回空列表。");
            return codeNameMap;
        }

        for (com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup group : bheMekaGroup.getMekaList()) {
            if (group == null || group.getExistFlag() == null || group.getExistFlag() == 0) {
                continue;
            }
            String mekaName = group.getMekaName();
            String mekaCodeName = group.getMekaCodeName();
            if (mekaName == null || mekaCodeName == null) {
                continue;
            }
            String baseKey = normalizeKey(mekaName);
            String codeName = normalizeCode(mekaCodeName);
            if (!baseKey.isEmpty() && !codeName.isEmpty()) {
                codeNameMap.put(baseKey, codeName);
            }
        }

        log.info("载入 BHE 机体 codeName 映射: {} 个", codeNameMap.size());
        return codeNameMap;
    }

    // ===== GRP 查找方法 =====

    /**
     * 在 BHE BatVoiceGrp 中按 codeName 查找 BatVoiceGroup。
     * <p>
     * 遍历 voiceList，跳过 existFlag==0 的条目，
     * 以 characterCodeName 与目标 codeName 做忽略大小写比较。
     *
     * @param grp      BHE BatVoiceGrp 实例
     * @param codeName 目标机体 codeName（大写）
     * @return 匹配的 BatVoiceGroup，未找到时返回 null
     */
    public com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup findBatVoiceGroupByCode(
            com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp grp,
            String codeName
    ) {
        if (grp == null || grp.getVoiceList() == null || codeName == null) {
            return null;
        }
        for (com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup group : grp.getVoiceList()) {
            if (group == null || group.getExistFlag() == null || group.getExistFlag() == 0) {
                continue;
            }
            String code = group.getCharacterCodeName();
            if (code != null && codeName.equalsIgnoreCase(code.trim())) {
                return group;
            }
        }
        log.warn("未找到 BatVoiceGroup: codeName={}", codeName);
        return null;
    }

    /**
     * 在 BHE MekaGroupGrp 中按 codeName 查找 MekaGroup。
     * <p>
     * 遍历 mekaList，跳过 existFlag==0 的条目，
     * 以 mekaCodeName 与目标 codeName 做忽略大小写比较。
     *
     * @param grp      BHE MekaGroupGrp 实例
     * @param codeName 目标机体 codeName（大写）
     * @return 匹配的 MekaGroup，未找到时返回 null
     */
    public com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup findMekaGroupByCode(
            com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp grp,
            String codeName
    ) {
        if (grp == null || grp.getMekaList() == null || codeName == null) {
            return null;
        }
        for (com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup group : grp.getMekaList()) {
            if (group == null || group.getExistFlag() == null || group.getExistFlag() == 0) {
                continue;
            }
            if (codeName.equalsIgnoreCase(group.getMekaCodeName())) {
                return group;
            }
        }
        log.warn("未找到 MekaGroup: codeName={}", codeName);
        return null;
    }

    /**
     * 在 BHE WazaGroupGrp 中按 codeName 查找 WazaGroupEntry。
     * <p>
     * 遍历 wazaList，跳过 existFlag==0 的条目，
     * 以 wazaCodeName 与目标 codeName 做忽略大小写比较。
     *
     * @param grp      BHE WazaGroupGrp 实例
     * @param codeName 目标机体 codeName（大写）
     * @return 匹配的 WazaGroupEntry，未找到时返回 null
     */
    public com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry findWazaGroupByCode(
            com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp grp,
            String codeName
    ) {
        if (grp == null || grp.getWazaList() == null || codeName == null) {
            return null;
        }
        for (com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry entry : grp.getWazaList()) {
            if (entry == null || entry.getExistFlag() == null || entry.getExistFlag() == 0) {
                continue;
            }
            if (codeName.equalsIgnoreCase(entry.getWazaCodeName())) {
                return entry;
            }
        }
        log.warn("未找到 WazaGroup: codeName={}", codeName);
        return null;
    }

    /**
     * 在 BHE SpriteGroupGrp 中按 codeName 查找 SpriteGroupEntry。
     * <p>
     * 遍历 spriteList，跳过 existFlag==0 的条目，
     * 以 spriteCodeName 与目标 codeName 做忽略大小写比较。
     *
     * @param grp      BHE SpriteGroupGrp 实例
     * @param codeName 目标机体 codeName（大写）
     * @return 匹配的 SpriteGroupEntry，未找到时返回 null
     */
    public com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry findSpriteGroupByCode(
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp grp,
            String codeName
    ) {
        if (grp == null || grp.getSpriteList() == null || codeName == null) {
            return null;
        }
        for (com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry entry : grp.getSpriteList()) {
            if (entry == null || entry.getExistFlag() == null || entry.getExistFlag() == 0) {
                continue;
            }
            if (codeName.equalsIgnoreCase(entry.getSpriteCodeName())) {
                return entry;
            }
        }
        log.warn("未找到 SpriteGroup: codeName={}", codeName);
        return null;
    }

    // ===== 辅助方法 =====

    /**
     * 标准化 key：去除首尾空白并转为小写。
     *
     * @param key 原始 key
     * @return 标准化后的 key，null 时返回空字符串
     */
    public String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    /**
     * 标准化 codeName：去除首尾空白并转为大写。
     *
     * @param codeName 原始 codeName
     * @return 标准化后的 codeName，null 时返回空字符串
     */
    public String normalizeCode(String codeName) {
        return codeName == null ? "" : codeName.trim().toUpperCase();
    }

    /**
     * 生成变体 key：在 baseKey 前加上前缀（如 "c_"、"s_"、"g_"、"m_"）。
     * <p>
     * 用于定位同一机体的不同 spm 变体文件。
     *
     * @param prefix  前缀（如 "c_"）
     * @param baseKey 基础 key
     * @return 拼接后的变体 key；prefix 或 baseKey 为空时直接返回 baseKey
     */
    public String variantKey(String prefix, String baseKey) {
        if (prefix == null || baseKey == null || baseKey.isEmpty()) {
            return baseKey;
        }
        return prefix + baseKey;
    }
}
