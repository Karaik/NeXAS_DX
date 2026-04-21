package com.giga.nexas.service;

import cn.hutool.core.util.StrUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.Bsdx;
import com.giga.nexas.dto.bsdx.BsdxGenerator;
import com.giga.nexas.dto.bsdx.BsdxParser;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.GLOBAL;
import com.giga.nexas.dto.bsdx.bin.generator.BinGenerator;
import com.giga.nexas.dto.bsdx.bin.parser.BinParser;
import com.giga.nexas.dto.bsdx.bin.parser.GLOBALParser;
import com.giga.nexas.dto.bsdx.dat.generator.DatGenerator;
import com.giga.nexas.dto.bsdx.dat.parser.DatParser;
import com.giga.nexas.dto.bsdx.grp.generator.GrpGenerator;
import com.giga.nexas.dto.bsdx.grp.parser.GrpParser;
import com.giga.nexas.dto.bsdx.mek.generator.MekGenerator;
import com.giga.nexas.dto.bsdx.mek.parser.MekParser;
import com.giga.nexas.dto.bsdx.spm.generator.SpmGenerator;
import com.giga.nexas.dto.bsdx.spm.parser.SpmParser;
import com.giga.nexas.dto.bsdx.waz.generator.WazGenerator;
import com.giga.nexas.dto.bsdx.waz.parser.WazParser;
import com.giga.nexas.exception.OperationException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BsdxBinService {

    private static final String BIN_EXTENSION = "bin";
    private static final String GLOBAL_FILE_NAME = "__global";

    private final Map<String, BsdxParser<?>> parserMap = new HashMap<>();
    private final Map<String, BsdxGenerator<?>> generatorMap = new HashMap<>();
    private final GLOBALParser globalParser = new GLOBALParser();
    private final Map<Path, GLOBAL> globalCache = new HashMap<>();

    public BsdxBinService() {
        // 注册parser
        registerParser(new SpmParser());
        registerParser(new MekParser());
        registerParser(new WazParser());
        registerParser(new DatParser());
        registerParser(new BinParser());
        registerParser(new GrpParser());

        // 注册generator
        registerGenerator(new SpmGenerator());
        registerGenerator(new MekGenerator());
        registerGenerator(new WazGenerator());
        registerGenerator(new DatGenerator());
        registerGenerator(new GrpGenerator());
        registerGenerator(new BinGenerator());
    }

    public BsdxBinService(List<BsdxParser<?>> bsdxParsers) {
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

    public ResponseDTO<?> parse(String path, String charset) throws IOException {
        String ext = getFileExtension(path);
        String fileName = getFileName(path);
        BsdxParser<?> bsdxParser = resolveParser(path, ext, fileName);
        if (bsdxParser == null) {
            throw new OperationException(500, "unsupported file type for parsing: " + ext);
        }

        GLOBAL siblingGlobal = null;
        if (BIN_EXTENSION.equalsIgnoreCase(ext) && !GLOBAL_FILE_NAME.equalsIgnoreCase(fileName)) {
            siblingGlobal = loadSiblingGlobal(Paths.get(path), charset);
        }

        byte[] data = Files.readAllBytes(Paths.get(path));
        Bsdx parsed = bsdxParser.parse(data, getFileName(path), charset);
        attachGlobalSymbolsIfNeeded(ext, fileName, parsed, siblingGlobal);
        parsed.setExtensionName(ext);
        return new ResponseDTO<>(parsed, "ok");
    }

    /**
     * 解析阶段优先处理 BSDX `bin` 的 `__GLOBAL.bin` 特例。
     *
     * <p>这样可以避免 `GLOBALParser` 与普通 `BinParser` 都声明支持 `bin`
     * 时出现注册覆盖，同时也能保证普通 `bin` 在业务解析前先有机会装入全局环境。
     */
    private BsdxParser<?> resolveParser(String path, String ext, String fileName) {
        if (BIN_EXTENSION.equalsIgnoreCase(ext) && GLOBAL_FILE_NAME.equalsIgnoreCase(fileName)) {
            return globalParser;
        }
        return parserMap.get(ext);
    }

    /**
     * 对普通 BSDX `bin` 注入同目录 `__GLOBAL.bin` 的符号表。
     *
     * <p>这个约束只对 BSDX 的 `bin` 生效，不会影响 `dat/grp/mek/spm/waz`。
     * 同时 `__GLOBAL.bin` 自己不会再次递归注入自己。
     */
    private void attachGlobalSymbolsIfNeeded(String ext, String fileName, Bsdx parsed, GLOBAL siblingGlobal) {
        if (!(parsed instanceof Bin bin)) {
            return;
        }
        if (!BIN_EXTENSION.equalsIgnoreCase(ext) || GLOBAL_FILE_NAME.equalsIgnoreCase(fileName)) {
            return;
        }

        if (siblingGlobal != null) {
            bin.setGlobalSymbols(siblingGlobal.getSymbolTable());
        }
    }

    /**
     * 读取与目标 `bin` 同目录的 `__GLOBAL.bin`。
     *
     * <p>这里做了目录级缓存，避免批量测试时重复解析同一份全局符号表。
     */
    private GLOBAL loadSiblingGlobal(Path binPath, String charset) throws IOException {
        Path parent = binPath.getParent();
        if (parent == null) {
            return null;
        }
        Path globalPath = parent.resolve("__GLOBAL.bin");
        if (!Files.exists(globalPath)) {
            return null;
        }
        GLOBAL cached = globalCache.get(globalPath.toAbsolutePath().normalize());
        if (cached != null) {
            return cached;
        }

        byte[] data = Files.readAllBytes(globalPath);
        GLOBAL parsed = globalParser.parse(data, GLOBAL_FILE_NAME, charset);
        parsed.setExtensionName(BIN_EXTENSION);
        globalCache.put(globalPath.toAbsolutePath().normalize(), parsed);
        return parsed;
    }

    public <T extends Bsdx> ResponseDTO<?> generate(String path, T obj, String charset) throws IOException {
        String ext = getJsonExtension(obj);
        BsdxGenerator<T> gen = (BsdxGenerator<T>) generatorMap.get(ext);
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

    private String getJsonExtension(Bsdx obj) {
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

    public Map<String, BsdxParser<?>> getParserMap() {
        return new HashMap<>(parserMap);
    }

    public Map<String, BsdxGenerator<?>> getGeneratorMap() {
        return new HashMap<>(generatorMap);
    }
}
