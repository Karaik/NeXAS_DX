package com.giga.nexas.transfer.bhe2bsdx.meka.sou.menu;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;


public class PatchMenuSpmStep {

    
    private final MenuSpmImageChainRebuilder rebuilder;

    public PatchMenuSpmStep() {
        this(new MenuSpmImageChainRebuilder());
    }

    public PatchMenuSpmStep(MenuSpmImageChainRebuilder rebuilder) {
        this.rebuilder = rebuilder == null ? new MenuSpmImageChainRebuilder() : rebuilder;
    }

    public void patch(MenuOverrideContext context) {
        if (context == null) {
            throw new IllegalArgumentException("MenuOverrideContext 不能为空");
        }
        MenuSlotMapping mapping = context.getSlotMapping();
        if (mapping == null) {
            throw new IllegalStateException("缺少菜单槽位映射，无法 patch 菜单 SPM");
        }

        MenuOverrideSpec spec = context.getSpec();
        Path externalRoot = context.getRequest() == null ? null : context.getRequest().getExternalStaticAssetRoot();
        Spm patchedMekaPilotSpm = patchMekaPilotSpm(
                context.getBsdxBaseline(),
                mapping,
                context.getMenuMek(),
                spec,
                externalRoot
        );
        Spm patchedSelectMenuMekaSpm = patchSelectMenuMekaSpm(
                context.getBsdxBaseline(),
                mapping,
                context.getMenuMek(),
                spec,
                externalRoot
        );

        context.setPatchedMekaPilotSpm(patchedMekaPilotSpm);
        context.setPatchedSelectMekaMenuMekaSpm(patchedSelectMenuMekaSpm);
        context.getAudit().addSpmPatchNote("patched MekaPilot.spm anim " + mapping.getPilotAnimIndex());
        context.getAudit().addSpmPatchNote("patched SelectMekaMenuMeka.spm anim " + mapping.getSelectMenuAnimIndex());
    }

    public Spm patchMekaPilotSpm(
            TsukuyomiBsdxBaselineBundle baseline,
            MenuSlotMapping mapping,
            Mek menuMek,
            MenuOverrideSpec spec,
            Path externalRoot
    ) {
        if (baseline == null || baseline.getMekaPilotSpm() == null) {
            return null;
        }
        Spm target = copySpm(baseline.getMekaPilotSpm());
        updateAnimName(target, mapping.getPilotAnimIndex(), buildPilotAnimName(menuMek));
        rebuilder.rebuild(
                target,
                mapping.getPilotAnimIndex(),
                spec.getMekaPilotImageNames(),
                externalRoot,
                spec.getMekaPilotLayoutPolicy()
        );
        return target;
    }

    public Spm patchSelectMenuMekaSpm(
            TsukuyomiBsdxBaselineBundle baseline,
            MenuSlotMapping mapping,
            Mek menuMek,
            MenuOverrideSpec spec,
            Path externalRoot
    ) {
        if (baseline == null || baseline.getSelectMekaMenuMekaSpm() == null) {
            return null;
        }
        Spm target = copySpm(baseline.getSelectMekaMenuMekaSpm());
        updateAnimName(target, mapping.getSelectMenuAnimIndex(), buildSelectMenuAnimName(menuMek));
        rebuilder.rebuild(
                target,
                mapping.getSelectMenuAnimIndex(),
                spec.getSelectMenuMekaImageNames(),
                externalRoot,
                spec.getSelectMenuMekaLayoutPolicy()
        );
        return target;
    }

    public void updateAnimName(Spm spm, int animIndex, String animName) {
        if (spm == null || spm.getAnimData() == null) {
            return;
        }
        if (animIndex < 0 || animIndex >= spm.getAnimData().size()) {
            return;
        }
        Spm.SPMAnimData anim = spm.getAnimData().get(animIndex);
        if (anim == null) {
            return;
        }
        anim.setAnimName(animName);
        anim.setNumPat(anim.getPatData() == null ? 0 : anim.getPatData().size());
    }

    public String buildPilotAnimName(Mek menuMek) {
        if (menuMek == null || menuMek.getMekBasicInfo() == null) {
            return "";
        }
        return safeTrim(menuMek.getMekBasicInfo().getPilotNameKanji());
    }

    public String buildSelectMenuAnimName(Mek menuMek) {
        if (menuMek == null || menuMek.getMekBasicInfo() == null) {
            return "";
        }

        String pilot = safeTrim(menuMek.getMekBasicInfo().getPilotNameKanji());
        String mekaName = safeTrim(menuMek.getMekBasicInfo().getMekName());
        String pilotRoma = safeTrim(menuMek.getMekBasicInfo().getPilotNameRoma());
        String mekaRoma = safeTrim(menuMek.getMekBasicInfo().getMekNameEnglish());

        StringBuilder builder = new StringBuilder();
        if (!pilot.isEmpty()) {
            builder.append(pilot);
        }
        if (!mekaName.isEmpty()) {
            if (builder.length() > 0) {
                builder.append("/");
            }
            builder.append(mekaName);
        }

        String roma = joinNonEmpty(pilotRoma, mekaRoma);
        if (!roma.isEmpty()) {
            builder.append("：").append(roma);
        }
        return builder.toString();
    }

