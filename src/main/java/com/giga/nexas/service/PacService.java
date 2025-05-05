package com.giga.nexas.service;

import cn.hutool.core.util.StrUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.util.PacUtil;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PacService {

    private final static String PAC_SUFFIX = ".pac";

    public ResponseDTO unPac(String sourcePath) throws Exception {
        String output = PacUtil.unpack(sourcePath);

        ResponseDTO responseDTO = new ResponseDTO<>(output);

        return responseDTO;
    }

    public ResponseDTO pac(String sourcePath, String compressMode) throws Exception {
        String output = PacUtil.pack(sourcePath, compressMode);

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