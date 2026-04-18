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
 * 外部依赖；term / InfoCollection 由 term 包统一完成全量语义转换。</p>
 *
 * <p>公共 WAZ 不新增顶层 WazaGroup，而是追加到 BSDX 已经会预加载的公共宿主 WAZ。
 * 公共 SPM 按实际数据分两种：BSDX 已有同名宿主时追加到宿主，不存在宿主时新增 bhe_* entry。
 * 运行时只认 GRP 顶层 index、WAZ skill index、SPM anim/page/image index；
 * 因此 append plan 必须记录 sourceIndex 到 targetIndex 与内部 base offset 的映射，
 * 不能让下游步骤靠文件名猜目标位置。</p>
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
     * BHE 源公共 WazaGroup index -> preparedBaseline 中的公共宿主 WazaGroup index。
     *
     * <p>公共 WAZ 追加到 Effect/Tama/Laser/Bomb 等宿主文件后，外部引用仍指向宿主顶层 index，
     * 但 skill index 必须加上追加前的宿主 skill 数。</p>
     */
    private Map<Integer, Integer> sourceWazIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, String> sourceWazIndexToTargetFileName = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceWazIndexToTargetSkillBase = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceWazIndexToTargetSkillCount = new LinkedHashMap<>();

    /**
     * BHE 源 SpriteGroup index -> preparedBaseline 中的目标 SpriteGroup index。
     *
     * <p>同名公共 SPM 会追加到 BSDX 宿主文件；BHE-only 公共 SPM 才新增 bhe_* entry。
     * CEventSprite 除了 spmFileSequence，还带 actionGroupNumber，因此同名宿主追加时也要记录
     * action group base，供公共 WAZ 与单机体 WAZ 同步偏移。</p>
     */
    private Map<Integer, Integer> sourceSpriteIndexToTargetIndex = new LinkedHashMap<>();
    private Map<Integer, String> sourceSpriteIndexToTargetFileName = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceSpriteIndexToTargetImageBase = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceSpriteIndexToTargetPageBase = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceSpriteIndexToTargetActionGroupBase = new LinkedHashMap<>();
    private Map<Integer, Integer> sourceSpriteIndexToTargetActionGroupCount = new LinkedHashMap<>();

    /**
     * BHE 源 `(SeGroup, SeItem)` -> `BHE_SE_PUBLIC` 聚合组内 item index。
     */
    private Map<String, Integer> sourceSePairToTargetItemIndex = new LinkedHashMap<>();
    private Map<String, String> sourceSePairToTargetFileName = new LinkedHashMap<>();

    public static String sePairKey(int sourceSeGroupIndex, int sourceSeItemIndex) {
        return sourceSeGroupIndex + ":" + sourceSeItemIndex;
    }
}
