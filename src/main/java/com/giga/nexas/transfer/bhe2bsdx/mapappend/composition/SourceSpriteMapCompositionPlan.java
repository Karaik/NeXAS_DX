package com.giga.nexas.transfer.bhe2bsdx.mapappend.composition;

import lombok.Data;

import java.nio.file.Path;

@Data
public class SourceSpriteMapCompositionPlan {

    private int spriteMapIndex;
    private String sourceText;
    private String sourceFileName;
    private String targetText;
    private String targetFileName;
    private Path sourcePath;
    private com.giga.nexas.dto.bhe.spm.Spm sourceSpm;
    private com.giga.nexas.dto.bsdx.spm.Spm bsdxSpm;
    private String parseProblem;
    private int pageCount;
    private int imageCount;
    private int animCount;
    private int chipCount;
    private int hitCount;
    private int patPageNum;
    private int pageOffset;
    private int imageOffset;
    private int animOffset;

    public boolean isLoaded() {
        return sourceSpm != null && bsdxSpm != null && parseProblem == null;
    }
}
