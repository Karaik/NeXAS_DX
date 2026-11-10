package com.giga.nexas.dto.bsdx.map.generator;

import cn.hutool.core.io.FileUtil;
import com.giga.nexas.dto.bsdx.BsdxGenerator;
import com.giga.nexas.dto.bsdx.map.MapData;
import com.giga.nexas.io.BinaryWriter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @Author 这位同学(Karaik)
 * @Date 2026/5/14
 * @Description MapDataGenerator
 */
public class MapDataGenerator implements BsdxGenerator<MapData> {

    @Override
    public String supportExtension() {
        return "map";
    }

    @Override
    public void generate(String path, MapData mapData, String charset) throws IOException {
        FileUtil.mkdir(FileUtil.getParent(path, 1));

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(path));
             BinaryWriter writer = new BinaryWriter(os, charset)) {

            // 1. header
            writer.writeNullTerminatedString(mapData.getMagic());
            writer.writeInt(mapData.getWidth());
            writer.writeInt(mapData.getHeight());

            // 2. tile block
            for (MapData.TileRecord record : mapData.getTileRecords()) {
                writeTileRecord(writer, record);
            }

            // 3. first 16-byte block
            writeCameraRectBlock(writer, mapData.getCameraRectBlock());

            // 4. two 8-byte blocks
            writeRawPointGroupBlock(writer, mapData.getRawPointGroup0());
            writeRawPointGroupBlock(writer, mapData.getRawPointGroup1());

            // 5. three 16-byte blocks
            writeRawRectGroupBlock(writer, mapData.getRawRectGroup0());
            writeRawRectGroupBlock(writer, mapData.getRawRectGroup1());
            writeRawRectGroupBlock(writer, mapData.getRawRectGroup2());

            // 6. resource slot blocks
            for (int i = 0; i < MapData.RESOURCE_SLOT_COUNT; i++) {
                writeResourceSlotBlock(writer, mapData.getNamedResourceSlotBlocks().get(i));
            }

            // 7. script group blocks
            for (int i = 0; i < MapData.SCRIPT_GROUP_COUNT; i++) {
                writeScriptGroupBlock(writer, mapData.getScriptEntryGroupBlocks().get(i));
            }

            // 8. tail strings
            writer.writeNullTerminatedString(mapData.getForegroundImage());
            writer.writeNullTerminatedString(mapData.getSpriteMap());
        }
    }

    private void writeTileRecord(BinaryWriter writer, MapData.TileRecord record) throws IOException {
        writer.writeInt(record.getPackedValue0());
        writer.writeInt(record.getPackedValue1());
        writer.writeByte(record.getByte0());
        writer.writeByte(record.getByte1());
    }

    private void writeCameraRectBlock(BinaryWriter writer, MapData.CameraRectBlock block) throws IOException {
        writer.writeInt(block.getCount());
        for (MapData.CameraRectEntry entry : block.getEntries()) {
            writer.writeInt(entry.getLeft());
            writer.writeInt(entry.getTop());
            writer.writeInt(entry.getRight());
            writer.writeInt(entry.getBottom());
        }
    }

    private void writeRawPointGroupBlock(BinaryWriter writer, MapData.RawPointGroupBlock block) throws IOException {
        writer.writeInt(block.getCount());
        for (MapData.RawPointGroupEntry entry : block.getEntries()) {
            writer.writeInt(entry.getRawValue0());
            writer.writeInt(entry.getRawValue1());
        }
    }

    private void writeRawRectGroupBlock(BinaryWriter writer, MapData.RawRectGroupBlock block) throws IOException {
        writer.writeInt(block.getCount());
        for (MapData.RawRectGroupEntry entry : block.getEntries()) {
            writer.writeInt(entry.getRawValue0());
            writer.writeInt(entry.getRawValue1());
            writer.writeInt(entry.getRawValue2());
            writer.writeInt(entry.getRawValue3());
        }
    }

    private void writeResourceSlotBlock(BinaryWriter writer, MapData.NamedResourceSlotBlock block) throws IOException {
        writer.writeNullTerminatedString(block.getSlotText());
        writer.writeInt(block.getParam0());
        writer.writeInt(block.getParam1());
        writer.writeInt(block.getParam2());
        writer.writeInt(block.getParam3());
        writer.writeInt(block.getParam4());
    }

    private void writeScriptGroupBlock(BinaryWriter writer, MapData.ScriptEntryGroupBlock block) throws IOException {
        writer.writeInt(block.getCount());
        for (MapData.ScriptEntry entry : block.getEntries()) {
            writer.writeInt(entry.getTypeId());
            writer.writeInt(entry.getX());
            writer.writeInt(entry.getY());
        }
    }
}
