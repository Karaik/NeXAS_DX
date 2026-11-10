package com.giga.nexas.transfer.bhe2bsdx.mapappend.composition;

import com.giga.nexas.dto.bsdx.spm.Spm;

import java.util.ArrayList;
import java.util.List;

public class BheMapAppendSpmComposer {

    public Spm compose(MultipleSpriteMapListCompositionPlan plan) {
        if (plan == null || !plan.isComposable()) {
            throw new IllegalArgumentException("composition plan must be composable");
        }

        Spm composed = new Spm();
        composed.setExtensionName("spm");
        composed.setSpmVersion(resolveVersion(plan));
        composed.setPatPageNum(plan.getComposedPatPageNum());
        composed.setPageData(new ArrayList<>());
        composed.setImageData(new ArrayList<>());
        composed.setAnimData(new ArrayList<>());

        for (SourceSpriteMapCompositionPlan sourcePlan : plan.getSourceSpriteMaps()) {
            Spm source = sourcePlan.getBsdxSpm();
            if (source == null) {
                continue;
            }
            appendImages(composed, source);
            appendPages(composed, source, sourcePlan.getImageOffset());
            appendAnims(composed, source, sourcePlan.getPageOffset(), composed.getPatPageNum());
        }

        composed.setNumImageData(composed.getImageData().size());
        composed.setNumPageData(composed.getPageData().size());
        composed.setNumAnimData(composed.getAnimData().size());
        return composed;
    }

    private String resolveVersion(MultipleSpriteMapListCompositionPlan plan) {
        for (SourceSpriteMapCompositionPlan sourcePlan : plan.getSourceSpriteMaps()) {
            Spm spm = sourcePlan.getBsdxSpm();
            if (spm != null && spm.getSpmVersion() != null && !spm.getSpmVersion().isBlank()) {
                return spm.getSpmVersion();
            }
        }
        return "SPM VER-2.00";
    }

    private void appendImages(Spm composed, Spm source) {
        if (source.getImageData() == null) {
            return;
        }
        for (Spm.SPMImageData image : source.getImageData()) {
            composed.getImageData().add(copyImage(image));
        }
    }

    private void appendPages(Spm composed, Spm source, int imageOffset) {
        if (source.getPageData() == null) {
            return;
        }
        for (Spm.SPMPageData page : source.getPageData()) {
            composed.getPageData().add(copyPage(page, imageOffset));
        }
    }

    private void appendAnims(Spm composed, Spm source, int pageOffset, int composedPatPageNum) {
        if (source.getAnimData() == null) {
            return;
        }
        int sourcePatPageNum = source.getPatPageNum() == null ? 0 : Math.max(0, source.getPatPageNum());
        for (Spm.SPMAnimData anim : source.getAnimData()) {
            composed.getAnimData().add(copyAnim(anim, pageOffset, sourcePatPageNum, composedPatPageNum));
        }
    }

    private Spm.SPMImageData copyImage(Spm.SPMImageData source) {
        Spm.SPMImageData target = new Spm.SPMImageData();
        if (source != null) {
            target.setImageName(source.getImageName());
        }
        return target;
    }

    private Spm.SPMPageData copyPage(Spm.SPMPageData source, int imageOffset) {
        Spm.SPMPageData target = new Spm.SPMPageData();
        if (source == null) {
            target.setNumChipData(0);
            target.setHitFlag(0L);
            target.setHitRects(new ArrayList<>());
            target.setChipData(new ArrayList<>());
            target.setPageRect(copyRect(null));
            return target;
        }
        target.setPageWidth(valueOrZero(source.getPageWidth()));
        target.setPageHeight(valueOrZero(source.getPageHeight()));
        target.setPageRect(copyRect(source.getPageRect()));
        target.setPageOption(valueOrZero(source.getPageOption()));
        target.setRotateCenterX(valueOrZero(source.getRotateCenterX()));
        target.setRotateCenterY(valueOrZero(source.getRotateCenterY()));
        target.setHitFlag(valueOrZero(source.getHitFlag()));
        target.setUnk3(source.getUnk3());
        target.setHitRects(copyHits(source.getHitRects()));
        target.setChipData(copyChips(source.getChipData(), imageOffset));
        target.setNumChipData(target.getChipData().size());
        return target;
    }

