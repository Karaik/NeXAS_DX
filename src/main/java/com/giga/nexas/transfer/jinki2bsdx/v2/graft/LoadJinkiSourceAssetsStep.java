package com.giga.nexas.transfer.jinki2bsdx.v2.graft;

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
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoadJinkiSourceAssetsStep {

    /**
     * JINKI 源资产解析使用的字符集。
     *
     * <p>虽然 JINKI 和 BSDX 来自不同游戏包，但文本资源仍按日文 Windows 编码解析；
     * 这里和旧 pipeline 保持一致，避免 DAT/animName 等字符串在 DTO 层已经变形。</p>
     */
    private static final String CHARSET = "windows-31j";

    /**
     * 源侧二进制解析服务。
     *
     * <p>当前 JINKI 源资源可以复用 BSDX DTO/parser 读取；
     * 未来 BHE 若不能直接复用，应在源游戏转换层新增 converter，而不是改这个加载 step 的业务职责。</p>
     */
    private final BsdxBinService bsdxBinService;

    public LoadJinkiSourceAssetsStep() {
        this(new BsdxBinService());
    }

    public LoadJinkiSourceAssetsStep(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public JinkiPackageBundle load(AkaoGraftRequest request) {
        JinkiPackageBundle bundle = new JinkiPackageBundle();
        try {
            ensureDirectory(request.getJinkiGrpDir());
            ensureDirectory(request.getJinkiDatDir());
            ensureDirectory(request.getJinkiMekDir());
            ensureDirectory(request.getJinkiSpmDir());
            ensureDirectory(request.getJinkiWazDir());

            bundle.setBatVoiceGrp(parseRequired(request.getJinkiGrpDir().resolve("BatVoice.grp"), BatVoiceGrp.class));
            bundle.setMekaGroupGrp(parseRequired(request.getJinkiGrpDir().resolve("MekaGroup.grp"), MekaGroupGrp.class));
            bundle.setSeGroupGrp(parseRequired(request.getJinkiGrpDir().resolve("SeGroup.grp"), SeGroupGrp.class));
            bundle.setSpriteGroupGrp(parseRequired(request.getJinkiGrpDir().resolve("SpriteGroup.grp"), SpriteGroupGrp.class));
            bundle.setWazaGroupGrp(parseRequired(request.getJinkiGrpDir().resolve("WazaGroup.grp"), WazaGroupGrp.class));

            bundle.setMekaDat(parseRequired(request.getJinkiDatDir().resolve("Meka.dat"), Dat.class));
            bundle.setWeaponEquipDat(parseRequiredWithFallback(
                    request.getJinkiDatDir().resolve("WeaponEquip.dat"),
                    request.getExternalStaticAssetRoot() == null ? null : request.getExternalStaticAssetRoot().resolve("WeaponEquip.dat"),
                    Dat.class
            ));
            bundle.setMekaPilotDat(parseOptional(request.getJinkiDatDir().resolve("MekaPilot.dat"), Dat.class));

            bundle.setAkaoMek(parseRequired(request.getJinkiMekDir().resolve(request.getMekFileName()), Mek.class));
            bundle.setSpmByFileName(parseAll(request.getJinkiSpmDir(), "*.spm", Spm.class));
            bundle.setWazByFileName(parseAll(request.getJinkiWazDir(), "*.waz", Waz.class));
            return bundle;
        } catch (IOException e) {
            throw new IllegalStateException("加载 JINKI 源资产失败", e);
        }
    }

    private void ensureDirectory(Path path) {
        if (!Files.exists(path) || !Files.isDirectory(path)) {
            throw new IllegalStateException("资源目录不存在: " + path);
        }
    }

    private <T> T parseRequired(Path path, Class<T> type) throws IOException {
        if (!Files.exists(path)) {
            throw new IllegalStateException("资源文件不存在: " + path);
        }
        ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
        return type.cast(dto.getData());
    }

    private <T> T parseRequiredWithFallback(Path primary, Path fallback, Class<T> type) throws IOException {
        if (primary != null && Files.exists(primary)) {
            return parseRequired(primary, type);
        }
        if (fallback != null && Files.exists(fallback)) {
            return parseRequired(fallback, type);
        }
        throw new IllegalStateException("资源文件不存在: " + primary + " | fallback=" + fallback);
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
            stream.forEach(path -> {
                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
                    result.put(path.getFileName().toString(), type.cast(dto.getData()));
                } catch (IOException e) {
                    throw new IllegalStateException("解析文件失败: " + path, e);
                }
            });
        }
        return result.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .collect(LinkedHashMap::new,
                        (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }
}
