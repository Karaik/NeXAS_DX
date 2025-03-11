package com.giga.nexasdxeditor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@SpringBootTest
public class OtherTest {

    private static final Logger log = LoggerFactory.getLogger(OtherTest.class);

    @Test
    void splitGalScript() {
        String inputFilePath = "D:\\A\\script\\062.spt.txt";
        String outputBaseName = "D:\\A\\script\\output\\062.spt.";
        String splitKeyword = "split";

        String content = FileUtil.readString(inputFilePath, StandardCharsets.UTF_8);
        List<String> parts = StrUtil.split(content, splitKeyword);

        for (int i = 0; i < parts.size(); i++) {
            String outputFilePath = String.format("%s%02d.txt", outputBaseName, i + 1);
            FileUtil.writeString(parts.get(i), new File(outputFilePath), StandardCharsets.UTF_8);
            log.info("outputFilePath: {}", outputFilePath);
        }
    }

    @Test
    void mergeGalScript() {
        String inputFolderPath = "D:\\A\\script\\output\\";
        String outputFilePath = "D:\\A\\script\\merged\\062_merged.spt.txt";

        // 获取output目录下的所有.txt文件
        List<File> files = Arrays.stream(new File(inputFolderPath).listFiles((dir, name) -> name.endsWith(".txt")))
                .sorted()
                .collect(Collectors.toList());

        StringBuilder mergedContent = new StringBuilder();
        for (File file : files) {
            String content = FileUtil.readString(file, StandardCharsets.UTF_8);
            mergedContent.append(content);
//                    .append("\n"); // 换行
            log.info("Merging file: {}", file.getAbsolutePath());
        }

        FileUtil.mkdir(new File(outputFilePath).getParentFile());

        FileUtil.writeString(mergedContent.toString(), new File(outputFilePath), StandardCharsets.UTF_8);
        log.info("Merged file created at: {}", outputFilePath);
    }

    @Test
    void processGalScript() {
        String inputFilePath = "D:\\A\\script\\062.spt.txt";
        String outputFilePath = "D:\\A\\script\\processed\\062.spt.txt";

        // 读取文件
        List<String> lines = FileUtil.readLines(inputFilePath, StandardCharsets.UTF_8);

        // 处理文本
        List<String> processedLines = lines.stream()
                .map(this::processLine)
                .collect(Collectors.toList());

        // 确保输出目录存在
        FileUtil.mkdir(new File(outputFilePath).getParentFile());

        // 写入处理后的文件
        FileUtil.writeUtf8Lines(processedLines, outputFilePath);
        log.info("Processed file saved at: {}", outputFilePath);
    }

    /**
     * 处理单行文本
     */
    private String processLine(String line) {
        if (!line.contains("●")) {
            return line; // 如果没有 ●，直接返回
        }

        // 检查是否有「」的内容
        if (line.contains("「") && line.contains("」")) {
            return removeQuotedText(line);
        } else {
            return removeTextAfterSecondBullet(line);
        }
    }

    /**
     * 删除「」内的文本
     */
    private String removeQuotedText(String line) {
        int startIndex = line.indexOf("「"); // 找到第一个「
        int endIndex = line.lastIndexOf("[\\r][\\n]"); // 找到最后一个 [\r][\n]

        if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
            return line.substring(0, startIndex + 1) + "」" + line.substring(endIndex);
        }

        return line;
    }

    /**
     * 删除第二个 ● 之后的文本（直到最后的 \r\n）
     */
    private String removeTextAfterSecondBullet(String line) {
        Pattern pattern = Pattern.compile("●.*?● "); // 匹配 `● ... ● `
        Matcher matcher = pattern.matcher(line);

        if (matcher.find()) {
            // 找到第二个●，删除它之后的内容
            return line.substring(0, matcher.end()).trim() + " 。[\\r][\\n]";
        }

        return line;
    }


}
