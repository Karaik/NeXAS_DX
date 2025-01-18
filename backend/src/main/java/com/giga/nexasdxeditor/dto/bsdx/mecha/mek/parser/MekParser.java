package com.giga.nexasdxeditor.dto.bsdx.mecha.mek.parser;

import cn.hutool.core.util.ByteUtil;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.Mek;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteOrder;
import java.util.Arrays;

@Slf4j
public class MekParser {

    // 每个机体的数据区块大小固定 共有22个数据
    public static final Integer MEK_INFO_DATA_BLOCK_QUANTITY = 88 / 4;

    // 每个武装的数据区块大小固定 共有17个数据
    public static final Integer MEK_WEAPON_DATA_BLOCK_QUANTITY = 68 / 4;

    // mek文件

    public static Mek parseMek(byte[] bytes) {
        Mek mek = new Mek();
        try {

            // 解析头
            parseMekHead(mek, bytes);

            // 按MekBlocks分割数组
            Mek.MekHead mekHead = mek.getMekHead();
            // 读取机体信息块
            byte[] bodyInfoBlock = Arrays.copyOfRange(bytes, mekHead.getSequence1(), mekHead.getSequence2());
            // 读取未知信息块1
            byte[] unknownInfo1Block = Arrays.copyOfRange(bytes, mekHead.getSequence2(), mekHead.getSequence3());
            // 读取武装信息块
            byte[] weaponInfoBlock = Arrays.copyOfRange(bytes, mekHead.getSequence3(), mekHead.getSequence4());
            // 读取AI信息块1
            byte[] aiInfo1Block = Arrays.copyOfRange(bytes, mekHead.getSequence4(), mekHead.getSequence5());
            // 读取AI信息块2
            byte[] aiInfo2Block = Arrays.copyOfRange(bytes, mekHead.getSequence5(), mekHead.getSequence6());

            // 解析机体




        } catch (Exception e) {
            log.info("error === {}", e.getMessage());
        }

        return mek;
    }

    private static Integer readInt32(byte[] bytes, int start) {
        // 小端字节序
        return ByteUtil.bytesToInt(bytes, start, ByteOrder.LITTLE_ENDIAN);
    }

    private static void parseMekHead(Mek mek, byte[] bytes) {
        Mek.MekHead mekHead = mek.getMekHead();

        mekHead.setSequence1(readInt32(bytes, 0));
        mekHead.setSequence2(readInt32(bytes, 4));
        mekHead.setSequence3(readInt32(bytes, 8));
        mekHead.setSequence4(readInt32(bytes, 12));
        mekHead.setSequence5(readInt32(bytes, 16));
        mekHead.setSequence6(readInt32(bytes, 20));

        // 计算各块大小
        mek.getMekBlocks().calculateBlockSizes(mekHead);

    }

    private static void parseMekInfo(Mek mek, byte[] bytes) {
        Mek.MekBasicInfo mekHead = mek.getMekBasicInfo();



    }

}
