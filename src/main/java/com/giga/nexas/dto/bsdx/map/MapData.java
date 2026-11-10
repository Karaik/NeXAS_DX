package com.giga.nexas.dto.bsdx.map;

import com.giga.nexas.dto.bsdx.Bsdx;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2026/5/14
 * @Description BSDX MapData
 *
 * 当前结构依据 BSDX exe 主读链确认，并已对 220 份 BSDX `.map` 样本完成
 * byte-for-byte roundtrip 验证。
 *
 * 当前已确认结构：
 * 1. magic
 * 2. width / height
 * 3. tile 主块 (10-byte record)
 * 4. 1 x 16-byte block
 * 5. 2 x 8-byte block
 * 6. 3 x 16-byte block
 * 7. 8 x (cstring + 5 * int32)
 * 8. 9 x (count + count * 12-byte record)
 * 9. foregroundImage
 * 10. spriteMap
 *
 * 对当前 BSDX 220 份 `.map` 样本验证后，
 * `spriteMap` 之后不存在额外剩余字节。
 *
 * 命名约定：
 * - 已确认语义的块：直接使用语义名
 * - 仅确认结构、未完全钉死语义的块：统一使用 `raw*` / `packed*`
 */
@Data
public class MapData extends Bsdx {

    public static final int RESOURCE_SLOT_COUNT = 8;
    public static final int SCRIPT_GROUP_COUNT = 9;

    public String fileName; // 仅记录用

    private String magic;
    private Integer width;
    private Integer height;

    // Tile 主块：已确认磁盘结构为 int32 + int32 + byte + byte，业务语义未完全钉死
    private List<TileRecord> tileRecords = new ArrayList<>();

    // 第一个 16-byte 表：已确认用于当前地图边界矩形
    private CameraRectBlock cameraRectBlock = new CameraRectBlock();

    // 两个 8-byte 表：仅确认结构，具体业务语义未完全钉死
    private RawPointGroupBlock rawPointGroup0 = new RawPointGroupBlock();
    private RawPointGroupBlock rawPointGroup1 = new RawPointGroupBlock();

    // 三个 16-byte 表：仅确认结构，具体业务语义未完全钉死
    private RawRectGroupBlock rawRectGroup0 = new RawRectGroupBlock();
    private RawRectGroupBlock rawRectGroup1 = new RawRectGroupBlock();
    private RawRectGroupBlock rawRectGroup2 = new RawRectGroupBlock();

    // 8 组 cstring + 5 * int32
    private List<NamedResourceSlotBlock> namedResourceSlotBlocks = new ArrayList<>();

    // 9 组 count + count * 12-byte：当前已确认单条为 typeId + x + y
    private List<ScriptEntryGroupBlock> scriptEntryGroupBlocks = new ArrayList<>();

    private String foregroundImage;
    private String spriteMap;

    public MapData() {
    }

    public MapData(String fileName) {
        this.fileName = fileName;
    }

    @Data
    public static class TileRecord {
        private Integer packedValue0;
        private Integer packedValue1;
        private Byte byte0;
        private Byte byte1;
    }

    @Data
    public static class CameraRectEntry {
        private Integer left;
        private Integer top;
        private Integer right;
        private Integer bottom;
    }

    @Data
    public static class CameraRectBlock {
        private Integer count;
        private List<CameraRectEntry> entries = new ArrayList<>();
    }

    @Data
    public static class RawRectGroupEntry {
        private Integer rawValue0;
        private Integer rawValue1;
        private Integer rawValue2;
        private Integer rawValue3;
    }

    @Data
    public static class RawPointGroupEntry {
        private Integer rawValue0;
        private Integer rawValue1;
    }

    @Data
    public static class RawRectGroupBlock {
        private Integer count;
        private List<RawRectGroupEntry> entries = new ArrayList<>();
    }

    @Data
    public static class RawPointGroupBlock {
        private Integer count;
        private List<RawPointGroupEntry> entries = new ArrayList<>();
    }

    @Data
    public static class NamedResourceSlotBlock {
        public Integer slotNum; // 仅记录用
        private String slotText;
        private Integer param0;
        private Integer param1;
        private Integer param2;
        private Integer param3;
        private Integer param4;
    }

    @Data
    public static class ScriptEntryGroupBlock {
        public Integer groupNum; // 仅记录用
        private Integer count;
        private List<ScriptEntry> entries = new ArrayList<>();
    }

    @Data
    public static class ScriptEntry {
        private Integer typeId;
        private Integer x;
        private Integer y;
    }

}
