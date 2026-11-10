package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.nio.file.Path;

/**
 * BHE .map 内部的一条资源引用。
 *
 * <p>import plan construction 在这里固定“引用原文、来源类型、源文件定位结果和问题级别”，
 * 后续步骤只消费这些字段，不再重新解析 .map 或重新判定缺失资源。</p>
 */
@Data
public class BheMapResourceReference {

    private BheMapReferenceType type;
    private String sourceText;
    private String sourceFileName;
    private String targetText;
    private String targetFileName;
    private Path sourcePath;
    private BheMapReferenceStatus status;
    private String problem;
    private Integer resourceSlotNum;
    private Integer resourceSlotParam0;
    private Integer resourceSlotParam1;
    private Integer resourceSlotParam2;
    private Integer resourceSlotParam3;
    private Integer resourceSlotParam4;

    public boolean isFound() {
        return status == BheMapReferenceStatus.FOUND_DEFERRED;
    }

    public boolean isMissing() {
        return status == BheMapReferenceStatus.MISSING_NON_BLOCKING;
    }
}
