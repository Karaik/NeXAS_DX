package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 记录 step4 之后得到的最终目标索引。
 *
 * <p>这里保存的是迁移结果表，而不是推导规则。
 * step6/7 之后所有内部重定向都只认这里的“源 -> 目标”映射。</p>
 */
@Data
public class GrpAppendPlan {

    private int mekaGroupIndex = -1;
    private int wazaGroupIndex = -1;
    private int spriteGroupIndex = -1;
    private int batVoiceGroupIndex = -1;

    private Map<Integer, Integer> sourceWazGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceSpriteGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceSeGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Map<Integer, Integer>> sourceSeItemIndexToTargetIndexByGroup = new LinkedHashMap<>();
}
