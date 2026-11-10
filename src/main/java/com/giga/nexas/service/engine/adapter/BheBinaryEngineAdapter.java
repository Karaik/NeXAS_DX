package com.giga.nexas.service.engine.adapter;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bhe.Bhe;
import com.giga.nexas.dto.bhe.grp.Grp;
import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.dto.bhe.spm.Spm;
import com.giga.nexas.exception.OperationException;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.engine.BinaryEngineAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static com.giga.nexas.controller.consts.MainConst.MAP_EXT;

/**
 * Adapter for BHE engine parse/generate workflows.
 */
public class BheBinaryEngineAdapter implements BinaryEngineAdapter {

    private final BheBinService service = new BheBinService();
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Override
    public Set<String> supportedParseExtensions() {
        return service.getParserMap().keySet();
    }

    @Override
    public Set<String> supportedGenerateExtensions() {
        return service.getGeneratorMap().keySet();
    }

    @Override
    public Path parse(Path inputFile, Path outputDir, String charset) throws IOException {
        ResponseDTO<?> response = service.parse(inputFile.toString(), charset);
        String json = JSONUtil.toJsonStr(response.getData());
        Files.createDirectories(outputDir);
        Path target = outputDir.resolve(inputFile.getFileName().toString() + ".json");
        FileUtil.writeUtf8String(json, target.toFile());
        return target;
    }

    @Override
    public Path generate(Path jsonFile, Path outputDir, String charset) throws IOException {
        String jsonStr = FileUtil.readUtf8String(jsonFile.toFile());
        Bhe base = mapper.readValue(jsonStr, Bhe.class);
        String ext = base.getExtensionName();
        if (ext == null) {
            throw new OperationException(500, "extensionName missing in JSON: " + jsonFile.getFileName());
        }
        Bhe payload = mapPayload(ext.toLowerCase(), jsonStr);
        Files.createDirectories(outputDir);
        Path target = outputDir.resolve(FileUtil.mainName(jsonFile.toFile()));
        service.generate(target.toString(), payload, charset);
        return target;
    }

    private Bhe mapPayload(String ext, String json) throws IOException {
        return switch (ext) {
            case "spm" -> mapper.readValue(json, Spm.class);
            case "grp" -> mapper.readValue(json, Grp.class);
            case MAP_EXT -> mapper.readValue(json, MapData.class);
            default -> throw new OperationException(500, "unsupported BHE generation extension: " + ext);
        };
    }
}

