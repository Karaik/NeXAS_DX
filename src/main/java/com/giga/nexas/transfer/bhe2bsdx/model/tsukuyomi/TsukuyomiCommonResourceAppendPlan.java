package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Tsukuyomi 第 0 步的指定公共资源追加计划。
 *
 * <p>该计划用于记录“哪些公共 WAZ 被显式指定追加”，以及后续由这些 WAZ 推导出的 SPM / SE 依赖。</p>
 */
@Data
public class TsukuyomiCommonResourceAppendPlan {

    private List<String> specifiedCommonWazFiles = new ArrayList<>();
    private List<String> resolvedCommonSpmFiles = new ArrayList<>();
    private List<String> resolvedCommonSeRefs = new ArrayList<>();
    private List<String> notes = new ArrayList<>();
}
