package com.giga.nexasdxeditor.io;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
/*
* 参考自：https://github.com/jaymarvels/BinaryReaderDotNet
* */
public class BinaryReader {

    private RandomAccessFile randomAccessFile;
    private int position;

    public BinaryReader(RandomAccessFile randomAccessFile) throws IOException {
        this.randomAccessFile = randomAccessFile;
        this.position = 0;
    }

    public void rewind(int offset) throws IOException {
        long currentPosition = randomAccessFile.getFilePointer();
        long newPosition = currentPosition - offset;

        if (newPosition < 0) {
            throw new IOException("Cannot rewind beyond the start of the stream");
        }

        // 使用 RandomAccessFile 的 seek 方法调整文件指针
        randomAccessFile.seek(newPosition);
        position = (int) newPosition;
    }

    public int readInt32() throws IOException {
        incrementPosition(4);
        return ByteBuffer.wrap(this.readBytes(4))
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
    }

    public long readInt64() throws IOException {
        incrementPosition(8);
        return ByteBuffer.wrap(this.readBytes(8))
                .order(ByteOrder.LITTLE_ENDIAN)
                .getLong();
    }

    public long readUInt32() throws IOException {
        incrementPosition(4);
        return this.readInt32() & 0xFFFFFFFFL;
    }

    public int readInt16() throws IOException {
        incrementPosition(2);
        return ByteBuffer.wrap(this.readBytes(2))
                .order(ByteOrder.LITTLE_ENDIAN)
                .getShort();
    }

    public int readUInt16() throws IOException {
        incrementPosition(2);
        return this.readInt16() & 0xFFFF;
    }

    public short readShort() throws IOException {
        incrementPosition(2);
        return ByteBuffer.wrap(this.readBytes(2))
                .order(ByteOrder.LITTLE_ENDIAN)
                .getShort();
    }

    public String readString() throws IOException {
        incrementPosition(this.getStringLength());
        return new String(this.readBytes(this.getStringLength()));
    }

    public boolean readBoolean() throws IOException {
        incrementPosition(1);
        return this.readBytes(1)[0] != 0;
    }

    public float readSingle() throws IOException {
        incrementPosition(4);
        return ByteBuffer.wrap(this.readBytes(4))
                .order(ByteOrder.LITTLE_ENDIAN)
                .getFloat();
    }

    private int getStringLength() throws IOException {
        int count = 0;
        int shift = 0;
        boolean more = true;
        while (more) {
            byte[] byteBuffer = this.readBytes(1);
            byte b = byteBuffer[0];
            count |= (b & 0x7F) << shift;
            shift += 7;
            if ((b & 0x80) == 0) {
                more = false;
            }
        }
        return count;
    }

    public byte readByte() throws IOException {
        incrementPosition(1);
        return ByteBuffer.wrap(this.readBytes(1))
                .order(ByteOrder.LITTLE_ENDIAN)
                .get();
    }

    public byte[] readBytes(int length) throws IOException {
        byte[] bytes = new byte[length];
        randomAccessFile.read(bytes);
        incrementPosition(length);
        return bytes;
    }

    private void incrementPosition(int increment) {
        position += increment;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) throws IOException {
        this.position = position;
        randomAccessFile.seek(position);
    }

    public void setPosition(long position) throws IOException {
        this.position = (int) position;
        randomAccessFile.seek(position);
    }
}
