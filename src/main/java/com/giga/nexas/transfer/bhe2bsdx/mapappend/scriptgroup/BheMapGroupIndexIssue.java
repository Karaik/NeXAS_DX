package com.giga.nexas.transfer.bhe2bsdx.mapappend.scriptgroup;

import lombok.Data;

/**
 * BHE map script group compatibility 问题。
 *
 * <p>这些问题用于阻止不自洽的 MapData conversion 规则进入正式写入流程。</p>
 */
@Data
public class BheMapGroupIndexIssue {

    private String sourceMapFileName;
    private Integer groupNum;
    private Integer groupIndex;
    private Integer typeId;
    private String message;
}
