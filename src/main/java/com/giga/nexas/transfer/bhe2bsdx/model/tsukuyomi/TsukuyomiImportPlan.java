package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 记录 Tsukuyomi graft 第一阶段所需导入闭包和挂接目标的计划对象。
 */
@Data
public class TsukuyomiImportPlan {

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
