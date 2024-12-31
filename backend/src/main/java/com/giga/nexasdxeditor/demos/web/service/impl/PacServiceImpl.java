package com.giga.nexasdxeditor.demos.web.service.impl;

import cn.hutool.core.util.StrUtil;
import com.giga.nexasdxeditor.demos.web.service.PacService;
import com.giga.nexasdxeditor.demos.web.util.PacUtil;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PacServiceImpl implements PacService {


    @Override
    public Map<String, String> unPac(String sourcePath) throws Exception {
        String normalizedPath = StrUtil.replace(sourcePath, "\\", "/");
        String fileNameWithExtension = StrUtil.subAfter(normalizedPath, "/", true);
        String fileName = StrUtil.removeSuffix(fileNameWithExtension, ".pac");
        String targetPath = sourcePath + "/" + fileName;
        String output = PacUtil.unpack(sourcePath, targetPath);
        // 获取目标路径下的所有文件并存入Map
        File targetDirectory = new File(targetPath);
        Map<String, String> fileMap = new HashMap<>();

        if (targetDirectory.exists() && targetDirectory.isDirectory()) {
            // 获取文件夹下所有文件
            List<String> fileNames = Files.list(targetDirectory.toPath())
                    .filter(Files::isRegularFile) // 只获取文件（排除目录）
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());

            // 将文件名作为key，文件路径作为value存入Map
            for (String name : fileNames) {
                fileMap.put(name, targetPath + "/" + name); // 或者使用其他文件属性
            }
        }

        // 返回文件名Map，可以根据需求调整返回格式
        return fileMap;
    }

    @Override
    public Map<String, String> pac(String sourcePath) throws Exception {
        return Map.of();
    }
}