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

    // 这些列表是 Step 8 的落盘白名单，不代表最终一定 raw copy；
    // WAZ 会在后续根据 GrpAppendPlan 决定是重建主 WAZ、合并辅助 WAZ，还是直接复用。
    private List<String> requiredMekFiles = new ArrayList<>();
    private List<String> requiredWazFiles = new ArrayList<>();
    private List<String> requiredSpmFiles = new ArrayList<>();

    // 只用于日志和审计，方便看这次 graft 实际把哪些资源纳入了处理范围。
    private List<String> grpAppendTargets = new ArrayList<>();
    private List<String> programMaterialSyncTargets = new ArrayList<>();
    private List<String> mekRebindTargets = new ArrayList<>();
    private List<String> wazRebindTargets = new ArrayList<>();

    // 文件名到源/目标 group index 的目录表；Step 4 会基于它们判断复用还是 append。
    private Map<String, Integer> sourceSpriteIndexByFileName = new LinkedHashMap<>();
    private Map<String, Integer> sourceWazIndexByFileName = new LinkedHashMap<>();
    private Map<String, Integer> targetSpriteIndexByFileName = new LinkedHashMap<>();
    private Map<String, Integer> targetWazIndexByFileName = new LinkedHashMap<>();

    // 从 WAZ 递归闭包里真实发现的外部引用；这些才会进入 group 映射和资源输出。
    private Map<Integer, String> referencedSourceWazFileNameByGroupIndex = new LinkedHashMap<>();
    private Map<Integer, String> referencedSourceSpriteFileNameByGroupIndex = new LinkedHashMap<>();
    private Map<Integer, List<Integer>> referencedSourceSeItemIndicesByGroupIndex = new LinkedHashMap<>();

    // 发现了引用但无法解析文件/索引时记录在这里，避免静默漏资源。
    private List<String> unresolvedResources = new ArrayList<>();

    private List<String> notes = new ArrayList<>();
}
