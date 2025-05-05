package com.giga.nexas.util;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PacUtil {

    private static final String NEXAS_PAC_EXE = "NexasPack.exe";

    // 获取指定的命令行
    private static String getExePath(String exeName) throws IOException {
        // 临时目录
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "nexas_exe");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // src/main/resources/exe
        String[] filesToExtract = {
                "NexasPack.exe",
                "zlib.dll",
                "libzstd.dll"
        };

        ClassLoader classLoader = PacUtil.class.getClassLoader();

        for (String fileName : filesToExtract) {
            File target = new File(tempDir, fileName);
            if (!target.exists()) {
                try (InputStream in = classLoader.getResourceAsStream("exe/" + fileName);
                     OutputStream out = new FileOutputStream(target)) {
                    if (in == null) {
                        throw new IOException("找不到资源: exe/" + fileName);
                    }
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }
                if (fileName.endsWith(".exe")) {
                    target.setExecutable(true); // 赋予可执行权限
                }
            }
        }

        return new File(tempDir, exeName).getAbsolutePath();
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

    /**
     * 执行 pac 解包命令
     *
     * @param pacPath pac 文件路径
     * @return 解包过程输出
     * @throws IOException 执行失败
     */
    public static String unpack(String pacPath) throws IOException {
        String exe = getExePath(NEXAS_PAC_EXE);
        return executeCommand(List.of(exe, pacPath), false, null);
    }

    /**
     * 执行 pac 封包命令，并模拟输入压缩方式
     *
     * @param folderPath 输入文件夹路径
     * @param compressMode 压缩方式（0=no compress, 4=zlib, 7=zstd）
     * @return 封包过程输出
     * @throws IOException 执行失败
     */
    public static String pack(String folderPath, String compressMode) throws IOException {
        String exe = getExePath(NEXAS_PAC_EXE);
        return executeCommand(List.of(exe, folderPath), true, compressMode);
    }

    /**
     * 执行命令，并可选模拟输入
     *
     * @param commandArgs 命令字符串
     * @param simulateInput 是否模拟输入
     * @param inputText 模拟输入内容（回车结尾）
     * @return 执行过程控制台输出
     * @throws IOException 执行失败
     */
    private static String executeCommand(List<String> commandArgs, boolean simulateInput, String inputText) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(commandArgs);
        builder.redirectErrorStream(true);

        File exeDir = new File(System.getProperty("java.io.tmpdir"), "nexas_exe");
        builder.directory(exeDir);
        builder.environment().put("PATH", exeDir.getAbsolutePath() + File.pathSeparator + System.getenv("PATH"));

        Process process = builder.start();

        if (simulateInput && inputText != null) {
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
                writer.write(inputText);
                writer.newLine(); // 模拟按下回车
                writer.flush();
            }
        }

        String encoding = System.getProperty("sun.jnu.encoding");
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), simulateInput?encoding:"windows-31j"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }

        return output.toString();
    }

}
