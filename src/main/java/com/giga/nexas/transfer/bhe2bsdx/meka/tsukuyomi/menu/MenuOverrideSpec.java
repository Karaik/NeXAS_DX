package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Tsukuyomi 菜单覆写的最小输入 profile。
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MenuOverrideSpec {

    /**
     * `SelectMekaMenu.dat` 中要覆写的可见菜单行号。
     *
     * <p>这是 zero-based 行号。当前 Tsukuyomi 占第 26 个可见槽位，
     * 所以这里固定为 25。</p>
     */
    private int visibleSlotIndex;

    /**
     * 用来借菜单状态值的 donor 行号。
     *
     * <p>当前 Tsukuyomi 借可见槽上一行的状态，所以这里固定为 24。</p>
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

    /**
     * 当前 Tsukuyomi 固定菜单输入。
     */
    public static MenuOverrideSpec defaultTsukuyomiMenuOverride() {
        MenuOverrideSpec spec = new MenuOverrideSpec();
        spec.visibleSlotIndex = 25;
        spec.stateDonorRowIndex = 24;
        spec.mekaPilotImageNames = List.of(
                "MOD_001_HELL_TSUKUYOMI_001.png",
                "MOD_001_HELL_MEKA_TSUKUYOMI_001.png"
        );
        spec.selectMenuMekaImageNames = List.of(
                "MOD_001_SelectMekaMenuMeka_Sakurabi_001.png",
                "MOD_001_SelectMekaMenuMeka_Sakurabi_002.png"
        );
        spec.mekaPilotLayoutPolicy = MenuLayoutPolicy.MEKA_PILOT_MEDIAN_ANCHOR;
        spec.selectMenuMekaLayoutPolicy = MenuLayoutPolicy.ORIGIN_CENTER;
        return spec;
    }
}
