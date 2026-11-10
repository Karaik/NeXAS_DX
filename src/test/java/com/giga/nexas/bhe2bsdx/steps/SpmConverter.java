package com.giga.nexas.bhe2bsdx.steps;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.spm.Spm;

import java.util.ArrayList;
import java.util.List;

/**
 * spm 转换：
 * 保留 SPM2.02 结构并迁移 hitbox。
 * - hitRects 使用 BHE 的 transHitbox() 转换到 BSDX hitArea
 * - hitFlag 按 hitRects 数量设置低位 bit
 */
public class SpmConverter {

    public Spm convert(com.giga.nexas.dto.bhe.spm.Spm bheSpm) {
        if (bheSpm == null) {
            return null;
        }
        Spm bsdxSpm = new Spm();

        // 顶层字段拷贝，嵌套列表重建以补齐 BSDX 字段
        BeanUtil.copyProperties(bheSpm, bsdxSpm, "pageData", "imageData", "animData",
                "numPageData", "numImageData", "numAnimData");
        bsdxSpm.setSpmVersion(bsdxSpm.getSpmVersion() != null ? bsdxSpm.getSpmVersion() : "SPM VER-2.02");
        bsdxSpm.setNumPageData(safeSize(bheSpm.getPageData()));
        bsdxSpm.setNumImageData(safeSize(bheSpm.getImageData()));
        bsdxSpm.setNumAnimData(safeSize(bheSpm.getAnimData()));
        bsdxSpm.setPatPageNum(bheSpm.getPatPageNum() != null ? bheSpm.getPatPageNum() : 0);

        if (bheSpm.getImageData() != null) {
            List<Spm.SPMImageData> dstImages = new ArrayList<>(bheSpm.getImageData().size());
            for (com.giga.nexas.dto.bhe.spm.Spm.SPMImageData src : bheSpm.getImageData()) {
                Spm.SPMImageData dst = new Spm.SPMImageData();
                BeanUtil.copyProperties(src, dst);
                dstImages.add(dst);
            }
            bsdxSpm.setImageData(dstImages);
        }

        if (bheSpm.getAnimData() != null) {
            List<Spm.SPMAnimData> dstAnimData = new ArrayList<>(bheSpm.getAnimData().size());
            for (com.giga.nexas.dto.bhe.spm.Spm.SPMAnimData srcAnim : bheSpm.getAnimData()) {
                Spm.SPMAnimData dstAnim = new Spm.SPMAnimData();
                BeanUtil.copyProperties(srcAnim, dstAnim, "patData");
                dstAnim.setNumPat(srcAnim.getNumPat() != null ? srcAnim.getNumPat() : safeSize(srcAnim.getPatData()));
                dstAnim.setAnimRotateDirection(srcAnim.getAnimRotateDirection() != null ? srcAnim.getAnimRotateDirection() : 0);
                dstAnim.setAnimReverseDirection(srcAnim.getAnimReverseDirection() != null ? srcAnim.getAnimReverseDirection() : 0);
                if (srcAnim.getPatData() != null) {
                    List<Spm.SPMPatData> patList = new ArrayList<>(srcAnim.getPatData().size());
                    for (com.giga.nexas.dto.bhe.spm.Spm.SPMPatData srcPat : srcAnim.getPatData()) {
                        Spm.SPMPatData dstPat = new Spm.SPMPatData();
                        BeanUtil.copyProperties(srcPat, dstPat);
                        patList.add(dstPat);
                    }
                    dstAnim.setPatData(patList);
                }
                dstAnimData.add(dstAnim);
            }
            bsdxSpm.setAnimData(dstAnimData);
            bsdxSpm.setNumAnimData(dstAnimData.size());
        }

        if (bheSpm.getPageData() != null) {
            List<Spm.SPMPageData> dstPages = new ArrayList<>(bheSpm.getPageData().size());
            for (com.giga.nexas.dto.bhe.spm.Spm.SPMPageData srcPage : bheSpm.getPageData()) {
                Spm.SPMPageData dstPage = new Spm.SPMPageData();
                BeanUtil.copyProperties(srcPage, dstPage, "hitRects", "chipData", "numChipData", "hitFlag", "unk3");
                dstPage.setNumChipData(safeSize(srcPage.getChipData()));
                dstPage.setPageRect(toBsdxRect(srcPage.getPageRect()));
                dstPage.setPageWidth(valueOrZero(dstPage.getPageWidth()));
                dstPage.setPageHeight(valueOrZero(dstPage.getPageHeight()));
                dstPage.setPageOption(longOrZero(srcPage.getPageOption()));
                dstPage.setRotateCenterX(valueOrZero(srcPage.getRotateCenterX()));
                dstPage.setRotateCenterY(valueOrZero(srcPage.getRotateCenterY()));
                // SPM2.02 的 unk3：没有 hitRects 时强制归零
                dstPage.setUnk3(srcPage.getHitRects() != null && !srcPage.getHitRects().isEmpty() ? dstPage.getUnk3() : (byte) 0);

                if (srcPage.getChipData() != null) {
                    List<Spm.SPMChipData> chipData = new ArrayList<>(srcPage.getChipData().size());
                    for (com.giga.nexas.dto.bhe.spm.Spm.SPMChipData srcChip : srcPage.getChipData()) {
                        Spm.SPMChipData dstChip = new Spm.SPMChipData();
                        BeanUtil.copyProperties(srcChip, dstChip);
                        dstChip.setDstRect(toBsdxRect(srcChip.getDstRect()));
                        dstChip.setSrcRect(toBsdxRect(srcChip.getSrcRect()));
                        dstChip.setChipWidth(valueOrZero(dstChip.getChipWidth()));
                        dstChip.setChipHeight(valueOrZero(dstChip.getChipHeight()));
                        dstChip.setDrawOption(longOrZero(srcChip.getDrawOption()));
                        dstChip.setDrawOptionValue(longOrZero(srcChip.getDrawOptionValue()));
                        dstChip.setOption(valueOrZero(srcChip.getOption()));
                        chipData.add(dstChip);
                    }
                    dstPage.setChipData(chipData);
                    dstPage.setNumChipData(chipData.size());
                }

                // hitbox: 形状映射 + 默认值补齐
                List<Spm.SPMHitArea> dstHitAreas = new ArrayList<>();
                if (srcPage.getHitRects() != null) {
                    for (com.giga.nexas.dto.bhe.spm.Spm.SPMHitArea srcHit : srcPage.getHitRects()) {
                        Spm.SPMHitArea mapped = srcHit.transHitbox();
                        normalizeHitArea(mapped);
                        dstHitAreas.add(mapped);
                    }
                }
                dstPage.setHitRects(dstHitAreas);
                dstPage.setHitFlag(buildHitFlag(dstHitAreas.size()));
                dstPages.add(dstPage);
            }
            bsdxSpm.setPageData(dstPages);
            bsdxSpm.setNumPageData(dstPages.size());
        }

        return bsdxSpm;
    }

    private Spm.SPMRect toBsdxRect(com.giga.nexas.dto.bhe.spm.Spm.SPMRect src) {
        Spm.SPMRect rect = new Spm.SPMRect();
        rect.setLeft(src != null ? valueOrZero(src.getLeft()) : 0);
        rect.setTop(src != null ? valueOrZero(src.getTop()) : 0);
        rect.setRight(src != null ? valueOrZero(src.getRight()) : 0);
        rect.setBottom(src != null ? valueOrZero(src.getBottom()) : 0);
        return rect;
    }

    private void normalizeHitArea(Spm.SPMHitArea hitArea) {
        if (hitArea.getHitRect() == null) {
            hitArea.setHitRect(toBsdxRect(null));
        }
        if (hitArea.getUnk0() == null) {
            hitArea.setUnk0(1);
        }
        if (hitArea.getUnk1() == null) {
            hitArea.setUnk1(0);
        }
        if (hitArea.getUnk2() == null) {
            hitArea.setUnk2(0);
        }
    }

    private long buildHitFlag(int hitCount) {
        long flag = 0;
        for (int i = 0; i < hitCount && i < 32; i++) {
            flag |= (1L << i);
        }
        return flag;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private long longOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
