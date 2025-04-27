package com.giga.nexasdxeditor.service;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.Bsdx;
import com.giga.nexasdxeditor.dto.bsdx.mek.Mek;

import java.io.IOException;

public interface BinService {
    ResponseDTO parse(String path, String charset) throws IOException;
    <T extends Bsdx> ResponseDTO<?> generate(String path, T obj, String charset) throws IOException;
}
