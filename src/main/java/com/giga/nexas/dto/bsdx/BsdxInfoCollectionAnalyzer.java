package com.giga.nexas.dto.bsdx;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.grp.groupmap.TermGrp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * BSDX collection 语义分析器。
 *
 * <p>它的职责只有一件事：
 * 给定一条 {@link BsdxInfoCollection}，结合 `grpBsdxJson/Term.grp.json`，
 * 还原出这条 collection 对应的 DSL 主语法链。
 *
 * <p>当前分析器只处理“语法链如何装配”这一层，不执行游戏逻辑。
 * 也就是说，它负责回答：
 * - 这条 collection 起始于哪个 group
 * - 每一步命中了哪个 item
 * - 下一跳 group 是谁
 * - 终止发生在哪里
 *
 * 但它不直接回答：
 * - 这条 DSL 在战斗逻辑里最终如何生效
 * - int2 的最终逻辑角色
 * - intList3/intList4 在所有宿主中的精确业务语义
 */
public final class BsdxInfoCollectionAnalyzer {

    private static final Path TERM_JSON_PATH = Paths.get("src/main/resources/grpBsdxJson/Term.grp.json");

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static volatile TermGrp cachedTermGrp;

    private BsdxInfoCollectionAnalyzer() {
    }

    public static BsdxInfoCollectionSemantic analyze(BsdxInfoCollection collection) {
        BsdxInfoCollectionSemantic semantic = new BsdxInfoCollectionSemantic();
        semantic.setTermSource(TERM_JSON_PATH.toString());

        if (collection == null) {
            semantic.getWarnings().add("collection is null");
            return semantic;
        }

        semantic.setInt1(collection.getInt1());
        semantic.setInt2(collection.getInt2());
        semantic.setTypeListSnapshot(copyList(collection.getTypeList()));
        semantic.setParamListSnapshot(copyList(collection.getParamList()));
        semantic.setIntList3Snapshot(copyList(collection.getIntList3()));
        semantic.setIntList4Snapshot(copyList(collection.getIntList4()));

        TermGrp termGrp = getTermGrp(semantic);
        if (termGrp == null || termGrp.getTermList() == null || termGrp.getTermList().isEmpty()) {
            return semantic;
        }

        Integer currentGroupIndex = collection.getInt1();
        if (currentGroupIndex == null) {
            semantic.getWarnings().add("int1 is null");
            return semantic;
        }

        semantic.setStartGroupIndex(currentGroupIndex);

        List<Integer> typeList = collection.getTypeList() == null ? List.of() : collection.getTypeList();
        if (typeList.isEmpty()) {
            semantic.getWarnings().add("typeList is empty");
            return semantic;
        }

        List<String> pathByName = new ArrayList<>();
        List<String> pathByDescription = new ArrayList<>();
        int consumedSteps = 0;

        for (int depth = 0; depth < typeList.size(); depth++) {
            /*
             * 主语法链的核心规则：
             * 当前轮并不是“用 typeList 算 group”，而是：
             * 1. 第一轮先站在 int1 指向的 group 上
             * 2. 之后每一轮站在哪个 group，上一步 item 的 param2 说了算
             *
             * 所以 typeList 只负责“在当前组里选哪一个 item”，
             * 而不负责“下一步去哪个组”。
             */
            if (!isValidGroupIndex(termGrp, currentGroupIndex)) {
                semantic.getWarnings().add("group index out of range: " + currentGroupIndex);
                break;
            }

            TermGrp.TermGroup group = termGrp.getTermList().get(currentGroupIndex);
            Integer itemIndex = typeList.get(depth);
            if (itemIndex == null) {
                semantic.getWarnings().add("typeList[" + depth + "] is null");
                break;
            }
            if (!isValidItemIndex(group, itemIndex)) {
                semantic.getWarnings().add("item index out of range: group=" + currentGroupIndex + ", item=" + itemIndex);
                break;
            }

            TermGrp.TermItem item = group.getTermItemList().get(itemIndex);
            BsdxInfoCollectionSemantic.Step step = new BsdxInfoCollectionSemantic.Step();
            step.setDepth(depth);
            step.setGroupIndex(currentGroupIndex);
            step.setTermGroupName(group.getTermGroupName());
            step.setTermGroupCodeName(group.getTermGroupCodeName());
            step.setItemIndex(itemIndex);
            step.setTermItemName(item.getTermItemName());
            step.setTermItemCodeName(item.getTermItemCodeName());
            step.setTermItemDescription(item.getTermItemDescription());
            step.setParam1(item.getParam1());
            step.setParam2(item.getParam2());
            step.setSyntaxByName(buildStepSyntaxByName(group, item));
            step.setSyntaxByDescription(buildStepSyntaxByDescription(group, item));
            semantic.getSteps().add(step);

            if (depth == 0) {
                /*
                 * 只在第一步记录起始 group。
                 * 这个字段后续做文档和迁移对照时很重要，
                 * 因为它定义了整条 DSL 的“根类别”。
                 */
                semantic.setStartGroupCodeName(group.getTermGroupCodeName());
            }

            pathByName.add(step.getSyntaxByName());
            pathByDescription.add(step.getSyntaxByDescription());
            consumedSteps++;

            Integer nextGroupIndex = item.getParam2();
            if (nextGroupIndex == null) {
                semantic.getWarnings().add("step " + depth + " has null param2");
                break;
            }

            semantic.setLastParam1(item.getParam1());
            semantic.setLastParam2(nextGroupIndex);
            currentGroupIndex = nextGroupIndex;

            if (nextGroupIndex < 0) {
                /*
                 * param2 < 0 表示主语法链终止。
                 *
                 * 这里不把剩余 typeList 视为错误，而是保留到 trailingTypeListSnapshot。
                 * 原因是根据反汇编链，主链在负数终止后会直接结束，
                 * raw typeList 仍可能保留尾部残留值。
                 */
                semantic.setTerminated(true);
                if (depth + 1 < typeList.size()) {
                    semantic.setTrailingTypeListSnapshot(copyList(typeList.subList(depth + 1, typeList.size())));
                }
                break;
            }
        }

        semantic.setConsumedStepCount(consumedSteps);
        semantic.setSyntaxPathByName(String.join(" -> ", pathByName));
        semantic.setSyntaxPathByDescription(String.join(" -> ", pathByDescription));

        if (!semantic.isTerminated() && consumedSteps == typeList.size()) {
            semantic.getWarnings().add("typeList consumed without terminal param2");
        }

        return semantic;
    }

