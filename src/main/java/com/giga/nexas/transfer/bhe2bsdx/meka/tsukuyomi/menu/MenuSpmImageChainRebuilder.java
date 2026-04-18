package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu;

import com.giga.nexas.dto.bsdx.spm.Spm;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 重建菜单 SPM 的 anim -> page -> chip -> imageData -> PNG 链。
 *
 * <p>菜单图不能只改 imageName。PNG 尺寸变了以后，pageWidth/pageHeight、pageRect、
 * chip src/dst rect 都必须同步改，否则游戏会按旧矩形绘制，出现裁切或错位。</p>
 *
 * <p>存在两种布局策略：
 * MekaPilot 使用历史 pilot 动画样本的中位锚点，保持角色站位；
 * SelectMekaMenuMeka 使用原点居中，保持选机菜单机体图在槽位中心。</p>
 */
public class MenuSpmImageChainRebuilder {

    public record ImageSize(int width, int height) {
    }

    public record AnchorSample(double centerX, double bottom) {
    }

    public record ResolvedImageSlot(int imageDataIndex, boolean appended) {
    }

    public void rebuild(
            Spm target,
            int animIndex,
            List<String> imageNames,
            Path externalRoot,
            MenuLayoutPolicy policy
    ) {
        if (target == null || target.getAnimData() == null || target.getPageData() == null) {
            return;
        }
        if (animIndex < 0 || animIndex >= target.getAnimData().size()) {
            throw new IllegalStateException("菜单 SPM animIndex 越界: " + animIndex);
        }
        if (imageNames == null || imageNames.isEmpty()) {
            return;
        }
        if (externalRoot == null || !Files.exists(externalRoot)) {
            throw new IllegalStateException("外部静态资源目录不存在，无法重建菜单 SPM 贴图链: " + externalRoot);
        }

        Spm.SPMAnimData anim = target.getAnimData().get(animIndex);
        List<Integer> targetPages = collectAnimPageIndices(anim);
        if (targetPages.size() != imageNames.size()) {
            throw new IllegalStateException(
                    "菜单 SPM anim 的 page 数量与目标 PNG 数量不一致: animIndex=" + animIndex
                            + ", pages=" + targetPages.size()
                            + ", images=" + imageNames.size()
            );
        }

        for (int i = 0; i < imageNames.size(); i++) {
            Integer pageIndex = targetPages.get(i);
            if (pageIndex == null || pageIndex < 0 || pageIndex >= target.getPageData().size()) {
                throw new IllegalStateException("菜单 SPM anim 指向无效 page: animIndex=" + animIndex + ", page=" + pageIndex);
            }

            String imageName = imageNames.get(i);
            ImageSize size = readPngSize(externalRoot.resolve(imageName));
            Spm.SPMPageData currentPage = target.getPageData().get(pageIndex);
            Spm.SPMPageData templatePage = selectStructureTemplatePage(target, targetPages, pageIndex);
            Spm.SPMChipData templateChip = firstChip(templatePage);
            ResolvedImageSlot imageSlot = resolveImageSlotForPage(target, currentPage);
            Spm.SPMRect rect = calculateRect(target, animIndex, i, size, policy, templatePage);

            // 这里是“换菜单 PNG 后自动适配尺寸”的核心落点。
            // 对 MekaPilot.spm / SelectMekaMenuMeka.spm 来说，只要 MenuOverrideSpec 指到新 PNG，
            // readPngSize 会读取真实宽高，calculateRect 会根据 layout policy 重算 page/chip rect。
            // 单纯替换机体菜单图时不要手写 pageWidth/pageHeight/srcRect/dstRect；手写只留给有明确逆向依据的特殊站位策略。
            // imageData 槽位可复用时复用，不能复用时才 append。
            // 这样能保持旧 SPM 中非目标 page 对同一 imageNo 的引用不被误改。
            target.getImageData().get(imageSlot.imageDataIndex()).setImageName(imageName);
            target.getPageData().set(
                    pageIndex,
                    rebuildPageFromTemplate(templatePage, templateChip, imageSlot.imageDataIndex(), size, rect)
            );
        }

        target.setNumPageData(target.getPageData().size());
        target.setNumImageData(target.getImageData() == null ? 0 : target.getImageData().size());
        anim.setNumPat(anim.getPatData() == null ? 0 : anim.getPatData().size());
    }

    public List<Integer> collectAnimPageIndices(Spm.SPMAnimData anim) {
        List<Integer> pageIndices = new ArrayList<>();
        if (anim == null || anim.getPatData() == null) {
            return pageIndices;
        }
        for (Spm.SPMPatData patData : anim.getPatData()) {
            if (patData == null || patData.getPageNo() == null) {
                continue;
            }
            for (Integer pageNo : patData.getPageNo()) {
                if (pageNo != null) {
                    pageIndices.add(pageNo);
                }
            }
        }
        return pageIndices;
    }

