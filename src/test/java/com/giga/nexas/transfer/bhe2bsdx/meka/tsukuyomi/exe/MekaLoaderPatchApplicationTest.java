package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.exe;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.ExePatchPlan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MekaLoaderPatchApplicationTest {

    private static final Path ORIGINAL_EXE = Path.of("src/main/resources/bsdx_game/BaldrSky.exe");
    private static final int CONSTRUCTOR_STATE_INSTRUCTION = 0x0648A4;
    private static final int CONSTRUCTOR_STATE_OPERAND = 0x0648A6;

    @TempDir
    Path temporary;

    private final Map<Path, byte[]> inputHashes = new LinkedHashMap<>();

    @BeforeEach
    void rememberOriginalExecutable() throws Exception {
        rememberInput(ORIGINAL_EXE);
    }

    @AfterEach
    void everyInputRemainsByteIdentical() {
        assertAll("生产Apply不得原地修改原版或任何临时输入", inputHashes.entrySet().stream()
                .map(entry -> (Executable) () -> assertArrayEquals(entry.getValue(), sha256(entry.getKey()),
                        "输入SHA-256发生变化: " + entry.getKey())));
    }

    @Test
    void productionApplyCanRepeatTheSameTsukuyomiProfileWithoutChangingBytes() throws Exception {
        Path legacy = retainedV2Input();
        ExePatchProfile profile = ExePatchProfile.forCapacities(104, 105, 104, 105);
        ApplyExePatchStep production = new ApplyExePatchStep();
        TsukuyomiExePatchPlan firstPlan = plan(legacy);

        production.apply(request("first-output"), firstPlan, profile);
        Path firstOutput = requireOutput(firstPlan);
        byte[] firstBytes = Files.readAllBytes(firstOutput);
        assertEquals(105, Byte.toUnsignedInt(firstBytes[0x1E3F40]));
        assertEquals(552, integer(firstBytes, CONSTRUCTOR_STATE_OPERAND));
        assertEquals(980, integer(firstBytes, 0x1E5840));

        rememberInput(firstOutput);
        TsukuyomiExePatchPlan repeatedPlan = plan(firstOutput);
        ExePatchAudit repeatedAudit = production.apply(request("repeated-output"), repeatedPlan, profile);
        Path repeatedOutput = requireOutput(repeatedPlan);

        assertNotEquals(firstOutput, repeatedOutput, "重复Apply须输出到另一临时目录");
        assertArrayEquals(firstBytes, Files.readAllBytes(repeatedOutput), "重复应用完整profile必须逐字节幂等");
        assertTrue(repeatedAudit.getAppliedSites().isEmpty(), "目标指令不得被当作源指令重新迁移");
        assertEquals(profile.getSites().size(), repeatedAudit.getAlreadyPatchedSites().size());
    }

    @Test
    void productionApplyRejectsChangedLoaderOpcodeWithoutPublishingAnExecutable() throws Exception {
        byte[] corrupted = Files.readAllBytes(retainedV2Input());
        assertEquals(0x8D, Byte.toUnsignedInt(corrupted[CONSTRUCTOR_STATE_INSTRUCTION]));
        // 只破坏临时副本中的LEA操作码，位移仍是合法的旧布局544。
        corrupted[CONSTRUCTOR_STATE_INSTRUCTION] = (byte) 0x8B;
        Path input = temporary.resolve("changed-opcode.exe");
        Files.write(input, corrupted);
        rememberInput(input);

        assertProductionRejectsWithoutOutput(input, "opcode-rejected-output");
    }

    @Test
    void productionApplyRejectsUnknownIntermediateLoaderLayoutWithoutPublishingAnExecutable() throws Exception {
        byte[] corrupted = Files.readAllBytes(retainedV2Input());
        assertEquals(544, integer(corrupted, CONSTRUCTOR_STATE_OPERAND));
        // V2保留103项布局，目标是105项；104项的548既不是source也不是target。
        ByteBuffer.wrap(corrupted, CONSTRUCTOR_STATE_OPERAND, Integer.BYTES)
                .order(ByteOrder.LITTLE_ENDIAN).putInt(548);
        Path input = temporary.resolve("unknown-layout.exe");
        Files.write(input, corrupted);
        rememberInput(input);

        assertProductionRejectsWithoutOutput(input, "layout-rejected-output");
    }

    private Path retainedV2Input() throws Exception {
        AkaoGraftRequest request = new AkaoGraftRequest();
        request.setPlanExeCapacityPatch(true);
        request.setExeOutputDir(temporary.resolve("retained-v2"));
        ExePatchPlan plan = new ExePatchPlan();
        plan.setSourceExePath(ORIGINAL_EXE);
        new com.giga.nexas.transfer.jinki2bsdx.v2.exe.ApplyExePatchStep().apply(request, plan,
                com.giga.nexas.transfer.jinki2bsdx.v2.exe.ExePatchProfile.defaultAkaoCompatibilityProfile());

        Path legacy = plan.getOutputExePath();
        assertNotNull(legacy);
        assertTrue(legacy.toAbsolutePath().normalize().startsWith(temporary.toAbsolutePath().normalize()));
        byte[] bytes = Files.readAllBytes(legacy);
        assertEquals(104, Byte.toUnsignedInt(bytes[0x1E3F40]));
        assertEquals(104, Byte.toUnsignedInt(bytes[0x063A85]));
        assertEquals(103, integer(bytes, 0x0648AB));
        assertEquals(964, integer(bytes, 0x1E5840));
        rememberInput(legacy);
        return legacy;
    }

    private void assertProductionRejectsWithoutOutput(Path input, String outputName) throws Exception {
        TsukuyomiGraftRequest request = request(outputName);
        TsukuyomiExePatchPlan plan = plan(input);
        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new ApplyExePatchStep().apply(request, plan,
                        ExePatchProfile.forCapacities(104, 105, 104, 105)));

        assertTrue(failure.getMessage().contains("CMekaLoader"), "必须因loader指令校验拒绝，而不是无关IO错误");
        assertNull(plan.getOutputExePath(), "拒绝的计划不得发布最终EXE路径");
        assertFalse(plan.isPatched());
        if (Files.exists(request.getExeOutputDir())) {
            try (var paths = Files.walk(request.getExeOutputDir())) {
                assertTrue(paths.noneMatch(Files::isRegularFile), "拒绝时不能遗留最终或部分输出文件");
            }
        }
    }

    private TsukuyomiGraftRequest request(String outputName) {
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setPlanExeCapacityPatch(true);
        request.setExeOutputDir(temporary.resolve(outputName));
        return request;
    }

    private static TsukuyomiExePatchPlan plan(Path input) {
        TsukuyomiExePatchPlan plan = new TsukuyomiExePatchPlan();
        plan.setSourceExePath(input);
        plan.setRequiredMekaCapacity(105);
        plan.setRequiredWeaponEquipRows(105);
        return plan;
    }

    private Path requireOutput(TsukuyomiExePatchPlan plan) {
        Path output = plan.getOutputExePath();
        assertNotNull(output);
        assertTrue(Files.isRegularFile(output));
        assertTrue(output.toAbsolutePath().normalize().startsWith(temporary.toAbsolutePath().normalize()));
        assertTrue(plan.isPatched());
        return output;
    }

    private void rememberInput(Path input) throws Exception {
        inputHashes.put(input, sha256(input));
    }

    private static byte[] sha256(Path file) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));
    }

    private static int integer(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
}
