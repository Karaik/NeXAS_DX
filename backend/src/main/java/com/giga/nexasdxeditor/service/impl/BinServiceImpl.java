package com.giga.nexasdxeditor.service.impl;

import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.Script;
import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.service.BinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class BinServiceImpl implements BinService {
    private static final Logger log = LoggerFactory.getLogger(BinServiceImpl.class);

    @Override
    public ResponseDTO parse(String path) throws IOException {
        // 创建一个File对象
        File file = new File(path);

        // 确保文件存在
        if (!file.exists()) {
            throw new IOException("文件不存在: " + path);
        }

        // 创建一个RandomAccessFile对象，传递给BinaryReader
        RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r"); // 只读模式
        BinaryReader binaryReader = new BinaryReader(randomAccessFile);

        // 使用BinaryReader来解析脚本
        String s = Script.parseScript(new Script(), binaryReader, false);
        return new ResponseDTO<>();
    }

}
