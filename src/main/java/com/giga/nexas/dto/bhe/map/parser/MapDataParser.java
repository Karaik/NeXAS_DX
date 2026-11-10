package com.giga.nexas.dto.bhe.map.parser;

import com.giga.nexas.dto.bhe.BheParser;
import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.io.BinaryReader;

import java.util.ArrayList;
import java.util.List;

public class MapDataParser implements BheParser<MapData> {

    private static final String MAGIC_PREFIX = "MAPDATA";

    @Override
    public String supportExtension() {
        return "map";
    }

    @Override
    public MapData parse(byte[] data, String filename, String charset) {
        BinaryReader reader = new BinaryReader(data, charset);
        MapData mapData = new MapData(filename);

        String magic = reader.readNullTerminatedString();
        mapData.setMagic(magic);
        if (!hasMagicPrefix(magic)) {
            throw new IllegalArgumentException("unexpected map magic: " + magic);
        }
        if (magic.length() < 16) {
            throw new IllegalArgumentException("unexpected bhe map version text: " + magic);
        }
        ensureVersionDigits(magic);
        int versionScore = readVersionScore(magic);
        if (versionScore > 100) {
            throw new IllegalArgumentException("unsupported bhe map version: " + versionScore);
        }

        mapData.setWidth(reader.readInt());
        mapData.setHeight(reader.readInt());

        List<MapData.TileRecord> tileRecords = new ArrayList<>();
        for (int y = 0; y < mapData.getHeight(); y++) {
            for (int x = 0; x < mapData.getWidth(); x++) {
                tileRecords.add(readTileRecord(reader));
            }
        }
        mapData.setTileRecords(tileRecords);

        mapData.setCameraRectBlock(readCameraRectBlock(reader));

        mapData.setRawPointGroup0(readRawPointGroupBlock(reader));
        mapData.setRawPointGroup1(readRawPointGroupBlock(reader));

        mapData.setRawRectGroup0(readRawRectGroupBlock(reader));
        mapData.setRawRectGroup1(readRawRectGroupBlock(reader));
        mapData.setRawRectGroup2(readRawRectGroupBlock(reader));

        List<MapData.NamedResourceSlotBlock> resourceSlotBlocks = new ArrayList<>();
        for (int i = 0; i < MapData.RESOURCE_SLOT_COUNT; i++) {
            resourceSlotBlocks.add(readResourceSlotBlock(reader, i));
        }
        mapData.setNamedResourceSlotBlocks(resourceSlotBlocks);

        List<MapData.ScriptEntryGroupBlock> scriptGroupBlocks = new ArrayList<>();
        for (int i = 0; i < MapData.SCRIPT_GROUP_COUNT; i++) {
            scriptGroupBlocks.add(readScriptGroupBlock(reader, i, versionScore));
        }
        mapData.setScriptEntryGroupBlocks(scriptGroupBlocks);

        mapData.setForegroundImage(reader.readNullTerminatedString());

        if (versionScore >= 100) {
            byte spriteMapCount = reader.readByte();
            mapData.setSpriteMapCount(spriteMapCount);
            List<String> spriteMapList = new ArrayList<>();
            if (spriteMapCount > 0) {
                for (int i = 0; i < spriteMapCount; i++) {
                    spriteMapList.add(reader.readNullTerminatedString());
                }
            }
            mapData.setSpriteMapList(spriteMapList);
        } else {
            mapData.setSpriteMap(reader.readNullTerminatedString());
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

    private MapData.ScriptEntryGroupBlock readScriptGroupBlock(BinaryReader reader, int groupNum, int versionScore) {
        MapData.ScriptEntryGroupBlock block = new MapData.ScriptEntryGroupBlock();
        block.setGroupNum(groupNum);
        int count = reader.readInt();
        block.setCount(count);
        List<MapData.ScriptEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MapData.ScriptEntry entry = new MapData.ScriptEntry();
            if (versionScore >= 100) {
                entry.setGroupIndex(reader.readInt());
            } else {
                entry.setGroupIndex(0);
            }
            entry.setTypeId(reader.readInt());
            entry.setX(reader.readInt());
            entry.setY(reader.readInt());
            entries.add(entry);
        }
        block.setEntries(entries);
        return block;
    }

    private int readVersionScore(String magic) {
        return parseAsciiDigit(magic, 12) * 100
                + parseAsciiDigit(magic, 14) * 10
                + parseAsciiDigit(magic, 15);
    }

    private void ensureVersionDigits(String magic) {
        if (!isAsciiDigit(magic, 12) || !isAsciiDigit(magic, 14) || !isAsciiDigit(magic, 15)) {
            throw new IllegalArgumentException("unexpected bhe map version text: " + magic);
        }
    }

    private int parseAsciiDigit(String text, int index) {
        if (index < 0 || index >= text.length()) {
            return 0;
        }
        char c = text.charAt(index);
        if (c < '0' || c > '9') {
            return 0;
        }
        return c - '0';
    }

    private boolean hasMagicPrefix(String text) {
        if (text.length() < MAGIC_PREFIX.length()) {
            return false;
        }
        for (int i = 0; i < MAGIC_PREFIX.length(); i++) {
            if (text.charAt(i) != MAGIC_PREFIX.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private boolean isAsciiDigit(String text, int index) {
        if (index < 0 || index >= text.length()) {
            return false;
        }
        char c = text.charAt(index);
        return c >= '0' && c <= '9';
    }
}
