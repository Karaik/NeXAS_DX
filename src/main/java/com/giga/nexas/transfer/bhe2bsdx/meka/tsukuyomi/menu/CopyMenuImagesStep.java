package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu;

import com.giga.nexas.dto.bsdx.spm.Spm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 复制菜单 SPM 实际引用到的 PNG。
 *
 * <p>复制范围来自 patched SPM 的 imageData，而不是 spec 的四张图硬编码。
 * 这样如果重建规则未来复用了额外图片，输出目录也能跟着 SPM 引用自动补齐。</p>
 *
 * <p>最后仍然校验 spec 指定的四张 MOD PNG 必须存在，保证当前 Tsukuyomi 菜单最小交付物没有漏。</p>
 */
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
                context.getPatchedSelectMekaMenuMekaSpm()
        );
        for (String imageName : imageNames) {
            copyImage(externalRoot, outputRoot, imageName, audit);
        }
        assertRequiredSpecImagesCopied(context.getSpec(), outputRoot);
    }

    public Set<String> collectImageNames(Spm... spms) {
        Set<String> imageNames = new LinkedHashSet<>();
        if (spms == null) {
            return imageNames;
        }
        for (Spm spm : spms) {
            if (spm == null || spm.getImageData() == null) {
                continue;
            }
            for (Spm.SPMImageData imageData : spm.getImageData()) {
                if (imageData == null || imageData.getImageName() == null || imageData.getImageName().isBlank()) {
                    continue;
                }
                imageNames.add(imageData.getImageName());
            }
        }
        return imageNames;
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
