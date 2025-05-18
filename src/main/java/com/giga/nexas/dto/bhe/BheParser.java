package com.giga.nexas.dto.bhe;

public interface BheParser<T extends Bhe> {

    String supportExtension();

    T parse(byte[] data, String filename, String charset);
}