    public static TermGrp getCachedTermGrp() {
        BsdxInfoCollectionSemantic temp = new BsdxInfoCollectionSemantic();
        return getTermGrp(temp);
    }

    private static TermGrp getTermGrp(BsdxInfoCollectionSemantic semantic) {
        /*
         * term.grp 是整套 DSL 的定义源。
         * 这里做进程级缓存，避免批量扫描大量 WAZ JSON 时反复解析同一份 term。
         */
        TermGrp local = cachedTermGrp;
        if (local != null) {
            return local;
        }
        synchronized (BsdxInfoCollectionAnalyzer.class) {
            local = cachedTermGrp;
            if (local != null) {
                return local;
            }
            if (!Files.exists(TERM_JSON_PATH)) {
                semantic.getWarnings().add("term json not found: " + TERM_JSON_PATH);
                return null;
            }
            try {
                local = MAPPER.readValue(TERM_JSON_PATH.toFile(), TermGrp.class);
                cachedTermGrp = local;
                return local;
            } catch (IOException e) {
                semantic.getWarnings().add("failed to load term json: " + e.getMessage());
                return null;
            }
        }
    }

    private static boolean isValidGroupIndex(TermGrp termGrp, Integer groupIndex) {
        return groupIndex != null
                && groupIndex >= 0
                && groupIndex < termGrp.getTermList().size();
    }

    private static boolean isValidItemIndex(TermGrp.TermGroup group, Integer itemIndex) {
        return group != null
                && group.getTermItemList() != null
                && itemIndex != null
                && itemIndex >= 0
                && itemIndex < group.getTermItemList().size();
    }

    /*
     * 这两种 syntax 都直接使用 term 内原始字段，不再额外做命名加工：
     * - Name：更适合人类阅读
     * - Description：更适合做稳定语义锚点
     */
    private static String buildStepSyntaxByName(TermGrp.TermGroup group, TermGrp.TermItem item) {
        return safe(group.getTermGroupCodeName()) + "/" + safe(item.getTermItemName());
    }

    private static String buildStepSyntaxByDescription(TermGrp.TermGroup group, TermGrp.TermItem item) {
        return safe(group.getTermGroupCodeName()) + "/" + safe(item.getTermItemDescription());
    }

    private static List<Integer> copyList(List<Integer> source) {
        return source == null ? new ArrayList<>() : new ArrayList<>(source);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
