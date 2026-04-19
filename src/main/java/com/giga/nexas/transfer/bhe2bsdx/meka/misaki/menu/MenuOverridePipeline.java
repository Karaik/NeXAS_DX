package com.giga.nexas.transfer.bhe2bsdx.meka.misaki.menu;

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


public class MenuOverridePipeline {

    
    private static final String CHARSET = "windows-31j";

    
    private final ResolveMenuSlotStep resolveMenuSlotStep;

    
    private final PatchMenuDatStep patchMenuDatStep;

    
    private final PatchMenuSpmStep patchMenuSpmStep;

    
    private final CopyMenuImagesStep copyMenuImagesStep;

    
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
        MenuOverrideContext context = new MenuOverrideContext(
                request,
                tsukuyomiPackage,
                bsdxBaseline,
                grpAppendPlan,
                menuMek,
                outputRoot,
                spec
        );
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
