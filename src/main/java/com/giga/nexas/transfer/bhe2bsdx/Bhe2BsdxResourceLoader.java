package com.giga.nexas.transfer.bhe2bsdx;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * BHE→BSDX 资源加载器。
 * <p>
 * 从 {@link Bhe2BsdxConfig} 中配置的目录读取各类二进制资源文件（grp/mek/waz/spm/dat），
 * 解析后以 baseName→DTO 的 Map 形式返回，供后续转换流程使用。
 */
@Slf4j
public class Bhe2BsdxResourceLoader {

    private final Bhe2BsdxConfig config;
    private final BsdxBinService bsdxBinService;
    private final BheBinService bheBinService;

    public Bhe2BsdxResourceLoader(Bhe2BsdxConfig config) {
        this.config = config;
        this.bsdxBinService = new BsdxBinService();
        this.bheBinService = new BheBinService();
    }

    // ===== grp =====

    /**
     * 注册全部 BSDX grp 资源。
     * 遍历 bsdxGrpDir 下所有 *.grp 文件，解析后以小写 baseName 为 key 存入 Map。
     */
    public Map<String, com.giga.nexas.dto.bsdx.grp.Grp> registerBsdxGrp() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> grpMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(config.getBsdxGrpDir(), "*.grp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), config.getCharset());
                    com.giga.nexas.dto.bsdx.grp.Grp grp = (com.giga.nexas.dto.bsdx.grp.Grp) dto.getData();
                    grpMap.put(baseName, grp);
                } catch (Exception e) {
                    log.warn("解析 bsdxGrp 失败: {}", fileName);
                }
            }
        }

        return grpMap;
    }

    /**
     * 注册全部 BHE grp 资源。
     * 遍历 bheGrpDir 下所有 *.grp 文件，解析后以小写 baseName 为 key 存入 Map。
     */
    public Map<String, com.giga.nexas.dto.bhe.grp.Grp> registerBheGrp() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.grp.Grp> grpMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(config.getBheGrpDir(), "*.grp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), config.getCharset());
                    com.giga.nexas.dto.bhe.grp.Grp grp = (com.giga.nexas.dto.bhe.grp.Grp) dto.getData();
                    grpMap.put(baseName, grp);
                } catch (Exception e) {
                    log.warn("解析 bheGrp 失败: {}", fileName);
                }
            }
        }

        return grpMap;
    }

    // ===== mek =====

    /**
     * 注册全部 BSDX mek 资源。
     * 遍历 bsdxMekDir 下所有 *.mek 文件，解析后以小写 baseName 为 key 存入 Map。
     */
    public Map<String, com.giga.nexas.dto.bsdx.mek.Mek> registerBsdxMek() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> mekMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(config.getBsdxMekDir(), "*.mek")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), config.getCharset());
                    com.giga.nexas.dto.bsdx.mek.Mek mek = (com.giga.nexas.dto.bsdx.mek.Mek) dto.getData();
                    mekMap.put(baseName, mek);
                } catch (Exception e) {
                    log.warn("解析 bsdxMek 失败: {}", fileName);
                }
            }
        }

        return mekMap;
    }

    /**
     * 注册全部 BHE mek 资源。
     * 遍历 bheMekDir 下所有 *.mek 文件，解析后以小写 baseName 为 key 存入 Map。
     */
    public Map<String, com.giga.nexas.dto.bhe.mek.Mek> registerBheMek() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.mek.Mek> mekMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(config.getBheMekDir(), "*.mek")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), config.getCharset());
                    com.giga.nexas.dto.bhe.mek.Mek mek = (com.giga.nexas.dto.bhe.mek.Mek) dto.getData();
                    mekMap.put(baseName, mek);
                } catch (Exception e) {
                    log.warn("解析 bheMek 失败: {}", fileName);
                }
            }
        }

        return mekMap;
    }

    // ===== waz =====

    /**
     * 注册全部 BSDX waz 资源。
     * 遍历 bsdxWazDir 下所有 *.waz 文件，解析后以小写 baseName 为 key 存入 Map。
     */
    public Map<String, com.giga.nexas.dto.bsdx.waz.Waz> registerBsdxWaz() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> wazMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(config.getBsdxWazDir(), "*.waz")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), config.getCharset());
                    com.giga.nexas.dto.bsdx.waz.Waz waz = (com.giga.nexas.dto.bsdx.waz.Waz) dto.getData();
                    wazMap.put(baseName, waz);
                } catch (Exception e) {
                    log.warn("解析 bsdxWaz 失败: {}", fileName);
                }
            }
        }

        return wazMap;
    }

    /**
     * 注册全部 BHE waz 资源。
     * 遍历 bheWazDir 下所有 *.waz 文件，解析后以小写 baseName 为 key 存入 Map。
     */
    public Map<String, com.giga.nexas.dto.bhe.waz.Waz> registerBheWaz() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.waz.Waz> wazMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(config.getBheWazDir(), "*.waz")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), config.getCharset());
                    com.giga.nexas.dto.bhe.waz.Waz waz = (com.giga.nexas.dto.bhe.waz.Waz) dto.getData();
                    wazMap.put(baseName, waz);
                } catch (Exception e) {
                    log.warn("解析 bheWaz 失败: {}", fileName);
                }
            }
        }

        return wazMap;
    }

    // ===== spm =====
    // spm 在多个版本中无差异，但已确定 2.0.0 在 BHE 中多了关于 hitbox 的信息

    /**
     * 注册全部 BSDX spm 资源。
     * 遍历 bsdxSpmDir 下所有 *.spm 文件，解析后以小写 baseName 为 key 存入 Map。
     */
    public Map<String, com.giga.nexas.dto.bsdx.spm.Spm> registerBsdxSpm() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> spmMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(config.getBsdxSpmDir(), "*.spm")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), config.getCharset());
                    com.giga.nexas.dto.bsdx.spm.Spm spm = (com.giga.nexas.dto.bsdx.spm.Spm) dto.getData();
                    spmMap.put(baseName, spm);
                } catch (Exception e) {
                    log.warn("解析 bsdxSpm 失败: {}", fileName);
                }
            }
        }

        return spmMap;
    }

    /**
     * 注册全部 BHE spm 资源。
     * 遍历 bheSpmDir 下所有 *.spm 文件，解析后以小写 baseName 为 key 存入 Map。
     */
    public Map<String, com.giga.nexas.dto.bhe.spm.Spm> registerBheSpm() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.spm.Spm> spmMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(config.getBheSpmDir(), "*.spm")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), config.getCharset());
                    com.giga.nexas.dto.bhe.spm.Spm spm = (com.giga.nexas.dto.bhe.spm.Spm) dto.getData();
                    spmMap.put(baseName, spm);
                } catch (Exception e) {
                    log.warn("解析 bheSpm 失败: {}", fileName);
                }
            }
        }

        return spmMap;
    }

    // ===== dat =====
    // dat 无差别，全为 csv

    /**
     * 加载指定的 BSDX dat 文件。
     *
     * @param fileName dat 文件名（如 "SelectMekaMenu.dat"）
     * @return 解析后的 Dat 对象，解析失败时返回 null
     */
    public Dat loadBsdxDat(String fileName) throws IOException {
        Path path = config.getBsdxDatDir().resolve(fileName);
        try {
            ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), config.getCharset());
            return (Dat) dto.getData();
        } catch (Exception e) {
            log.warn("解析 bsdxDat 失败: {}", fileName);
            return null;
        }
    }
}
