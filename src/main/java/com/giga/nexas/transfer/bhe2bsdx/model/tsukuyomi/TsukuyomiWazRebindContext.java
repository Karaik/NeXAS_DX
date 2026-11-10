package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import com.giga.nexas.dto.bsdx.waz.Waz;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WAZ 重绑上下文。
 */
@Data
public class TsukuyomiWazRebindContext {

    private Waz sourceWaz;
    private String sourceFileName;

    private Integer targetMainWazGroupIndex;
    private Integer targetMainSpriteGroupIndex;
    private Integer targetMainBatVoiceGroupIndex;

    private Map<Integer, String> sourceWazFileNameByGroupIndex = new LinkedHashMap<>();
    private Map<Integer, String> sourceSpriteFileNameByGroupIndex = new LinkedHashMap<>();

    private Map<String, Integer> targetWazGroupIndexByFileName = new LinkedHashMap<>();
    private Map<String, Integer> targetSpriteGroupIndexByFileName = new LinkedHashMap<>();

    private Map<Integer, Integer> sourceToTargetWazGroupIndex = new LinkedHashMap<>();
    private Map<Integer, Map<Integer, Integer>> sourceToTargetWazSkillIndexByGroup = new LinkedHashMap<>();

    private Map<Integer, Integer> sourceToTargetSpriteGroupIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceToTargetSeGroupIndex = new LinkedHashMap<>();
    private Map<Integer, Map<Integer, Integer>> sourceToTargetSeItemIndexByGroup = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceToTargetBatVoiceGroupIndex = new LinkedHashMap<>();
}
