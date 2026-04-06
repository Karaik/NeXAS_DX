package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * JINKI -> BSDX 的 AKAO graft 流程请求对象。
 */
@Data
public class AkaoGraftRequest {

    private Path jinkiGrpDir = Paths.get("src/main/resources/game/jinki/grp");
    private Path jinkiDatDir = Paths.get("src/main/resources/game/jinki/dat");
    private Path jinkiMekDir = Paths.get("src/main/resources/game/jinki/mek");
    private Path jinkiSpmDir = Paths.get("src/main/resources/game/jinki/spm");
    private Path jinkiWazDir = Paths.get("src/main/resources/game/jinki/waz");

    private Path bsdxGrpDir = Paths.get("src/main/resources/game/bsdx/grp");
    private Path bsdxDatDir = Paths.get("src/main/resources/game/bsdx/dat");
    private Path bsdxMekDir = Paths.get("src/main/resources/game/bsdx/mek");
    private Path bsdxSpmDir = Paths.get("src/main/resources/game/bsdx/spm");
    private Path bsdxWazDir = Paths.get("src/main/resources/game/bsdx/waz");

    private Path externalStaticAssetRoot = Paths.get("D:/BDY/NeXAS_Resources/jinki_resources");
    private Path targetExePath = Paths.get("src/main/resources/ida-reverse/BaldrSky.exe");
    private Path exeOutputDir = Paths.get("src/main/resources/out");
    private String pacCompressMode = "4";

    private String mekaCodeName = "AKAO";
    private String wazCodeName = "AKAO";
    private String spriteCodeName = "0001";
    private String spriteFileName = "moribito_2.spm";
    private String mekFileName = "Akao.mek";
    private String wazFileName = "Akao.waz";
    private Integer fixedMekaGroupIndex = null;

    private boolean patchMenuData = true;
    private boolean planExeCapacityPatch = true;
}
