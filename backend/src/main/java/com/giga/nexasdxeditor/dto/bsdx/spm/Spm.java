package com.giga.nexasdxeditor.dto.bsdx.spm;

import com.giga.nexasdxeditor.dto.bsdx.Bsdx;
import lombok.Data;

import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/3/14
 * @Description Spm
 * 参考自
 * https://github.com/koukdw/Aquarium_tools/blob/main/research/fileformats.md
 */
@Data
public class Spm extends Bsdx {

    private String spmVersion;
    private Integer numPageData;
    private List<SPMPageData> pageData;
    private Integer numImageData;
    private List<SPMImageData> imageData;
    private Integer patPageNum;
    private Integer numAnimData;
    private List<SPMAnimData> animData;

    @Data
    public static class SPMRect {
        private Integer left;
        private Integer top;
        private Integer right;
        private Integer bottom;
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
    }

    @Data
    public static class SPMHitArea {
        private SPMRect hitRect;
        private Integer unk0;
        private Integer unk1;
        private Integer unk2;
    }

    @Data
    public static class SPMImageData {
        private String imageName;
    }

    @Data
    public static class SPMPatData {
        private Integer waitFrame;
        private List<Integer> pageNo; // size = patPageNum
    }

    @Data
    public static class SPMAnimData {
        private String animName;
        private Integer numPat;
        private Integer animRotateDirection;
        private Integer animReverseDirection;
        private List<SPMPatData> patData; // size = numPat & 65535
    }

}
