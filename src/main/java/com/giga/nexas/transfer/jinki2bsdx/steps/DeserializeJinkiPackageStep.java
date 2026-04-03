package com.giga.nexas.transfer.jinki2bsdx.steps;

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

/**
 * 负责把包内 JINKI 二进制资源反序列化成 DTO。
 */
public class DeserializeJinkiPackageStep {

    private static final String CHARSET = "windows-31j";

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    public JinkiPackageBundle deserializePackage(AkaoGraftRequest request) {
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

            // JINKI 当前包内可能没有 MekaPilot.dat。
            // 这层对当前 graft 主链不是硬依赖，所以按可选输入处理。
            bundle.setMekaPilotDat(parseOptional(request.getJinkiDatDir().resolve("MekaPilot.dat"), Dat.class));

            bundle.setAkaoMek(parseRequired(request.getJinkiMekDir().resolve(request.getMekFileName()), Mek.class));
            bundle.setSpmByFileName(parseAll(request.getJinkiSpmDir(), "*.spm", Spm.class));
            bundle.setWazByFileName(parseAll(request.getJinkiWazDir(), "*.waz", Waz.class));
            return bundle;
        } catch (IOException e) {
            throw new IllegalStateException("反序列化 JINKI 资源失败", e);
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
