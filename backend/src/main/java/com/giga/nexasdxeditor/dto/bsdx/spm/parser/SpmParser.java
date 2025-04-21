package com.giga.nexasdxeditor.dto.bsdx.spm.parser;

import com.giga.nexasdxeditor.dto.Parser;
import com.giga.nexasdxeditor.dto.bsdx.spm.Spm;
import com.giga.nexasdxeditor.io.BinaryReader;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/4/21
 * @Description SpmParser
 */
public class SpmParser implements Parser<Spm> {


    @Override
    public String supportExtension() {
        return "spm";
    }

    @Override
    public Spm parse(byte[] data, String filename, String charset) {
        BinaryReader binaryReader = new BinaryReader(data);
        Spm spm = Spm.parse(binaryReader);
        return spm;
    }
}
