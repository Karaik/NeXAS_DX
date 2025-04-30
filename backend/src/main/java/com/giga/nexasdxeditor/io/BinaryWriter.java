package com.giga.nexasdxeditor.io;

import cn.hutool.core.util.ByteUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

public class BinaryWriter implements AutoCloseable {

    private final OutputStream outputStream;
    private boolean littleEndian = true;
    private Charset charset = Charset.forName("windows-31j");

    public BinaryWriter(OutputStream outputStream) {
        this.outputStream = new BufferedOutputStream(outputStream);
    }

    public BinaryWriter(OutputStream outputStream, String charsetName) {
        this(outputStream);
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

    public void writeByte(byte value) throws IOException {
        outputStream.write(value);
    }

    public void writeBytes(byte[] bytes) throws IOException {
        outputStream.write(bytes);
    }

    public void writeInt(int value) throws IOException {
        byte[] bytes = ByteUtil.intToBytes(value, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        outputStream.write(bytes);
    }

    public void writeShort(short value) throws IOException {
        byte[] bytes = ByteUtil.shortToBytes(value, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        outputStream.write(bytes);
    }

    public void writeLong(long value) throws IOException {
        byte[] bytes = ByteUtil.longToBytes(value, littleEndian ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        outputStream.write(bytes);
    }

    public void writeDouble(double value) throws IOException {
        long bits = Double.doubleToLongBits(value);
        writeLong(bits);
    }

    public void writeNullTerminatedString(String str) throws IOException {
        byte[] bytes = StrUtil.bytes(str, charset);
        if (!StrUtil.isEmpty(str)) {
            outputStream.write(bytes);
        }
        outputStream.write(0); // 写入 null 终止符
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }
}