    public Spm copySpm(Spm source) {
        Spm target = new Spm();
        target.setExtensionName(source.getExtensionName());
        target.setSpmVersion(source.getSpmVersion());
        target.setPatPageNum(source.getPatPageNum());

        List<Spm.SPMImageData> imageData = new ArrayList<>();
        if (source.getImageData() != null) {
            for (Spm.SPMImageData image : source.getImageData()) {
                imageData.add(copyImageData(image));
            }
        }
        target.setImageData(imageData);
        target.setNumImageData(imageData.size());

        List<Spm.SPMPageData> pageData = new ArrayList<>();
        if (source.getPageData() != null) {
            for (Spm.SPMPageData page : source.getPageData()) {
                pageData.add(copyPageData(page));
            }
        }
        target.setPageData(pageData);
        target.setNumPageData(pageData.size());

        List<Spm.SPMAnimData> animData = new ArrayList<>();
        if (source.getAnimData() != null) {
            for (Spm.SPMAnimData anim : source.getAnimData()) {
                animData.add(copyAnimData(anim));
            }
        }
        target.setAnimData(animData);
        target.setNumAnimData(animData.size());
        return target;
    }

    private Spm.SPMImageData copyImageData(Spm.SPMImageData source) {
        Spm.SPMImageData target = new Spm.SPMImageData();
        if (source != null) {
            target.setImageName(source.getImageName());
        }
        return target;
    }

    private Spm.SPMPageData copyPageData(Spm.SPMPageData source) {
        Spm.SPMPageData target = new Spm.SPMPageData();
        if (source == null) {
            return target;
        }
        target.setNumChipData(source.getNumChipData());
        target.setPageWidth(source.getPageWidth());
        target.setPageHeight(source.getPageHeight());
        target.setPageRect(copyRect(source.getPageRect()));
        target.setPageOption(source.getPageOption());
        target.setRotateCenterX(source.getRotateCenterX());
        target.setRotateCenterY(source.getRotateCenterY());
        target.setHitFlag(source.getHitFlag());
        target.setUnk3(source.getUnk3());

        List<Spm.SPMHitArea> hitRects = new ArrayList<>();
        if (source.getHitRects() != null) {
            for (Spm.SPMHitArea hitArea : source.getHitRects()) {
                hitRects.add(copyHitArea(hitArea));
            }
        }
        target.setHitRects(hitRects);

        List<Spm.SPMChipData> chipData = new ArrayList<>();
        if (source.getChipData() != null) {
            for (Spm.SPMChipData chip : source.getChipData()) {
                chipData.add(copyChipData(chip));
            }
        }
        target.setChipData(chipData);
        target.setNumChipData(chipData.size());
        return target;
    }

    private Spm.SPMChipData copyChipData(Spm.SPMChipData source) {
        Spm.SPMChipData target = new Spm.SPMChipData();
        if (source == null) {
            return target;
        }
        target.setImageNo(source.getImageNo());
        target.setDstRect(copyRect(source.getDstRect()));
        target.setChipWidth(source.getChipWidth());
        target.setChipHeight(source.getChipHeight());
        target.setSrcRect(copyRect(source.getSrcRect()));
        target.setDrawOption(source.getDrawOption());
        target.setUnk5(source.getUnk5());
        target.setDrawOptionValue(source.getDrawOptionValue());
        target.setOption(source.getOption());
        return target;
    }

    private Spm.SPMAnimData copyAnimData(Spm.SPMAnimData source) {
        Spm.SPMAnimData target = new Spm.SPMAnimData();
        if (source == null) {
            return target;
        }
        target.setAnimName(source.getAnimName());
        target.setNumPat(source.getNumPat());
        target.setAnimRotateDirection(source.getAnimRotateDirection());
        target.setAnimReverseDirection(source.getAnimReverseDirection());

        List<Spm.SPMPatData> patData = new ArrayList<>();
        if (source.getPatData() != null) {
            for (Spm.SPMPatData pat : source.getPatData()) {
                Spm.SPMPatData copiedPat = new Spm.SPMPatData();
                copiedPat.setWaitFrame(pat == null ? null : pat.getWaitFrame());
                copiedPat.setPageNo(pat == null || pat.getPageNo() == null ? new ArrayList<>() : new ArrayList<>(pat.getPageNo()));
                patData.add(copiedPat);
            }
        }
        target.setPatData(patData);
        target.setNumPat(patData.size());
        return target;
    }

    private Spm.SPMHitArea copyHitArea(Spm.SPMHitArea source) {
        Spm.SPMHitArea target = new Spm.SPMHitArea();
        if (source == null) {
            return target;
        }
        target.setUnk0(source.getUnk0());
        target.setHitRect(copyRect(source.getHitRect()));
        target.setUnk1(source.getUnk1());
        target.setUnk2(source.getUnk2());
        return target;
    }

    private Spm.SPMRect copyRect(Spm.SPMRect source) {
        Spm.SPMRect target = new Spm.SPMRect();
        if (source == null) {
            return target;
        }
        target.setLeft(source.getLeft());
        target.setTop(source.getTop());
        target.setRight(source.getRight());
        target.setBottom(source.getBottom());
        return target;
    }

    private String joinNonEmpty(String left, String right) {
        if (left == null || left.isBlank()) {
            return right == null ? "" : right;
        }
        if (right == null || right.isBlank()) {
            return left;
        }
        return left + "/" + right;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }
}