    private Spm.SPMAnimData copyAnim(
            Spm.SPMAnimData source,
            int pageOffset,
            int sourcePatPageNum,
            int composedPatPageNum
    ) {
        Spm.SPMAnimData target = new Spm.SPMAnimData();
        if (source == null) {
            target.setPatData(new ArrayList<>());
            target.setNumPat(0);
            target.setAnimRotateDirection(0);
            target.setAnimReverseDirection(0);
            return target;
        }
        target.setAnimName(source.getAnimName());
        target.setAnimRotateDirection(valueOrZero(source.getAnimRotateDirection()));
        target.setAnimReverseDirection(valueOrZero(source.getAnimReverseDirection()));
        target.setPatData(copyPats(source.getPatData(), pageOffset, sourcePatPageNum, composedPatPageNum));
        target.setNumPat(source.getNumPat() == null ? target.getPatData().size() : source.getNumPat());
        return target;
    }

    private List<Spm.SPMChipData> copyChips(List<Spm.SPMChipData> sourceList, int imageOffset) {
        List<Spm.SPMChipData> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (Spm.SPMChipData source : sourceList) {
            Spm.SPMChipData target = new Spm.SPMChipData();
            if (source != null) {
                target.setImageNo(valueOrZero(source.getImageNo()) + imageOffset);
                target.setDstRect(copyRect(source.getDstRect()));
                target.setChipWidth(valueOrZero(source.getChipWidth()));
                target.setChipHeight(valueOrZero(source.getChipHeight()));
                target.setSrcRect(copyRect(source.getSrcRect()));
                target.setDrawOption(valueOrZero(source.getDrawOption()));
                target.setUnk5(source.getUnk5());
                target.setDrawOptionValue(valueOrZero(source.getDrawOptionValue()));
                target.setOption(valueOrZero(source.getOption()));
            }
            targetList.add(target);
        }
        return targetList;
    }

    private List<Spm.SPMHitArea> copyHits(List<Spm.SPMHitArea> sourceList) {
        List<Spm.SPMHitArea> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (Spm.SPMHitArea source : sourceList) {
            Spm.SPMHitArea target = new Spm.SPMHitArea();
            if (source != null) {
                target.setUnk0(valueOrZero(source.getUnk0()));
                target.setHitRect(copyRect(source.getHitRect()));
                target.setUnk1(valueOrZero(source.getUnk1()));
                target.setUnk2(valueOrZero(source.getUnk2()));
            }
            targetList.add(target);
        }
        return targetList;
    }

    private List<Spm.SPMPatData> copyPats(
            List<Spm.SPMPatData> sourceList,
            int pageOffset,
            int sourcePatPageNum,
            int composedPatPageNum
    ) {
        List<Spm.SPMPatData> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (Spm.SPMPatData source : sourceList) {
            Spm.SPMPatData target = new Spm.SPMPatData();
            target.setWaitFrame(source == null ? 0 : valueOrZero(source.getWaitFrame()));
            target.setPageNo(remapPageNos(source == null ? null : source.getPageNo(),
                    pageOffset,
                    sourcePatPageNum,
                    composedPatPageNum));
            targetList.add(target);
        }
        return targetList;
    }

    private List<Integer> remapPageNos(
            List<Integer> sourcePageNos,
            int pageOffset,
            int sourcePatPageNum,
            int composedPatPageNum
    ) {
        List<Integer> targetPageNos = new ArrayList<>();
        for (int index = 0; index < composedPatPageNum; index++) {
            Integer sourcePageNo = sourcePageNos != null && index < sourcePageNos.size() ? sourcePageNos.get(index) : -1;
            if (index >= sourcePatPageNum) {
                targetPageNos.add(-1);
                continue;
            }
            targetPageNos.add(sourcePageNo == null || sourcePageNo < 0 ? sourcePageNo : sourcePageNo + pageOffset);
        }
        return targetPageNos;
    }

    private Spm.SPMRect copyRect(Spm.SPMRect source) {
        Spm.SPMRect target = new Spm.SPMRect();
        if (source != null) {
            target.setLeft(valueOrZero(source.getLeft()));
            target.setTop(valueOrZero(source.getTop()));
            target.setRight(valueOrZero(source.getRight()));
            target.setBottom(valueOrZero(source.getBottom()));
            return target;
        }
        target.setLeft(0);
        target.setTop(0);
        target.setRight(0);
        target.setBottom(0);
        return target;
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }
}
