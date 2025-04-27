package com.giga.nexasdxeditor.dto.bsdx;

public interface BsdxParser<T extends Bsdx> {

    String supportExtension();

    T parse(byte[] data, String filename, String charset);
}