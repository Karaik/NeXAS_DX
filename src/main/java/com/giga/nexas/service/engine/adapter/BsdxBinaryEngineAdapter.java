package com.giga.nexas.service.engine.adapter;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.Bsdx;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.map.MapData;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.exception.OperationException;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.service.engine.BinaryEngineAdapter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static com.giga.nexas.controller.consts.MainConst.*;

/**
 * Adapter for BSDX engine parse/generate workflows.
 */
public class BsdxBinaryEngineAdapter implements BinaryEngineAdapter {

    private final BsdxBinService service = new BsdxBinService();
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
        Bsdx base = mapper.readValue(jsonStr, Bsdx.class);
        String ext = base.getExtensionName();
        if (ext == null) {
            throw new OperationException(500, "extensionName missing in JSON: " + jsonFile.getFileName());
        }
        Bsdx payload = mapPayload(ext.toLowerCase(), jsonStr);
        Files.createDirectories(outputDir);
        Path target = outputDir.resolve(FileUtil.mainName(jsonFile.toFile()));
        service.generate(target.toString(), payload, charset);
        return target;
    }

    private Bsdx mapPayload(String ext, String json) throws IOException {
        return switch (ext) {
            case WAZ_EXT -> mapper.readValue(json, Waz.class);
            case MEK_EXT -> mapper.readValue(json, Mek.class);
            case SPM_EXT -> mapper.readValue(json, Spm.class);
            case GRP_EXT -> mapper.readValue(json, Grp.class);
            case BIN_EXT -> mapper.readValue(json, Bin.class);
            case DAT_EXT -> mapper.readValue(json, Dat.class);
            case MAP_EXT -> mapper.readValue(json, MapData.class);
            default -> throw new OperationException(500, "unsupported extension for BSDX: " + ext);
        };
    }
}
