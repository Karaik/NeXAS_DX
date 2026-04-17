package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import lombok.Data;

import java.nio.file.Path;

/**
 * 记录 Update3.pac 打包结果。
 */
@Data
public class TsukuyomiPacPackPlan {

    private boolean packed;
    private Path sourceFolder;
    private Path outputPacPath;
    private String compressMode;
    private String packLog;
}
