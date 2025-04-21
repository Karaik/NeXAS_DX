package com.giga.nexasdxeditor.io;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class BinaryReader {

    private final byte[] data;
    private int position = 0;
    private boolean littleEndian = true;
    private Charset charset = Charset.forName("Shift-JIS");

    public BinaryReader(byte[] data) {
        this.data = data;
    }

    public BinaryReader(byte[] data, String charsetName) {
        this(data);
        this.charset = CharsetUtil.charset(charsetName);
    }

    public void setLittleEndian(boolean littleEndian) {
        this.littleEndian = littleEndian;
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(String charsetName) {
        this.charset = CharsetUtil.charset(charsetName);
    }

    public int readInt() {
        checkAvailable(4);
        byte[] bytes = readBytes(4);
        return littleEndian ? ByteUtil.bytesToInt(bytes, ByteOrder.LITTLE_ENDIAN)
                : ByteUtil.bytesToInt(bytes, ByteOrder.BIG_ENDIAN);
    }

    public short readShort() {
        checkAvailable(2);
        byte[] bytes = readBytes(2);
        return littleEndian ? ByteUtil.bytesToShort(bytes, ByteOrder.LITTLE_ENDIAN)
                : ByteUtil.bytesToShort(bytes, ByteOrder.BIG_ENDIAN);
    }

    public long readLong() {
        checkAvailable(8);
        byte[] bytes = readBytes(8);
        return littleEndian ? ByteUtil.bytesToLong(bytes, ByteOrder.LITTLE_ENDIAN)
                : ByteUtil.bytesToLong(bytes, ByteOrder.BIG_ENDIAN);
    }

    public static double readDouble(byte[] bytes, int start) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes, start, 8)
                .order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getDouble();
    }

    public String readNullTerminatedString() {
        int start = position;
        while (position < data.length && data[position] != 0) {
            position++;
        }
        byte[] bytes = ArrayUtil.sub(data, start, position);
        position++; // 跳过 null
        return StrUtil.str(bytes, charset);
    }

    public byte[] readBytes(int length) {
        checkAvailable(length);
        byte[] bytes = ArrayUtil.sub(data, position, position + length);
        position += length;
        return bytes;
    }

    private void checkAvailable(int length) {
        if (position + length > data.length) {
            throw new IndexOutOfBoundsException("剩余字节不足：" + (data.length - position) + " < " + length);
        }
    }

    public void seek(int newPosition) {
        if (newPosition < 0 || newPosition > data.length) {
            throw new IllegalArgumentException("无效偏移位置：" + newPosition);
        }
        this.position = newPosition;
    }

    public int getPosition() {
        return position;
    }

    public boolean hasRemaining() {
        return position < data.length;
    }

    public int remaining() {
        return data.length - position;
    }
}