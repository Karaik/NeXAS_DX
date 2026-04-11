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

    // WAZ 顶层组号映射：JINKI 的 WazaGroup index -> BSDX 输出后的 WazaGroup index。
    // 例如 JINKI 里的 Tama/Bomb 子链会先通过这张表把 wazFileNo 重写到目标侧。
    private Map<Integer, Integer> sourceWazGroupIndexToTargetIndex = new LinkedHashMap<>();

    // WAZ 内部 skill 映射：每个源 WazaGroup 下，source skill index -> target skill index。
    // key-based merge 后，JINKI 的同名 skill 可能复用 BSDX 旧槽，新增 skill 才 append 到尾部。
    private Map<Integer, Map<Integer, Integer>> sourceWazSkillIndexToTargetIndexByGroup = new LinkedHashMap<>();

    // 目标 WazaGroup.param 必须等于输出 WAZ 的真实 skill 数，否则运行时会按旧上界截断。
    private Map<Integer, Integer> targetWazSkillCountByGroupIndex = new LinkedHashMap<>();

    // Sprite/voice/se 这些仍是顶层 group 级映射，供 mek/waz 内部引用重写使用。
    private Map<Integer, Integer> sourceSpriteGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceBatVoiceGroupIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceSeGroupIndexToTargetIndex = new LinkedHashMap<>();

    // SE 除了 group 要重写，组内 item 也可能因为 append/去重而变化，所以单独保存二级映射。
    private Map<Integer, Map<Integer, Integer>> sourceSeItemIndexToTargetIndexByGroup = new LinkedHashMap<>();
}
