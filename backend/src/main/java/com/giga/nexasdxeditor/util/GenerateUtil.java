package com.giga.nexasdxeditor.util;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class GenerateUtil {

    public static void putString(ByteBuffer buffer, String value, String charset) {
        if (value != null) {
            buffer.put(value.getBytes(Charset.forName(charset)));
        }
        buffer.put((byte) 0x00);
    }

    public static int calculateStringSize(String value, String charset) {
        if (value == null) {
            return 1; // 仅终止符
        }
        return value.getBytes(Charset.forName(charset)).length + 1; // 字符串长度 + 终止符
    }

}
