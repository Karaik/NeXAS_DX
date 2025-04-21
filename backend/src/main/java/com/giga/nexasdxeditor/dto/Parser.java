package com.giga.nexasdxeditor.dto;

public interface Parser<T> {

    String supportExtension();

    T parse(byte[] data, String filename, String charset);
}