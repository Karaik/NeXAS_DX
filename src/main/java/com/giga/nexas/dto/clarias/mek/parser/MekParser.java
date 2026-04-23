package com.giga.nexas.dto.clarias.mek.parser;

import com.giga.nexas.dto.clarias.ClariasParser;
import com.giga.nexas.dto.clarias.mek.Mek;
import com.giga.nexas.dto.clarias.mek.checker.MekChecker;
import com.giga.nexas.exception.OperationException;
import com.giga.nexas.io.BinaryReader;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Map;

@Slf4j
public class MekParser implements ClariasParser<Mek> {

    private static final int BASIC_INFO_LEADING_INT_COUNT = 12;
    private static final int BASIC_INFO_TRAILING_INT_COUNT = 6;
    private static final int WEAPON_LEADING_INT_COUNT = 5;
    private static final int WEAPON_TRAILING_INT_COUNT = 4;

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

            mek.setRawBlock1(block1);
            mek.setRawBlock2(block2);
            mek.setRawBlock3(block3);
            mek.setRawBlock4(block4);
            mek.setRawBlock5(block5);

            parseBlock1(mek, block1, charset);
            parsePairBlock(mek, block2);
            parseWeaponInfo(mek, block3, charset);
            parseOpaqueBlock4(mek, block4);
            parseOpaqueBlock5(mek, block5);
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

        for (int i = 0; i < BASIC_INFO_LEADING_INT_COUNT; i++) {
            basicInfo.getLeadingInts().add(reader.readInt());
        }
        basicInfo.setFlagByte(reader.readByte());
        for (int i = 0; i < BASIC_INFO_TRAILING_INT_COUNT; i++) {
            basicInfo.getTrailingInts().add(reader.readInt());
        }
    }

    private static void parsePairBlock(Mek mek, byte[] bytes) {
        Mek.MekPairBlock pairBlock = mek.getMekPairBlock();
        pairBlock.setRawBytes(bytes);

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

    private static void parseOpaqueBlock4(Mek mek, byte[] bytes) {
        mek.setRawBlock4(bytes);
    }

    private static void parseOpaqueBlock5(Mek mek, byte[] bytes) {
        mek.setRawBlock5(bytes);
    }

    private static void parseTailBlock(Mek mek, byte[] bytes) {
        mek.getMekBlocks().setMaterialBlockSize(bytes.length);
        mek.getMekMaterialBlock().setRawBytes(bytes);
    }
}
