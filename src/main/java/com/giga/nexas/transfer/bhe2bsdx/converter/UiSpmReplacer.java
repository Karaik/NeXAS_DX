package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class UiSpmReplacer {

    private static final String DEFAULT_TARGET_PILOT_NAME = "菜ノ葉";
    private static final String DEFAULT_TARGET_MEKA_CODE = "NANOHA";

    @Data
    public static class UiSpmReplaceResult {
        private boolean mekaPilotReplaced;
        private boolean selectMekaMenuReplaced;
        private int mekaPilotAnimIndex = -1;
        private int selectMekaMenuAnimIndex = -1;
    }

    public UiSpmReplaceResult replaceTargetSlotUiSpm(
            Spm mekaPilotSpm,
            Spm selectMekaMenuMekaSpm,
            Spm sourceMSpm,
            Spm sourceSSpm,
            Dat selectMekaMenuDat,
            MekaGroupGrp bsdxMekaGroup,
            Mek sourceMek,
            Mek targetMek,
            String targetCodeName,
            boolean keepTargetKey
    ) {
        UiSpmReplaceResult result = new UiSpmReplaceResult();
        String resolvedTargetCode = normalizeCode(targetCodeName, DEFAULT_TARGET_MEKA_CODE);
        String targetPilotName = resolveTargetPilotName(targetMek, DEFAULT_TARGET_PILOT_NAME);

        if (mekaPilotSpm != null && sourceMSpm != null) {
            int animIndex = findAnimIndexByName(mekaPilotSpm, targetPilotName);
            if (animIndex < 0 && !DEFAULT_TARGET_PILOT_NAME.equals(targetPilotName)) {
                animIndex = findAnimIndexByName(mekaPilotSpm, DEFAULT_TARGET_PILOT_NAME);
            }
            if (animIndex >= 0) {
                List<Integer> pageIndices = collectPageIndices(mekaPilotSpm.getAnimData().get(animIndex));
                int imageIndex = resolveTargetImageIndex(mekaPilotSpm, pageIndices);
                if (!pageIndices.isEmpty()) {
                    replacePages(mekaPilotSpm, pageIndices, sourceMSpm, imageIndex);
                    replaceImageName(mekaPilotSpm, imageIndex, firstImageName(sourceMSpm));
                    if (!keepTargetKey) {
                        String pilotName = extractPilotName(sourceMek);
                        if (!pilotName.isEmpty()) {
                            mekaPilotSpm.getAnimData().get(animIndex).setAnimName(pilotName);
                        }
                    }
                    result.setMekaPilotReplaced(true);
                    result.setMekaPilotAnimIndex(animIndex);
                    log.info("Step6: MekaPilot 替换完成 (animIndex={})", animIndex);
                }
            } else {
                log.warn("Step6: MekaPilot 未找到目标 animName={}", targetPilotName);
            }
        }

        if (selectMekaMenuMekaSpm != null && sourceSSpm != null) {
            int targetMekaIndex = findMekaIndexByCode(bsdxMekaGroup, resolvedTargetCode);
            Integer animIndex = resolveSelectMenuAnimIndex(selectMekaMenuDat, targetMekaIndex);
            if (animIndex != null && animIndex >= 0
                    && animIndex < safeSize(selectMekaMenuMekaSpm.getAnimData())) {
                Spm.SPMAnimData animData = selectMekaMenuMekaSpm.getAnimData().get(animIndex);
                List<Integer> pageIndices = collectPageIndices(animData);
                int imageIndex = resolveTargetImageIndex(selectMekaMenuMekaSpm, pageIndices);
                if (!pageIndices.isEmpty()) {
                    replacePages(selectMekaMenuMekaSpm, pageIndices, sourceSSpm, imageIndex);
                    replaceImageName(selectMekaMenuMekaSpm, imageIndex, firstImageName(sourceSSpm));
                    if (!keepTargetKey) {
                        String menuName = buildSelectMenuName(sourceMek);
                        if (!menuName.isEmpty()) {
                            animData.setAnimName(menuName);
                        }
                    }
                    result.setSelectMekaMenuReplaced(true);
                    result.setSelectMekaMenuAnimIndex(animIndex);
                    log.info("Step6: SelectMekaMenuMeka 替换完成 (animIndex={})", animIndex);
                }
            } else {
                log.warn("Step6: SelectMekaMenuMeka 未找到目标 animIndex (mekaIndex={})", targetMekaIndex);
            }
        }

        return result;
    }

    private int findAnimIndexByName(Spm spm, String targetName) {
        if (spm == null || spm.getAnimData() == null || targetName == null) {
            return -1;
        }
        String normalizedTarget = normalizeName(targetName);
        for (int i = 0; i < spm.getAnimData().size(); i++) {
            String name = normalizeName(spm.getAnimData().get(i).getAnimName());
            if (normalizedTarget.equals(name)) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return name.replace("\r", "").replace("\n", "").trim();
    }

    private List<Integer> collectPageIndices(Spm.SPMAnimData animData) {
        List<Integer> indices = new ArrayList<>();
        if (animData == null || animData.getPatData() == null) {
            return indices;
        }
        for (Spm.SPMPatData pat : animData.getPatData()) {
            if (pat == null || pat.getPageNo() == null) {
                continue;
            }
            for (Integer pageNo : pat.getPageNo()) {
                if (pageNo != null) {
                    indices.add(pageNo);
                }
            }
        }
        return indices;
    }

    private int resolveTargetImageIndex(Spm targetSpm, List<Integer> pageIndices) {
        if (targetSpm == null || pageIndices == null) {
            return 0;
        }
        for (Integer pageIndex : pageIndices) {
            Spm.SPMPageData page = getPage(targetSpm, pageIndex);
            if (page == null || page.getChipData() == null) {
                continue;
            }
            for (Spm.SPMChipData chip : page.getChipData()) {
                if (chip != null && chip.getImageNo() != null) {
                    return chip.getImageNo();
                }
            }
        }
        return 0;
    }

    private void replacePages(Spm targetSpm, List<Integer> targetPageIndices, Spm sourceSpm, int targetImageIndex) {
        if (targetSpm == null || sourceSpm == null || targetPageIndices == null
                || sourceSpm.getPageData() == null || sourceSpm.getPageData().isEmpty()) {
            return;
        }
        List<Spm.SPMPageData> sourcePages = sourceSpm.getPageData();
        for (int i = 0; i < targetPageIndices.size(); i++) {
            Integer targetIndex = targetPageIndices.get(i);
            if (targetIndex == null || targetIndex < 0) {
                continue;
            }
            Spm.SPMPageData sourcePage = sourcePages.get(i % sourcePages.size());
            Spm.SPMPageData copied = copyPage(sourcePage, targetImageIndex);
            setPage(targetSpm, targetIndex, copied);
        }
    }

    private void replaceImageName(Spm targetSpm, int targetImageIndex, String imageName) {
        if (targetSpm == null || targetSpm.getImageData() == null || imageName == null) {
            return;
        }
        if (targetImageIndex < 0 || targetImageIndex >= targetSpm.getImageData().size()) {
            return;
        }
        Spm.SPMImageData targetImage = targetSpm.getImageData().get(targetImageIndex);
        if (targetImage != null) {
            targetImage.setImageName(imageName);
        }
    }

    private String firstImageName(Spm spm) {
        if (spm == null || spm.getImageData() == null || spm.getImageData().isEmpty()) {
            return null;
        }
        Spm.SPMImageData image = spm.getImageData().get(0);
        return image != null ? image.getImageName() : null;
    }

    private Spm.SPMPageData copyPage(Spm.SPMPageData src, int imageIndex) {
        Spm.SPMPageData dst = new Spm.SPMPageData();
        if (src == null) {
            return dst;
        }
        dst.setNumChipData(valueOrZero(src.getNumChipData()));
        dst.setPageWidth(valueOrZero(src.getPageWidth()));
        dst.setPageHeight(valueOrZero(src.getPageHeight()));
        dst.setPageRect(copyRect(src.getPageRect()));
        dst.setPageOption(longOrZero(src.getPageOption()));
        dst.setRotateCenterX(valueOrZero(src.getRotateCenterX()));
        dst.setRotateCenterY(valueOrZero(src.getRotateCenterY()));
        dst.setHitFlag(longOrZero(src.getHitFlag()));
        dst.setUnk3(src.getUnk3());
        dst.setHitRects(copyHitAreas(src.getHitRects()));

        List<Spm.SPMChipData> chips = new ArrayList<>();
        if (src.getChipData() != null) {
            for (Spm.SPMChipData chip : src.getChipData()) {
                chips.add(copyChip(chip, imageIndex));
            }
        }
        dst.setChipData(chips);
        dst.setNumChipData(chips.size());
        return dst;
    }

    private Spm.SPMChipData copyChip(Spm.SPMChipData src, int imageIndex) {
        Spm.SPMChipData dst = new Spm.SPMChipData();
        if (src == null) {
            return dst;
        }
        dst.setImageNo(imageIndex);
        dst.setDstRect(copyRect(src.getDstRect()));
        dst.setChipWidth(valueOrZero(src.getChipWidth()));
        dst.setChipHeight(valueOrZero(src.getChipHeight()));
        dst.setSrcRect(copyRect(src.getSrcRect()));
        dst.setDrawOption(longOrZero(src.getDrawOption()));
        dst.setUnk5(src.getUnk5());
        dst.setDrawOptionValue(longOrZero(src.getDrawOptionValue()));
        dst.setOption(valueOrZero(src.getOption()));
        return dst;
    }

    private Spm.SPMRect copyRect(Spm.SPMRect src) {
        Spm.SPMRect rect = new Spm.SPMRect();
        if (src == null) {
            rect.setLeft(0);
            rect.setTop(0);
            rect.setRight(0);
            rect.setBottom(0);
            return rect;
        }
        rect.setLeft(valueOrZero(src.getLeft()));
        rect.setTop(valueOrZero(src.getTop()));
        rect.setRight(valueOrZero(src.getRight()));
        rect.setBottom(valueOrZero(src.getBottom()));
        return rect;
    }

    private List<Spm.SPMHitArea> copyHitAreas(List<Spm.SPMHitArea> src) {
        List<Spm.SPMHitArea> result = new ArrayList<>();
        if (src == null) {
            return result;
        }
        for (Spm.SPMHitArea hitArea : src) {
            result.add(copyHitArea(hitArea));
        }
        return result;
    }

    private Spm.SPMHitArea copyHitArea(Spm.SPMHitArea src) {
        Spm.SPMHitArea dst = new Spm.SPMHitArea();
        if (src == null) {
            return dst;
        }
        dst.setUnk0(valueOrZero(src.getUnk0()));
        dst.setHitRect(copyRect(src.getHitRect()));
        dst.setUnk1(valueOrZero(src.getUnk1()));
        dst.setUnk2(valueOrZero(src.getUnk2()));
        return dst;
    }

    private Spm.SPMPageData getPage(Spm spm, Integer index) {
        if (spm == null || spm.getPageData() == null || index == null) {
            return null;
        }
        if (index < 0 || index >= spm.getPageData().size()) {
            return null;
        }
        return spm.getPageData().get(index);
    }

    private void setPage(Spm spm, Integer index, Spm.SPMPageData page) {
        if (spm == null || spm.getPageData() == null || index == null) {
            return;
        }
        if (index < 0 || index >= spm.getPageData().size()) {
            return;
        }
        spm.getPageData().set(index, page);
    }

    private int findMekaIndexByCode(MekaGroupGrp bsdxMekaGroup, String code) {
        if (bsdxMekaGroup == null || bsdxMekaGroup.getMekaList() == null || code == null) {
            return -1;
        }
        for (int i = 0; i < bsdxMekaGroup.getMekaList().size(); i++) {
            MekaGroupGrp.MekaGroup meka = bsdxMekaGroup.getMekaList().get(i);
            if (meka != null && code.equalsIgnoreCase(meka.getMekaCodeName())) {
                return i;
            }
        }
        return -1;
    }

    private Integer resolveSelectMenuAnimIndex(Dat selectMekaMenuDat, int mekaIndex) {
        if (selectMekaMenuDat == null || selectMekaMenuDat.getData() == null || mekaIndex < 0) {
            return null;
        }
        for (List<Object> row : selectMekaMenuDat.getData()) {
            if (row == null || row.size() < 2) {
                continue;
            }
            Integer rowMekaIndex = toInt(row.get(0));
            if (rowMekaIndex != null && rowMekaIndex == mekaIndex) {
                return toInt(row.get(1));
            }
        }
        return null;
    }

    private String extractPilotName(Mek meka) {
        if (meka == null || meka.getMekBasicInfo() == null) {
            return "";
        }
        return safeTrim(meka.getMekBasicInfo().getPilotNameKanji());
    }

    private String buildSelectMenuName(Mek meka) {
        if (meka == null || meka.getMekBasicInfo() == null) {
            return "";
        }
        String pilot = safeTrim(meka.getMekBasicInfo().getPilotNameKanji());
        String mekaName = safeTrim(meka.getMekBasicInfo().getMekName());
        String pilotRoma = safeTrim(meka.getMekBasicInfo().getPilotNameRoma());
        String mekaRoma = safeTrim(meka.getMekBasicInfo().getMekNameEnglish());
        if (pilot.isEmpty() && mekaName.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(pilot);
        if (!mekaName.isEmpty()) {
            builder.append("/").append(mekaName);
        }
        String roma = joinNonEmpty(pilotRoma, mekaRoma);
        if (!roma.isEmpty()) {
            builder.append("：").append(roma);
        }
        builder.append("\r");
        return builder.toString();
    }

    private String joinNonEmpty(String left, String right) {
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return left + "/" + right;
    }

    private String safeTrim(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeCode(String code, String fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }
        return code.trim().toUpperCase();
    }

    private String resolveTargetPilotName(Mek targetMek, String fallback) {
        if (targetMek == null || targetMek.getMekBasicInfo() == null) {
            return fallback;
        }
        String pilotName = safeTrim(targetMek.getMekBasicInfo().getPilotNameKanji());
        return pilotName.isEmpty() ? fallback : pilotName;
    }

    private Integer toInt(Object value) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            long longValue = (Long) value;
            return (int) longValue;
        }
        if (value instanceof Short) {
            return ((Short) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
