package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term;

import com.giga.nexas.dto.bhe.BheInfoCollection;
import com.giga.nexas.dto.bhe.BheInfoCollectionAnalyzer;
import com.giga.nexas.dto.bhe.BheInfoCollectionSemantic;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionAnalyzer;
import com.giga.nexas.dto.bsdx.grp.groupmap.TermGrp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BHE InfoCollection -> BSDX InfoCollection 的语义重编译器。
 *
 * <p>这里不按数字索引搬运，而是先借助 BHE term.grp 还原语义链，再用 BSDX term.grp
 * 重新编译目标索引。BHE 独有语义必须经过本类集中规则处理，避免在 WAZ/MEK 调用点散落 fallback。</p>
 */
public class BheInfoCollectionTermConverter {

    private static final int BHE_OBJECTPOS_GROUP = 2;
    private static final int BHE_OBJECTPOS2_GROUP = 3;

    private final TermRegistry targetRegistry = new TermRegistry(BsdxInfoCollectionAnalyzer.getCachedTermGrp());

    /** 有界样例与完整分类计数避免在全资源转换中持有全部对象图路径。 */
    public record AuxAudit(String context, String dictionary, List<Integer> source,
                           List<Integer> target, boolean heightLoss, boolean anchorNotRead) { }
    private final List<AuxAudit> auxAudit = new ArrayList<>();
    private final java.util.Map<String, Long> auxAuditCounts = new java.util.LinkedHashMap<>();

    public List<AuxAudit> getAuxAudit() { return List.copyOf(auxAudit); }
    public java.util.Map<String, Long> getAuxAuditCounts() { return java.util.Map.copyOf(auxAuditCounts); }

    public BsdxInfoCollection convert(BheInfoCollection source) {
        return convert(source, "InfoCollection");
    }

    public BsdxInfoCollection convert(BheInfoCollection source, String context) {
        return convert(source, context, AuxConsumerPurpose.EXPRESSION);
    }

    public BsdxInfoCollection convert(BheInfoCollection source, String context, AuxConsumerPurpose purpose) {
        if (source == null) {
            return emptyCollection();
        }

        BheInfoCollectionSemantic semantic = BheInfoCollectionAnalyzer.analyze(source);
        if (!semantic.getWarnings().isEmpty()) {
            throw new IllegalStateException("BHE term 语义解析失败: " + context + " " + semantic.getWarnings());
        }

        ConversionPlan plan = buildMainChain(source, semantic, context);
        BsdxInfoCollection target = new BsdxInfoCollection();
        target.setInt1(plan.int1());
        target.getTypeList().addAll(plan.typeList());
        target.getParamList().addAll(plan.paramList());
        AuxConsumerPurpose consumer = resolveAuxConsumer(source, semantic, purpose, context);
        target.getIntList3().addAll(convertAuxList(source.getIntList3(), consumer, context + ".aux3"));
        target.getIntList4().addAll(convertAuxList(source.getIntList4(), consumer, context + ".aux4"));
        target.setInt2(source.getInt2() == null ? 0 : source.getInt2());
        return target;
    }

    public void rewriteCopiedBsdxCollection(BsdxInfoCollection target, String context) {
        rewriteCopiedBsdxCollection(target, context, AuxConsumerPurpose.EXPRESSION);
    }

    public void rewriteCopiedBsdxCollection(BsdxInfoCollection target, String context, AuxConsumerPurpose purpose) {
        if (target == null) {
            return;
        }
        BsdxInfoCollection converted = convert(toBheCollection(target), context, purpose);
        target.setInt1(converted.getInt1());
        target.getTypeList().clear();
        target.getTypeList().addAll(converted.getTypeList());
        target.getParamList().clear();
        target.getParamList().addAll(converted.getParamList());
        target.getIntList3().clear();
        target.getIntList3().addAll(converted.getIntList3());
        target.getIntList4().clear();
        target.getIntList4().addAll(converted.getIntList4());
        target.setInt2(converted.getInt2());
    }

