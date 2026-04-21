package com.giga.nexas.controller.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Hell 关卡编辑器里的机体速查条目。
 *
 * <p>它属于编辑器引用数据层，负责把 `mekaIndex`、
 * `MekaGroup.grp` 里的名称/代号、以及可选的 `.mek` 文件信息
 * 统一整理成 UI 可直接显示和搜索的一条记录。
 *
 * <p>输入来自 `MekaGroup.grp` 和可选的 `.mek` 解析结果；
 * 输出是供速查表、敌机槽位联动标签、追加关卡辅助填写使用的只读模型。
 */
@Getter
@Builder
public class MekaLookupEntry {

    /**
     * 运行时使用的机体编号，也是 `HellConfig[3..10]` 里出现的值。
     */
    private final int mekaIndex;

    /**
     * `MekaGroup.grp` 中登记的机体名称，通常也是基础文件名。
     */
    private final String mekaName;

    /**
     * `MekaGroup.grp` 中登记的机体代号。
     */
    private final String mekaCodeName;

    /**
     * 会话里可命中的 `.mek` 文件名。
     */
    private final String mekFileName;

    /**
     * `.mek` 内部的显示名；缺失时回退到 `mekaName`。
     */
    private final String displayName;

    /**
     * `.mek` 内部的英文名；缺失时允许为空。
     */
    private final String displayNameEnglish;

    /**
     * 供表格和筛选直接使用的组合标题。
     */
    public String getDisplayTitle() {
        return displayName != null && !displayName.isBlank() ? displayName : mekaName;
    }

    /**
     * 供搜索框做宽松匹配的归一化文本。
     */
    public String getSearchText() {
        return (mekaIndex + " "
                + nullToEmpty(mekaName) + " "
                + nullToEmpty(mekaCodeName) + " "
                + nullToEmpty(displayName) + " "
                + nullToEmpty(displayNameEnglish) + " "
                + nullToEmpty(mekFileName)).toLowerCase();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
