package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

/**
 * BHE map import 审计问题的结构化记录。
 *
 * <p>sourceGroupResourceName 和 sourceMapFileName 用来把问题绑定回 plan entry；
 * message 只给日志和人工阅读使用，不作为后续逻辑判断依据。</p>
 */
@Data
public class BheMapAppendProblem {

    private BheMapAppendProblemType type;
    private BheMapAppendProblemSeverity severity;
    private String sourceGroupResourceName;
    private String sourceMapFileName;
    private String targetName;
    private String referenceText;
    private String message;
}
