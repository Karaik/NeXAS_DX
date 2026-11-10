import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.service.BsdxBinService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 调用仓库生产解析器，解码 Mod 解包目录中的 BSDX 机械数据。
 * 输出均为可丢弃的中间文件，只保存在 target/ 下；分析器不会把辅助代码复制到生产源码。
 */
public final class ModSampleJsonDumper {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("waz", "mek", "grp");

    private ModSampleJsonDumper() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException(
                    "用法：ModSampleJsonDumper <Mod 解包目录> <输出目录>"
            );
        }

        Path inputDir = Paths.get(args[0]).toAbsolutePath().normalize();
        Path outputDir = Paths.get(args[1]).toAbsolutePath().normalize();
        if (!Files.isDirectory(inputDir)) {
            throw new IllegalArgumentException("未找到 Mod 解包目录：" + inputDir);
        }
        Files.createDirectories(outputDir);

        BsdxBinService service = new BsdxBinService();
        ObjectMapper mapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT);

        List<Path> inputs;
        try (Stream<Path> stream = Files.list(inputDir)) {
            inputs = stream
                    .filter(Files::isRegularFile)
                    .filter(ModSampleJsonDumper::isSupported)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                    .toList();
        }

        for (Path input : inputs) {
            ResponseDTO<?> response = service.parse(input.toString(), "windows-31j");
            Path output = outputDir.resolve(input.getFileName().toString() + ".json");
            writeUtf8Json(mapper, output, response.getData());
            System.out.printf("已解码 %s -> %s%n", input.getFileName(), output);
        }
    }

    private static boolean isSupported(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return false;
        }
        return SUPPORTED_EXTENSIONS.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    private static void writeUtf8Json(ObjectMapper mapper, Path output, Object value) throws IOException {
        String json = mapper.writeValueAsString(value)
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "\r\n") + "\r\n";
        Files.writeString(output, json, StandardCharsets.UTF_8);
    }
}
