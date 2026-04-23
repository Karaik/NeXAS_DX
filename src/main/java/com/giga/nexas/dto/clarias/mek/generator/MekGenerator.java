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

    private static final int MATERIAL_FIXED_REGULAR_ENTRY_COUNT = 7;

    @Override
    public String supportExtension() {
        return "mek";
    }

    @Override
    public void generate(String path, Mek mek, String charset) throws IOException {
        FileUtil.mkdir(FileUtil.getParent(path, 1));
        File newFile = new File(path);

        byte[] block1 = serializeBlock1(mek, charset);
        byte[] block2 = serializePairBlock(mek);
        byte[] block3 = serializeMekWeaponInfoMap(mek, charset);
        byte[] block4 = serializeMekAiInfoMap(mek, charset);
        byte[] block5 = serializeMekVoiceInfo(mek, charset);
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
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BinaryWriter writer = new BinaryWriter(baos, charset)) {
            writer.writeNullTerminatedString(info.getStringField1());
            writer.writeNullTerminatedString(info.getStringField2());
            writer.writeNullTerminatedString(info.getStringField3());
            writer.writeNullTerminatedString(info.getStringField4());
            writer.writeNullTerminatedString(info.getStringField5());

            writer.writeInt(info.getIntField1());
            writer.writeInt(info.getIntField2());
            writer.writeInt(info.getIntField3());
            writer.writeInt(info.getIntField4());
            writer.writeInt(info.getIntField5());
            writer.writeInt(info.getIntField6());
            writer.writeInt(info.getIntField7());
            writer.writeInt(info.getIntField8());
            writer.writeInt(info.getIntField9());
            writer.writeInt(info.getIntField10());
            writer.writeInt(info.getIntField11());
            writer.writeInt(info.getIntField12());
            writer.writeByte(info.getByteField1() == null ? 0 : info.getByteField1());
            writer.writeInt(info.getIntField13());
            writer.writeInt(info.getIntField14());
            writer.writeInt(info.getIntField15());
            writer.writeInt(info.getIntField16());
            writer.writeInt(info.getIntField17());
            writer.writeInt(info.getIntField18());
        }
        return baos.toByteArray();
    }

    private static byte[] serializeMekWeaponInfoMap(Mek mek, String charset) throws IOException {
        Map<Integer, Mek.MekWeaponInfo> weaponInfoMap = mek.getMekWeaponInfoMap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BinaryWriter writer = new BinaryWriter(baos, charset)) {
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
        }
        return baos.toByteArray();
    }

    private static byte[] serializeMekAiInfoMap(Mek mek, String charset) throws IOException {
        List<Mek.MekAiInfo> aiInfos = mek.getMekAiInfoList();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BinaryWriter writer = new BinaryWriter(baos, charset)) {
            writer.writeInt(aiInfos.size());
            for (Mek.MekAiInfo ai : aiInfos) {
                writer.writeNullTerminatedString(ai.getStringField1());
                writer.writeNullTerminatedString(ai.getStringField2());

                List<CCpuEvent> events = ai.getCpuEventList();
                writer.writeInt(events.size());
                for (CCpuEvent cpuEvent : events) {
                    short type = cpuEvent.getType();
                    writer.writeShort(type);
                    cpuEvent.writeInfo(writer);
                }
            }
            int trailingZeroByteCount = mek.getAiTrailingZeroByteCount() == null ? 0 : mek.getAiTrailingZeroByteCount();
            for (int i = 0; i < trailingZeroByteCount; i++) {
                writer.writeByte((byte) 0);
            }
        }
        return baos.toByteArray();
    }

    private static byte[] serializeMekVoiceInfo(Mek mek, String charset) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BinaryWriter writer = new BinaryWriter(baos, charset)) {
            Mek.MekVoiceInfo voiceInfo = mek.getMekVoiceInfo();
            List<Mek.MekVoiceInfo.Emotion> emotions = voiceInfo.getEmotions();
            List<Mek.MekVoiceInfo.VoiceSlot> voiceSlots = voiceInfo.getVoiceSlots();

            writer.writeInt(voiceInfo.getVersion());

            writer.writeInt(emotions.size());
            for (Mek.MekVoiceInfo.Emotion emotion : emotions) {
                writer.writeNullTerminatedString(emotion.getName());
                writer.writeNullTerminatedString(emotion.getToken());
            }

            writer.writeInt(voiceSlots.size());
            for (Mek.MekVoiceInfo.VoiceSlot voiceSlot : voiceSlots) {
                writer.writeNullTerminatedString(voiceSlot.getName());
                writer.writeNullTerminatedString(voiceSlot.getToken());
            }

            int slotCount = voiceSlots.size();
            for (List<List<Mek.MekVoiceInfo.Entry>> row : voiceInfo.getTable()) {
                for (int i = 0; i < slotCount; i++) {
                    List<Mek.MekVoiceInfo.Entry> cell = row.get(i);
                    writer.writeInt(cell.size());
                    for (Mek.MekVoiceInfo.Entry entry : cell) {
                        writer.writeInt(entry.getVoiceType() == null ? 0 : entry.getVoiceType());
                        writer.writeInt(entry.getGroupId() == null ? 0 : entry.getGroupId());
                        writer.writeInt(entry.getWeight() == null ? 0 : entry.getWeight());
                    }
                }
            }
        }
        return baos.toByteArray();
    }

    private static byte[] serializeTailBlock(Mek mek) {
        Mek.MekMaterialBlock block = mek.getMekMaterialBlock();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BinaryWriter writer = new BinaryWriter(baos, "windows-31j")) {
            List<Mek.MekMaterialBlock.PluginEntry> stream = buildMaterialEntryStream(block);

            for (int i = 0; i < MATERIAL_FIXED_REGULAR_ENTRY_COUNT; i++) {
                writeMaterialEntry(writer, stream.get(i));
            }

            int extraRegularCount = resolveExtraRegularCount(block);
            writer.writeInt(extraRegularCount);

            for (int i = 0; i < extraRegularCount; i++) {
                writeMaterialEntry(writer, stream.get(MATERIAL_FIXED_REGULAR_ENTRY_COUNT + i));
            }

            for (int i = MATERIAL_FIXED_REGULAR_ENTRY_COUNT + extraRegularCount; i < stream.size(); i++) {
                writeMaterialEntry(writer, stream.get(i));
            }
        } catch (IOException e) {
            throw new RuntimeException("failed to serialize clarias mek material block", e);
        }
        return baos.toByteArray();
    }

    private static byte[] serializePairBlock(Mek mek) throws IOException {
        Mek.MekPairBlock pairBlock = mek.getMekPairBlock();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BinaryWriter writer = new BinaryWriter(baos, "windows-31j")) {
            for (Mek.MekPairBlock.Pair pair : pairBlock.getUnkPair()) {
                writer.writeInt(pair.getInt1());
                writer.writeInt(pair.getInt2());
            }
        }
        return baos.toByteArray();
    }

    private static List<Mek.MekMaterialBlock.PluginEntry> buildMaterialEntryStream(Mek.MekMaterialBlock block) {
        List<Mek.MekMaterialBlock.PluginEntry> regularEntries = block.getRegularEntries();
        List<Mek.MekMaterialBlock.PluginEntry> trailingEntries = block.getTrailingEntries();
        List<Mek.MekMaterialBlock.PluginEntry> entries = block.getEntries();

        int regularCount = resolveRegularCount(block);
        if (regularCount < MATERIAL_FIXED_REGULAR_ENTRY_COUNT) {
            throw new OperationException(500, "materialBlock regularCount < " + MATERIAL_FIXED_REGULAR_ENTRY_COUNT);
        }

        List<Mek.MekMaterialBlock.PluginEntry> stream = new java.util.ArrayList<>();
        if (regularEntries != null && !regularEntries.isEmpty()) {
            stream.addAll(regularEntries);
        } else if (entries != null && entries.size() >= regularCount) {
            stream.addAll(entries.subList(0, regularCount));
        } else {
            throw new OperationException(500, "materialBlock missing regular entries");
        }

        if (trailingEntries != null && !trailingEntries.isEmpty()) {
            stream.addAll(trailingEntries);
        } else if (entries != null && entries.size() > regularCount) {
            stream.addAll(entries.subList(regularCount, entries.size()));
        }

        if (stream.size() < regularCount) {
            throw new OperationException(500, "materialBlock stream size < regularCount");
        }
        return stream;
    }

    private static int resolveRegularCount(Mek.MekMaterialBlock block) {
        if (block.getRegularEntries() != null && !block.getRegularEntries().isEmpty()) {
            return block.getRegularEntries().size();
        }
        if (block.getRegularCount() != null) {
            return block.getRegularCount();
        }
        if (block.getExtraRegularCount() != null) {
            return MATERIAL_FIXED_REGULAR_ENTRY_COUNT + block.getExtraRegularCount();
        }
        throw new OperationException(500, "materialBlock requires regularEntries/regularCount/extraRegularCount");
    }

    private static int resolveExtraRegularCount(Mek.MekMaterialBlock block) {
        int regularCount = resolveRegularCount(block);
        return regularCount - MATERIAL_FIXED_REGULAR_ENTRY_COUNT;
    }

    private static void writeMaterialEntry(BinaryWriter writer, Mek.MekMaterialBlock.PluginEntry entry) throws IOException {
        writePairGroupSegment(writer, entry.getSpriteGroups());
        writeIntGroupSegment(writer, entry.getSeGroups());
        writeIntGroupSegment(writer, entry.getVoiceGroups());
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
