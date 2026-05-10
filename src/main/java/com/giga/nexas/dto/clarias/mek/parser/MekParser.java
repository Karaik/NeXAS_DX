package com.giga.nexas.dto.clarias.mek.parser;

import com.giga.nexas.dto.clarias.ClariasParser;
import com.giga.nexas.dto.clarias.mek.Mek;
import com.giga.nexas.dto.clarias.mek.checker.MekChecker;
import com.giga.nexas.dto.clarias.mek.mekcpu.CCpuEvent;
import com.giga.nexas.dto.clarias.mek.mekcpu.CCpuEventAttack;
import com.giga.nexas.dto.clarias.mek.mekcpu.CCpuEventMove;
import com.giga.nexas.dto.clarias.mek.mekcpu.CCpuEventUnknown;
import com.giga.nexas.exception.OperationException;
import com.giga.nexas.io.BinaryReader;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class MekParser implements ClariasParser<Mek> {

    private static final int WEAPON_LEADING_INT_COUNT = 5;
    private static final int WEAPON_TRAILING_INT_COUNT = 4;
    private static final int MATERIAL_FIXED_REGULAR_ENTRY_COUNT = 7;

    @Override
    public String supportExtension() {
        return "mek";
    }

    @Override
    public Mek parse(byte[] bytes, String filename, String charset) {
        Mek mek = new Mek();
        try {
            parseMekHead(mek, bytes);
            if (MekChecker.checkMek(mek, bytes)) {
                throw new OperationException(500, "invalid file header!");
            }
            mek.setFileName(filename);

            Mek.MekHead head = mek.getMekHead();
            byte[] block1 = Arrays.copyOfRange(bytes, head.getSequence1(), head.getSequence2());
            byte[] block2 = Arrays.copyOfRange(bytes, head.getSequence2(), head.getSequence3());
            byte[] block3 = Arrays.copyOfRange(bytes, head.getSequence3(), head.getSequence4());
            byte[] block4 = Arrays.copyOfRange(bytes, head.getSequence4(), head.getSequence5());
            byte[] block5 = Arrays.copyOfRange(bytes, head.getSequence5(), head.getSequence6());
            byte[] block6 = Arrays.copyOfRange(bytes, head.getSequence6(), bytes.length);

            parseBlock1(mek, block1, charset);
            parsePairBlock(mek, block2);
            parseWeaponInfo(mek, block3, charset);
            parseAiBlock(mek, block4, charset);
            parseVoiceBlock(mek, block5, charset);
            parseTailBlock(mek, block6);
        } catch (Exception e) {
            log.info("error === {}", e.getMessage());
            throw e;
        }
        return mek;
    }

    private static void parseMekHead(Mek mek, byte[] bytes) {
        Mek.MekHead mekHead = mek.getMekHead();
        BinaryReader reader = new BinaryReader(bytes);
        mekHead.setSequence1(reader.readInt());
        mekHead.setSequence2(reader.readInt());
        mekHead.setSequence3(reader.readInt());
        mekHead.setSequence4(reader.readInt());
        mekHead.setSequence5(reader.readInt());
        mekHead.setSequence6(reader.readInt());
        mek.getMekBlocks().calculateBlockSizes(mekHead);
    }

    private static void parseBlock1(Mek mek, byte[] bytes, String charset) {
        Mek.MekBasicInfo basicInfo = mek.getMekBasicInfo();
        BinaryReader reader = new BinaryReader(bytes, charset);

        basicInfo.setStringField1(reader.readNullTerminatedString());
        basicInfo.setStringField2(reader.readNullTerminatedString());
        basicInfo.setStringField3(reader.readNullTerminatedString());
        basicInfo.setStringField4(reader.readNullTerminatedString());
        basicInfo.setStringField5(reader.readNullTerminatedString());

        basicInfo.setIntField1(reader.readInt());
        basicInfo.setIntField2(reader.readInt());
        basicInfo.setIntField3(reader.readInt());
        basicInfo.setIntField4(reader.readInt());
        basicInfo.setIntField5(reader.readInt());
        basicInfo.setIntField6(reader.readInt());
        basicInfo.setIntField7(reader.readInt());
        basicInfo.setIntField8(reader.readInt());
        basicInfo.setIntField9(reader.readInt());
        basicInfo.setIntField10(reader.readInt());
        basicInfo.setIntField11(reader.readInt());
        basicInfo.setIntField12(reader.readInt());
        basicInfo.setByteField1(reader.readByte());
        basicInfo.setIntField13(reader.readInt());
        basicInfo.setIntField14(reader.readInt());
        basicInfo.setIntField15(reader.readInt());
        basicInfo.setIntField16(reader.readInt());
        basicInfo.setIntField17(reader.readInt());
        basicInfo.setIntField18(reader.readInt());
    }

    private static void parsePairBlock(Mek mek, byte[] bytes) {
        Mek.MekPairBlock pairBlock = mek.getMekPairBlock();
        BinaryReader reader = new BinaryReader(bytes);
        while (reader.getPosition() + 8 <= bytes.length) {
            Mek.MekPairBlock.Pair pair = new Mek.MekPairBlock.Pair();
            pair.setInt1(reader.readInt());
            pair.setInt2(reader.readInt());
            pairBlock.getUnkPair().add(pair);
        }
    }

    private static void parseWeaponInfo(Mek mek, byte[] bytes, String charset) {
        Map<Integer, Mek.MekWeaponInfo> weaponInfoMap = mek.getMekWeaponInfoMap();
        BinaryReader reader = new BinaryReader(bytes, charset);

        int weaponCount = reader.readInt();
        for (int i = 0; i < weaponCount; i++) {
            Mek.MekWeaponInfo weaponInfo = new Mek.MekWeaponInfo();
            weaponInfo.setOffset(mek.getMekHead().getSequence3() + reader.getPosition());
            int enabled = reader.readInt();
            weaponInfo.setEnabled(enabled);

            if (enabled == 0) {
                weaponInfoMap.put(i, weaponInfo);
                continue;
            }

            weaponInfo.setStringField1(reader.readNullTerminatedString());
            weaponInfo.setStringField2(reader.readNullTerminatedString());
            weaponInfo.setStringField3(reader.readNullTerminatedString());

            for (int j = 0; j < WEAPON_LEADING_INT_COUNT; j++) {
                weaponInfo.getLeadingInts().add(reader.readInt());
            }
            weaponInfo.setFlagByte(reader.readByte());
            for (int j = 0; j < WEAPON_TRAILING_INT_COUNT; j++) {
                weaponInfo.getTrailingInts().add(reader.readInt());
            }

            int variantCount = reader.readInt();
            weaponInfo.setVariantCount(variantCount);
            for (int j = 0; j < variantCount; j++) {
                Mek.MekWeaponInfo.Variant variant = new Mek.MekWeaponInfo.Variant();
                variant.setInt1(reader.readInt());
                variant.setInt2(reader.readInt());
                variant.setInt3(reader.readInt());
                variant.setInt4(reader.readInt());
                variant.setInt5(reader.readInt());
                variant.setInt6(reader.readInt());
                variant.setInt7(reader.readInt());
                weaponInfo.getVariants().add(variant);
            }

            weaponInfoMap.put(i, weaponInfo);
        }
    }

    private static void parseAiBlock(Mek mek, byte[] bytes, String charset) {
        List<Mek.MekAiInfo> aiInfoList = mek.getMekAiInfoList();
        BinaryReader reader = new BinaryReader(bytes, charset);

        int aiCount = reader.readInt();
        for (int i = 0; i < aiCount; i++) {
            Mek.MekAiInfo aiInfo = new Mek.MekAiInfo();
            aiInfo.setStringField1(reader.readNullTerminatedString());
            aiInfo.setStringField2(reader.readNullTerminatedString());

            int cpuEventCount = reader.readInt();
            for (int j = 0; j < cpuEventCount; j++) {
                short type = reader.readShort();
                CCpuEvent event = parseAiEvent(bytes, charset, reader, aiCount, i, cpuEventCount, j, type);
                event.setType(type);
                aiInfo.getCpuEventList().add(event);
            }
            aiInfoList.add(aiInfo);
        }

        int trailingZeroByteCount = 0;
        while (reader.hasRemaining()) {
            int value = reader.readByte() & 0xFF;
            if (value != 0) {
                throw new OperationException(500, "unexpected non-zero AI trailing byte: " + value);
            }
            trailingZeroByteCount++;
        }
        mek.setAiTrailingZeroByteCount(trailingZeroByteCount);
    }

    private static void parseVoiceBlock(Mek mek, byte[] bytes, String charset) {
        Mek.MekVoiceInfo voiceInfo = mek.getMekVoiceInfo();
        BinaryReader reader = new BinaryReader(bytes, charset);

        voiceInfo.setVersion(reader.readInt());

        int emotionCount = reader.readInt();
        for (int i = 0; i < emotionCount; i++) {
            Mek.MekVoiceInfo.Emotion emotion = new Mek.MekVoiceInfo.Emotion();
            emotion.setName(reader.readNullTerminatedString());
            emotion.setToken(reader.readNullTerminatedString());
            voiceInfo.getEmotions().add(emotion);
        }

        int voiceSlotCount = reader.readInt();
        for (int i = 0; i < voiceSlotCount; i++) {
            Mek.MekVoiceInfo.VoiceSlot voiceSlot = new Mek.MekVoiceInfo.VoiceSlot();
            voiceSlot.setName(reader.readNullTerminatedString());
            voiceSlot.setToken(reader.readNullTerminatedString());
            voiceInfo.getVoiceSlots().add(voiceSlot);
        }

        int rowCount = 0;
        while (reader.getPosition() < bytes.length) {
            List<List<Mek.MekVoiceInfo.Entry>> row = new ArrayList<>(voiceSlotCount);
            for (int i = 0; i < voiceSlotCount; i++) {
                int entryCount = reader.readInt();
                List<Mek.MekVoiceInfo.Entry> cell = new ArrayList<>(entryCount);
                for (int j = 0; j < entryCount; j++) {
                    Mek.MekVoiceInfo.Entry entry = new Mek.MekVoiceInfo.Entry();
                    entry.setVoiceType(reader.readInt());
                    entry.setGroupId(reader.readInt());
                    entry.setWeight(reader.readInt());
                    cell.add(entry);
                }
                row.add(cell);
            }
            voiceInfo.getTable().add(row);
            rowCount++;
        }

        voiceInfo.builtinEmotionCount = rowCount - emotionCount;
    }

    private static void parseTailBlock(Mek mek, byte[] bytes) {
        mek.getMekBlocks().setMaterialBlockSize(bytes.length);
        parseMekMaterialBlock(mek, bytes);
    }

    private static void parseMekMaterialBlock(Mek mek, byte[] blockBytes) {
        Mek.MekMaterialBlock out = new Mek.MekMaterialBlock();
        BinaryReader reader = new BinaryReader(blockBytes);

        List<Mek.MekMaterialBlock.PluginEntry> allEntries = new ArrayList<>();
        for (int i = 0; i < MATERIAL_FIXED_REGULAR_ENTRY_COUNT; i++) {
            allEntries.add(readMaterialEntry(reader));
        }

        int extraRegularCount = reader.readInt();
        if (extraRegularCount < 0) {
            throw new OperationException(500, "invalid extraRegularCount: " + extraRegularCount);
        }
        out.setExtraRegularCount(extraRegularCount);

        for (int i = 0; i < extraRegularCount; i++) {
            allEntries.add(readMaterialEntry(reader));
        }

        while (reader.getPosition() < blockBytes.length) {
            int before = reader.getPosition();
            allEntries.add(readMaterialEntry(reader));
            if (reader.getPosition() <= before) {
                throw new OperationException(500, "material entry parse made no progress at " + before);
            }
        }

        int regularCount = MATERIAL_FIXED_REGULAR_ENTRY_COUNT + extraRegularCount;
        out.setRegularCount(regularCount);
        out.setEntries(new ArrayList<>(allEntries));

        int split = Math.min(regularCount, allEntries.size());
        out.setRegularEntries(new ArrayList<>(allEntries.subList(0, split)));
        out.setTrailingEntries(new ArrayList<>(allEntries.subList(split, allEntries.size())));
        mek.setMekMaterialBlock(out);
    }

    private static Mek.MekMaterialBlock.PluginEntry readMaterialEntry(BinaryReader reader) {
        int start = reader.getPosition();
        Mek.MekMaterialBlock.PluginEntry entry = new Mek.MekMaterialBlock.PluginEntry();
        entry.setSpriteGroups(readPairGroupSegment(reader, "spriteGroups"));
        entry.setSeGroups(readIntGroupSegment(reader, "seGroups"));
        entry.setVoiceGroups(readIntGroupSegment(reader, "voiceGroups"));
        entry.setOffset(start);
        entry.setLength(reader.getPosition() - start);
        return entry;
    }

    private static List<int[]> readPairGroupSegment(BinaryReader reader, String label) {
        int groupCount = reader.readInt();
        if (groupCount < 0) {
            throw new OperationException(500, "negative " + label + " groupCount at " + reader.getPosition());
        }

        List<int[]> groups = new ArrayList<>(groupCount);
        for (int i = 0; i < groupCount; i++) {
            int pairCount = reader.readInt();
            if (pairCount < 0) {
                throw new OperationException(500, "negative " + label + " pairCount at " + reader.getPosition());
            }

            int[] values = new int[pairCount * 2];
            for (int j = 0; j < pairCount; j++) {
                values[j * 2] = reader.readInt();
                values[j * 2 + 1] = reader.readInt();
            }
            groups.add(values);
        }
        return groups;
    }

    private static List<int[]> readIntGroupSegment(BinaryReader reader, String label) {
        int groupCount = reader.readInt();
        if (groupCount < 0) {
            throw new OperationException(500, "negative " + label + " groupCount at " + reader.getPosition());
        }

        List<int[]> groups = new ArrayList<>(groupCount);
        for (int i = 0; i < groupCount; i++) {
            int valueCount = reader.readInt();
            if (valueCount < 0) {
                throw new OperationException(500, "negative " + label + " valueCount at " + reader.getPosition());
            }

            int[] values = new int[valueCount];
            for (int j = 0; j < valueCount; j++) {
                values[j] = reader.readInt();
            }
            groups.add(values);
        }
        return groups;
    }

    private static CCpuEvent parseAiEvent(byte[] bytes, String charset, BinaryReader reader, int aiCount, int aiIndex,
                                          int cpuEventCount, int cpuEventIndex, short type) {
        if (type == 1) {
            CCpuEventMove event = new CCpuEventMove();
            event.readInfo(reader);
            return event;
        }
        if (type == 2) {
            CCpuEventAttack event = new CCpuEventAttack();
            event.readInfo(reader);
            return event;
        }

        if (cpuEventIndex != cpuEventCount - 1) {
            throw new OperationException(500, "unsupported non-terminal AI type: " + type + " at " + reader.getPosition());
        }

        int eventPayloadStart = reader.getPosition();
        int eventEnd;
        if (aiIndex == aiCount - 1) {
            eventEnd = bytes.length;
        } else {
            eventEnd = findNextAiHeaderPosition(bytes, charset, eventPayloadStart, aiCount - aiIndex - 1);
            if (eventEnd < 0) {
                throw new OperationException(500, "failed to find next AI header after type " + type + " at " + eventPayloadStart);
            }
        }

        byte[] payloadBytes = Arrays.copyOfRange(bytes, eventPayloadStart, eventEnd);
        BinaryReader eventReader = new BinaryReader(payloadBytes, charset);
        CCpuEventUnknown event = new CCpuEventUnknown();
        event.readInfo(eventReader);
        reader.seek(eventEnd);
        return event;
    }

    private static int findNextAiHeaderPosition(byte[] bytes, String charset, int searchStart, int remainingAiCount) {
        for (int pos = searchStart; pos < bytes.length - 8; pos++) {
            int string1End = findNullTerminator(bytes, pos);
            if (string1End < 0 || string1End == pos) {
                continue;
            }

            int string2Start = string1End + 1;
            int string2End = findNullTerminator(bytes, string2Start);
            if (string2End < 0 || string2End == string2Start) {
                continue;
            }

            int countOffset = string2End + 1;
            if (countOffset + 4 > bytes.length) {
                continue;
            }
            int cpuEventCount = readIntLE(bytes, countOffset);
            if (cpuEventCount < 0 || cpuEventCount > 64) {
                continue;
            }

            int firstEventOffset = countOffset + 4;
            if (cpuEventCount > 0) {
                if (firstEventOffset + 2 > bytes.length) {
                    continue;
                }
                short firstType = readShortLE(bytes, firstEventOffset);
                if (!isKnownAiEventType(firstType)) {
                    continue;
                }
            }

            try {
                String stringField1 = new String(bytes, pos, string1End - pos, charset);
                String stringField2 = new String(bytes, string2Start, string2End - string2Start, charset);
                if (!isPlausibleAiHeaderString(stringField1) || !isPlausibleAiHeaderString(stringField2)) {
                    continue;
                }
                if (canParseRemainingAiInfos(bytes, charset, pos, remainingAiCount)) {
                    return pos;
                }
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    private static boolean canParseRemainingAiInfos(byte[] bytes, String charset, int startPos, int remainingAiCount) {
        try {
            BinaryReader reader = new BinaryReader(bytes, charset);
            reader.seek(startPos);

            for (int aiIndex = 0; aiIndex < remainingAiCount; aiIndex++) {
                reader.readNullTerminatedString();
                reader.readNullTerminatedString();
                int cpuEventCount = reader.readInt();

                for (int cpuEventIndex = 0; cpuEventIndex < cpuEventCount; cpuEventIndex++) {
                    short type = reader.readShort();

                    if (type == 1) {
                        new CCpuEventMove().readInfo(reader);
                        continue;
                    }
                    if (type == 2) {
                        new CCpuEventAttack().readInfo(reader);
                        continue;
                    }

                    if (cpuEventIndex != cpuEventCount - 1) {
                        return false;
                    }

                    if (aiIndex == remainingAiCount - 1) {
                        reader.seek(bytes.length);
                    } else {
                        int nextAiHeaderPosition = findNextAiHeaderPosition(
                                bytes, charset, reader.getPosition(), remainingAiCount - aiIndex - 1);
                        if (nextAiHeaderPosition < 0) {
                            return false;
                        }
                        reader.seek(nextAiHeaderPosition);
                    }
                }
            }
            return hasOnlyZeroBytes(bytes, reader.getPosition(), bytes.length);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int findNullTerminator(byte[] bytes, int start) {
        for (int i = start; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                return i;
            }
        }
        return -1;
    }

    private static int readIntLE(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private static short readShortLE(byte[] bytes, int offset) {
        return (short) ((bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8));
    }

    private static boolean isKnownAiEventType(short type) {
        return type == 1 || type == 2 || type == 3 || type == 7 || type == 8 || type == 21;
    }

    private static boolean hasOnlyZeroBytes(byte[] bytes, int start, int end) {
        for (int i = start; i < end; i++) {
            if (bytes[i] != 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPlausibleAiHeaderString(String value) {
        if (value.isEmpty() || value.length() > 64) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isISOControl(ch)) {
                return false;
            }
        }
        return true;
    }
}
