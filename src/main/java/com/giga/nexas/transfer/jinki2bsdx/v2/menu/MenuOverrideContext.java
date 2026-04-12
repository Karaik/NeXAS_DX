package com.giga.nexas.transfer.jinki2bsdx.v2.menu;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;
import lombok.Data;

import java.nio.file.Path;

/**
 * 菜单 override pipeline 的内部上下文。
 *
 * <p>它把稳定输入和阶段产物放在一个对象里，避免每个 step 都扩一长串参数。
 * 但它仍然只服务菜单后置覆盖，不承载完整 graft 主线，也不承载 exe/pac 状态。</p>
 *
 * <p>构造器只接收输入字段；slot mapping、patched dat/spm 等结果必须由对应 step 写入。
 * 这能让测试明确知道每个 step 的产出边界。</p>
 */
@Data
public class MenuOverrideContext {

    /**
     * 本次 graft 的顶层请求。
     *
     * <p>菜单层主要使用其中的外部静态资源目录和菜单 patch 开关；
     * 不在这里解析主 MEK/WAZ 资源入口，避免菜单层反向污染通用 graft 主线。</p>
     */
    private final AkaoGraftRequest request;

    /**
     * 已加载的 JINKI 源资产包。
     *
     * <p>菜单 DAT patch 需要从源侧 `Meka.dat` 中找到 BSDX 没有的 source-only 行，
     * 用作追加目标机体的 DAT 模板。</p>
     */
    private final JinkiPackageBundle jinkiPackage;

    /**
     * 当前 BSDX baseline 资产包。
     *
     * <p>菜单覆盖以 baseline 的 `Meka.dat`、`MekaPilot.dat`、
     * `SelectMekaMenu.dat`、`MekaPilot.spm`、`SelectMekaMenuMeka.spm` 为模板。</p>
     */
    private final BsdxBaselineBundle bsdxBaseline;

    /**
     * GRP append 阶段产出的索引映射结果。
     *
     * <p>菜单 DAT 最终写入的新机体 id 取自这里的 `mekaGroupIndex`，
     * 保证菜单指向的目标 id 和主 graft 追加的 MEK group id 一致。</p>
     */
    private final GrpAppendPlan grpAppendPlan;

    /**
     * 已重绑后的目标 MEK 对象。
     *
     * <p>菜单 SPM animName 会从这里读取机师名、机体名等展示文本；
     * 它不是菜单图链的数据源。</p>
     */
    private final Mek menuMek;

    /**
     * 菜单 DAT/SPM/PNG 的最终输出目录。
     *
     * <p>必须和主 graft 输出目录一致，因为后续 `Update3.pac` 只打包这一棵目录。</p>
     */
    private final Path outputRoot;

    /**
     * 菜单后置覆盖的客制化输入。
     *
     * <p>包括可见槽位、donor 行、目标 PNG 文件名和布局策略。
     * 换角色/换图优先改 spec，而不是改 step 算法。</p>
     */
    private final MenuOverrideSpec spec;

    /**
     * ResolveMenuSlotStep 推导出的菜单槽位关系。
     *
     * <p>后续 DAT 和 SPM step 都消费同一份 mapping，避免各 step 各自重新推导导致不一致。</p>
     */
    private MenuSlotMapping slotMapping;

    /**
     * patch 后的 `Meka.dat`。
     *
     * <p>它在 baseline 基础上追加或替换目标机体行，最终写回输出目录覆盖旧同名文件。</p>
     */
    private Dat patchedMekaDat;

    /**
     * patch 后的 `MekaPilot.dat`。
     *
     * <p>它把 donor pilot 行第 0 列改成 target meka index，让菜单 pilot 表指向新机体。</p>
     */
    private Dat patchedMekaPilotDat;

    /**
     * patch 后的 `SelectMekaMenu.dat`。
     *
     * <p>它把可见菜单行改成 target meka index，并写回 mapping 推导出的 anim/state。</p>
     */
    private Dat patchedSelectMekaMenuDat;

    /**
     * patch 后的 `MekaPilot.spm`。
     *
     * <p>只重建 mapping 指向的 pilot anim，对应 PNG 来自 `MenuOverrideSpec.mekaPilotImageNames`。</p>
     */
    private Spm patchedMekaPilotSpm;

    /**
     * patch 后的 `SelectMekaMenuMeka.spm`。
     *
     * <p>只重建 mapping 指向的 select-menu anim，对应 PNG 来自
     * `MenuOverrideSpec.selectMenuMekaImageNames`。</p>
     */
    private Spm patchedSelectMekaMenuMekaSpm;

    /**
     * 菜单阶段审计信息。
     *
     * <p>不参与生成 bytes，只用于在 parity 失败时快速定位是 slot、DAT、SPM、写文件还是 PNG 复制出了差异。</p>
     */
    private final MenuOverrideAudit audit;

    public MenuOverrideContext(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan,
            Mek menuMek,
            Path outputRoot,
            MenuOverrideSpec spec
    ) {
        this.request = request;
        this.jinkiPackage = jinkiPackage;
        this.bsdxBaseline = bsdxBaseline;
        this.grpAppendPlan = grpAppendPlan;
        this.menuMek = menuMek;
        this.outputRoot = outputRoot;
        this.spec = spec == null ? MenuOverrideSpec.defaultAkaoMenuOverride() : spec;
        this.audit = new MenuOverrideAudit();
    }
}
