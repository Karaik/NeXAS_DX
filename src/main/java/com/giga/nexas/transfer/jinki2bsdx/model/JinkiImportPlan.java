package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 记录 AKAO graft 第一阶段所需导入闭包和后续挂接目标的计划对象。
 *
 * <p>本身不是 diff 结果，而是"按 JINKI 真源整体导入"的执行计划。
 * step4 会基于此计划中的源索引做 diff（{@code findXxxIndex} 匹配 BSDX 基线），
 * 决定复用已有索引还是尾插新条目。</p>
 */
@Data
public class JinkiImportPlan {

    private List<String> requiredMekFiles = new ArrayList<>();
    private List<String> requiredWazFiles = new ArrayList<>();
    private List<String> requiredSpmFiles = new ArrayList<>();

    private List<String> grpAppendTargets = new ArrayList<>();
    private List<String> programMaterialSyncTargets = new ArrayList<>();
    private List<String> mekRebindTargets = new ArrayList<>();
    private List<String> wazRebindTargets = new ArrayList<>();

    private Map<String, Integer> sourceSpriteIndexByFileName = new LinkedHashMap<>();
    private Map<String, Integer> sourceWazIndexByFileName = new LinkedHashMap<>();
    private Map<String, Integer> targetSpriteIndexByFileName = new LinkedHashMap<>();
    private Map<String, Integer> targetWazIndexByFileName = new LinkedHashMap<>();

    private Map<Integer, String> referencedSourceWazFileNameByGroupIndex = new LinkedHashMap<>();
    private Map<Integer, String> referencedSourceSpriteFileNameByGroupIndex = new LinkedHashMap<>();
    private Map<Integer, List<Integer>> referencedSourceSeItemIndicesByGroupIndex = new LinkedHashMap<>();

    private List<String> unresolvedResources = new ArrayList<>();

    private List<String> notes = new ArrayList<>();
}
