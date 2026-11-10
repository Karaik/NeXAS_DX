package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

import java.nio.file.Path;

/**
 * 记录 step11 打包 Update3.pac 的结果。
 */
@Data
public class PacPackPlan {

    private boolean packed;
    private Path sourceFolder;
    private Path outputPacPath;
    private String compressMode;
    private String packLog;
}
