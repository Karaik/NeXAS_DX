package com.giga.nexasdxeditor.service.impl;

import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.Mek;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.generator.MekGenerator;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.parser.MekParser;
import com.giga.nexasdxeditor.dto.bsdx.mecha.waz.Waz;
import com.giga.nexasdxeditor.dto.bsdx.mecha.waz.parser.WazParser;
import com.giga.nexasdxeditor.exception.BusinessException;
import com.giga.nexasdxeditor.service.BinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class BinServiceImpl implements BinService {
    private static final Logger log = LoggerFactory.getLogger(BinServiceImpl.class);

    @Override
    public ResponseDTO parse(String path, String charset) throws IOException {

        Object binFile = null;
        String fileExtension = getFileExtension(path);

        if ("mek".equalsIgnoreCase(fileExtension)) {
            byte[] mekFile = Files.readAllBytes(Paths.get(path));
            binFile = MekParser.parseMek(mekFile, charset);
        } else if ("waz".equalsIgnoreCase(fileExtension)) {
            byte[] wazFile = Files.readAllBytes(Paths.get(path));
            binFile = WazParser.parseWaz(wazFile);
        } else {
            throw new BusinessException(500, "文件上传错误！");
        }

        return new ResponseDTO<>(binFile, "ok");
    }

    @Override
    public ResponseDTO generate(String path, Mek mek, String charset) throws IOException {

        MekGenerator.generate(path, mek, charset);

        return new ResponseDTO<>(null, "ok");
    }

    private String getFileExtension(String path) {
        int lastDotIndex = path.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new BusinessException(500, "文件路径错误");
        }
        return path.substring(lastDotIndex + 1).toLowerCase();
    }

}
