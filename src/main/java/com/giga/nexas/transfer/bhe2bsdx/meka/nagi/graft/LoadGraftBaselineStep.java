package com.giga.nexas.transfer.bhe2bsdx.meka.nagi.graft;

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
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class LoadGraftBaselineStep {

    
    private static final String CHARSET = "windows-31j";

    
    private final BsdxBinService bsdxBinService;

    public LoadGraftBaselineStep() {
        this(new BsdxBinService());
    }

    public LoadGraftBaselineStep(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public TsukuyomiBsdxBaselineBundle load(TsukuyomiGraftRequest request) {
        TsukuyomiBsdxBaselineBundle bundle = new TsukuyomiBsdxBaselineBundle();

        try {
            Path bsdxGrpDir = request.resolveBsdxGrpDir();
            Path bsdxDatDir = request.resolveBsdxDatDir();
            Path bsdxMekDir = request.resolveBsdxMekDir();
            Path bsdxSpmDir = request.resolveBsdxSpmDir();
            Path bsdxWazDir = request.resolveBsdxWazDir();

            ensureDirectory(bsdxGrpDir);
            ensureDirectory(bsdxDatDir);
            ensureDirectory(bsdxMekDir);
            ensureDirectory(bsdxSpmDir);
            ensureDirectory(bsdxWazDir);

            bundle.setBatVoiceGrp(parseRequired(bsdxGrpDir.resolve("BatVoice.grp"), BatVoiceGrp.class));
            bundle.setMapGroupGrp(parseRequired(bsdxGrpDir.resolve("MapGroup.grp"), MapGroupGrp.class));
            bundle.setMekaGroupGrp(parseRequired(bsdxGrpDir.resolve("MekaGroup.grp"), MekaGroupGrp.class));
            bundle.setProgramMaterialGrp(parseRequired(bsdxGrpDir.resolve("ProgramMaterial.grp"), ProgramMaterialGrp.class));
            bundle.setSeGroupGrp(parseRequired(bsdxGrpDir.resolve("SeGroup.grp"), SeGroupGrp.class));
            bundle.setSpriteGroupGrp(parseRequired(bsdxGrpDir.resolve("SpriteGroup.grp"), SpriteGroupGrp.class));
            bundle.setWazaGroupGrp(parseRequired(bsdxGrpDir.resolve("WazaGroup.grp"), WazaGroupGrp.class));

            bundle.setMekaDat(parseRequired(bsdxDatDir.resolve("Meka.dat"), Dat.class));
            bundle.setMekaPilotDat(parseRequired(bsdxDatDir.resolve("MekaPilot.dat"), Dat.class));
            bundle.setSelectMekaMenuDat(parseRequired(bsdxDatDir.resolve("SelectMekaMenu.dat"), Dat.class));
            bundle.setWeaponEquipDat(parseRequired(bsdxDatDir.resolve("WeaponEquip.dat"), Dat.class));

            bundle.setMekByFileName(parseAll(bsdxMekDir, "*.mek", Mek.class));
            bundle.setSpmByFileName(parseAll(bsdxSpmDir, "*.spm", Spm.class));
            bundle.setWazByFileName(parseAll(bsdxWazDir, "*.waz", Waz.class));

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
