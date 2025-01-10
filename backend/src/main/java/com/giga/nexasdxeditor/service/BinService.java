package com.giga.nexasdxeditor.service;
import com.giga.nexasdxeditor.dto.ResponseDTO;

import java.io.IOException;

public interface BinService {
    ResponseDTO parse(String path) throws IOException;
}
