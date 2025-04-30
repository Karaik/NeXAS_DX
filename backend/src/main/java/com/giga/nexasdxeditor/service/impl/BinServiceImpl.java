package com.giga.nexasdxeditor.service.impl;

import cn.hutool.core.util.StrUtil;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.Bsdx;
import com.giga.nexasdxeditor.dto.bsdx.BsdxGenerator;
import com.giga.nexasdxeditor.dto.bsdx.BsdxParser;
import com.giga.nexasdxeditor.dto.bsdx.bin.parser.BinParser;
import com.giga.nexasdxeditor.dto.bsdx.dat.parser.DatParser;
import com.giga.nexasdxeditor.dto.bsdx.mek.generator.MekGenerator;
import com.giga.nexasdxeditor.dto.bsdx.mek.parser.MekParser;
import com.giga.nexasdxeditor.dto.bsdx.spm.parser.SpmParser;
import com.giga.nexasdxeditor.dto.bsdx.waz.generator.WazGenerator;
import com.giga.nexasdxeditor.dto.bsdx.waz.parser.WazParser;
import com.giga.nexasdxeditor.exception.BusinessException;
import com.giga.nexasdxeditor.service.BinService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BinServiceImpl implements BinService {

    private final Map<String, BsdxParser<?>> parserMap = new HashMap<>();
    private final Map<String, BsdxGenerator<?>> generatorMap = new HashMap<>();

    public BinServiceImpl() {
        // 注册parser
        registerParser(new SpmParser());
        registerParser(new MekParser());
        registerParser(new WazParser());
        registerParser(new DatParser());
        registerParser(new BinParser());

        // 注册generator
        registerGenerator(new WazGenerator());
        registerGenerator(new MekGenerator());
    }

    public BinServiceImpl(List<BsdxParser<?>> bsdxParsers) {
        for (BsdxParser<?> bsdxParser : bsdxParsers) {
            registerParser(bsdxParser);
        }
    }

    private void registerParser(BsdxParser<?> bsdxParser) {
        parserMap.put(bsdxParser.supportExtension().toLowerCase(), bsdxParser);
    }

    private void registerGenerator(BsdxGenerator<?> bsdxGenerator) {
        generatorMap.put(bsdxGenerator.supportExtension().toLowerCase(), bsdxGenerator);
    }

    @Override
    public ResponseDTO<?> parse(String path, String charset) throws IOException {
        String ext = getFileExtension(path);
        BsdxParser<?> bsdxParser = parserMap.get(ext);
        if (bsdxParser == null) {
            throw new BusinessException(500, "不支持的文件类型：" + ext);
        }

        byte[] data = Files.readAllBytes(Paths.get(path));
        Bsdx parsed = bsdxParser.parse(data, getFileName(path), charset);
        parsed.setExtensionName(ext);
        return new ResponseDTO<>(parsed, "ok");
    }

    @Override
    public <T extends Bsdx> ResponseDTO<?> generate(String path, T obj, String charset) throws IOException {
        String ext = getJsonExtension(obj);
        BsdxGenerator<T> gen = (BsdxGenerator<T>) generatorMap.get(ext);
        if (gen == null) {
            throw new BusinessException(500, "不支持的文件类型：" + ext);
        }
        gen.generate(path, obj, charset);
        return new ResponseDTO<>(null, "ok");
    }

    private String getFileExtension(String path) {
        int lastDotIndex = path.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new BusinessException(500, "文件路径错误");
        }
        return path.substring(lastDotIndex + 1).toLowerCase();
    }

    private String getJsonExtension(Bsdx obj) {
        String ext = obj.getExtensionName();
        if (StrUtil.isEmpty(ext)) {
            throw new BusinessException(500, "对象未指定扩展名");
        }
        return ext.toLowerCase();
    }

    public static String getFileName(String path) {
        String fileName = Paths.get(path).getFileName().toString();
        int extensionIndex = fileName.lastIndexOf(".");
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }
}