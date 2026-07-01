package com.giga.nexas.service;

import cn.hutool.core.util.StrUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bhe.Bhe;
import com.giga.nexas.dto.bhe.BheGenerator;
import com.giga.nexas.dto.bhe.BheParser;
import com.giga.nexas.dto.bhe.grp.generator.GrpGenerator;
import com.giga.nexas.dto.bhe.grp.parser.GrpParser;
import com.giga.nexas.dto.bhe.map.generator.MapDataGenerator;
import com.giga.nexas.dto.bhe.map.parser.MapDataParser;
import com.giga.nexas.dto.bhe.mek.parser.MekParser;
import com.giga.nexas.dto.bhe.spm.generator.SpmGenerator;
import com.giga.nexas.dto.bhe.spm.parser.SpmParser;
import com.giga.nexas.dto.bhe.waz.generator.WazGenerator;
import com.giga.nexas.dto.bhe.waz.parser.WazParser;
import com.giga.nexas.exception.OperationException;

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
        registerParser(new SpmParser());
        registerParser(new MekParser());
        registerParser(new WazParser());
//        registerParser(new DatParser());
//        registerParser(new BinParser());
        registerParser(new GrpParser());
        registerParser(new MapDataParser());

        // 注册generator
        registerGenerator(new SpmGenerator());
//        registerGenerator(new MekGenerator());
        registerGenerator(new WazGenerator());
        registerGenerator(new GrpGenerator());
        registerGenerator(new MapDataGenerator());
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
            throw new OperationException(500, "unsupported file type for parsing: " + ext);
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
            throw new OperationException(500, "unsupported file type for generation: " + ext);
        }
        gen.generate(path, obj, charset);
        return new ResponseDTO<>(null, "ok");
    }

    private String getFileExtension(String path) {
        int lastDotIndex = path.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new OperationException(500, "not an invalid file path!");
        }
        return path.substring(lastDotIndex + 1).toLowerCase();
    }

    private String getJsonExtension(Bhe obj) {
        String ext = obj.getExtensionName();
        if (StrUtil.isEmpty(ext)) {
            throw new OperationException(500, "can't find extension name from JSON file!");
        }
        return ext.toLowerCase();
    }

    public static String getFileName(String path) {
        String fileName = Paths.get(path).getFileName().toString();
        int extensionIndex = fileName.lastIndexOf(".");
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    public Map<String, BheParser<?>> getParserMap() {
        return new HashMap<>(parserMap);
    }

    public Map<String, BheGenerator<?>> getGeneratorMap() {
        return new HashMap<>(generatorMap);
    }
}
