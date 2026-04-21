package com.giga.nexas.bsdx;

import com.giga.nexas.dto.bsdx.bin.GLOBAL;
import com.giga.nexas.dto.bsdx.bin.parser.GLOBALParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * `__GLOBAL.bin` 真实结构的回归测试。
 *
 * <p>这类测试的意义不是做业务功能验证，而是防止后续有人再次把
 * `__GLOBAL.bin` 误解成“四张字符串表”之类的假模型。
 */
class GLOBALParserTest {

    private static final String CHARSET = "windows-31j";
    private static final Path GLOBAL_BIN = Paths.get("src/main/resources/game/bsdx/bin/__GLOBAL.bin");

    private final GLOBALParser parser = new GLOBALParser();

    /**
     * 验证当前真实 BSDX `__GLOBAL.bin` 的结构与已调查结果一致。
     */
    @Test
    void parseRealGlobalBinStructure() throws Exception {
        if (!Files.exists(GLOBAL_BIN)) {
            return;
        }

        byte[] data = Files.readAllBytes(GLOBAL_BIN);
        GLOBAL global = parser.parse(data, "__global", CHARSET);

        Assertions.assertEquals(442, global.getSymbolTable().size());
        Assertions.assertEquals(0, global.getReservedTable2().size());
        Assertions.assertEquals(0, global.getReservedTable3().size());
        Assertions.assertEquals(1720, global.getInitInstructions().size());
        Assertions.assertEquals("LUCK_TABLE", global.getSymbolTable().get(0));
        Assertions.assertTrue(global.getSymbolTable().contains("HP_NANOHA"));
        Assertions.assertTrue(global.getSymbolTable().contains("g_nqAttackCnt"));

        Set<Integer> opcodes = global.getInitInstructions().stream()
                .map(GLOBAL.InitInstruction::getOpcode)
                .collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(0, 8, 14), opcodes);
    }
}
