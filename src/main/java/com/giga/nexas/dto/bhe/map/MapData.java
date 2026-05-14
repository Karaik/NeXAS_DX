package com.giga.nexas.dto.bhe.map;

import com.giga.nexas.dto.bhe.Bhe;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MapData extends Bhe {

    public static final int RESOURCE_SLOT_COUNT = 8;
    public static final int SCRIPT_GROUP_COUNT = 9;

    public String fileName; // 仅记录用

    private String magic;
    private Integer width;
    private Integer height;

    private List<TileRecord> tileRecords = new ArrayList<>();

    private CameraRectBlock cameraRectBlock = new CameraRectBlock();

    private RawPointGroupBlock rawPointGroup0 = new RawPointGroupBlock();
    private RawPointGroupBlock rawPointGroup1 = new RawPointGroupBlock();

    private RawRectGroupBlock rawRectGroup0 = new RawRectGroupBlock();
    private RawRectGroupBlock rawRectGroup1 = new RawRectGroupBlock();
    private RawRectGroupBlock rawRectGroup2 = new RawRectGroupBlock();

    private List<NamedResourceSlotBlock> namedResourceSlotBlocks = new ArrayList<>();

    private List<ScriptEntryGroupBlock> scriptEntryGroupBlocks = new ArrayList<>();

    private String foregroundImage;
    private String spriteMap;

    // diff vs BSDX: BHE VER-1.00 第二段为 int8 count + count * cstring
    private Byte spriteMapCount;
    private List<String> spriteMapList = new ArrayList<>();

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
        // diff vs BSDX: BHE VER-1.00 单条最前面多一个 int32
        private Integer groupIndex;
        private Integer typeId;
        private Integer x;
        private Integer y;
    }
}
