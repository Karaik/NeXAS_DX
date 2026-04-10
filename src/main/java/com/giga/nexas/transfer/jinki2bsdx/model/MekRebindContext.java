package com.giga.nexas.transfer.jinki2bsdx.model;

import com.giga.nexas.dto.bsdx.mek.Mek;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * step7 中用于驱动 Mek 分片重建的上下文对象。
 */
@Data
public class MekRebindContext {

    private Mek sourceMek;

    private String sourceFileName;
    private Integer sourceWazFileSequence;
    private Integer sourceSpmFileSequence;

    private int targetWazaGroupIndex = -1;
    private int targetSpriteGroupIndex = -1;
    private int targetBatVoiceGroupIndex = -1;

    private Map<Integer, Integer> sourceSpriteGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceBatVoiceGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceSeGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Map<Integer, Integer>> sourceSeItemIndexToTargetIndexByGroup = new LinkedHashMap<>();
}
