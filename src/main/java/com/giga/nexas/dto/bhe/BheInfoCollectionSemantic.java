package com.giga.nexas.dto.bhe;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE collection 的外置语义结果。
 *
 * <p>这个类不是二进制 DTO，而是分析结果对象。
 * 它的设计目标是：
 * 1. 保留原始字段快照，方便回看
 * 2. 记录 term 驱动的主语法链
 * 3. 同时输出两种直接来自 term 的表达方式：
 *    - groupCodeName/itemName
 *    - groupCodeName/itemDescription
 *
 * <p>之所以同时保留这两套，是因为：
 * - `itemName` 更接近人类阅读时的自然语言
 * - `itemDescription` 更接近稳定的语义标识
 *
 * 这两者在后续迁移和文档表达中都有价值。
 */
@Data
public class BheInfoCollectionSemantic {

    private String termSource;

    private Integer int1;
    private Integer int2;

    private Integer startGroupIndex;
    private String startGroupCodeName;

    private Integer lastParam1;
    private Integer lastParam2;

    /**
     * 主链实际消费了多少个 typeList 节点。
     *
     * <p>这个值和原始 typeList.size() 不一定相同，
     * 因为主链可能在中途遇到负数 param2 后提前结束。
     */
    private int consumedStepCount;
    private boolean terminated;

    /**
     * 直接由 termGroupCodeName / termItemName 拼出的可读路径。
     */
    private String syntaxPathByName = "";

    /**
     * 直接由 termGroupCodeName / termItemDescription 拼出的稳定语义路径。
     */
    private String syntaxPathByDescription = "";

    private List<Integer> typeListSnapshot = new ArrayList<>();

    /**
     * 主链终止后，raw typeList 中未被继续消费的尾部残留值。
     *
     * <p>这些值当前不参与主语义路径拼装，但必须保留，
     * 因为它们属于真实数据的一部分。
     */
    private List<Integer> trailingTypeListSnapshot = new ArrayList<>();
    private List<Integer> paramListSnapshot = new ArrayList<>();
    private List<Integer> intList3Snapshot = new ArrayList<>();
    private List<Integer> intList4Snapshot = new ArrayList<>();

    private List<Step> steps = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    @Data
    public static class Step {
        private Integer depth;

        private Integer groupIndex;
        private String termGroupName;
        private String termGroupCodeName;

        private Integer itemIndex;
        private String termItemName;
        private String termItemCodeName;
        private String termItemDescription;

        private Integer param1;
        private Integer param2;

        /**
         * 直接由 termGroupCodeName / termItemName 组成的单步表达。
         */
        private String syntaxByName;

        /**
         * 直接由 termGroupCodeName / termItemDescription 组成的单步表达。
         */
        private String syntaxByDescription;
    }
}
