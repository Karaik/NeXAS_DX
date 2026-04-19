package com.giga.nexas.transfer.bhe2bsdx.meka.nagi.convert.source;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bhe.mek.Mek;
import com.giga.nexas.dto.bhe.spm.Spm;
import com.giga.nexas.dto.bhe.waz.Waz;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;


public class LoadRawBheSourceStep {

    private static final String CHARSET = "windows-31j";

    private final BheBinService bheBinService;
    private final BsdxBinService bsdxBinService;

    public LoadRawBheSourceStep() {
        this(new BheBinService(), new BsdxBinService());
    }

    public LoadRawBheSourceStep(BheBinService bheBinService, BsdxBinService bsdxBinService) {
        this.bheBinService = bheBinService == null ? new BheBinService() : bheBinService;
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public TsukuyomiRawSourceBundle load(TsukuyomiGraftRequest request) {
        TsukuyomiRawSourceBundle bundle = new TsukuyomiRawSourceBundle();
        if (request == null) {
            return bundle;
        }

        try {
            Path grpDir = request.resolveBheGrpDir();
            Path datDir = request.resolveBheDatDir();
            Path mekDir = request.resolveBheMekDir();
            Path privateWazDir = request.resolveBheWazDir();
            Path privateSpmDir = request.resolveBheSpmDir();

            ensureDirectory(grpDir, "BHE grp 目录");
            ensureDirectory(datDir, "BHE dat 目录");
            ensureDirectory(mekDir, "BHE mek 目录");
            ensureDirectory(privateWazDir, "BHE Tsukuyomi waz 目录");
            ensureDirectory(privateSpmDir, "BHE Tsukuyomi spm 目录");
            bundle.setBatVoiceGrp(parseRequiredBhe(grpDir.resolve("batvoice.grp"), BatVoiceGrp.class));
            bundle.setMekaGroupGrp(parseRequiredBhe(grpDir.resolve("mekagroup.grp"), MekaGroupGrp.class));
            bundle.setSeGroupGrp(parseRequiredBhe(grpDir.resolve("segroup.grp"), SeGroupGrp.class));
            bundle.setSpriteGroupGrp(parseRequiredBhe(grpDir.resolve("spritegroup.grp"), SpriteGroupGrp.class));
            bundle.setWazaGroupGrp(parseRequiredBhe(grpDir.resolve("wazagroup.grp"), WazaGroupGrp.class));
            bundle.setMekaDat(parseRequiredBsdx(datDir.resolve("meka.dat"), Dat.class));
            bundle.setWeaponEquipDat(parseOptionalBsdx(datDir.resolve("weaponequip.dat"), Dat.class));
            bundle.setMekaPilotDat(parseOptionalBsdx(datDir.resolve("mekapilot.dat"), Dat.class));

            bundle.setTsukuyomiMek(parseRequiredBhe(mekDir.resolve(request.getMekFileName()), Mek.class));
            bundle.setSpmByFileName(parseAllBhe(privateSpmDir, "*.spm", Spm.class));
            bundle.setWazByFileName(parseAllBhe(privateWazDir, "*.waz", Waz.class));
            loadCommonProjectileResources(privateWazDir, privateSpmDir, bundle);
            return bundle;
        } catch (IOException e) {
            throw new IllegalStateException("读取 BHE 原始资源失败", e);
        }
    }

    private void loadCommonProjectileResources(
            Path privateWazDir,
            Path privateSpmDir,
            TsukuyomiRawSourceBundle bundle
    ) throws IOException {
        loadCommonProjectileWaz(privateWazDir, bundle.getCommonProjectileWazByFileName());
        loadCommonProjectileSpm(privateSpmDir, bundle.getCommonProjectileSpmByFileName());
    }

    private void loadCommonProjectileWaz(Path privateWazDir, Map<String, Waz> wazByFileName) throws IOException {
        Path commonWazDir = privateWazDir == null ? null : privateWazDir.getParent();
        if (commonWazDir == null || !Files.isDirectory(commonWazDir)) {
            throw new IllegalStateException("BHE 公共 WAZ 目录不存在: " + commonWazDir);
        }
        for (String fileName : BheCommonProjectileResources.commonProjectileWazFileArray()) {
            Path path = commonWazDir.resolve(fileName);
            Waz waz = parseRequiredBhe(path, Waz.class);
            wazByFileName.put(path.getFileName().toString(), waz);
        }
    }

    private void loadCommonProjectileSpm(Path privateSpmDir, Map<String, Spm> spmByFileName) throws IOException {
        Path commonSpmDir = privateSpmDir == null ? null : privateSpmDir.getParent();
        if (commonSpmDir == null || !Files.isDirectory(commonSpmDir)) {
            throw new IllegalStateException("BHE 公共 SPM 目录不存在: " + commonSpmDir);
        }
        for (String fileName : BheCommonProjectileResources.commonProjectileSpmFileArray()) {
            Path path = commonSpmDir.resolve(fileName);
            Spm spm = parseRequiredBhe(path, Spm.class);
            spmByFileName.put(path.getFileName().toString(), spm);
        }
    }

    private void ensureDirectory(Path path, String label) {
        if (path == null || !Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalStateException(label + "不存在: " + path);
        }
    }

    private <T> T parseRequiredBhe(Path path, Class<T> type) throws IOException {
        if (!Files.exists(path)) {
            throw new IllegalStateException("BHE 必需资源文件不存在: " + path);
        }
        ResponseDTO<?> dto = bheBinService.parse(path.toString(), CHARSET);
        return type.cast(dto.getData());
    }

    private <T> T parseRequiredBsdx(Path path, Class<T> type) throws IOException {
        if (!Files.exists(path)) {
            throw new IllegalStateException("BSDX 解析器必需资源文件不存在: " + path);
        }
        ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
        return type.cast(dto.getData());
    }

    private <T> T parseOptionalBsdx(Path path, Class<T> type) throws IOException {
        if (path == null || !Files.exists(path)) {
            return null;
        }
        ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
        return type.cast(dto.getData());
    }

    private <T> Map<String, T> parseAllBhe(Path dir, String glob, Class<T> type) throws IOException {
        Map<String, T> result = new LinkedHashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path path : stream) {
                ResponseDTO<?> dto = bheBinService.parse(path.toString(), CHARSET);
                result.put(path.getFileName().toString(), type.cast(dto.getData()));
            }
        }
        return result.entrySet().stream()
                .sorted((left, right) -> left.getKey().toLowerCase(Locale.ROOT)
                        .compareTo(right.getKey().toLowerCase(Locale.ROOT)))
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }
}
