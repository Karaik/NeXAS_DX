package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 菜单 override 的后置覆盖流水线。
 *
 * <p>它不参与 MEK/WAZ 主资源闭包推导，而是在主线已经把 Tsukuyomi 挂到目标 group 之后，
 * 根据 BSDX 原菜单 dat/spm 的关系反推出要复用的 UI 槽位，并只覆盖这一组菜单产物。</p>
 *
 * <p>这一层的验收口径是 byte parity：`Meka.dat / MekaPilot.dat / SelectMekaMenu.dat`
 * 和两个菜单 SPM 生成结果必须和旧 pipeline 完全一致，菜单 PNG 集合也必须一致。</p>
 */
public class MenuOverridePipeline {

    /**
     * 菜单 DAT/SPM 生成时使用的字符集。
     *
     * <p>BSDX/TSUKUYOMI 资源文本沿用 Windows 日文编码；这里必须和旧 pipeline 一致，
     * 否则 DAT/SPM 中的日文 animName 或 pilotName bytes 会不同。</p>
     */
    private static final String CHARSET = "windows-31j";

    /**
     * 从 baseline DAT 关系推导可复用菜单槽位的 step。
     *
     * <p>后续所有菜单 DAT/SPM patch 都依赖它产出的 MenuSlotMapping。</p>
     */
    private final ResolveMenuSlotStep resolveMenuSlotStep;

    /**
     * 生成三个菜单 DAT 产物的 step。
     *
     * <p>负责 `Meka.dat`、`MekaPilot.dat`、`SelectMekaMenu.dat` 的行级覆写。</p>
     */
    private final PatchMenuDatStep patchMenuDatStep;

    /**
     * 生成两个菜单 SPM 产物的 step。
     *
     * <p>负责 animName 和菜单图片链重建，不负责复制 PNG 文件。</p>
     */
    private final PatchMenuSpmStep patchMenuSpmStep;

    /**
     * 复制 patched 菜单 SPM 实际引用 PNG 的 step。
     *
     * <p>只复制 imageData 中最终引用到的文件，避免扩大输出集合。</p>
     */
    private final CopyMenuImagesStep copyMenuImagesStep;

    /**
     * BSDX 二进制生成服务。
     *
     * <p>这里统一写出 DAT/SPM，确保序列化逻辑和旧 pipeline 使用同一套 BinService。</p>
     */
    private final BsdxBinService bsdxBinService;

    public MenuOverridePipeline() {
        this(
                new ResolveMenuSlotStep(),
                new PatchMenuDatStep(),
                new PatchMenuSpmStep(),
                new CopyMenuImagesStep(),
                new BsdxBinService()
        );
    }

    public MenuOverridePipeline(
            ResolveMenuSlotStep resolveMenuSlotStep,
            PatchMenuDatStep patchMenuDatStep,
            PatchMenuSpmStep patchMenuSpmStep,
            CopyMenuImagesStep copyMenuImagesStep,
            BsdxBinService bsdxBinService
    ) {
        this.resolveMenuSlotStep = resolveMenuSlotStep;
        this.patchMenuDatStep = patchMenuDatStep;
        this.patchMenuSpmStep = patchMenuSpmStep;
        this.copyMenuImagesStep = copyMenuImagesStep;
        this.bsdxBinService = bsdxBinService;
    }

    public MenuOverrideContext execute(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            Mek menuMek,
            Path outputRoot,
            MenuOverrideSpec spec
    ) {
        // context 是菜单后置覆盖的临时工作台：
        // slot mapping、patched dat/spm、audit 都留在这里，不污染通用 graft 主线上下文。
        MenuOverrideContext context = new MenuOverrideContext(
                request,
                tsukuyomiPackage,
                bsdxBaseline,
                grpAppendPlan,
                menuMek,
                outputRoot,
                spec
        );
        // 顺序不能随意调整：
        // dat patch 需要 slot mapping，spm patch 也需要同一套 mapping；
        // SPM 写出后才复制 imageData 中真实引用到的 PNG。
        resolveMenuSlotStep.resolve(context);
        patchMenuDatStep.patch(context);
        patchMenuSpmStep.patch(context);
        writeOutputs(context);
        copyMenuImagesStep.copy(context);
        return context;
    }

    public void writeOutputs(MenuOverrideContext context) {
        if (context == null || context.getOutputRoot() == null) {
            throw new IllegalArgumentException("菜单输出目录不能为空");
        }
        try {
            Files.createDirectories(context.getOutputRoot());
            writeDat(context.getOutputRoot(), "Meka.dat", context.getPatchedMekaDat(), context.getAudit());
            writeDat(context.getOutputRoot(), "MekaPilot.dat", context.getPatchedMekaPilotDat(), context.getAudit());
            writeDat(context.getOutputRoot(), "SelectMekaMenu.dat", context.getPatchedSelectMekaMenuDat(), context.getAudit());
            writeSpm(context.getOutputRoot(), "MekaPilot.spm", context.getPatchedMekaPilotSpm(), context.getAudit());
            writeSpm(context.getOutputRoot(), "SelectMekaMenuMeka.spm", context.getPatchedSelectMekaMenuMekaSpm(), context.getAudit());
        } catch (IOException e) {
            throw new IllegalStateException("写出菜单 override v2 产物失败", e);
        }
    }

    public void writeDat(Path outputRoot, String fileName, Dat dat, MenuOverrideAudit audit) throws IOException {
        if (dat == null) {
            return;
        }
        Path output = outputRoot.resolve(fileName);
        bsdxBinService.generate(output.toString(), dat, CHARSET);
        if (audit != null) {
            audit.addWrittenFile(output);
        }
    }

    public void writeSpm(Path outputRoot, String fileName, Spm spm, MenuOverrideAudit audit) throws IOException {
        if (spm == null) {
            return;
        }
        Path output = outputRoot.resolve(fileName);
        bsdxBinService.generate(output.toString(), spm, CHARSET);
        if (audit != null) {
            audit.addWrittenFile(output);
        }
    }
}
