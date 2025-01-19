package com.giga.nexasdxeditor.service;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.Mek;

import java.io.IOException;

public interface BinService {
    ResponseDTO parse(String path) throws IOException;
    ResponseDTO generate(String path, Mek mek) throws IOException;
}
