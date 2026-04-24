package com.giga.nexas.service;

import cn.hutool.core.util.StrUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.clarias.Clarias;
import com.giga.nexas.dto.clarias.ClariasGenerator;
import com.giga.nexas.dto.clarias.ClariasParser;
import com.giga.nexas.dto.clarias.dat.generator.DatGenerator;
import com.giga.nexas.dto.clarias.dat.parser.DatParser;
import com.giga.nexas.dto.clarias.grp.generator.GrpGenerator;
import com.giga.nexas.dto.clarias.grp.parser.GrpParser;
import com.giga.nexas.dto.clarias.mek.generator.MekGenerator;
import com.giga.nexas.dto.clarias.mek.parser.MekParser;
import com.giga.nexas.dto.clarias.waz.generator.WazGenerator;
import com.giga.nexas.dto.clarias.waz.parser.WazParser;
import com.giga.nexas.exception.OperationException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClariasBinService {

    private final Map<String, ClariasParser<?>> parserMap = new HashMap<>();
    private final Map<String, ClariasGenerator<?>> generatorMap = new HashMap<>();

    public ClariasBinService() {
        registerParser(new DatParser());
        registerParser(new GrpParser());
        registerParser(new MekParser());
        registerParser(new WazParser());
        registerGenerator(new DatGenerator());
        registerGenerator(new GrpGenerator());
        registerGenerator(new MekGenerator());
        registerGenerator(new WazGenerator());
    }

    public ClariasBinService(List<ClariasParser<?>> parsers) {
        for (ClariasParser<?> parser : parsers) {
            registerParser(parser);
        }
    }

    private void registerParser(ClariasParser<?> parser) {
        parserMap.put(parser.supportExtension().toLowerCase(), parser);
    }

    private void registerGenerator(ClariasGenerator<?> generator) {
        generatorMap.put(generator.supportExtension().toLowerCase(), generator);
    }

    public ResponseDTO<?> parse(String path, String charset) throws IOException {
        String ext = getFileExtension(path);
        ClariasParser<?> parser = parserMap.get(ext);
        if (parser == null) {
            throw new OperationException(500, "unsupported file type for parsing: " + ext);
        }

        byte[] data = Files.readAllBytes(Paths.get(path));
        Clarias parsed = parser.parse(data, getFileName(path), charset);
        parsed.setExtensionName(ext);
        return new ResponseDTO<>(parsed, "ok");
    }

    public <T extends Clarias> ResponseDTO<?> generate(String path, T obj, String charset) throws IOException {
        String ext = getJsonExtension(obj);
        ClariasGenerator<T> generator = (ClariasGenerator<T>) generatorMap.get(ext);
        if (generator == null) {
            throw new OperationException(500, "unsupported file type for generation: " + ext);
        }
        generator.generate(path, obj, charset);
        return new ResponseDTO<>(null, "ok");
    }

    private String getFileExtension(String path) {
        int lastDotIndex = path.lastIndexOf(".");
        if (lastDotIndex == -1) {
            throw new OperationException(500, "not an invalid file path!");
        }
        return path.substring(lastDotIndex + 1).toLowerCase();
    }

    private String getJsonExtension(Clarias obj) {
        String ext = obj.getExtensionName();
        if (StrUtil.isEmpty(ext)) {
            throw new OperationException(500, "can't find extension name from JSON file!");
        }
        return ext.toLowerCase();
    }

    public static String getFileName(String path) {
        String fileName = Paths.get(path).getFileName().toString().toLowerCase();
        int extensionIndex = fileName.lastIndexOf(".");
        return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
    }

    public Map<String, ClariasParser<?>> getParserMap() {
        return new HashMap<>(parserMap);
    }

    public Map<String, ClariasGenerator<?>> getGeneratorMap() {
        return new HashMap<>(generatorMap);
    }
}
