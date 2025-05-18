package com.giga.nexas.service;

import cn.hutool.core.util.StrUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bhe.Bhe;
import com.giga.nexas.dto.bhe.BheGenerator;
import com.giga.nexas.dto.bhe.BheParser;
import com.giga.nexas.dto.bhe.waz.parser.WazParser;
import com.giga.nexas.exception.BusinessException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BheBinService {

    private final Map<String, BheParser<?>> parserMap = new HashMap<>();
    private final Map<String, BheGenerator<?>> generatorMap = new HashMap<>();

    public BheBinService() {
        // 注册parser
//        registerParser(new SpmParser());
//        registerParser(new MekParser());
        registerParser(new WazParser());
//        registerParser(new DatParser());
//        registerParser(new BinParser());
//        registerParser(new GrpParser());

        // 注册generator
//        registerGenerator(new SpmGenerator());
//        registerGenerator(new MekGenerator());
//        registerGenerator(new WazGenerator());
//        registerGenerator(new GrpGenerator());
    }

    public BheBinService(List<BheParser<?>> bheParsers) {
        for (BheParser<?> bheParser : bheParsers) {
            registerParser(bheParser);
        }
    }

    private void registerParser(BheParser<?> bheParser) {
        parserMap.put(bheParser.supportExtension().toLowerCase(), bheParser);
    }

    private void registerGenerator(BheGenerator<?> bheGenerator) {
        generatorMap.put(bheGenerator.supportExtension().toLowerCase(), bheGenerator);
    }

    public ResponseDTO<?> parse(String path, String charset) throws IOException {
        String ext = getFileExtension(path);
        BheParser<?> bheParser = parserMap.get(ext);
        if (bheParser == null) {
            throw new BusinessException(500, "不支持的文件类型：" + ext);
        }

        byte[] data = Files.readAllBytes(Paths.get(path));
        Bhe parsed = bheParser.parse(data, getFileName(path), charset);
        parsed.setExtensionName(ext);
        return new ResponseDTO<>(parsed, "ok");
    }

    public <T extends Bhe> ResponseDTO<?> generate(String path, T obj, String charset) throws IOException {
        String ext = getJsonExtension(obj);
        BheGenerator<T> gen = (BheGenerator<T>) generatorMap.get(ext);
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

    private String getJsonExtension(Bhe obj) {
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