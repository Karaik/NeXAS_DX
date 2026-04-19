package com.giga.nexas.transfer.bhe2bsdx.meka.motoki.menu;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import lombok.Data;

import java.nio.file.Path;
import java.util.Objects;


@Data
public class MenuOverrideContext {

    
    private final TsukuyomiGraftRequest request;

    
    private final TsukuyomiPackageBundle tsukuyomiPackage;

    
    private final TsukuyomiBsdxBaselineBundle bsdxBaseline;

    
    private final TsukuyomiGrpAppendPlan grpAppendPlan;

    
    private final Mek menuMek;

    
    private final Path outputRoot;

    
    private final MenuOverrideSpec spec;

    
    private MenuSlotMapping slotMapping;

    
    private Dat patchedMekaDat;

    
    private Dat patchedMekaPilotDat;

    
    private Dat patchedSelectMekaMenuDat;

    
    private Spm patchedMekaPilotSpm;

    
    private Spm patchedSelectMekaMenuMekaSpm;

    
    private final MenuOverrideAudit audit;

    public MenuOverrideContext(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            Mek menuMek,
            Path outputRoot,
            MenuOverrideSpec spec
    ) {
        this.request = request;
        this.tsukuyomiPackage = tsukuyomiPackage;
        this.bsdxBaseline = bsdxBaseline;
        this.grpAppendPlan = grpAppendPlan;
        this.menuMek = menuMek;
        this.outputRoot = outputRoot;
        this.spec = Objects.requireNonNull(spec, "菜单覆盖 spec 不能为空，调用方必须显式传入槽位和 PNG 清单");
        this.audit = new MenuOverrideAudit();
    }
}
