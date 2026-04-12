package com.giga.nexas.transfer.jinki2bsdx.v2.menu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 菜单后置覆盖的最小输入 profile。
 *
 * <p>这个对象故意不承载 MEK/WAZ 主链信息，也不承载 exe patch 信息。
 * 它只描述“菜单层要覆盖哪个 BSDX 槽位、状态从哪行借、用哪几张 MOD PNG、图片如何对齐”。</p>
 *
 * <p>后续换角色或换源游戏时，优先替换这个 spec，而不是改菜单重建算法。
 * 这就是把客制化输入从主线 step 里剥离出来的第一层边界。</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MenuOverrideSpec {

    /**
     * `SelectMekaMenu.dat` 中要覆写的可见菜单行号。
     *
     * <p>这是玩家选机菜单上实际展示的槽位，不是目标 MEK group index。
     * ResolveMenuSlotStep 会从这一行读取原 donor 机体 id、菜单 anim index，
     * 后续 PatchMenuDatStep 再把这一行改成新追加的 target meka index。</p>
     */
    private int visibleSlotIndex;

    /**
     * 用来借菜单状态值的 donor 行号。
     *
     * <p>BSDX 的 `SelectMekaMenu.dat` 第 2 列会影响菜单状态/显示分支。
     * 非 null 时从 donor row 读取状态；null 时使用 visibleSlotIndex 对应行自己的状态。</p>
     */
    private Integer stateDonorRowIndex;

    /**
     * `MekaPilot.spm` 目标 anim 对应的 PNG 文件名列表。
     *
     * <p>列表顺序必须和目标 anim 的 pat/page 顺序一致。
     * 换图时改这里或替换外部资源目录中的同名 PNG；
     * MenuSpmImageChainRebuilder 会读取 PNG 尺寸并重算菜单 SPM 的 page/chip rect。</p>
     */
    private List<String> mekaPilotImageNames;

    /**
     * `SelectMekaMenuMeka.spm` 目标 anim 对应的 PNG 文件名列表。
     *
     * <p>这里控制选机菜单里的机体图。当前 AKAO 是两张图，
     * 因此列表必须有两个元素；数量不匹配会在重建图链时直接失败。</p>
     */
    private List<String> selectMenuMekaImageNames;

    /**
     * `MekaPilot.spm` 的贴图布局策略。
     *
     * <p>当前使用历史 pilot 动画样本的中心 X / 底边 Y 中位数作为锚点，
     * 目的是让不同尺寸的机师图仍然站在 BSDX 菜单里合理的位置。</p>
     */
    private MenuLayoutPolicy mekaPilotLayoutPolicy;

    /**
     * `SelectMekaMenuMeka.spm` 的贴图布局策略。
     *
     * <p>当前使用原点居中，适合选机菜单机体图。
     * 如果某台机体需要额外偏移，应扩展 spec/policy，而不是直接在 rebuilder 写死 rect。</p>
     */
    private MenuLayoutPolicy selectMenuMekaLayoutPolicy;

    /**
     * 当前 AKAO 迁移的固定菜单输入。
     *
     * <p>这些值来自已经跑通的旧 pipeline：
     * 可见槽位 index=24、state donor row=23、两张 pilot 图、两张 select menu 图。
     * V2 第一阶段必须 byte-identical 复刻这些输入，不能在这里顺手“泛化”。</p>
     */
    public static MenuOverrideSpec defaultAkaoMenuOverride() {
        MenuOverrideSpec spec = new MenuOverrideSpec();

        // TODO 客制化入口：如果要把角色绑到另一个可见菜单槽，优先改这里的 visibleSlotIndex。
        // 这个值对应 SelectMekaMenu.dat 的行号；ResolveMenuSlotStep 会从该行推导 mekaId / animIndex。
        // 不要把目标槽位硬编码到 ResolveMenuSlotStep 或 PatchMenuDatStep，否则下一台机体会很难复用。
        spec.visibleSlotIndex = 24;

        // TODO 客制化入口：如果 state 需要从别的 donor 行借，改这里的 stateDonorRowIndex。
        // null 表示直接使用 visibleSlotIndex 那一行自己的 state；非 null 表示从指定 donor row 第 2 列读取。
        // 这类 DAT 魔法行号属于“前置客制化输入”，不要散落到菜单 DAT patch 逻辑里。
        spec.stateDonorRowIndex = 23;

        // TODO 客制化入口：替换 MekaPilot.spm 的机师菜单图时，只改这组文件名或替换 externalStaticAssetRoot 下的同名 PNG。
        // 顺序必须和目标 anim 的 pat/page 顺序一致；当前 AKAO 是两张图，所以这里也是两个文件名。
        // MenuSpmImageChainRebuilder 会重新读取 PNG 宽高，并自动重算 pageWidth/pageHeight、pageRect、chip src/dst rect。
        // 这个“换图后自适应尺寸”的能力只覆盖菜单后置流程里的 MekaPilot.spm，不代表主战斗 SPM 也会自动适配。
        spec.mekaPilotImageNames = List.of(
                "MOD_001_HELL_AKAO_001.png",
                "MOD_001_HELL_MEKA_AKAO_001.png"
        );

        // TODO 客制化入口：替换 SelectMekaMenuMeka.spm 的选机菜单机体图时，只改这组文件名或替换同名 PNG。
        // 同样要求图片数量和目标 anim 的 page 数量一致；尺寸变化会在重建 SPM 图链时自动读 PNG 并重算。
        // 如果只是换图片，不需要手写 page/chip 的宽高和 rect 魔法值；如果要微调站位，应新增明确的 layout/offset 字段再在 rebuilder 中处理。
        spec.selectMenuMekaImageNames = List.of(
                "MOD_001_SelectMekaMenuMeka_Moribito_2_001.png",
                "MOD_001_SelectMekaMenuMeka_Moribito_2_002.png"
        );

        // TODO 客制化入口：如果新角色菜单站位规则不同，先新增 MenuLayoutPolicy 或在 spec 增加显式 offset 字段。
        // 不要在 MenuSpmImageChainRebuilder 里临时写死某台机体的 rect 数字；那会让后续 BHE/其他角色难以继承。
        spec.mekaPilotLayoutPolicy = MenuLayoutPolicy.MEKA_PILOT_MEDIAN_ANCHOR;
        spec.selectMenuMekaLayoutPolicy = MenuLayoutPolicy.ORIGIN_CENTER;
        return spec;
    }
}
