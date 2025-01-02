package com.giga.nexasdxeditor.service.impl;

import com.giga.nexasdxeditor.service.BinService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.Charset;

@Service
public class BinServiceImpl implements BinService {
    private static final Logger log = LoggerFactory.getLogger(BinServiceImpl.class);

    @Override
    public void convertBin2Hex(MultipartFile waz, MultipartFile mek) throws IOException {
        // 读取第一个文件
        byte[] fileBytes = waz.getBytes();
        // 打开输出流显示十六进制
        StringBuilder hexOutput = new StringBuilder();
        StringBuilder asciiOutput = new StringBuilder();
        long filePointerAddress = 0;

        // 以16字节为单位处理文件内容
        for (int i = 0; i < fileBytes.length; i += 16) {
            hexOutput.setLength(0);  // 清空十六进制输出缓存
            asciiOutput.setLength(0); // 清空字符输出缓存

            // 输出当前内存地址（使用文件指针地址）
            hexOutput.append(String.format("%08X  ", filePointerAddress)); // 输出当前内存地址
            filePointerAddress += 16;

            // 处理16个字节的内容
            int bytesRead = Math.min(16, fileBytes.length - i);  // 读取剩余字节数
            StringBuilder hexPart = new StringBuilder();
            StringBuilder asciiPart = new StringBuilder();

            // 输出前8个字节的十六进制
            for (int j = 0; j < 8; j++) {
                if (j < bytesRead) {
                    hexPart.append(String.format("%02X ", fileBytes[i + j])); // 十六进制输出
                } else {
                    hexPart.append("   "); // 填充空白对齐
                }
            }

            // 输出后8个字节的十六进制
            for (int j = 8; j < 16; j++) {
                if (j < bytesRead) {
                    hexPart.append(String.format("%02X ", fileBytes[i + j])); // 十六进制输出
                } else {
                    hexPart.append("   "); // 填充空白对齐
                }
            }

            hexOutput.append(hexPart);  // 拼接到十六进制输出部分

            // 使用正确的字符编码（例如 Shift-JIS）转换字节为字符串
            byte[] segment = new byte[bytesRead];
            System.arraycopy(fileBytes, i, segment, 0, bytesRead);
            String asciiString = new String(segment, Charset.forName("Shift-JIS"));

            // 输出十六进制和字符
            System.out.println(hexOutput.toString() + " | " + asciiString);
        }

        System.out.println("Conversion complete.");
    }

}
