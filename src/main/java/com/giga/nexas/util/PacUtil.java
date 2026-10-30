package com.giga.nexas.util;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class PacUtil {
    private static final String NEXAS_PAC_EXE = "NexasPack.exe";

    /** GUI兼容入口：工具不写入原PAC目录；构建应使用显式ownedWorkRoot重载。 */
    public static String unpack(String pacPath) throws IOException {
        Path legacyWorkspace = Files.createTempDirectory("nexas-pac-unpack-");
        Throwable failure = null;
        try {
            return unpack(pacPath, legacyWorkspace);
        } catch (IOException | RuntimeException | Error error) {
            failure = error;
            throw error;
        } finally {
            cleanupOwnedDirectory(legacyWorkspace, failure);
        }
    }

    /** 在调用方拥有的工作根内缓存工具和创建单次临时目录，不改变原PAC位置及解包语义。 */
    public static String unpack(String pacPath, Path ownedWorkRoot) throws IOException {
        Path source = Path.of(pacPath).toAbsolutePath().normalize();
        return run(source, false, null, ownedWorkRoot);
    }

    /** GUI兼容入口：工具位于待封包目录的父级，不混入flat资源。 */
    public static String pack(String folderPath, String compressMode) throws IOException {
        Path source = Path.of(folderPath).toAbsolutePath().normalize();
        if (source.getParent() == null) throw new IOException("pack source has no operation parent: " + source);
        return pack(source.toString(), compressMode, source.getParent());
    }

    /** 构建入口显式指定本轮owned工作根；压缩方式0/4/7仍原样交给NexasPack。 */
    public static String pack(String folderPath, String compressMode, Path ownedWorkRoot) throws IOException {
        return run(Path.of(folderPath).toAbsolutePath().normalize(), true, compressMode, ownedWorkRoot);
    }

    private static String run(Path source, boolean pack, String inputText, Path ownedWorkRoot) throws IOException {
        if (ownedWorkRoot == null) throw new IllegalArgumentException("ownedWorkRoot must not be null");
        Path toolDirectory = ownedWorkRoot.toAbsolutePath().normalize().resolve(".nexas-tools");
        if (pack) validatePackToolDirectory(source, toolDirectory);
        Path executable = getExePath(toolDirectory);
        Path session = Files.createTempDirectory(toolDirectory, "session-");
        Throwable failure = null;
        try {
            return executeCommand(List.of(executable.toString(), source.toString()), pack, inputText, session);
        } catch (IOException | RuntimeException | Error error) {
            failure = error;
            throw error;
        } finally {
            // executeCommand returns or throws only after its owned native process has exited.
            cleanupOwnedDirectory(session, failure);
        }
    }

    private static void validatePackToolDirectory(Path source, Path toolDirectory) throws IOException {
        if (toolDirectory.toAbsolutePath().normalize().startsWith(source.toAbsolutePath().normalize()))
            throw new IOException("owned tools must be outside the folder being packed: " + toolDirectory);
    }

    private static synchronized Path getExePath(Path toolDirectory) throws IOException {
        Files.createDirectories(toolDirectory);
        for (String fileName : List.of(NEXAS_PAC_EXE, "zlib.dll", "libzstd.dll")) {
            Path target = toolDirectory.resolve(fileName);
            if (Files.exists(target)) {
                if (!Files.isRegularFile(target)) throw new IOException("native tool is not a regular file: " + target);
                continue;
            }
            try (InputStream input = PacUtil.class.getClassLoader().getResourceAsStream("exe/" + fileName)) {
                if (input == null) throw new IOException("can't find resource: exe/" + fileName);
                try {
                    Files.copy(input, target);
                } catch (IOException error) {
                    try { Files.deleteIfExists(target); }
                    catch (IOException cleanup) { error.addSuppressed(cleanup); }
                    throw error;
                }
            }
            if (fileName.endsWith(".exe")) target.toFile().setExecutable(true);
        }
        return toolDirectory.resolve(NEXAS_PAC_EXE).toAbsolutePath().normalize();
    }

    private static ProcessBuilder createProcessBuilder(List<String> commandArgs, Path session) {
        Path executableDirectory = Path.of(commandArgs.get(0)).toAbsolutePath().normalize().getParent();
        ProcessBuilder builder = new ProcessBuilder(commandArgs);
        builder.redirectErrorStream(true);
        builder.directory(executableDirectory.toFile());
        var environment = builder.environment();
        environment.put("PATH", executableDirectory + File.pathSeparator + environment.getOrDefault("PATH", ""));
        environment.put("TEMP", session.toAbsolutePath().normalize().toString());
        environment.put("TMP", session.toAbsolutePath().normalize().toString());
        return builder;
    }

    private static String executeCommand(List<String> arguments, boolean input, String text, Path session) throws IOException {
        Charset encoding = input
                ? Charset.forName(System.getProperty("sun.jnu.encoding", Charset.defaultCharset().name()))
                : Charset.forName("windows-31j");
        Process process = createProcessBuilder(arguments, session).start();
        try {
            return collectProcess(process, input, text, encoding);
        } finally {
            // Also cover a failure while the output collector itself is being initialized.
            boolean interrupted = stopAndConfirmExit(process);
            closeQuietly(process.getOutputStream());
            closeQuietly(process.getInputStream());
            closeQuietly(process.getErrorStream());
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static String collectProcess(Process process, boolean simulateInput, String inputText, Charset encoding) throws IOException {
        StringBuilder output = new StringBuilder();
        AtomicReference<IOException> readFailure = new AtomicReference<>();
        Thread drainer = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), encoding))) {
                for (String line; (line = reader.readLine()) != null;) output.append(line).append(System.lineSeparator());
            } catch (IOException error) {
                readFailure.set(error);
                process.destroyForcibly();
            }
        }, "nexas-pac-output");
        drainer.setDaemon(true);
        boolean interrupted = false;
        try {
            drainer.start();
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), encoding))) {
                if (simulateInput && inputText != null) { writer.write(inputText); writer.newLine(); writer.flush(); }
            }
            int exitCode = process.waitFor();
            drainer.join();
            if (readFailure.get() != null) throw readFailure.get();
            if (exitCode != 0) throw new IOException("NexasPack exited with code " + exitCode + System.lineSeparator() + output);
            return output.toString();
        } catch (InterruptedException error) {
            interrupted = true;
            InterruptedIOException failure = new InterruptedIOException("interrupted while waiting for NexasPack to exit");
            failure.initCause(error);
            throw failure;
        } finally {
            interrupted |= stopAndConfirmExit(process);
            closeQuietly(process.getOutputStream());
            closeQuietly(process.getInputStream());
            closeQuietly(process.getErrorStream());
            interrupted |= joinUninterruptibly(drainer);
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    private static boolean stopAndConfirmExit(Process process) {
        boolean interrupted = Thread.interrupted();
        if (process.isAlive()) process.destroy();
        while (process.isAlive()) {
            try {
                if (!process.waitFor(500, TimeUnit.MILLISECONDS)) process.destroyForcibly();
            } catch (InterruptedException error) {
                interrupted = true;
                process.destroyForcibly();
            }
        }
        for (;;) {
            try { process.waitFor(); return interrupted; }
            catch (InterruptedException error) { interrupted = true; if (process.isAlive()) process.destroyForcibly(); }
        }
    }

    private static boolean joinUninterruptibly(Thread thread) {
        boolean interrupted = false;
        for (;;) {
            try { thread.join(); return interrupted; }
            catch (InterruptedException error) { interrupted = true; }
        }
    }

    private static void closeQuietly(Closeable stream) {
        try { stream.close(); }
        catch (IOException ignored) { }
    }

    private static void cleanupOwnedDirectory(Path directory, Throwable failure) throws IOException {
        boolean interrupted = Thread.interrupted();
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        } catch (IOException cleanup) {
            if (failure != null) failure.addSuppressed(cleanup);
            else throw cleanup;
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }
}
