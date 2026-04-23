package com.giga.nexas.dto.clarias.mek.generator;

import cn.hutool.core.io.FileUtil;
import com.giga.nexas.dto.clarias.ClariasGenerator;
import com.giga.nexas.dto.clarias.mek.Mek;
import com.giga.nexas.dto.clarias.mek.mekcpu.CCpuEvent;
import com.giga.nexas.exception.OperationException;
import com.giga.nexas.io.BinaryWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class MekGenerator implements ClariasGenerator<Mek> {

    @Override
    public String supportExtension() {
        return "mek";
    }

    @Override
    public void generate(String path, Mek mek, String charset) throws IOException {
        FileUtil.mkdir(FileUtil.getParent(path, 1));
        File newFile = new File(path);

        byte[] block1 = hasRaw(mek.getRawBlock1()) ? mek.getRawBlock1() : serializeBlock1(mek, charset);
        byte[] block2 = hasRaw(mek.getRawBlock2()) ? mek.getRawBlock2() : serializePairBlock(mek);
        byte[] block3 = hasRaw(mek.getRawBlock3()) ? mek.getRawBlock3() : serializeMekWeaponInfoMap(mek, charset);
        byte[] block4 = hasRaw(mek.getRawBlock4()) ? mek.getRawBlock4() : serializeMekAiInfoMap(mek, charset);
        byte[] block5 = hasRaw(mek.getRawBlock5()) ? mek.getRawBlock5() : serializeSimpleBlock(mek, charset);
        byte[] block6 = serializeTailBlock(mek);

        int sequence1 = 24;
        int sequence2 = sequence1 + block1.length;
        int sequence3 = sequence2 + block2.length;
        int sequence4 = sequence3 + block3.length;
        int sequence5 = sequence4 + block4.length;
        int sequence6 = sequence5 + block5.length;

        try (FileOutputStream fos = new FileOutputStream(newFile);
             BinaryWriter writer = new BinaryWriter(fos, charset)) {
            writer.writeInt(sequence1);
            writer.writeInt(sequence2);
            writer.writeInt(sequence3);
            writer.writeInt(sequence4);
            writer.writeInt(sequence5);
            writer.writeInt(sequence6);

            writer.writeBytes(block1);
            writer.writeBytes(block2);
            writer.writeBytes(block3);
            writer.writeBytes(block4);
            writer.writeBytes(block5);
            writer.writeBytes(block6);
        }
    }

    private static byte[] serializeBlock1(Mek mek, String charset) throws IOException {
        Mek.MekBasicInfo info = mek.getMekBasicInfo();
        Mek.MekPairBlock pairBlock = mek.getMekPairBlock();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BinaryWriter writer = new BinaryWriter(baos, charset)) {
            writer.writeNullTerminatedString(info.getStringField1());
            writer.writeNullTerminatedString(info.getStringField2());
            writer.writeNullTerminatedString(info.getStringField3());
            writer.writeNullTerminatedString(info.getStringField4());
            writer.writeNullTerminatedString(info.getStringField5());

            for (Integer value : info.getLeadingInts()) {
                writer.writeInt(value);
            }
            writer.writeByte(info.getFlagByte() == null ? 0 : info.getFlagByte());
            for (Integer value : info.getTrailingInts()) {
                writer.writeInt(value);
            }

            if (pairBlock.getRawBytes() != null && pairBlock.getRawBytes().length > 0) {
                writer.writeBytes(pairBlock.getRawBytes());
            } else {
                for (Mek.MekPairBlock.Pair pair : pairBlock.getUnkPair()) {
                    writer.writeInt(pair.getInt1());
                    writer.writeInt(pair.getInt2());
                }
            }
            return baos.toByteArray();
        }
    }

    private static byte[] serializeMekWeaponInfoMap(Mek mek, String charset) throws IOException {
        Map<Integer, Mek.MekWeaponInfo> weaponInfoMap = mek.getMekWeaponInfoMap();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BinaryWriter writer = new BinaryWriter(baos, charset)) {
            writer.writeInt(weaponInfoMap.size());
            for (Mek.MekWeaponInfo weaponInfo : weaponInfoMap.values()) {
                writer.writeInt(weaponInfo.getEnabled() == null ? 0 : weaponInfo.getEnabled());
                if (weaponInfo.getEnabled() == null || weaponInfo.getEnabled() == 0) {
                    continue;
                }
                writer.writeNullTerminatedString(weaponInfo.getStringField1());
                writer.writeNullTerminatedString(weaponInfo.getStringField2());
                writer.writeNullTerminatedString(weaponInfo.getStringField3());

                for (Integer value : weaponInfo.getLeadingInts()) {
                    writer.writeInt(value);
                }
                writer.writeByte(weaponInfo.getFlagByte() == null ? 0 : weaponInfo.getFlagByte());
                for (Integer value : weaponInfo.getTrailingInts()) {
                    writer.writeInt(value);
                }

                List<Mek.MekWeaponInfo.Variant> variants = weaponInfo.getVariants();
                int variantCount = weaponInfo.getVariantCount() == null ? variants.size() : weaponInfo.getVariantCount();
                writer.writeInt(variantCount);
                for (int i = 0; i < variantCount; i++) {
                    Mek.MekWeaponInfo.Variant variant = variants.get(i);
                    writer.writeInt(variant.getInt1());
                    writer.writeInt(variant.getInt2());
                    writer.writeInt(variant.getInt3());
                    writer.writeInt(variant.getInt4());
                    writer.writeInt(variant.getInt5());
                    writer.writeInt(variant.getInt6());
                    writer.writeInt(variant.getInt7());
                }
            }
            return baos.toByteArray();
        }
    }

    private static byte[] serializeMekAiInfoMap(Mek mek, String charset) throws IOException {
        List<Mek.MekAiInfo> aiInfos = mek.getMekAiInfoList();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BinaryWriter writer = new BinaryWriter(baos, charset)) {
            writer.writeInt(aiInfos.size());
            for (Mek.MekAiInfo ai : aiInfos) {
                writer.writeNullTerminatedString(ai.getStringField1());
                writer.writeNullTerminatedString(ai.getStringField2());

                List<CCpuEvent> events = ai.getCpuEventList();
                writer.writeInt(events.size());
                for (CCpuEvent cpuEvent : events) {
                    short type = cpuEvent.getType();
                    writer.writeShort(type);
                    if (type == 1 || type == 2) {
                        cpuEvent.writeInfo(writer);
                    } else {
                        throw new OperationException(500, "unexpected AI type: " + type);
                    }
                }
            }
            return baos.toByteArray();
        }
    }

    private static byte[] serializeSimpleBlock(Mek mek, String charset) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BinaryWriter writer = new BinaryWriter(baos, charset)) {
            writer.writeInt(mek.getMekSimpleBlockEntries().size());
            for (Mek.MekSimpleBlockEntry entry : mek.getMekSimpleBlockEntries()) {
                writer.writeInt(entry.getInt1());
                writer.writeInt(entry.getInt2());
                writer.writeInt(entry.getInt3());
            }
            return baos.toByteArray();
        }
    }

    private static byte[] serializeTailBlock(Mek mek) {
        Mek.MekMaterialBlock block = mek.getMekMaterialBlock();
        if (hasRaw(block.getRawBytes())) {
            return block.getRawBytes();
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BinaryWriter writer = new BinaryWriter(baos, "windows-31j")) {
            for (Mek.MekMaterialBlock.MaterialSnapshot snapshot : block.getSnapshots()) {
                writePairGroupSegment(writer, snapshot.getGroupSegment1());
                writeIntGroupSegment(writer, snapshot.getGroupSegment2());
                writeIntGroupSegment(writer, snapshot.getGroupSegment3());
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("failed to serialize clarias mek material block", e);
        }
    }

    private static byte[] serializePairBlock(Mek mek) throws IOException {
        Mek.MekPairBlock pairBlock = mek.getMekPairBlock();
        byte[] rawBytes = pairBlock.getRawBytes();
        if (rawBytes != null && rawBytes.length > 0) {
            return rawBytes;
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BinaryWriter writer = new BinaryWriter(baos, "windows-31j")) {
            for (Mek.MekPairBlock.Pair pair : pairBlock.getUnkPair()) {
                writer.writeInt(pair.getInt1());
                writer.writeInt(pair.getInt2());
            }
            return baos.toByteArray();
        }
    }

    private static boolean hasRaw(byte[] rawBytes) {
        return rawBytes != null && rawBytes.length > 0;
    }

    private static void writePairGroupSegment(BinaryWriter writer, List<int[]> groups) throws IOException {
        int groupCount = groups == null ? 0 : groups.size();
        writer.writeInt(groupCount);
        if (groups == null) {
            return;
        }
        for (int[] arr : groups) {
            int pairCount = arr == null ? 0 : arr.length / 2;
            writer.writeInt(pairCount);
            if (arr == null) {
                continue;
            }
            for (int i = 0; i < pairCount; i++) {
                writer.writeInt(arr[i * 2]);
                writer.writeInt(arr[i * 2 + 1]);
            }
        }
    }

    private static void writeIntGroupSegment(BinaryWriter writer, List<int[]> groups) throws IOException {
        int groupCount = groups == null ? 0 : groups.size();
        writer.writeInt(groupCount);
        if (groups == null) {
            return;
        }
        for (int[] arr : groups) {
            int len = arr == null ? 0 : arr.length;
            writer.writeInt(len);
            if (arr == null) {
                continue;
            }
            for (int value : arr) {
                writer.writeInt(value);
            }
        }
    }
}
