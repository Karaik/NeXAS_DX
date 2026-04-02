package com.giga.nexas.dto.bsdx;

import java.util.ArrayList;
import java.util.List;

/**
 * BSDX collection 的外置语义结果。
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
 * - itemName 更接近人类阅读时的自然语言
 * - itemDescription 更接近稳定的语义标识
 *
 * 这两者在后续迁移和文档表达中都有价值。
 */
public class BsdxInfoCollectionSemantic {

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

    public String getTermSource() {
        return termSource;
    }

    public void setTermSource(String termSource) {
        this.termSource = termSource;
    }

    public Integer getInt1() {
        return int1;
    }

    public void setInt1(Integer int1) {
        this.int1 = int1;
    }

    public Integer getInt2() {
        return int2;
    }

    public void setInt2(Integer int2) {
        this.int2 = int2;
    }

    public Integer getStartGroupIndex() {
        return startGroupIndex;
    }

    public void setStartGroupIndex(Integer startGroupIndex) {
        this.startGroupIndex = startGroupIndex;
    }

    public String getStartGroupCodeName() {
        return startGroupCodeName;
    }

    public void setStartGroupCodeName(String startGroupCodeName) {
        this.startGroupCodeName = startGroupCodeName;
    }

    public Integer getLastParam1() {
        return lastParam1;
    }

    public void setLastParam1(Integer lastParam1) {
        this.lastParam1 = lastParam1;
    }

    public Integer getLastParam2() {
        return lastParam2;
    }

    public void setLastParam2(Integer lastParam2) {
        this.lastParam2 = lastParam2;
    }

    public int getConsumedStepCount() {
        return consumedStepCount;
    }

    public void setConsumedStepCount(int consumedStepCount) {
        this.consumedStepCount = consumedStepCount;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public String getSyntaxPathByName() {
        return syntaxPathByName;
    }

    public void setSyntaxPathByName(String syntaxPathByName) {
        this.syntaxPathByName = syntaxPathByName;
    }

    public String getSyntaxPathByDescription() {
        return syntaxPathByDescription;
    }

    public void setSyntaxPathByDescription(String syntaxPathByDescription) {
        this.syntaxPathByDescription = syntaxPathByDescription;
    }

    public List<Integer> getTypeListSnapshot() {
        return typeListSnapshot;
    }

    public void setTypeListSnapshot(List<Integer> typeListSnapshot) {
        this.typeListSnapshot = typeListSnapshot;
    }

    public List<Integer> getTrailingTypeListSnapshot() {
        return trailingTypeListSnapshot;
    }

    public void setTrailingTypeListSnapshot(List<Integer> trailingTypeListSnapshot) {
        this.trailingTypeListSnapshot = trailingTypeListSnapshot;
    }

    public List<Integer> getParamListSnapshot() {
        return paramListSnapshot;
    }

    public void setParamListSnapshot(List<Integer> paramListSnapshot) {
        this.paramListSnapshot = paramListSnapshot;
    }

    public List<Integer> getIntList3Snapshot() {
        return intList3Snapshot;
    }

    public void setIntList3Snapshot(List<Integer> intList3Snapshot) {
        this.intList3Snapshot = intList3Snapshot;
    }

    public List<Integer> getIntList4Snapshot() {
        return intList4Snapshot;
    }

    public void setIntList4Snapshot(List<Integer> intList4Snapshot) {
        this.intList4Snapshot = intList4Snapshot;
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void setSteps(List<Step> steps) {
        this.steps = steps;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

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

        public Integer getDepth() {
            return depth;
        }

        public void setDepth(Integer depth) {
            this.depth = depth;
        }

        public Integer getGroupIndex() {
            return groupIndex;
        }

        public void setGroupIndex(Integer groupIndex) {
            this.groupIndex = groupIndex;
        }

        public String getTermGroupName() {
            return termGroupName;
        }

        public void setTermGroupName(String termGroupName) {
            this.termGroupName = termGroupName;
        }

        public String getTermGroupCodeName() {
            return termGroupCodeName;
        }

        public void setTermGroupCodeName(String termGroupCodeName) {
            this.termGroupCodeName = termGroupCodeName;
        }

        public Integer getItemIndex() {
            return itemIndex;
        }

        public void setItemIndex(Integer itemIndex) {
            this.itemIndex = itemIndex;
        }

        public String getTermItemName() {
            return termItemName;
        }

        public void setTermItemName(String termItemName) {
            this.termItemName = termItemName;
        }

        public String getTermItemCodeName() {
            return termItemCodeName;
        }

        public void setTermItemCodeName(String termItemCodeName) {
            this.termItemCodeName = termItemCodeName;
        }

        public String getTermItemDescription() {
            return termItemDescription;
        }

        public void setTermItemDescription(String termItemDescription) {
            this.termItemDescription = termItemDescription;
        }

        public Integer getParam1() {
            return param1;
        }

        public void setParam1(Integer param1) {
            this.param1 = param1;
        }

        public Integer getParam2() {
            return param2;
        }

        public void setParam2(Integer param2) {
            this.param2 = param2;
        }

        public String getSyntaxByName() {
            return syntaxByName;
        }

        public void setSyntaxByName(String syntaxByName) {
            this.syntaxByName = syntaxByName;
        }

        public String getSyntaxByDescription() {
            return syntaxByDescription;
        }

        public void setSyntaxByDescription(String syntaxByDescription) {
            this.syntaxByDescription = syntaxByDescription;
        }
    }
}
