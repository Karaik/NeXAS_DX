package com.giga.nexas.transfer.bhe2bsdx.meka.motoki.graft;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;


public class LoadTsukuyomiSourceAssetsStep {

    private static final String CHARSET = "windows-31j";

    private final BsdxBinService bsdxBinService;

    public LoadTsukuyomiSourceAssetsStep() {
        this(new BsdxBinService());
    }

    public LoadTsukuyomiSourceAssetsStep(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public TsukuyomiPackageBundle load(TsukuyomiGraftRequest request) {
        TsukuyomiPackageBundle bundle = new TsukuyomiPackageBundle();
        if (request == null) {
            return bundle;
        }

        try {
            Path bheGrpDir = request.resolveBheGrpDir();
            Path bheDatDir = request.resolveBheDatDir();
            Path bheMekDir = request.resolveBheMekDir();
            Path bheWazDir = request.resolveBheWazDir();
            Path bheSpmDir = request.resolveBheSpmDir();

            ensureDirectory(bheGrpDir);
            ensureDirectory(bheDatDir);
            ensureDirectory(bheMekDir);
            ensureDirectory(bheWazDir);
            ensureDirectory(bheSpmDir);

            bundle.setBatVoiceGrp(parseRequired(bheGrpDir.resolve("batvoice.grp"), BatVoiceGrp.class));
            bundle.setMekaGroupGrp(parseRequired(bheGrpDir.resolve("mekagroup.grp"), MekaGroupGrp.class));
            bundle.setSeGroupGrp(parseRequired(bheGrpDir.resolve("segroup.grp"), SeGroupGrp.class));
            bundle.setSpriteGroupGrp(parseRequired(bheGrpDir.resolve("spritegroup.grp"), SpriteGroupGrp.class));
            bundle.setWazaGroupGrp(parseRequired(bheGrpDir.resolve("wazagroup.grp"), WazaGroupGrp.class));

            bundle.setMekaDat(parseRequired(bheDatDir.resolve("meka.dat"), Dat.class));
            bundle.setWeaponEquipDat(parseOptional(bheDatDir.resolve("weaponequip.dat"), Dat.class));
            bundle.setMekaPilotDat(parseOptional(bheDatDir.resolve("mekapilot.dat"), Dat.class));

            bundle.setTsukuyomiMek(parseRequired(bheMekDir.resolve(request.getMekFileName()), Mek.class));
            bundle.setSpmByFileName(parseSelectedSpmFiles(bheSpmDir));
            bundle.setWazByFileName(parseSelectedWazFiles(bheWazDir));
            return bundle;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load Tsukuyomi BHE source assets", e);
        }
    }

    private void ensureDirectory(Path path) {
        if (path == null || !Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalStateException("Source directory does not exist: " + path);
        }
    }

    private <T> T parseRequired(Path path, Class<T> type) throws IOException {
        if (!Files.exists(path)) {
            throw new IllegalStateException("Required source file is missing: " + path);
        }
        ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
        return type.cast(dto.getData());
    }

    private <T> T parseOptional(Path path, Class<T> type) throws IOException {
        if (!Files.exists(path)) {
            return null;
        }
        ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
        return type.cast(dto.getData());
    }

    private <T> Map<String, T> parseAll(Path dir, String glob, Class<T> type) throws IOException {
        Map<String, T> result = new LinkedHashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob)) {
            for (Path path : stream) {
                ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
                result.put(path.getFileName().toString(), type.cast(dto.getData()));
            }
        }
        return result;
    }

    private Map<String, Spm> parseSelectedSpmFiles(Path dir) throws IOException {
        Map<String, Spm> result = new LinkedHashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.spm")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (BheCommonProjectileResources.isCommonProjectileSpm(fileName)) {
                    continue;
                }
                ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
                result.put(fileName, Spm.class.cast(dto.getData()));
            }
        }
        return result;
    }

    private Map<String, Waz> parseSelectedWazFiles(Path dir) throws IOException {
        Map<String, Waz> result = new LinkedHashMap<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.waz")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (BheCommonProjectileResources.isCommonProjectileWaz(fileName)) {
                    continue;
                }
                ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
                result.put(fileName, Waz.class.cast(dto.getData()));
            }
        }
        return result;
    }
}
