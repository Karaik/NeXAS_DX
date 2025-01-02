package com.giga.nexasdxeditor.util;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PacUtil {

    private static final String ZBSPAC_EXE_NAME = "zbspac.exe";
    private static final String ZLIBPAC_EXE_NAME = "pac_pack.exe";

    // 获取指定的命令行
    private static String getExePath(String exeName) throws IOException, URISyntaxException {
        // 获取exe文件所在的文件夹资源
        InputStream exeFolderStream = PacUtil.class.getResourceAsStream("/exe/");

        if (exeFolderStream == null) {
            throw new IOException("无法找到exe文件夹");
        }

        // 创建临时目录
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "exe_temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // 获取exe文件夹路径
        Path exeFolderPath = Paths.get(PacUtil.class.getResource("/exe/").toURI());
        File folder = new File(exeFolderPath.toString());

        // 确保是文件夹
        if (folder.isDirectory()) {
            // 遍历exe文件夹中的所有文件并复制到临时目录
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    // 复制每个文件到临时目录
                    copyFile(file, new File(tempDir, file.getName()));
                }
            }
        } else {
            throw new IOException("exe文件夹不是一个有效目录");
        }

        // 找到指定exe文件并返回其路径
        File exeFile = new File(tempDir, exeName);
        if (!exeFile.exists()) {
            throw new IOException(exeName + "文件未找到");
        }

        return exeFile.getAbsolutePath();
    }
    private static void copyFile(File source, File destination) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    // 执行命令
    private static String executeCommand(String command, boolean simulateEnter) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command.split(" "));
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        if (simulateEnter) {
            try (OutputStream outputStream = process.getOutputStream()) {
                outputStream.write('\n');  // 模拟回车键（按下Enter）
                outputStream.flush();
            }
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName(simulateEnter?"GBK":"Shift-JIS")))) { //前为打包程序输出编码，后为解包
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
            process.waitFor();
            return output.toString();
        } catch (InterruptedException e) {
            throw new IOException("命令执行被中断", e);
        }
    }

    // 打包操作
    public static String pack(String sourcePath) throws Exception{

        String exePath = getExePath(ZLIBPAC_EXE_NAME);
        String command = exePath + " " + sourcePath;
        String output = executeCommand(command, true);

        return output;
    }

    // 解包操作
    public static String unpack(String sourcePath, String targetPath) throws Exception{

        String exePath = getExePath(ZBSPAC_EXE_NAME);
        String command = exePath + " unpack " + sourcePath + " " + targetPath;
        String output = executeCommand(command, false);

        return output;
    }

}
