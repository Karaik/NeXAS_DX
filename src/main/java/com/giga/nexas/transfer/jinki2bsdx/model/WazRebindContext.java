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

    // 源侧 group index -> 文件名，用来把 CEventWazaSelect/CEventSprite 里的数字解释成人类可查的资源。
    private Map<Integer, String> sourceWazFileNameByGroupIndex = new LinkedHashMap<>();
    private Map<Integer, String> sourceSpriteFileNameByGroupIndex = new LinkedHashMap<>();

    // 目标侧文件名 -> group index，提供最终运行时会消费的索引。
    private Map<String, Integer> targetWazGroupIndexByFileName = new LinkedHashMap<>();
    private Map<String, Integer> targetSpriteGroupIndexByFileName = new LinkedHashMap<>();

    // CEventWazaSelect 需要两层重写：先改 wazFileNo，再改该 WAZ 内部的 wazSequenceNo。
    private Map<Integer, Integer> sourceToTargetWazGroupIndex = new LinkedHashMap<>();
    private Map<Integer, Map<Integer, Integer>> sourceToTargetWazSkillIndexByGroup = new LinkedHashMap<>();

    // 其他资源引用沿用 group/item 映射表，重建 WAZ 时递归写回到事件对象里。
    private Map<Integer, Integer> sourceToTargetSpriteGroupIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceToTargetSeGroupIndex = new LinkedHashMap<>();
    private Map<Integer, Map<Integer, Integer>> sourceToTargetSeItemIndexByGroup = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceToTargetBatVoiceGroupIndex = new LinkedHashMap<>();
}