    private ConversionPlan buildMainChain(
            BheInfoCollection source,
            BheInfoCollectionSemantic semantic,
            String context
    ) {
        if (semantic.getSteps().isEmpty()) {
            return unconditional();
        }

        if (requiresWholeConditionFallback(semantic)) {
            return unconditional();
        }

        List<TargetStep> targetSteps = new ArrayList<>();
        boolean collapsedOperatorChain = false;
        for (BheInfoCollectionSemantic.Step step : semantic.getSteps()) {
            if (isOperatorStep(step)) {
                /*
                 * BSDX term.grp 没有 OPERATOR_TWO / OPERATOR 这两组。
                 * 这里保留运算链前面的“取哪个参数”语义，把 BHE 的运行时修正量折叠掉，
                 * 产物会成为 BSDX 可解析的直接 PARAM/POS 或 PARAM/VECTOR 表达。
                 */
                collapsedOperatorChain = true;
                break;
            }
            TargetStep targetStep = mapStep(step, context);
            if (targetStep == null) {
                return unconditional();
            }
            targetSteps.add(targetStep);
        }

        if (targetSteps.isEmpty()) {
            return unconditional();
        }

        List<Integer> typeList = new ArrayList<>();
        for (TargetStep targetStep : targetSteps) {
            typeList.add(targetStep.itemIndex());
        }

        Integer int1 = targetSteps.get(0).groupIndex();
        List<Integer> paramList = collapsedOperatorChain
                ? new ArrayList<>()
                : copyList(source.getParamList());
        return new ConversionPlan(int1, typeList, paramList);
    }

    private TargetStep mapStep(BheInfoCollectionSemantic.Step step, String context) {
        String group = step.getTermGroupCodeName();
        String item = step.getTermItemDescription();

        TargetStep exact = targetRegistry.find(group, item);
        if (exact != null) {
            return exact;
        }

        TargetStep fallback = mapKnownMissingStep(group, item);
        if (fallback != null) {
            return fallback;
        }

        throw new IllegalStateException("未审计的 BHE term 语义: " + context + " " + group + "/" + item);
    }

    private TargetStep mapKnownMissingStep(String group, String item) {
        if ("OBJECT4".equals(group) && item != null && item.matches("PARAMCOUNT[1-4]")) {
            /*
             * BHE 把参数计数拆成 1/2/3/4 四个入口，BSDX 只有 PARAMCOUNT。
             * 比较符和 paramList 由 COMPARE 链路承接，所以这里是“细分折叠”，不是无条件丢弃。
             */
            return targetRegistry.require("OBJECT4", "PARAMCOUNT");
        }
        if ("OBJECT".equals(group)) {
            return switch (item) {
                case "HIT_WAZA" -> targetRegistry.require("OBJECT", "HIT");
                // ROCK 读取已保存目标；ENEMY 会在求值时重新搜索最近敌人。
                case "MULTILOCK_LOCK", "MULTILOCK_LOCK_B",
                        "MULTILOCK_LOCK_N", "MULTILOCK_LENGTH" ->
                        targetRegistry.require("OBJECT", "ROCK");
                default -> null;
            };
        }
        if ("OBJECTPOS".equals(group)) {
            return targetRegistry.require("OBJECTPOS", mapObjectPosDescription(item));
        }
        if ("OBJECTPOS2".equals(group)) {
            return targetRegistry.require("OBJECTPOS2", mapObjectPos2Description(item));
        }
        if ("OBJECT3".equals(group)) {
            return switch (item) {
                case "ACTION_ATTACKCATEGORY", "ACTION_ATTACKBUTTONTYPE",
                        "ACTION_ATTACKTYPE", "ACTION_CPU_SPECIALNO" ->
                        // BSDX 缺少攻击细分类，只保留“处于攻击动作”这个可承载语义。
                        targetRegistry.require("OBJECT3", "ACTION_ATTACK");
                case "ACTION_SDASH_END" -> targetRegistry.require("OBJECT3", "ACTION_SDASH");
                case "ACTION_BDASH_END" -> targetRegistry.require("OBJECT3", "ACTION_BDASH");
                case "ACTION_ELEC", "ACTION_FIRE", "ACTION_OIL" ->
                        // 这几个是 BHE 独有受击/异常细分，BSDX 侧以泛用受击状态承接。
                        targetRegistry.require("OBJECT3", "ACTION_NOKEALL");
                default -> null;
            };
        }
        if ("ANGLE_SEL".equals(group)) {
            if ("DAMAGE_LASEREDA".equals(item)) {
                // BSDX 没有伤害激光枝角度来源；同族激光角度里 LASER02 是最接近承载。
                return targetRegistry.require("ANGLE_SEL", "LASER02");
            }
            if (item != null && item.startsWith("MULTILOCK_")) {
                // 多锁定角度来源无法承载，退到最稳定的 sprite 角度来源。
                return targetRegistry.require("ANGLE_SEL", "SPRITE");
            }
        }
        if ("PARAMCOMP".equals(group) && "HEIGHTANGLE".equals(item)) {
            // BSDX 没有 HEIGHTANGLE，只保留高度比较本身。
            return targetRegistry.require("PARAMCOMP", "HEIGHT");
        }
        if ("PARAM".equals(group) && ("HP".equals(item) || "HP_PS".equals(item))) {
            // HP 参数会进入 BHE 运算子链；BSDX 无该入口，退到数值参数槽位。
            return targetRegistry.require("PARAM", "VAL");
        }
        return null;
    }

