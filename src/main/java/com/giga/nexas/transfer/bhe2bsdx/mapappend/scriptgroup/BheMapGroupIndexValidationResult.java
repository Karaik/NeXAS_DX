package com.giga.nexas.transfer.bhe2bsdx.mapappend.scriptgroup;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE map 脚本 groupIndex 前置验证结果。
 *
 * <p>结果对象必须能被测试直接断言，不能只靠日志判断是否可继续转换。</p>
 */
@Data
public class BheMapGroupIndexValidationResult {

    private int validatedMapCount;
    private int inspectedScriptEntryCount;
    private List<BheMapGroupIndexIssue> issues = new ArrayList<>();

    public boolean isPassed() {
        return issues.isEmpty();
    }

    public String issueSummary() {
        List<String> lines = new ArrayList<>();
        for (BheMapGroupIndexIssue issue : issues) {
            lines.add(issue.getSourceMapFileName()
                    + " groupNum=" + issue.getGroupNum()
                    + " groupIndex=" + issue.getGroupIndex()
                    + " typeId=" + issue.getTypeId()
                    + " " + issue.getMessage());
        }
        return String.join(System.lineSeparator(), lines);
    }
}
