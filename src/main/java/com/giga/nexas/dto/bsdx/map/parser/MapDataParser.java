package com.giga.nexas.dto.bsdx.map.parser;

import com.giga.nexas.dto.bsdx.BsdxParser;
import com.giga.nexas.dto.bsdx.map.MapData;
import com.giga.nexas.io.BinaryReader;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2026/5/14
 * @Description MapDataParser
 */
public class MapDataParser implements BsdxParser<MapData> {

    private static final String MAGIC_PREFIX = "MAPDATA";

    @Override
    public String supportExtension() {
        return "map";
    }

    @Override
    public MapData parse(byte[] data, String filename, String charset) {
        BinaryReader reader = new BinaryReader(data, charset);
        MapData mapData = new MapData(filename);

        // 1. header
        String magic = reader.readNullTerminatedString();
        mapData.setMagic(magic);
        if (!magic.startsWith(MAGIC_PREFIX)) {
            throw new IllegalArgumentException("unexpected map magic: " + magic);
        }
        mapData.setWidth(reader.readInt());
        mapData.setHeight(reader.readInt());

        // 2. tile block
        List<MapData.TileRecord> tileRecords = new ArrayList<>();
        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                tileRecords.add(readTileRecord(reader));
            }
        }
        mapData.setTileRecords(tileRecords);

        // 3. first 16-byte block
        mapData.setCameraRectBlock(readCameraRectBlock(reader));

        // 4. two 8-byte blocks
        mapData.setRawPointGroup0(readRawPointGroupBlock(reader));
        mapData.setRawPointGroup1(readRawPointGroupBlock(reader));

        // 5. three 16-byte blocks
        mapData.setRawRectGroup0(readRawRectGroupBlock(reader));
        mapData.setRawRectGroup1(readRawRectGroupBlock(reader));
        mapData.setRawRectGroup2(readRawRectGroupBlock(reader));

        // 6. resource slot blocks
        List<MapData.NamedResourceSlotBlock> resourceSlotBlocks = new ArrayList<>();
        for (int i = 0; i < MapData.RESOURCE_SLOT_COUNT; i++) {
            resourceSlotBlocks.add(readResourceSlotBlock(reader, i));
        }
        mapData.setNamedResourceSlotBlocks(resourceSlotBlocks);

        // 7. script group blocks
        List<MapData.ScriptEntryGroupBlock> scriptGroupBlocks = new ArrayList<>();
        for (int i = 0; i < MapData.SCRIPT_GROUP_COUNT; i++) {
            scriptGroupBlocks.add(readScriptGroupBlock(reader, i));
        }
        mapData.setScriptEntryGroupBlocks(scriptGroupBlocks);

        // 8. tail strings
        mapData.setForegroundImage(reader.readNullTerminatedString());
        mapData.setSpriteMap(reader.readNullTerminatedString());

        if (reader.hasRemaining()) {
            throw new IllegalArgumentException("unexpected trailing bytes after spriteMap: " + reader.remaining());
        }

        return mapData;
    }

    private MapData.TileRecord readTileRecord(BinaryReader reader) {
        MapData.TileRecord record = new MapData.TileRecord();
        record.setPackedValue0(reader.readInt());
        record.setPackedValue1(reader.readInt());
        record.setByte0(reader.readByte());
        record.setByte1(reader.readByte());
        return record;
    }

    private MapData.CameraRectBlock readCameraRectBlock(BinaryReader reader) {
        int count = reader.readInt();
        MapData.CameraRectBlock block = new MapData.CameraRectBlock();
        block.setCount(count);
        List<MapData.CameraRectEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MapData.CameraRectEntry entry = new MapData.CameraRectEntry();
            entry.setLeft(reader.readInt());
            entry.setTop(reader.readInt());
            entry.setRight(reader.readInt());
            entry.setBottom(reader.readInt());
            list.add(entry);
        }
        block.setEntries(list);
        return block;
    }

    private MapData.RawPointGroupBlock readRawPointGroupBlock(BinaryReader reader) {
        int count = reader.readInt();
        MapData.RawPointGroupBlock block = new MapData.RawPointGroupBlock();
        block.setCount(count);
        List<MapData.RawPointGroupEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MapData.RawPointGroupEntry entry = new MapData.RawPointGroupEntry();
            entry.setRawValue0(reader.readInt());
            entry.setRawValue1(reader.readInt());
            list.add(entry);
        }
        block.setEntries(list);
        return block;
    }

    private MapData.RawRectGroupBlock readRawRectGroupBlock(BinaryReader reader) {
        int count = reader.readInt();
        MapData.RawRectGroupBlock block = new MapData.RawRectGroupBlock();
        block.setCount(count);
        List<MapData.RawRectGroupEntry> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MapData.RawRectGroupEntry entry = new MapData.RawRectGroupEntry();
            entry.setRawValue0(reader.readInt());
            entry.setRawValue1(reader.readInt());
            entry.setRawValue2(reader.readInt());
            entry.setRawValue3(reader.readInt());
            list.add(entry);
        }
        block.setEntries(list);
        return block;
    }

    private MapData.NamedResourceSlotBlock readResourceSlotBlock(BinaryReader reader, int slotNum) {
        MapData.NamedResourceSlotBlock block = new MapData.NamedResourceSlotBlock();
        block.setSlotNum(slotNum);
        block.setSlotText(reader.readNullTerminatedString());
        block.setParam0(reader.readInt());
        block.setParam1(reader.readInt());
        block.setParam2(reader.readInt());
        block.setParam3(reader.readInt());
        block.setParam4(reader.readInt());
        return block;
    }

    private MapData.ScriptEntryGroupBlock readScriptGroupBlock(BinaryReader reader, int groupNum) {
        MapData.ScriptEntryGroupBlock block = new MapData.ScriptEntryGroupBlock();
        block.setGroupNum(groupNum);
        int count = reader.readInt();
        block.setCount(count);
        List<MapData.ScriptEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MapData.ScriptEntry entry = new MapData.ScriptEntry();
            entry.setTypeId(reader.readInt());
            entry.setX(reader.readInt());
            entry.setY(reader.readInt());
            entries.add(entry);
        }
        block.setEntries(entries);
        return block;
    }
}
