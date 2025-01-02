package com.giga.nexasdxeditor.service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface BinService {
    void convertBin2Hex(MultipartFile waz, MultipartFile mek) throws IOException;
}
