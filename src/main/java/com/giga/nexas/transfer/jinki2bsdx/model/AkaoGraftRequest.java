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
    /**
     * JINKI <code>SpriteGroup.grp[n].spriteCodeName</code>
     *
     * <p>这里不是文件名，也不是 SPM 文件内部字段，而是 JINKI 的 SpriteGroup.grp
     * 注册表里那一行的 codeName。大部分机体或资源会用机体名/资源名做 codeName，
     * 但 AKAO 这条源数据里就是 {@code 0001}，戏画你在干嘛？。</p>
     *
     * <p>例 1：AKAO 当前主 sprite 入口。</p>
     *
     * <pre>{@code
     * index = 33
     * existFlag = 1
     * spriteFileName = moribito_2.spm
     * spriteCodeName = 0001
     * param = 0
     * }</pre>
     *
     * <p>例 2：普通机体/资源名式 codeName。</p>
     *
     * <pre>{@code
     * index = 10
     * existFlag = 1
     * spriteFileName = masa.spm
     * spriteCodeName = MASA
     * param = 0
     * }</pre>
     *
     * <p>它和 {@link #spriteFileName} 会一起用于定位源 JINKI SpriteGroup 条目：
     * {@code spriteCodeName=0001} 负责匹配 GRP 里的身份 key，
     * {@code spriteFileName=moribito_2.spm} 负责匹配实际 SPM 文件名。找到该条目后，
     * pipeline 才能把 MEK 的 {@code spmFileSequence}、WAZ 里的 {@code CEventSprite}
     * 引用以及 ProgramMaterial 相关结构重绑到 BSDX 目标 SpriteGroup 索引。</p>
     */
    private String spriteCodeName = "0001";
    private String spriteFileName = "moribito_2.spm";
    private String mekFileName = "Akao.mek";
    private String wazFileName = "Akao.waz";
    /**
     * 为 null 时使用追加机体到 MekaGroup 尾部，为非 null 时强制写入指定 MekaGroup 索引。
     */
    private Integer fixedMekaGroupIndex = null;

    private boolean patchMenuData = true;
    /**
     * 是否生成并应用 exe 容量相关 patch。
     */
    private boolean planExeCapacityPatch = true;
}
