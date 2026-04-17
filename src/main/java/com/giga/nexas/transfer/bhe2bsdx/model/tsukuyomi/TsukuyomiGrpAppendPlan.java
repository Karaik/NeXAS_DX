package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 记录 step4 之后得到的最终目标索引。
 */
@Data
public class TsukuyomiGrpAppendPlan {

    private int mekaGroupIndex = -1;
    private int wazaGroupIndex = -1;
    private int spriteGroupIndex = -1;
    private int batVoiceGroupIndex = -1;

    private Map<Integer, Integer> sourceWazGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Map<Integer, Integer>> sourceWazSkillIndexToTargetIndexByGroup = new LinkedHashMap<>();
    private Map<Integer, Integer> targetWazSkillCountByGroupIndex = new LinkedHashMap<>();

    private Map<Integer, Integer> sourceSpriteGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceBatVoiceGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceSeGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Map<Integer, Integer>> sourceSeItemIndexToTargetIndexByGroup = new LinkedHashMap<>();
}
