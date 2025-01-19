package com.giga.nexasdxeditor.util;


import cn.hutool.core.util.ByteUtil;

import java.nio.ByteOrder;

/**
 * @Author 这位同学(Karaik)
 */
public class ParserUtil {

    // 常用于块与块之间分割符
    public static final byte[] FLAG_DATA =  new byte[] { (byte) 0x01, 0x00, 0x00, 0x00 };

    // 大概、可能、也许是代表同一个块之间的功能分隔符
    public static final byte[] SPLIT_DATA = new byte[] { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };

    /**
     * 以32位的小端有符号整型方式读取int
     */
    public static Integer readInt32(byte[] bytes, int start) {
        // 小端字节序
        return ByteUtil.bytesToInt(bytes, start, ByteOrder.LITTLE_ENDIAN);
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

}
