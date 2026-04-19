package com.giga.nexas.transfer.bhe2bsdx.meka.freja.menu;

import com.giga.nexas.dto.bsdx.spm.Spm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class CopyMenuImagesStep {

    public void copy(MenuOverrideContext context) {
        if (context == null) {
            throw new IllegalArgumentException("MenuOverrideContext 不能为空");
        }
        Path externalRoot = context.getRequest() == null ? null : context.getRequest().getExternalStaticAssetRoot();
        Path outputRoot = context.getOutputRoot();
        MenuOverrideAudit audit = context.getAudit();

        Set<String> imageNames = collectImageNames(
                context.getPatchedMekaPilotSpm(),
                context.getSlotMapping() == null ? -1 : context.getSlotMapping().getPilotAnimIndex(),
                context.getPatchedSelectMekaMenuMekaSpm(),
                context.getSlotMapping() == null ? -1 : context.getSlotMapping().getSelectMenuAnimIndex()
        );
        for (String imageName : imageNames) {
            copyImage(externalRoot, outputRoot, imageName, audit);
        }
        assertRequiredSpecImagesCopied(context.getSpec(), outputRoot);
    }

    public Set<String> collectImageNames(
            Spm pilotSpm,
            int pilotAnimIndex,
            Spm selectMenuMekaSpm,
            int selectMenuAnimIndex
    ) {
        Set<String> imageNames = new LinkedHashSet<>();
        collectImageNamesFromAnim(pilotSpm, pilotAnimIndex, imageNames);
        collectImageNamesFromAnim(selectMenuMekaSpm, selectMenuAnimIndex, imageNames);
        return imageNames;
    }

    private void collectImageNamesFromAnim(Spm spm, int animIndex, Set<String> imageNames) {
        if (spm == null || spm.getImageData() == null || spm.getPageData() == null || spm.getAnimData() == null
                || animIndex < 0 || animIndex >= spm.getAnimData().size()) {
            return;
        }
        Spm.SPMAnimData anim = spm.getAnimData().get(animIndex);
        if (anim == null || anim.getPatData() == null) {
            return;
        }
        for (Spm.SPMPatData pat : anim.getPatData()) {
            if (pat == null || pat.getPageNo() == null) {
                continue;
            }
            for (Integer pageNo : pat.getPageNo()) {
                collectImageNamesFromPage(spm, pageNo, imageNames);
            }
        }
    }

    private void collectImageNamesFromPage(Spm spm, Integer pageNo, Set<String> imageNames) {
        if (pageNo == null || pageNo < 0 || pageNo >= spm.getPageData().size()) {
            return;
        }
        Spm.SPMPageData page = spm.getPageData().get(pageNo);
        if (page == null || page.getChipData() == null) {
            return;
        }
        for (Spm.SPMChipData chip : page.getChipData()) {
            if (chip == null || chip.getImageNo() == null
                    || chip.getImageNo() < 0 || chip.getImageNo() >= spm.getImageData().size()) {
                continue;
            }
            Spm.SPMImageData imageData = spm.getImageData().get(chip.getImageNo());
            if (imageData == null || imageData.getImageName() == null || imageData.getImageName().isBlank()) {
                continue;
            }
            imageNames.add(imageData.getImageName());
        }
    }

    public void copyImage(Path sourceRoot, Path outputRoot, String imageName, MenuOverrideAudit audit) {
        if (sourceRoot == null || outputRoot == null || imageName == null || imageName.isBlank()) {
            return;
        }
        Path source = sourceRoot.resolve(imageName);
        if (!Files.exists(source)) {
            if (audit != null) {
                audit.addMissingImage(imageName);
            }
            return;
        }
        Path output = outputRoot.resolve(source.getFileName().toString());
        try {
            Files.createDirectories(outputRoot);
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            if (audit != null) {
                audit.addCopiedImage(output);
            }
        } catch (IOException e) {
            throw new IllegalStateException("复制菜单 PNG 失败: " + imageName, e);
        }
    }

    public void assertRequiredSpecImagesCopied(MenuOverrideSpec spec, Path outputRoot) {
        if (spec == null || outputRoot == null) {
            return;
        }
        assertImagesCopied(spec.getMekaPilotImageNames(), outputRoot);
        assertImagesCopied(spec.getSelectMenuMekaImageNames(), outputRoot);
    }

    private void assertImagesCopied(List<String> imageNames, Path outputRoot) {
        if (imageNames == null) {
            return;
        }
        for (String imageName : imageNames) {
            if (imageName == null || imageName.isBlank()) {
                continue;
            }
            if (!Files.exists(outputRoot.resolve(Path.of(imageName).getFileName().toString()))) {
                throw new IllegalStateException("菜单 MOD PNG 未复制到输出目录: " + imageName);
            }
        }
    }
}
