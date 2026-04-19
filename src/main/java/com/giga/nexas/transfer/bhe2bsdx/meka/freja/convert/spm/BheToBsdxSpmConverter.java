package com.giga.nexas.transfer.bhe2bsdx.meka.freja.convert.spm;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.spm.Spm;

import java.util.ArrayList;
import java.util.List;


public class BheToBsdxSpmConverter {

    public Spm convert(com.giga.nexas.dto.bhe.spm.Spm source) {
        Spm target = new Spm();
        if (source == null) {
            return target;
        }

        BeanUtil.copyProperties(source, target, "pageData", "imageData", "animData",
                "numPageData", "numImageData", "numAnimData");
        target.setSpmVersion(source.getSpmVersion() != null ? source.getSpmVersion() : "SPM VER-2.00");
        target.setPatPageNum(source.getPatPageNum() == null ? 0 : source.getPatPageNum());
        target.setImageData(convertImages(source.getImageData()));
        target.setAnimData(convertAnims(source.getAnimData()));
        target.setPageData(convertPages(source.getPageData()));
        target.setNumImageData(target.getImageData().size());
        target.setNumAnimData(target.getAnimData().size());
        target.setNumPageData(target.getPageData().size());
        return target;
    }

    private List<Spm.SPMImageData> convertImages(List<com.giga.nexas.dto.bhe.spm.Spm.SPMImageData> sourceList) {
        List<Spm.SPMImageData> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (com.giga.nexas.dto.bhe.spm.Spm.SPMImageData source : sourceList) {
            targetList.add(copyBean(source, new Spm.SPMImageData()));
        }
        return targetList;
    }

    private List<Spm.SPMAnimData> convertAnims(List<com.giga.nexas.dto.bhe.spm.Spm.SPMAnimData> sourceList) {
        List<Spm.SPMAnimData> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (com.giga.nexas.dto.bhe.spm.Spm.SPMAnimData source : sourceList) {
            Spm.SPMAnimData target = copyBean(source, new Spm.SPMAnimData());
            target.setPatData(convertPats(source == null ? null : source.getPatData()));
            target.setNumPat(source == null || source.getNumPat() == null
                    ? target.getPatData().size()
                    : source.getNumPat());
            targetList.add(target);
        }
        return targetList;
    }

    private List<Spm.SPMPatData> convertPats(List<com.giga.nexas.dto.bhe.spm.Spm.SPMPatData> sourceList) {
        List<Spm.SPMPatData> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (com.giga.nexas.dto.bhe.spm.Spm.SPMPatData source : sourceList) {
            Spm.SPMPatData target = copyBean(source, new Spm.SPMPatData());
            if (source != null && source.getPageNo() != null) {
                target.setPageNo(new ArrayList<>(source.getPageNo()));
            }
            targetList.add(target);
        }
        return targetList;
    }

    private List<Spm.SPMPageData> convertPages(List<com.giga.nexas.dto.bhe.spm.Spm.SPMPageData> sourceList) {
        List<Spm.SPMPageData> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (com.giga.nexas.dto.bhe.spm.Spm.SPMPageData source : sourceList) {
            Spm.SPMPageData target = copyBean(source, new Spm.SPMPageData());
            target.setPageRect(convertRect(source == null ? null : source.getPageRect()));
            target.setChipData(convertChips(source == null ? null : source.getChipData()));
            target.setHitRects(convertHitAreas(source == null ? null : source.getHitRects()));
            target.setNumChipData(target.getChipData().size());
            target.setHitFlag(source == null || source.getHitFlag() == null ? 0L : source.getHitFlag());
            target.setUnk3((byte) 0);
            targetList.add(target);
        }
        return targetList;
    }

    private List<Spm.SPMChipData> convertChips(List<com.giga.nexas.dto.bhe.spm.Spm.SPMChipData> sourceList) {
        List<Spm.SPMChipData> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (com.giga.nexas.dto.bhe.spm.Spm.SPMChipData source : sourceList) {
            Spm.SPMChipData target = copyBean(source, new Spm.SPMChipData());
            target.setDstRect(convertRect(source == null ? null : source.getDstRect()));
            target.setSrcRect(convertRect(source == null ? null : source.getSrcRect()));
            target.setUnk5((byte) 0);
            targetList.add(target);
        }
        return targetList;
    }

    private List<Spm.SPMHitArea> convertHitAreas(List<com.giga.nexas.dto.bhe.spm.Spm.SPMHitArea> sourceList) {
        List<Spm.SPMHitArea> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (com.giga.nexas.dto.bhe.spm.Spm.SPMHitArea source : sourceList) {
            if (source == null) {
                targetList.add(normalizeHitArea(null));
                continue;
            }
            targetList.add(normalizeHitArea(source.transHitbox()));
        }
        return targetList;
    }

    private Spm.SPMRect convertRect(com.giga.nexas.dto.bhe.spm.Spm.SPMRect source) {
        Spm.SPMRect target = new Spm.SPMRect();
        if (source != null) {
            BeanUtil.copyProperties(source, target);
        }
        target.setLeft(valueOrZero(target.getLeft()));
        target.setTop(valueOrZero(target.getTop()));
        target.setRight(valueOrZero(target.getRight()));
        target.setBottom(valueOrZero(target.getBottom()));
        return target;
    }

    private Spm.SPMHitArea normalizeHitArea(Spm.SPMHitArea source) {
        Spm.SPMHitArea target = source == null ? new Spm.SPMHitArea() : source;
        if (target.getHitRect() == null) {
            target.setHitRect(convertRect(null));
        }
        target.setUnk0(valueOrDefault(target.getUnk0(), 1));
        target.setUnk1(valueOrZero(target.getUnk1()));
        target.setUnk2(valueOrZero(target.getUnk2()));
        return target;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private int valueOrDefault(Integer value, int defaultValue) {
        return value == null ? defaultValue : value;
    }

    private <T> T copyBean(Object source, T target) {
        if (source != null) {
            BeanUtil.copyProperties(source, target);
        }
        return target;
    }
}
