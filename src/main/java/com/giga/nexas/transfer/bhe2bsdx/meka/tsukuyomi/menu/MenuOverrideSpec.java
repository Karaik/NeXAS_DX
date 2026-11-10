package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Tsukuyomi 菜单覆写的最小输入 profile。
 *
 * <p>调用方负责传入具体槽位和 PNG 文件名。pipeline 只消费本对象，
 * 不在主流程里写死菜单素材路径。</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MenuOverrideSpec {

    /**
     * `SelectMekaMenu.dat` 中要覆写的可见菜单行号。
     *
     * <p>这是 zero-based 行号。第 26 个可见槽位应传入 25。</p>
     */
    private int visibleSlotIndex;

    /**
     * 用来借菜单状态值的 donor 行号。
     */
    private Integer stateDonorRowIndex;

    /**
     * `MekaPilot.spm` 目标 anim 对应的 PNG 列表。
     */
    private List<String> mekaPilotImageNames;

    /**
     * `SelectMekaMenuMeka.spm` 目标 anim 对应的 PNG 列表。
     */
    private List<String> selectMenuMekaImageNames;

    /**
     * `MekaPilot.spm` 的布局策略。
     */
    private MenuLayoutPolicy mekaPilotLayoutPolicy;

    /**
     * `SelectMekaMenuMeka.spm` 的布局策略。
     */
    private MenuLayoutPolicy selectMenuMekaLayoutPolicy;

}
