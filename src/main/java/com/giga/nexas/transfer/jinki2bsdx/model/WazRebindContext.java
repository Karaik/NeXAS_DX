package com.giga.nexas.transfer.jinki2bsdx.model;

import com.giga.nexas.dto.bsdx.waz.Waz;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * step7 重建 Akao.waz 时使用的上下文。
 */
@Data
public class WazRebindContext {

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
    private Map<Integer, Integer> sourceToTargetSpriteGroupIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceToTargetSeGroupIndex = new LinkedHashMap<>();
    private Map<Integer, Map<Integer, Integer>> sourceToTargetSeItemIndexByGroup = new LinkedHashMap<>();
}
