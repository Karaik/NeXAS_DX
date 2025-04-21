package com.giga.nexasdxeditor.service.impl;

import com.giga.nexasdxeditor.dto.Parser;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.mek.Mek;
import com.giga.nexasdxeditor.dto.bsdx.mek.generator.MekGenerator;
import com.giga.nexasdxeditor.dto.bsdx.mek.parser.MekParser;
import com.giga.nexasdxeditor.dto.bsdx.spm.parser.SpmParser;
import com.giga.nexasdxeditor.dto.bsdx.waz.parser.WazParser;
import com.giga.nexasdxeditor.exception.BusinessException;
import com.giga.nexasdxeditor.service.BinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BinServiceImpl implements BinService {

    private final Map<String, Parser<?>> parserMap = new HashMap<>();

    public BinServiceImpl() {
        register(new SpmParser());
        register(new MekParser());
        register(new WazParser());
    }

    public BinServiceImpl(List<Parser<?>> parsers) {
        for (Parser<?> parser : parsers) {
            register(parser);
        }
    }

    private void register(Parser<?> parser) {
        parserMap.put(parser.supportExtension().toLowerCase(), parser);
    }

    @Override
    public ResponseDTO<?> parse(String path, String charset) throws IOException {
        String ext = getFileExtension(path);
        Parser<?> parser = parserMap.get(ext);
        if (parser == null) {
            throw new BusinessException(500, "不支持的文件类型：" + ext);
        }

        byte[] data = Files.readAllBytes(Paths.get(path));
        Object parsed = parser.parse(data, getFileName(path), charset);
        return new ResponseDTO<>(parsed, "ok");
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

    public static String getFileName(String path) {
        String fileName = Paths.get(path).getFileName().toString();
        int extensionIndex = fileName.lastIndexOf(".");
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }
}