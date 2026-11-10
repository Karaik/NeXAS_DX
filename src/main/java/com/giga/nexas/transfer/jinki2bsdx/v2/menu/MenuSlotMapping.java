package com.giga.nexas.transfer.jinki2bsdx.v2.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 从 BSDX 菜单 dat 关系中推导出的目标槽位。
 *
 * <p>这个 mapping 是 ResolveMenuSlotStep 的产物，后续 DAT/SPM/PNG 三段都共用它。
 * 它只保存整数索引，不保存 dat/spm 对象本身，避免 step 之间偷改大对象导致调试困难。</p>
 *
 * <p>字段保持只读：菜单槽位一旦解析出来，后续步骤只能消费，不能重新解释。</p>
 */
@Getter
@AllArgsConstructor
public class MenuSlotMapping {

    /**
     * `SelectMekaMenu.dat` 中被覆写的行号。
     *
     * <p>它来自 `MenuOverrideSpec.visibleSlotIndex`，后续 PatchMenuDatStep 会在这一行写入
     * 新追加的 target meka index，并保留/回写 anim 和 state。</p>
     */
    private final int selectMenuRowIndex;

    /**
     * 覆写前菜单行指向的 BSDX donor meka id。
     *
     * <p>这个值来自 `SelectMekaMenu.dat` 第 0 列，用来反查 `MekaPilot.dat` 中对应的 pilot 行。
     * 它不是 AKAO 的目标 id；目标 id 来自 `GrpAppendPlan.mekaGroupIndex`。</p>
     */
    private final int sourceMekaIndex;

    /**
     * `SelectMekaMenuMeka.spm` 中要重建的 anim index。
     *
     * <p>这个值来自 `SelectMekaMenu.dat` 第 1 列。PatchMenuSpmStep 会用它找到选机菜单机体图的 anim，
     * 再用 `MenuOverrideSpec.selectMenuMekaImageNames` 重建该 anim 的 page/chip/imageData 链。</p>
     */
    private final int selectMenuAnimIndex;

    /**
     * `SelectMekaMenu.dat` 第 2 列的状态值。
     *
     * <p>可能来自 donor row，也可能来自目标可见行自身。PatchMenuDatStep 会把它写回目标菜单行，
     * 保持旧 pipeline 的菜单状态行为。</p>
     */
    private final int selectMenuState;

    /**
     * `MekaPilot.dat` 中 donor pilot 对应的行号。
     *
     * <p>通过 `sourceMekaIndex` 反查得到。PatchMenuDatStep 会在这一行把第 0 列改成 target meka index。</p>
     */
    private final int pilotRowIndex;

    /**
     * `MekaPilot.spm` 中要重建的 anim index。
     *
     * <p>当前 BSDX 菜单结构中 pilot anim index 与 `MekaPilot.dat` 行号一致，
     * 因此 ResolveMenuSlotStep 直接令它等于 pilotRowIndex，并校验 animData 范围。</p>
     */
    private final int pilotAnimIndex;
}
