package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;

/**
 * 把 JINKI 层的结果整理成 BHE 当前层可直接消费的基线视图。
 *
 * <p>这一步放在 {@code convert} 包下，而不是继续耦合在
 * {@code TsukuyomiGraftPipeline} 里，原因是它本质上属于“输入归一化”：
 * 当前层并不关心上一层是怎么跑出来的，只关心上一层交过来的结果，
 * 要如何整理成一份当前层可读的基线资源包。</p>
 *
 * <p>这里的职责不是做新的嫁接，也不是重新跑 JINKI 流程。
 * 它只做一件事：从 {@link AkaoGraftResult} 中挑出当前层需要继承的对象，
 * 按“JINKI 当前层已补丁改写 / 重绑对象优先，缺省时回退到 JINKI 基线原对象”的规则，组一份
 * {@link TsukuyomiBsdxBaselineBundle}。</p>
 *
 * <p>这样做的直接收益是：
 * 后面的 BHE 主线拿到的是一份结构稳定、语义明确的基线，
 * 不需要在主流程编排层里到处展开
 * “这个字段到底取补丁改写后的版本还是基线原值”的判断。</p>
 */
public class BuildBaselineFromJinkiResultStep {

    /**
     * 从 JINKI 结果构造 Tsukuyomi 当前层使用的基线视图。
     *
     * <p>当前规则分成四层：</p>
     *
     * <p>1. 顶层 group/dat/spm/waz/mek 容器先继承 JINKI 基线。</p>
     *
     * <p>2. JINKI 结果里已经产出更晚对象时，当前层必须继承更晚对象，例如：</p>
     * <p>- 补丁改写后的 `Meka.dat` / `MekaPilot.dat` / `SelectMekaMenu.dat`</p>
     * <p>- 补丁改写后的 `MekaPilot.spm` / `SelectMekaMenuMeka.spm`</p>
     * <p>- 已经重绑后的 `AKAO.mek` / `AKAO.waz`</p>
     * <p>那么当前层要优先继承这些“更晚”的对象，而不是回退到基线原值。</p>
     *
     * <p>3. `ProgramMaterial.grp` 也遵循同样原则：</p>
     * <p>JINKI 层完成同步扩容后，当前层优先继承 `syncedProgramMaterial`，
     * 因为那代表真正可运行的外层数组状态。</p>
     *
     * <p>4. `spmByFileName / wazByFileName / mekByFileName` 这些映射表也要同步修正：</p>
     * <p>不能只拷贝基线映射表，还要把 JINKI 已经重绑后的对象覆盖回去，
     * 否则后续按文件名取资源时，会取到旧对象而不是 JINKI 当前层成果物。</p>
     *
     * <p>注意：这里构建出来的是“当前层消费视图”，不是最终抽象形态。
     * 后续收敛目标是把“上一层资源包直接串给下一层”做成统一继承协议，
     * 当前步骤是这条协议正式抽出前的 Tsukuyomi 专用交接实现。</p>
     */
    public TsukuyomiBsdxBaselineBundle buildBaselineFromJinkiResult(AkaoGraftResult inheritedJinkiResult) {
        TsukuyomiBsdxBaselineBundle inheritedBaseline = new TsukuyomiBsdxBaselineBundle();
        if (inheritedJinkiResult == null || inheritedJinkiResult.getBsdxBaseline() == null) {
            return inheritedBaseline;
        }

        var jinkiBaseline = inheritedJinkiResult.getBsdxBaseline();

        // 第一层：直接继承 JINKI 基线中已经存在的 group 结构。
        // 这些对象代表了“原始 BSDX + JINKI 当前层之前的结构基础”。
        inheritedBaseline.setBatVoiceGrp(jinkiBaseline.getBatVoiceGrp());
        inheritedBaseline.setMapGroupGrp(jinkiBaseline.getMapGroupGrp());
        inheritedBaseline.setMekaGroupGrp(jinkiBaseline.getMekaGroupGrp());
        inheritedBaseline.setSeGroupGrp(jinkiBaseline.getSeGroupGrp());
        inheritedBaseline.setSpriteGroupGrp(jinkiBaseline.getSpriteGroupGrp());
        inheritedBaseline.setWazaGroupGrp(jinkiBaseline.getWazaGroupGrp());

        // ProgramMaterial 的首选输入是 JINKI 当前层同步扩容后的结果。
        // 同步扩容结果存在时，后续数组长度以该版本为准，不回退到旧基线。
        inheritedBaseline.setProgramMaterialGrp(
                inheritedJinkiResult.getSyncedProgramMaterial() != null
                        ? inheritedJinkiResult.getSyncedProgramMaterial()
                        : jinkiBaseline.getProgramMaterialGrp()
        );

        // 第二层：DAT 资源优先继承 JINKI 已经补丁改写过的版本。
        // 这些 DAT 往往已经带了菜单行、slot 覆写等结果，不能再回退到旧基线。
        inheritedBaseline.setMekaDat(
                inheritedJinkiResult.getPatchedMekaDat() != null
                        ? inheritedJinkiResult.getPatchedMekaDat()
                        : jinkiBaseline.getMekaDat()
        );
        inheritedBaseline.setMekaPilotDat(
                inheritedJinkiResult.getPatchedMekaPilotDat() != null
                        ? inheritedJinkiResult.getPatchedMekaPilotDat()
                        : jinkiBaseline.getMekaPilotDat()
        );
        inheritedBaseline.setSelectMekaMenuDat(
                inheritedJinkiResult.getPatchedSelectMekaMenuDat() != null
                        ? inheritedJinkiResult.getPatchedSelectMekaMenuDat()
                        : jinkiBaseline.getSelectMekaMenuDat()
        );

        // WeaponEquip.dat 当前仍然直接继承 JINKI 基线。
        // 后续接入 JINKI sidecar WeaponEquip 结果对象后，这里改为优先继承更晚版本。
        inheritedBaseline.setWeaponEquipDat(jinkiBaseline.getWeaponEquipDat());

        // 第三层：先把基线原有映射表全量带过来，保留文件名 -> 对象 的基本索引面。
        inheritedBaseline.getMekByFileName().putAll(jinkiBaseline.getMekByFileName());
        inheritedBaseline.getSpmByFileName().putAll(jinkiBaseline.getSpmByFileName());
        inheritedBaseline.getWazByFileName().putAll(jinkiBaseline.getWazByFileName());

        // JINKI 当前层重绑后的 AKAO.mek 存在时，用它覆盖回 mek map。
        // 这样后续按文件名取到的就是 JINKI 最新版本，而不是旧基线里的老版本。
        if (inheritedJinkiResult.getReboundAkaoMek() != null
                && inheritedJinkiResult.getReboundAkaoMek().getFileName() != null) {
            inheritedBaseline.getMekByFileName().put(
                    inheritedJinkiResult.getReboundAkaoMek().getFileName(),
                    inheritedJinkiResult.getReboundAkaoMek()
            );
        }

        // WAZ 同理：JINKI 生成的新 AKAO.waz 存在时，必须覆盖回文件名索引表。
        if (inheritedJinkiResult.getReboundAkaoWaz() != null
                && inheritedJinkiResult.getReboundAkaoWaz().getFileName() != null) {
            inheritedBaseline.getWazByFileName().put(
                    inheritedJinkiResult.getReboundAkaoWaz().getFileName(),
                    inheritedJinkiResult.getReboundAkaoWaz()
            );
        }

        // 第四层：菜单相关 SPM 优先继承 JINKI 已经补丁改写过的版本。
        // 这两个对象既要单独挂在基线上，也要同步放回 spmByFileName 映射里。
        inheritedBaseline.setMekaPilotSpm(
                inheritedJinkiResult.getPatchedMekaPilotSpm() != null
                        ? inheritedJinkiResult.getPatchedMekaPilotSpm()
                        : jinkiBaseline.getMekaPilotSpm()
        );
        inheritedBaseline.setSelectMekaMenuMekaSpm(
                inheritedJinkiResult.getPatchedSelectMekaMenuMekaSpm() != null
                        ? inheritedJinkiResult.getPatchedSelectMekaMenuMekaSpm()
                        : jinkiBaseline.getSelectMekaMenuMekaSpm()
        );

        if (inheritedBaseline.getMekaPilotSpm() != null) {
            inheritedBaseline.getSpmByFileName().put("MekaPilot.spm", inheritedBaseline.getMekaPilotSpm());
        }
        if (inheritedBaseline.getSelectMekaMenuMekaSpm() != null) {
            inheritedBaseline.getSpmByFileName().put("SelectMekaMenuMeka.spm", inheritedBaseline.getSelectMekaMenuMekaSpm());
        }

        // TODO 20260417：
        // 现在这一步仍然是“从 JINKI 结果手工拼一个 Tsukuyomi 基线视图”。
        // 后续收敛目标：把“链式成果物直接作为资源包继续向下传”定型为通用交接规则对象。
        return inheritedBaseline;
    }
}
