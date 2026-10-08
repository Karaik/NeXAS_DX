package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.service.BsdxBinService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** 不重生成未改原版池，尤其保留原版 SPM 解析器尚未建模的尾部 bytes。 */
public final class BaselineAssetComparison {
    private static final Set<String> RECORD_ONLY = Set.of("fileName", "extensionName", "offset", "length", "unitDescription", "description");
    private final BsdxBinService service;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Path, Map<String, Path>> indices = new HashMap<>();

    public BaselineAssetComparison(BsdxBinService service) { this.service = service; }

    public boolean unchanged(Path originalDir, String filename, Object candidate) throws IOException {
        if (originalDir == null || candidate == null || !Files.isDirectory(originalDir)) return false;
        Path normalized = originalDir.toAbsolutePath().normalize();
        Map<String, Path> index = indices.get(normalized);
        if (index == null) {
            index = new HashMap<>();
            try (var files = Files.list(normalized)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) index.put(LayeredAssetResolver.normalize(file.getFileName().toString()), file);
            }
            indices.put(normalized, index);
        }
        Path original = index.get(LayeredAssetResolver.normalize(filename));
        if (original == null) return false;
        Object parsed = service.parse(original.toString(), "windows-31j").getData();
        return content(mapper.valueToTree(parsed)).equals(content(mapper.valueToTree(candidate)));
    }

    private JsonNode content(JsonNode node) {
        if (node instanceof ObjectNode object) {
            object.remove(RECORD_ONLY);
            object.elements().forEachRemaining(this::content);
        } else if (node.isArray()) node.elements().forEachRemaining(this::content);
        return node;
    }
}