    private boolean requiresWholeConditionFallback(BheInfoCollectionSemantic semantic) {
        String path = semantic.getSyntaxPathByDescription();
        /*
         * 这些条件依赖 BHE 独有系统状态或独有子系统，BSDX term.grp 没有同级承载。
         * 为了避免生成无法解析的条件链，按迁移方案降级为 PARENT/UNCONDITIONAL。
         */
        return path.startsWith("PARENT/OBJECT1 -> OBJECT1/MULTILOCK")
                || path.startsWith("PARENT/OBJECT1 -> OBJECT1/ATTR")
                || path.startsWith("PARENT/OBJECT1 -> OBJECT1/FLAG")
                || path.equals("PARENT/NORMAL -> NORMAL/BATTLESCRIPT")
                || path.equals("PARENT/NORMAL -> NORMAL/EQUIPMODE")
                || path.equals("PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/HITRECT_FIRE");
    }

    private boolean isOperatorStep(BheInfoCollectionSemantic.Step step) {
        return "OPERATOR_TWO".equals(step.getTermGroupCodeName())
                || "OPERATOR".equals(step.getTermGroupCodeName());
    }

    private AuxConsumerPurpose resolveAuxConsumer(BheInfoCollection source, BheInfoCollectionSemantic semantic,
                                                  AuxConsumerPurpose purpose, String context) {
        Integer first = source.getTypeList() == null || source.getTypeList().isEmpty() ? null : source.getTypeList().get(0);
        int branch = first == null ? -1 : first;
        if (purpose == AuxConsumerPurpose.EXPRESSION) {
            String root = semantic.getStartGroupCodeName();
            purpose = switch (root == null ? "" : root) {
                case "POS" -> AuxConsumerPurpose.POSITION;
                case "ANGLE" -> AuxConsumerPurpose.ANGLE;
                case "PARAM" -> AuxConsumerPurpose.PARAMETER;
                case "PARENT" -> AuxConsumerPurpose.OBJECT_REFERENCE;
                default -> AuxConsumerPurpose.UNKNOWN;
            };
        }
        return switch (purpose) {
            case ANGLE -> switch (branch) {
                case 1, 2 -> AuxConsumerPurpose.POSITION;
                case 3 -> AuxConsumerPurpose.OBJECT_REFERENCE;
                default -> AuxConsumerPurpose.UNUSED;
            };
            case POSITION -> branch == 1 || branch == 3 ? purpose : AuxConsumerPurpose.UNUSED;
            case PARAMETER -> branch == 0 ? AuxConsumerPurpose.UNUSED : AuxConsumerPurpose.OBJECT_REFERENCE;
            default -> purpose;
        };
    }

