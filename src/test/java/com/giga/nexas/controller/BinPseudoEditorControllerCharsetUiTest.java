package com.giga.nexas.controller;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.generator.BinGenerator;
import com.giga.nexas.service.BsdxBinService;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BIN 伪代码编辑器的日文编码 UI 回归测试。
 *
 * <p>这组测试直接覆盖脚本编辑器界面的完整链路：
 * 1. 加载 `.bin`
 * 2. 切到 pseudo 模式
 * 3. 修改含字符串的伪代码
 * 4. 点击 `Compile Back`
 * 5. 校验回写后的 `.bin` 字符串表与 raw bytes 使用 `windows-31j`
 */
class BinPseudoEditorControllerCharsetUiTest {

    private static final String CHARSET = "windows-31j";

    /**
     * 测试用二进制解析服务。
     */
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    /**
     * 测试用二进制生成器。
     */
    private final BinGenerator binGenerator = new BinGenerator();

    /**
     * 初始化 JavaFX runtime。
     */
    @BeforeAll
    static void bootstrapJavaFx() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
            // toolkit already started
        }
    }

    /**
     * 验证脚本编辑器 UI 回编日文字面量后，`.bin` 字符串表和 raw bytes 都保持 `windows-31j`。
     */
    @Test
    void compileBackFromPseudoEditorUiWritesJapaneseStringsUsingWindows31j() throws Exception {
        Path tempDir = Files.createTempDirectory("bin-pseudo-editor-charset-ui");
        try {
            Path binPath = tempDir.resolve("sample.bin");
            writeSampleBin(binPath);

            AtomicReference<BinPseudoEditorController.EmbeddedEditorHandle> handleRef = new AtomicReference<>();
            onFxAndWait(() -> handleRef.set(BinPseudoEditorController.createEmbeddedEditor(binPath, CHARSET, text -> { })));

            BinPseudoEditorController controller = handleRef.get().controller();
            RadioButton pseudoModeButton = readField(controller, "pseudoModeButton", RadioButton.class);
            TextArea pseudoArea = readField(controller, "pseudoArea", TextArea.class);
            Button compileButton = readField(controller, "compileButton", Button.class);
            javafx.scene.control.Label fileLabel = readField(controller, "fileLabel", javafx.scene.control.Label.class);
            javafx.scene.control.Label statusLabel = readField(controller, "statusLabel", javafx.scene.control.Label.class);

            String japanese = "テスト文字列";
            onFxAndWait(() -> {
                pseudoModeButton.fire();
                pseudoArea.setText(pseudoArea.getText().replace("\"stage_start\"", "\"" + japanese + "\""));
                Assertions.assertTrue(fileLabel.getText().contains("*"));
                Assertions.assertTrue(statusLabel.getText().contains("Unsaved changes"));
                compileButton.fire();
            });

            ResponseDTO<?> response = bsdxBinService.parse(binPath.toString(), CHARSET);
            Bin rebuilt = (Bin) response.getData();
            byte[] bytes = Files.readAllBytes(binPath);

            Assertions.assertTrue(rebuilt.getStringTable().contains(japanese));
            Assertions.assertTrue(containsBytes(bytes, japanese.getBytes(Charset.forName(CHARSET))));
            Assertions.assertFalse(containsBytes(bytes, japanese.getBytes(StandardCharsets.UTF_8)));
            Assertions.assertFalse(Files.exists(tempDir.resolve("sample.bin.bak")));
        } finally {
            deleteRecursively(tempDir);
        }
    }

    /**
     * 生成一个最小但可走旧 pseudo renderer/recognizer 主链的测试样本。
     */
    private void writeSampleBin(Path binPath) throws Exception {
        Bin bin = new Bin();
        bin.setExtensionName("bin");
        bin.setCharset(CHARSET);
        bin.setPreCount(0);
        bin.setPreInstructions(new byte[0]);
        bin.setProperties(List.of("flag"));
        bin.setStringTable(List.of("stage_start"));
        bin.setConstants(Map.of(0, new Integer[]{0}));

        List<Bin.Instruction> instructions = new ArrayList<>();
        instructions.add(inst(0x1B, 1));
        instructions.add(inst(0, 0));
        instructions.add(inst(5, 1));
        instructions.add(inst(0, 13));
        instructions.add(inst(5, 0));
        instructions.add(inst(7, (2 << 16) | 375));
        instructions.add(inst(0, 0));
        instructions.add(inst(8, 0));
        instructions.add(inst(0x41, 9));
        instructions.add(inst(1, 0x20));
        instructions.add(inst(0, 1));
        instructions.add(inst(0x2F, 0));
        bin.setInstructions(instructions);

        binGenerator.generate(binPath.toString(), bin, CHARSET);
    }

    /**
     * 构造一条测试指令。
     */
    private Bin.Instruction inst(int opcodeNum, int operandNum) {
        Bin.Instruction inst = new Bin.Instruction();
        inst.setOpcodeNum(opcodeNum);
        inst.setOperandNum(operandNum);
        return inst;
    }

    /**
     * 在 JavaFX 线程执行并等待完成。
     */
    private void onFxAndWait(FxRunnable runnable) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                runnable.run();
            } catch (Throwable throwable) {
                error.set(throwable);
            } finally {
                latch.countDown();
            }
        });
        Assertions.assertTrue(latch.await(20, TimeUnit.SECONDS), "Timed out while waiting for JavaFX task");
        if (error.get() != null) {
            throw new AssertionError(error.get());
        }
    }

    /**
     * 通过反射读取控制器私有字段。
     */
    private <T> T readField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    /**
     * 判断原始字节数组里是否出现了指定子序列。
     */
    private boolean containsBytes(byte[] source, byte[] target) {
        if (source == null || target == null || target.length == 0 || target.length > source.length) {
            return false;
        }
        for (int start = 0; start <= source.length - target.length; start++) {
            boolean matched = true;
            for (int offset = 0; offset < target.length; offset++) {
                if (source[start + offset] != target[offset]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    /**
     * 清理临时目录。
     */
    private void deleteRecursively(Path root) throws Exception {
        Files.walk(root)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> path.toFile().delete());
    }

    @FunctionalInterface
    private interface FxRunnable {
        void run() throws Exception;
    }
}
