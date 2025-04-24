package com.giga.nexasdxeditor.util;


import cn.hutool.core.util.ByteUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @Author 这位同学(Karaik)
 */
public class ParserUtil {

    // 常用于块与块之间分割符
    public static final byte[] FLAG_DATA =  new byte[] { (byte) 0x01, 0x00, 0x00, 0x00 };

    // 常用mek中，武装插槽块与块之间分割符
    public static final byte[] WEAPON_PLUGINS_FLAG_DATA =  new byte[] { (byte) 0x8A, 0x00, 0x00, 0x00 };
    public static final byte[] WEAPON_PLUGINS_FLAG_DATA_2 =  new byte[] { (byte) 0x88, 0x00, 0x00, 0x00 };

    // 大概、可能、也许是代表同一个块之间的功能分隔符
    public static final byte[] SPLIT_DATA = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

    // 数值
    public static final Integer DAT_COLUMN_TYPE_DATA = 0;

    // 字符串
    public static final Integer DAT_COLUMN_TYPE_STRING = 1;

    // 编号？
    public static final Integer DAT_COLUMN_TYPE_UNKNOWN = 2;

    /**
     * 以16位的小端有符号整型方式读取short
     */
    public static byte readInt8(byte[] bytes, int start) {
        return bytes[start];
    }

    /**
     * 以16位的小端有符号整型方式读取short
     */
    public static short readInt16(byte[] bytes, int start) {
        return ByteUtil.bytesToShort(bytes, start, ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * 以32位的小端有符号整型方式读取int
     */
    public static Integer readInt32(byte[] bytes, int start) {
        // 小端字节序
        return ByteUtil.bytesToInt(bytes, start, ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * 以64位的小端有符号整型方式读取long
     */
    public static long readInt64(byte[] bytes, int start) {
        // 小端字节序
        return ByteUtil.bytesToLong(bytes, start, ByteOrder.LITTLE_ENDIAN);
    }

    /**
     * 以小端字节序读取双精度浮点数
     */
    public static double readDouble(byte[] bytes, int start) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes, start, 8)
                .order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getDouble();
    }

    /**
     * 计算所需读取字符串长度
     */
    public static int findNullTerminator(byte[] bytes, int offset) {
        for (int i = offset; i < bytes.length; i++) {
            if (bytes[i] == 0x00) {
                // 字符串长度
                return i - offset;
            }
        }
        return -1;
    }

    /**
     * 判断句子是否大致上为日语
     */
    public static boolean isLikelyJapanese(String text) {
        int japaneseCharCount = 0;
        for (char c : text.toCharArray()) {
            if ((c >= '\u3040' && c <= '\u309F') || // 平假名
                    (c >= '\u30A0' && c <= '\u30FF') || // 片假名
                    (c >= '\u4E00' && c <= '\u9FFF') || // CJK 统一汉字（包括日语汉字）
                    (c >= '\uFF66' && c <= '\uFF9F')) { // 半角片假名
                japaneseCharCount++;
            }
        }
        // 阈值
        return (japaneseCharCount > text.length() * 0.5);
    }

}
