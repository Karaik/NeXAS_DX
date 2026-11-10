package com.giga.nexas.transfer.jinki2bsdx.v2.graft;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoadGraftBaselineStep {

    /**
     * BSDX baseline 资源解析使用的字符集。
     *
     * <p>所有 DAT/SPM/WAZ/MEK 中的文本都要和旧 pipeline 解析方式一致；
     * 字符集不同会导致后续即使逻辑相同，生成 bytes 也不同。</p>
     */
    private static final String CHARSET = "windows-31j";

    /**
     * baseline 二进制解析服务。
     *
     * <p>它读取当前层的 BSDX baseline。链式成果物模型下，这个 baseline
     * 未来可以来自上一层输出，而不一定是原始游戏目录。</p>
     */
    private final BsdxBinService bsdxBinService;

    public LoadGraftBaselineStep() {
        this(new BsdxBinService());
    }

    public LoadGraftBaselineStep(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public BsdxBaselineBundle load(AkaoGraftRequest request) {
        BsdxBaselineBundle bundle = new BsdxBaselineBundle();

        try {
            ensureDirectory(request.getBsdxGrpDir());
            ensureDirectory(request.getBsdxDatDir());
            ensureDirectory(request.getBsdxMekDir());
            ensureDirectory(request.getBsdxSpmDir());
            ensureDirectory(request.getBsdxWazDir());

            bundle.setBatVoiceGrp(parseRequired(request.getBsdxGrpDir().resolve("BatVoice.grp"), BatVoiceGrp.class));
            bundle.setMapGroupGrp(parseRequired(request.getBsdxGrpDir().resolve("MapGroup.grp"), MapGroupGrp.class));
            bundle.setMekaGroupGrp(parseRequired(request.getBsdxGrpDir().resolve("MekaGroup.grp"), MekaGroupGrp.class));
            bundle.setProgramMaterialGrp(parseRequired(request.getBsdxGrpDir().resolve("ProgramMaterial.grp"), ProgramMaterialGrp.class));
            bundle.setSeGroupGrp(parseRequired(request.getBsdxGrpDir().resolve("SeGroup.grp"), SeGroupGrp.class));
            bundle.setSpriteGroupGrp(parseRequired(request.getBsdxGrpDir().resolve("SpriteGroup.grp"), SpriteGroupGrp.class));
            bundle.setWazaGroupGrp(parseRequired(request.getBsdxGrpDir().resolve("WazaGroup.grp"), WazaGroupGrp.class));

            bundle.setMekaDat(parseRequired(request.getBsdxDatDir().resolve("Meka.dat"), Dat.class));
            bundle.setMekaPilotDat(parseRequired(request.getBsdxDatDir().resolve("MekaPilot.dat"), Dat.class));
            bundle.setSelectMekaMenuDat(parseRequired(request.getBsdxDatDir().resolve("SelectMekaMenu.dat"), Dat.class));
            bundle.setWeaponEquipDat(parseRequired(request.getBsdxDatDir().resolve("WeaponEquip.dat"), Dat.class));

            bundle.setMekByFileName(parseAll(request.getBsdxMekDir(), "*.mek", Mek.class));
            bundle.setSpmByFileName(parseAll(request.getBsdxSpmDir(), "*.spm", Spm.class));
            bundle.setWazByFileName(parseAll(request.getBsdxWazDir(), "*.waz", Waz.class));

            bundle.setMekaPilotSpm(bundle.getSpmByFileName().get("MekaPilot.spm"));
            bundle.setSelectMekaMenuMekaSpm(bundle.getSpmByFileName().get("SelectMekaMenuMeka.spm"));
            return bundle;
        } catch (IOException e) {
            throw new IllegalStateException("加载 graft baseline 失败", e);
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