    private List<Integer> convertAuxList(List<Integer> source, AuxConsumerPurpose consumer, String context) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        if (consumer == AuxConsumerPurpose.UNKNOWN) {
            throw new IllegalStateException("未知aux消费者: " + context + " " + source);
        }
        if (consumer == AuxConsumerPurpose.UNUSED) {
            recordAux(context, "NOT_READ", source, source, false, true);
            return copyList(source);
        }
        String dictionary = consumer == AuxConsumerPurpose.OBJECT_REFERENCE ? "OBJECT" : "OBJECTPOS";
        int sourceGroup = consumer == AuxConsumerPurpose.OBJECT_REFERENCE ? 1 : BHE_OBJECTPOS_GROUP;
        String sourceDescription = sourceAuxDescription(sourceGroup, source.get(0), context);
        String description = sourceDescription;
        if (targetRegistry.find(dictionary, description) == null) {
            description = switch (description) {
                case "HIT_WAZA" -> "HIT";
                case "MULTILOCK_LOCK", "MULTILOCK_LOCK_B", "MULTILOCK_LOCK_N", "MULTILOCK_LENGTH" -> "ROCK";
                default -> throw new IllegalStateException("未审计aux语义: " + context + " " + dictionary + "/" + description);
            };
        }
        List<Integer> target = copyList(source);
        target.set(0, targetRegistry.require(dictionary, description).itemIndex());
        boolean heightLoss = false;
        if (consumer == AuxConsumerPurpose.POSITION && source.size() > 1) {
            String anchor = sourceAuxDescription(BHE_OBJECTPOS2_GROUP, source.get(1), context);
            heightLoss = "CENTER".equals(anchor) || "TOP".equals(anchor);
            target.set(1, targetRegistry.require("OBJECTPOS2", mapObjectPos2Description(anchor)).itemIndex());
        }
        recordAux(context, dictionary, source, target, heightLoss, consumer == AuxConsumerPurpose.OBJECT_REFERENCE);
        return target;
    }

    private void recordAux(String context, String dictionary, List<Integer> source, List<Integer> target,
                           boolean loss, boolean anchorNotRead) {
        auxAuditCounts.merge(dictionary + (loss ? ":HEIGHT_LOSS" : ""), 1L, Long::sum);
        if (auxAudit.size() < 128) auxAudit.add(new AuxAudit(context, dictionary, copyList(source), copyList(target), loss, anchorNotRead));
    }

    private String sourceAuxDescription(int groupIndex, Integer itemIndex, String context) {
        var groups = BheInfoCollectionAnalyzer.getCachedTermGrp().getTermList();
        if (itemIndex == null || itemIndex < 0 || itemIndex >= groups.get(groupIndex).getTermItemList().size()) {
            throw new IllegalStateException("aux索引越界: " + context + " group=" + groupIndex + " item=" + itemIndex);
        }
        return groups.get(groupIndex).getTermItemList().get(itemIndex).getTermItemDescription();
    }

    private String mapObjectPosDescription(String description) {
        if (targetRegistry.find("OBJECTPOS", description) != null) return description;
        return switch (description) {
            case "HIT_WAZA" -> "HIT";
            case "MULTILOCK_LOCK", "MULTILOCK_LOCK_B", "MULTILOCK_LOCK_N", "MULTILOCK_LENGTH" -> "ROCK";
            default -> throw new IllegalStateException("未审计的 BHE OBJECTPOS: " + description);
        };
    }

    private String mapObjectPos2Description(String description) {
        if (targetRegistry.find("OBJECTPOS2", description) != null) {
            return description;
        }
        return switch (description) {
            case "CENTER", "TOP" -> "NONE";
            default -> throw new IllegalStateException("未审计的 BHE OBJECTPOS2 aux: " + description);
        };
    }

    private ConversionPlan unconditional() {
        TargetStep step = targetRegistry.require("PARENT", "UNCONDITIONAL");
        return new ConversionPlan(step.groupIndex(), List.of(step.itemIndex()), new ArrayList<>());
    }

    private BsdxInfoCollection emptyCollection() {
        BsdxInfoCollection target = new BsdxInfoCollection();
        target.setInt1(0);
        target.getTypeList().add(0);
        target.setInt2(0);
        return target;
    }

    private BheInfoCollection toBheCollection(BsdxInfoCollection source) {
        BheInfoCollection target = new BheInfoCollection();
        target.setInt1(source.getInt1());
        target.setTypeList(copyList(source.getTypeList()));
        target.setParamList(copyList(source.getParamList()));
        target.setIntList3(copyList(source.getIntList3()));
        target.setIntList4(copyList(source.getIntList4()));
        target.setInt2(source.getInt2());
        return target;
    }

    private List<Integer> copyList(List<Integer> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private record ConversionPlan(
            Integer int1,
            List<Integer> typeList,
            List<Integer> paramList
    ) {
    }

    private record TargetStep(
            int groupIndex,
            int itemIndex
    ) {
    }

    private static class TermRegistry {
        private final TermGrp termGrp;
        private final Set<String> keys = new HashSet<>();

        private TermRegistry(TermGrp termGrp) {
            this.termGrp = termGrp;
            for (int groupIndex = 0; groupIndex < termGrp.getTermList().size(); groupIndex++) {
                TermGrp.TermGroup group = termGrp.getTermList().get(groupIndex);
                for (int itemIndex = 0; itemIndex < group.getTermItemList().size(); itemIndex++) {
                    TermGrp.TermItem item = group.getTermItemList().get(itemIndex);
                    keys.add(key(group.getTermGroupCodeName(), item.getTermItemDescription()));
                }
            }
        }

        private TargetStep find(String groupCodeName, String itemDescription) {
            if (!keys.contains(key(groupCodeName, itemDescription))) {
                return null;
            }
            for (int groupIndex = 0; groupIndex < termGrp.getTermList().size(); groupIndex++) {
                TermGrp.TermGroup group = termGrp.getTermList().get(groupIndex);
                if (!group.getTermGroupCodeName().equals(groupCodeName)) {
                    continue;
                }
                for (int itemIndex = 0; itemIndex < group.getTermItemList().size(); itemIndex++) {
                    TermGrp.TermItem item = group.getTermItemList().get(itemIndex);
                    if (item.getTermItemDescription().equals(itemDescription)) {
                        return new TargetStep(groupIndex, itemIndex);
                    }
                }
            }
            return null;
        }

        private TargetStep require(String groupCodeName, String itemDescription) {
            TargetStep step = find(groupCodeName, itemDescription);
            if (step == null) {
                throw new IllegalStateException("BSDX term.grp 缺少目标语义: " + groupCodeName + "/" + itemDescription);
            }
            return step;
        }

        private String key(String groupCodeName, String itemDescription) {
            return groupCodeName + "/" + itemDescription;
        }
    }
}
