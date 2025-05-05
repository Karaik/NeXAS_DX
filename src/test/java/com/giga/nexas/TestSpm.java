package com.giga.nexas;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.service.BinService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class TestSpm {

    BinService binService = new BinService();

    private static final Path GAME_SPM_DIR = Paths.get("src/main/resources/game/bsdx/spm");
    private static final Path JSON_OUTPUT_DIR = Paths.get("src/main/resources/spmJson");
    private static final Path UPDATE3_DIR = Paths.get("src/main/resources/Update3");

    @Test
    void parseJson2Spm() throws IOException {
        Path inputFilePath = JSON_OUTPUT_DIR.resolve("Kou.json");
        String jsonStr1 = FileUtil.readUtf8String(inputFilePath.toFile());
        Spm bean = JSONUtil.toBean(jsonStr1, Spm.class);
        System.out.println("Loaded bean: " + bean.getClass().getSimpleName());

        String jsonStr2 = JSONUtil.toJsonStr(bean);
        JSONObject jsonObject1 = JSONUtil.parseObj(bean);
        JSONObject jsonObject2 = JSONUtil.parseObj(jsonStr2);
        boolean isEqual = jsonObject1.equals(jsonObject2);
        System.out.println("Result: " + (isEqual ? "✅ Equal" : "❌ Not Equal"));
    }

    @Test
    void testCreateTestSpmDat() throws IOException {
        List<Path> files = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(UPDATE3_DIR, "*.spm")) {
            for (Path path : stream) {
                files.add(path);
            }
        }

        if (!files.isEmpty()) {
            byte[] firstFileContent = FileUtil.readBytes(files.get(0).toFile());
            for (int i = 1; i < files.size(); i++) {
                FileUtil.writeBytes(firstFileContent, files.get(i).toFile());
                System.out.println("Overwritten: " + files.get(i));
            }
        }
    }

    @Test
    void testGenerateSpmJsonFiles() throws IOException {
        Files.createDirectories(JSON_OUTPUT_DIR);

        List<Spm> allSpmList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_SPM_DIR, "*.spm")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO parse = binService.parse(path.toString(), "windows-31j");
                    Spm spm = (Spm) parse.getData();
                    allSpmList.add(spm);
                } catch (Exception e) {
                    System.err.println("Parse failed for: " + fileName);
                }
            }
        }

        System.out.println("Total parsed: " + allSpmList.size());

        for (int i = 0; i < allSpmList.size(); i++) {
            Spm spm = allSpmList.get(i);
            String jsonStr = JSONUtil.toJsonStr(spm);
            Path outputPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + ".json");
            FileUtil.writeUtf8String(jsonStr, outputPath.toFile());
            System.out.println("Exported JSON: " + outputPath);
        }
    }
}