    public ImageSize readPngSize(Path pngPath) {
        if (pngPath == null || !Files.exists(pngPath)) {
            throw new IllegalStateException("缺少菜单 PNG: " + pngPath);
        }
        try {
            BufferedImage image = ImageIO.read(pngPath.toFile());
            if (image == null) {
                throw new IllegalStateException("无法读取菜单 PNG 尺寸: " + pngPath);
            }
            return new ImageSize(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            throw new IllegalStateException("读取菜单 PNG 尺寸失败: " + pngPath, e);
        }
    }

    public ResolvedImageSlot resolveImageSlotForPage(Spm target, Spm.SPMPageData page) {
        if (target.getImageData() == null) {
            target.setImageData(new ArrayList<>());
        }
        Spm.SPMChipData chip = firstChip(page);
        if (chip != null
                && chip.getImageNo() != null
                && chip.getImageNo() >= 0
                && chip.getImageNo() < target.getImageData().size()
                && countImageReferences(target, chip.getImageNo()) == 1) {
            return new ResolvedImageSlot(chip.getImageNo(), false);
        }

        // page 引用的 imageNo 被其他 page 共享时，直接改会污染别的动画。
        // append 新 imageData 是更安全的行为，也能被 final parity 捕捉。
        Spm.SPMImageData imageData = new Spm.SPMImageData();
        target.getImageData().add(imageData);
        return new ResolvedImageSlot(target.getImageData().size() - 1, true);
    }

    public int countImageReferences(Spm target, int imageNo) {
        int count = 0;
        if (target == null || target.getPageData() == null) {
            return count;
        }
        for (Spm.SPMPageData page : target.getPageData()) {
            if (page == null || page.getChipData() == null) {
                continue;
            }
            for (Spm.SPMChipData chip : page.getChipData()) {
                if (chip != null && chip.getImageNo() != null && chip.getImageNo() == imageNo) {
                    count++;
                }
            }
        }
        return count;
    }

    public Spm.SPMRect calculateRect(
            Spm target,
            int animIndex,
            int patIndex,
            ImageSize size,
            MenuLayoutPolicy policy
    ) {
        return calculateRect(target, animIndex, patIndex, size, policy, null);
    }

    public Spm.SPMRect calculateRect(
            Spm target,
            int animIndex,
            int patIndex,
            ImageSize size,
            MenuLayoutPolicy policy,
            Spm.SPMPageData templatePage
    ) {
        if (policy == MenuLayoutPolicy.ORIGIN_CENTER) {
            return calculateOriginCenteredRect(size);
        }
        if (policy == MenuLayoutPolicy.FIT_DONOR_BOX_BOTTOM_CENTER) {
            return calculateFitDonorBoxBottomCenterRect(size, templatePage);
        }
        if (policy == MenuLayoutPolicy.MEKA_PILOT_MEDIAN_ANCHOR) {
            return calculateMekaPilotMedianRect(target, animIndex, patIndex, size);
        }
        throw new IllegalStateException("未知菜单布局策略: " + policy);
    }

    public Spm.SPMRect calculateFitDonorBoxBottomCenterRect(ImageSize size, Spm.SPMPageData templatePage) {
        if (templatePage == null || templatePage.getPageRect() == null) {
            return calculateOriginCenteredRect(size);
        }
        Spm.SPMRect donorRect = templatePage.getPageRect();
        int donorWidth = Math.max(1, donorRect.getRight() - donorRect.getLeft());
        int donorHeight = Math.max(1, donorRect.getBottom() - donorRect.getTop());
        double scale = Math.min(1.0, Math.min(donorWidth / (double) size.width(), donorHeight / (double) size.height()));

        int drawWidth = Math.max(1, (int) Math.round(size.width() * scale));
        int drawHeight = Math.max(1, (int) Math.round(size.height() * scale));
        double donorCenterX = (donorRect.getLeft() + donorRect.getRight()) / 2.0;
        int left = (int) Math.round(donorCenterX - drawWidth / 2.0);
        int bottom = donorRect.getBottom();

        // 使用 donor 原页的中心 X / 底边作为约束框，不写角色专用坐标。
        // PNG 过高时只缩小到 donor 可视框内，避免选人画面只露出脚。
        return newRect(left, bottom - drawHeight, left + drawWidth, bottom);
    }

    public Spm.SPMRect calculateOriginCenteredRect(ImageSize size) {
        // SelectMekaMenuMeka.spm 使用原点居中。
        // 机体选机图需要固定偏移时，不要直接改这里的公式；给 MenuOverrideSpec 增加 offset 字段，
        // 或新增 MenuLayoutPolicy，把“为什么偏移、偏移多少”保留成可审计输入。
        int left = -Math.floorDiv(size.width(), 2);
        int top = -Math.floorDiv(size.height(), 2);
        return newRect(left, top, left + size.width(), top + size.height());
    }

    public Spm.SPMRect calculateMekaPilotMedianRect(Spm target, int animIndex, int patIndex, ImageSize size) {
        List<AnchorSample> samples = collectMekaPilotAnchorSamples(target, animIndex, patIndex);
        if (samples.isEmpty()) {
            throw new IllegalStateException("MekaPilot.spm 中找不到同质锚点样本: animIndex=" + animIndex + ", patIndex=" + patIndex);
        }

        double centerX = median(samples.stream().map(AnchorSample::centerX).toList());
        double bottom = median(samples.stream().map(AnchorSample::bottom).toList());
        // MekaPilot.spm 用历史 pilot 的中心 X / 底边 Y 中位数对齐。
        // 机师图需要特殊站位时，先扩展 MenuOverrideSpec 或新增 LayoutPolicy，
        // 再在这里应用偏移；不要把 rect 魔法数写散到调用方或重建算法里。
        // 以“中心 X + 底边 Y”作为锚点，比直接复制某一帧 rect 更稳：
        // 不同 pilot 图宽高可能不同，但底边和中心位置应该跟既有 pilot 群体保持一致。
        int left = (int) Math.round(centerX - size.width() / 2.0);
        int rectBottom = (int) Math.round(bottom);
        return newRect(left, rectBottom - size.height(), left + size.width(), rectBottom);
    }

    public List<AnchorSample> collectMekaPilotAnchorSamples(Spm target, int animIndex, int patIndex) {
        List<AnchorSample> samples = new ArrayList<>();
        if (target == null || target.getAnimData() == null || target.getPageData() == null) {
            return samples;
        }
        for (int sampleAnimIndex = 0; sampleAnimIndex < target.getAnimData().size() && sampleAnimIndex < animIndex; sampleAnimIndex++) {
            Spm.SPMAnimData anim = target.getAnimData().get(sampleAnimIndex);
            if (anim == null || anim.getPatData() == null || patIndex < 0 || patIndex >= anim.getPatData().size()) {
                continue;
            }
            Spm.SPMPatData patData = anim.getPatData().get(patIndex);
            if (patData == null || patData.getPageNo() == null || patData.getPageNo().isEmpty()) {
                continue;
            }
            Integer pageNo = patData.getPageNo().get(0);
            if (pageNo == null || pageNo < 0 || pageNo >= target.getPageData().size()) {
                continue;
            }
            Spm.SPMPageData page = target.getPageData().get(pageNo);
            if (!hasSingleChip(page)) {
                continue;
            }
            samples.add(toAnchorSample(page));
        }
        return samples;
    }

    public double median(List<Double> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalStateException("median 输入不能为空");
        }
        List<Double> sorted = values.stream().sorted(Comparator.naturalOrder()).toList();
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(mid);
        }
        return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
    }

    public Spm.SPMPageData rebuildPageFromTemplate(
            Spm.SPMPageData templatePage,
            Spm.SPMChipData templateChip,
            int imageNo,
            ImageSize size,
            Spm.SPMRect rect
    ) {
        Spm.SPMPageData page = copyPageData(templatePage);
        page.setNumChipData(1);
        page.setPageWidth(size.width());
        page.setPageHeight(size.height());
        page.setPageRect(copyRect(rect));

        Spm.SPMChipData chip = copyChipData(templateChip);
        chip.setImageNo(imageNo);
        chip.setChipWidth(size.width());
        chip.setChipHeight(size.height());
        chip.setSrcRect(newRect(0, 0, size.width(), size.height()));
        chip.setDstRect(copyRect(rect));
        page.setChipData(new ArrayList<>(List.of(chip)));
        return page;
    }

    public Spm.SPMPageData selectStructureTemplatePage(Spm target, List<Integer> targetPages, int currentPageIndex) {
        Spm.SPMPageData currentPage = target.getPageData().get(currentPageIndex);
        if (hasSingleChip(currentPage)) {
            return currentPage;
        }

        for (Integer pageIndex : targetPages) {
            if (pageIndex == null || pageIndex == currentPageIndex || pageIndex < 0 || pageIndex >= target.getPageData().size()) {
                continue;
            }
            Spm.SPMPageData siblingPage = target.getPageData().get(pageIndex);
            if (hasSingleChip(siblingPage)) {
                return siblingPage;
            }
        }

        for (Spm.SPMPageData page : target.getPageData()) {
            if (hasSingleChip(page)) {
                return page;
            }
        }

        throw new IllegalStateException("菜单 SPM 中找不到可作为模板的非空单 chip page");
    }

    public Spm.SPMChipData firstChip(Spm.SPMPageData page) {
        if (!hasSingleChip(page)) {
            return null;
        }
        return page.getChipData().get(0);
    }

    public boolean hasSingleChip(Spm.SPMPageData page) {
        return page != null && page.getChipData() != null && page.getChipData().size() == 1 && page.getChipData().get(0) != null;
    }

    private AnchorSample toAnchorSample(Spm.SPMPageData page) {
        Spm.SPMRect rect = page.getPageRect();
        return new AnchorSample(
                (rect.getLeft() + rect.getRight()) / 2.0,
                rect.getBottom()
        );
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

    private Spm.SPMRect newRect(int left, int top, int right, int bottom) {
        Spm.SPMRect rect = new Spm.SPMRect();
        rect.setLeft(left);
        rect.setTop(top);
        rect.setRight(right);
        rect.setBottom(bottom);
        return rect;
    }
}
