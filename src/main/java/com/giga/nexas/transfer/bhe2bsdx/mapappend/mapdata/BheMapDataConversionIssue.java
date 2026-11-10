package com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import lombok.Data;

/**
 * 单条 MapData conversion 问题。
 *
 * <p>业务不可转换场景写入 issue，不用异常表达，调用方据此决定是否允许落盘。</p>
 */
@Data
public class BheMapDataConversionIssue {

    private BheMapDataConversionIssueType type;
    private String sourceMapFileName;
    private BheMapReferenceType referenceType;
    private String referenceText;
    private Integer groupIndex;
    private Integer typeId;
    private Integer x;
    private Integer y;
    private String message;

    public static BheMapDataConversionIssue blocking(
            BheMapDataConversionIssueType type,
            String sourceMapFileName,
            String message
    ) {
        BheMapDataConversionIssue issue = new BheMapDataConversionIssue();
        issue.setType(type);
        issue.setSourceMapFileName(sourceMapFileName);
        issue.setMessage(message);
        return issue;
    }
}
