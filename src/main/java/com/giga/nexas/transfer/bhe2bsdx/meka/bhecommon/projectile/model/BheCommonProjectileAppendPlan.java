package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * BHE 公共弹幕资源簇追加计划。
 *
 * <p>本模型是公共弹幕资源层的最小承载体。redirect 实现需要补充公共 WAZ、
 * 公共 SPM、公共 SE 聚合组的目标索引和审计信息。Voice 只记录 SOU/MISAKI
 * 外部依赖；term / InfoCollection 通过独立 TODO 子问题承接全量语义转换。</p>
 *
 * <p>BHE 迁入资源在目标侧使用 bhe_* 命名空间，避免覆盖 baseline 里已有的同名文件。
 * 运行时只认 GRP 顶层 index、WAZ skill index、SPM anim/page/image index；
 * 因此 append plan 必须记录 sourceIndex 到 targetIndex 的映射，不能让下游步骤靠文件名猜目标位置。</p>
 *
 * <p>公共资源接入不能只追加文件。WAZ/SPM/SE 的顶层数量会影响 GRP、
 * ProgramMaterial 和 MEK material 尾部结构，redirect 写入这些资源前必须同步维护对应容量。</p>
 *
 * <p>该计划同时承担单机体 graft 的只读护栏：单机体阶段可以读取公共资源引用映射，
 * 但不能用本计划重复 append、merge 或输出公共 WAZ/SPM。公共资源接入产物属于
 * preparedBaseline 的既成事实，单机体阶段只能把私有 WAZ 中的公共引用重定向到该基线。</p>
 */
@Data
public class BheCommonProjectileAppendPlan {

    private int baseWazaGroupSize = -1;
    private int baseSpriteGroupSize = -1;
    private int baseSeGroupSize = -1;
    private int commonProjectileSeGroupIndex = -1;

    private List<String> commonProjectileWazFiles = new ArrayList<>();
    private List<String> commonProjectileSpmFiles = new ArrayList<>();
    private List<String> globalSeReferences = new ArrayList<>();
    private List<String> globalVoiceReferences = new ArrayList<>();
    private List<String> termReferences = new ArrayList<>();
    private List<String> notes = new ArrayList<>();

    /**
     * BHE 源公共 WazaGroup index -> preparedBaseline 中新增 bhe_* WazaGroup index。
     */
    private Map<Integer, Integer> sourceWazIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, String> sourceWazIndexToTargetFileName = new LinkedHashMap<>();

    /**
     * BHE 源 SpriteGroup index -> preparedBaseline 中新增 bhe_* SpriteGroup index。
     *
     * <p>公共 SPM 源 index 不是连续区间，`173` 会映射到紧凑追加段的最后一项。</p>
     */
    private Map<Integer, Integer> sourceSpriteIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, String> sourceSpriteIndexToTargetFileName = new LinkedHashMap<>();

    /**
     * BHE 源 `(SeGroup, SeItem)` -> `bhe_common_projectile_se` 聚合组内 item index。
     */
    private Map<String, Integer> sourceSePairToTargetItemIndex = new LinkedHashMap<>();
    private Map<String, String> sourceSePairToTargetFileName = new LinkedHashMap<>();

    public static String sePairKey(int sourceSeGroupIndex, int sourceSeItemIndex) {
        return sourceSeGroupIndex + ":" + sourceSeItemIndex;
    }
}
