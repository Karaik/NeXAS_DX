package com.giga.nexas.service.engine.adapter;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.clarias.Clarias;
import com.giga.nexas.dto.clarias.dat.Dat;
import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.dto.clarias.mek.Mek;
import com.giga.nexas.exception.OperationException;
import com.giga.nexas.service.ClariasBinService;
import com.giga.nexas.service.engine.BinaryEngineAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static com.giga.nexas.controller.consts.MainConst.DAT_EXT;
import static com.giga.nexas.controller.consts.MainConst.GRP_EXT;
import static com.giga.nexas.controller.consts.MainConst.MEK_EXT;

/**
 * Adapter for CLARIAS workflows.
 */
public class ClariasBinaryEngineAdapter implements BinaryEngineAdapter {

    private final ClariasBinService service = new ClariasBinService();
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
        Clarias base = mapper.readValue(jsonStr, Clarias.class);
        String ext = base.getExtensionName();
        if (ext == null) {
            throw new OperationException(500, "extensionName missing in JSON: " + jsonFile.getFileName());
        }
        Clarias payload = mapPayload(ext.toLowerCase(), jsonStr);
        Files.createDirectories(outputDir);
        Path target = outputDir.resolve(FileUtil.mainName(jsonFile.toFile()));
        service.generate(target.toString(), payload, charset);
        return target;
    }

    private Clarias mapPayload(String ext, String json) throws IOException {
        return switch (ext) {
            case DAT_EXT -> mapper.readValue(json, Dat.class);
            case GRP_EXT -> mapper.readValue(json, Grp.class);
            case MEK_EXT -> mapper.readValue(json, Mek.class);
            default -> throw new OperationException(500, "unsupported CLARIAS extension: " + ext);
        };
    }
}
