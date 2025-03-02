package com.giga.nexasdxeditor.service.impl;

import cn.hutool.core.util.StrUtil;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.service.PacService;
import com.giga.nexasdxeditor.util.PacUtil;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PacServiceImpl implements PacService {

    private final static String PAC_SUFFIX = ".pac";

    @Override
    public ResponseDTO unPac(String sourcePath) throws Exception {
        String normalizedSourcePath = StrUtil.replace(sourcePath, "\\", "/");
        String fileNameWithExtension = StrUtil.subAfter(normalizedSourcePath, "/", true);
        String fileName = StrUtil.removeSuffix(fileNameWithExtension, PAC_SUFFIX); // 文件名

        String targetPath = StrUtil.removeSuffix(sourcePath, fileName+PAC_SUFFIX) + fileName;
        String output = PacUtil.unpack(sourcePath, targetPath);

        Map<String, String> fileMap = this.getFileMap(targetPath);
        ResponseDTO<Map<String, String>> responseDTO = new ResponseDTO<>(fileMap, output);

        return responseDTO;
    }

    @Override
    public ResponseDTO pac(String sourcePath) throws Exception {
        String output = PacUtil.pack(sourcePath);

        ResponseDTO responseDTO = new ResponseDTO<>(output);

        return responseDTO;
    }

    private Map<String, String> getFileMap(String targetPath) throws Exception {
        // 获取目标路径下的所有文件并存入Map
        File targetDirectory = new File(targetPath);
        Map<String, String> fileMap = new HashMap<>();
        if (targetDirectory.exists() && targetDirectory.isDirectory()) {
            // 获取文件夹下所有文件
            List<String> fileNames = Files.list(targetDirectory.toPath())
                    .filter(Files::isRegularFile) // 只获取文件（排除目录）
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .toList();

            // 将文件名作为key，文件路径作为value存入Map
            for (String name : fileNames) {
                fileMap.put(name, targetPath + "\\" + name); // 或者使用其他文件属性
            }
        }

        // 返回文件名Map，可以根据需求调整返回格式
        return fileMap;
    }
}