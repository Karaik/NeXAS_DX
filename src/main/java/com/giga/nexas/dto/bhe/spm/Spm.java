package com.giga.nexas.dto.bhe.spm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.giga.nexas.dto.bhe.Bhe;
import com.giga.nexas.dto.bhe.spm.hitbox.*;
import com.giga.nexas.io.BinaryReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.List;

@Data
public class Spm extends Bhe {

    private String spmVersion;
    private Integer numPageData;
    private List<SPMPageData> pageData;
    private Integer numImageData;
    private List<SPMImageData> imageData;
    private Integer patPageNum;
    private Integer numAnimData;
    private List<SPMAnimData> animData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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
//        private byte unk5; // SPM2.02
        private Long drawOptionValue;
        private Integer option;
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
//        private byte unk3; // SPM2.02
        private List<SPMHitArea> hitRects;
        private List<SPMChipData> chipData;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "shapeType", visible = true)
    @JsonSubTypes(value = {
            @JsonSubTypes.Type(value = DefaultHitArea.class, name = "0"),
            @JsonSubTypes.Type(value = CRotatableRect.class, name = "1"),
            @JsonSubTypes.Type(value = CCircle.class, name = "2"),
            @JsonSubTypes.Type(value = C2DLineSegment.class, name = "7"),
            @JsonSubTypes.Type(value = C2DDot.class, name = "8"),
            @JsonSubTypes.Type(value = CBox.class, name = "9"),
            @JsonSubTypes.Type(value = CRotatableBox.class, name = "10"),
            @JsonSubTypes.Type(value = CSphere.class, name = "11")
    })
    @Data
    // todo
    // 20260331 6 hitbox转换，见 merge_hitrects_and_unk5.py
    public static class SPMHitArea {
        private Short id;         // u16
        private Short shapeType;  // u16

        // 旧格式解析时会直接写入这些字段
        public SPMRect hitRect = new SPMRect(0, 0, 0, 0);
        public Integer unk0 = 0;
        public Integer unk1 = 0;
        public Integer unk2 = 0;

        public void readInfo(BinaryReader reader) throws IOException {
            // 空实现用于继承
        }

        public com.giga.nexas.dto.bsdx.spm.Spm.SPMHitArea transHitbox() {
            int left = hitRect != null && hitRect.getLeft() != null ? hitRect.getLeft() : 0;
            int top = hitRect != null && hitRect.getTop() != null ? hitRect.getTop() : 0;
            int right = hitRect != null && hitRect.getRight() != null ? hitRect.getRight() : left;
            int bottom = hitRect != null && hitRect.getBottom() != null ? hitRect.getBottom() : top;
            return buildBsdxHitArea(left, top, right, bottom, null, null, null);
        }

        protected com.giga.nexas.dto.bsdx.spm.Spm.SPMHitArea buildBsdxHitArea(int left, int top, int right, int bottom, Integer zMin, Integer zMax, Integer overrideUnk0) {
            com.giga.nexas.dto.bsdx.spm.Spm.SPMHitArea area = new com.giga.nexas.dto.bsdx.spm.Spm.SPMHitArea();
            com.giga.nexas.dto.bsdx.spm.Spm.SPMRect rect = new com.giga.nexas.dto.bsdx.spm.Spm.SPMRect();
            rect.setLeft(left);
            rect.setTop(top);
            rect.setRight(right);
            rect.setBottom(bottom);
            area.setHitRect(rect);
            area.setUnk0(overrideUnk0 != null ? overrideUnk0 : mapShapeTypeForBsdx());
            area.setUnk1(zMin != null ? zMin : 0);
            area.setUnk2(zMax != null ? zMax : 0);
            return area;
        }

        private int mapShapeTypeForBsdx() {
            if (shapeType == null) {
                return 1;
            }
            return switch (shapeType) {
                case 0 -> 1;   // Default rectangle
                case 1 -> 2;   // Rotatable rect -> general body
                case 2 -> 4;   // Circle
                case 7 -> 3;   // 2D line
                case 8 -> 0;   // 2D dot
                case 9 -> 5;   // Box (3D axis-aligned)
                case 10 -> 7;  // Rotatable box (3D)
                case 11 -> 6;  // Sphere
                default -> 2;  // Fallback to main body
            };
        }
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
        private List<SPMPatData> patData;
    }

    @Data
    public static class SPMTailData {

    }

}
