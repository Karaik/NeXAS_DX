package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import com.giga.nexas.dto.bsdx.mek.Mek;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MEK 重绑上下文。
 */
@Data
public class TsukuyomiMekRebindContext {

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
