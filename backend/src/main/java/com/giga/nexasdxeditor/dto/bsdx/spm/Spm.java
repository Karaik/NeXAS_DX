package com.giga.nexasdxeditor.dto.bsdx.spm;

import com.giga.nexasdxeditor.io.BinaryReader;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/3/14
 * @Description Spm
 * 参考自
 * https://github.com/koukdw/Aquarium_tools/blob/main/research/fileformats.md
 */
@Data
public class Spm {

    private String spmVersion;
    private Integer numPageData;
    private List<SPMPageData> pageData;
    private Integer numImageData;
    private List<SPMImageData> imageData;
    private Integer patPageNum;
    private Integer numAnimData;
    private List<SPMAnimData> animData;

    public static Spm parse(BinaryReader reader) {
        Spm spm = new Spm();
        spm.setSpmVersion(reader.readNullTerminatedString());

        spm.setNumPageData(reader.readInt());
        List<SPMPageData> pageDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumPageData(); i++) {
            pageDataList.add(SPMPageData.parse(reader));
        }
        spm.setPageData(pageDataList);

        spm.setNumImageData(reader.readInt());
        List<SPMImageData> imageDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumImageData(); i++) {
            imageDataList.add(SPMImageData.parse(reader));
        }
        spm.setImageData(imageDataList);

        spm.setPatPageNum(reader.readInt());
        spm.setNumAnimData(reader.readInt());
        List<SPMAnimData> animDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumAnimData(); i++) {
            animDataList.add(SPMAnimData.parse(reader, spm.getPatPageNum()));
        }
        spm.setAnimData(animDataList);

        return spm;
    }

    @Data
    public static class SPMRect {
        private Integer left;
        private Integer top;
        private Integer right;
        private Integer bottom;

        private static SPMRect parse(BinaryReader reader) {
            SPMRect rect = new SPMRect();
            rect.setLeft(reader.readInt());
            rect.setTop(reader.readInt());
            rect.setRight(reader.readInt());
            rect.setBottom(reader.readInt());
            return rect;
        }
    }

    @Data
    public static class SPMChipData {
        private Integer imageNo;
        private SPMRect dstRect;
        private Integer chipWidth;
        private Integer chipHeight;
        private SPMRect srcRect;
        private Long drawOption;
        private Long drawOptionValue;
        private Integer option;
        // private byte unk5; // for SPM 2.02

        private static SPMChipData parse(BinaryReader reader) {
            SPMChipData chip = new SPMChipData();
            chip.setImageNo(reader.readInt());
            chip.setDstRect(SPMRect.parse(reader));
            chip.setChipWidth(reader.readInt());
            chip.setChipHeight(reader.readInt());
            chip.setSrcRect(SPMRect.parse(reader));
            chip.setDrawOption(reader.readInt() & 0xFFFFFFFFL);
            chip.setDrawOptionValue(reader.readInt() & 0xFFFFFFFFL);
            chip.setOption(reader.readInt());
            return chip;
        }
    }

    @Data
    public static class SPMPageData {
        private Integer numChipData;
        private Integer pageWidth;
        private Integer pageHeight;
        private SPMRect pageRect;
        private Long pageOption;
        private Integer rotateCenterX;
        private Integer rotateCenterY;
        private Long hitFlag;
        private List<SPMHitArea> hitRects;
        private List<SPMChipData> chipData;

        private static SPMPageData parse(BinaryReader reader) {
            SPMPageData page = new SPMPageData();
            page.setNumChipData(reader.readInt());
            page.setPageWidth(reader.readInt());
            page.setPageHeight(reader.readInt());
            page.setPageRect(SPMRect.parse(reader));
            page.setPageOption(reader.readInt() & 0xFFFFFFFFL);
            page.setRotateCenterX(reader.readInt());
            page.setRotateCenterY(reader.readInt());
            page.setHitFlag(reader.readInt() & 0xFFFFFFFFL);

            List<SPMHitArea> hits = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                if (((1 << (i & 31)) & page.hitFlag) != 0) {
                    hits.add(SPMHitArea.parse(reader));
                }
            }
            page.setHitRects(hits);

            List<SPMChipData> chips = new ArrayList<>();
            for (int i = 0; i < page.getNumChipData(); i++) {
                chips.add(SPMChipData.parse(reader));
            }
            page.setChipData(chips);
            return page;
        }

    }

    @Data
    public static class SPMHitArea {
        private SPMRect hitRect;
        private Integer unk0;
        private Integer unk1;
        private Integer unk2;

        private static SPMHitArea parse(BinaryReader reader) {
            SPMHitArea hit = new SPMHitArea();
            hit.setHitRect(SPMRect.parse(reader));
            hit.setUnk0(reader.readInt());
            hit.setUnk1(reader.readInt());
            hit.setUnk2(reader.readInt());
            return hit;
        }
    }

    @Data
    public static class SPMImageData {
        private String imageName;

        private static SPMImageData parse(BinaryReader reader) {
            SPMImageData img = new SPMImageData();
            img.setImageName(reader.readNullTerminatedString());
            return img;
        }
    }

    @Data
    public static class SPMPatData {
        private Integer waitFrame;
        private List<Integer> pageNo; // size = patPageNum

        private static SPMPatData parse(BinaryReader reader, int patPageNum) {
            SPMPatData pat = new SPMPatData();
            pat.setWaitFrame(reader.readInt());
            List<Integer> pageNos = new ArrayList<>();
            for (int i = 0; i < patPageNum; i++) {
                pageNos.add(reader.readInt());
            }
            pat.setPageNo(pageNos);
            return pat;
        }
    }

    @Data
    public static class SPMAnimData {
        private String animName;
        private Integer numPat;
        private Integer animRotateDirection;
        private Integer animReverseDirection;
        private List<SPMPatData> patData; // size = numPat & 65535

        private static SPMAnimData parse(BinaryReader reader, int patPageNum) {
            SPMAnimData anim = new SPMAnimData();
            anim.setAnimName(reader.readNullTerminatedString());
            anim.setNumPat(reader.readInt());
            anim.setAnimRotateDirection(reader.readInt());
            anim.setAnimReverseDirection(reader.readInt());

            int num = anim.getNumPat() & 0xFFFF; // 65535
            List<SPMPatData> pats = new ArrayList<>();
            for (int i = 0; i < num; i++) {
                pats.add(SPMPatData.parse(reader, patPageNum));
            }
            anim.setPatData(pats);
            return anim;
        }
    }

}
