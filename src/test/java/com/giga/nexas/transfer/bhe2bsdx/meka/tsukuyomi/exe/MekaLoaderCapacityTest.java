package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.exe;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MekaLoaderCapacityTest {

    private static final Path ORIGINAL_EXE = Path.of("src/main/resources/bsdx_game/BaldrSky.exe");

    @Test
    void retainedV2ThenBheChainExpandsTheActualLoaderLayoutTo114() throws Exception {
        Chain chain = chainTo114();
        byte[] legacy = chain.retainedV2();
        assertEquals(104, Byte.toUnsignedInt(legacy[0x1E3F40]));
        assertEquals(104, Byte.toUnsignedInt(legacy[0x063A85]));
        assertEquals(964, integer(legacy, 0x1E5840), "V2的既有loader布局必须保持103，不能改V2基线");
        assertEquals(544, integer(legacy, 0x0648A6));
        assertEquals(103, integer(legacy, 0x0648AB));
        byte[] exe = chain.finalExe();

        // 来自独立native布局审计的最终字面值，不从生产公式或site期望生成oracle。
        assertAll("114-entry native loader",
                () -> assertEquals(1052, integer(exe, 0x1E5840), "实际分配必须容纳114格及尾字段"),
                () -> assertEquals(588, integer(exe, 0x0648A6), "构造器state数组起点"),
                () -> assertEquals(1044, integer(exe, 0x0648A0), "构造器accounting尾字段"),
                () -> assertEquals(1048, integer(exe, 0x06489A), "构造器active尾字段"),
                () -> assertEquals(114, integer(exe, 0x0648AB), "真实清零循环计数"),
                () -> assertEquals(-456, integer(exe, 0x0648B2), "真实反向refcount位移"),
                () -> assertEquals(114, Byte.toUnsignedInt(exe[0x063A85]), "下游scan必须使用同一容量"));
        assertArrayEquals(chain.original(), Files.readAllBytes(ORIGINAL_EXE));
    }

    @Test
    void decodedConstructorLoopsClearEveryBheStageWithoutTouchingPrefixOrCanary() throws Exception {
        Chain chain = chainTo114();
        assertConstructorClears(chain.retainedV2(), new Layout(103, 544, 956, 960, 964));
        // 独立审计的各层字面布局；并不调用生产helper计算地址。
        Layout[] layouts = {
                new Layout(105, 552, 972, 976, 980), new Layout(106, 556, 980, 984, 988),
                new Layout(107, 560, 988, 992, 996), new Layout(108, 564, 996, 1000, 1004),
                new Layout(109, 568, 1004, 1008, 1012), new Layout(110, 572, 1012, 1016, 1020),
                new Layout(111, 576, 1020, 1024, 1028), new Layout(112, 580, 1028, 1032, 1036),
                new Layout(113, 584, 1036, 1040, 1044), new Layout(114, 588, 1044, 1048, 1052)
        };
        List<Executable> checks = new ArrayList<>();
        for (int index = 0; index < layouts.length; index++) {
            Layout expected = layouts[index];
            byte[] exe = chain.bheStages().get(index);
            checks.add(() -> assertConstructorClears(exe, expected));
        }
        assertAll("真实构造器清零指令的逐层执行", checks);
    }

    @Test
    void mekaIds102103104And113DoNotAliasLowStatesOrTailFields() throws Exception {
        byte[] exe = chainTo114().finalExe();
        assertAll("native ref/state indexing",
                () -> assertIndependentCells(exe, 102),
                () -> assertIndependentCells(exe, 103),
                () -> assertIndependentCells(exe, 104),
                () -> assertIndependentCells(exe, 113));
    }

    @Test
    void allNativeStateAccountingAndActiveConsumersUseTheSame114Layout() throws Exception {
        byte[] exe = chainTo114().finalExe();
        List<Executable> checks = new ArrayList<>();
        // file operand offsets + opcode/context，独立列出全部native消费者。
        for (Operand operand : List.of(
                op(0x0638D7, "83bc81", "01"), op(0x063A44, "8db7", ""),
                op(0x063AFF, "c7848e", "00000000"), op(0x063BFA, "c7848e", "02000000"),
                op(0x063CFB, "c78486", "03000000"), op(0x063F28, "c784bb", "04000000"),
                op(0x063FAD, "89acbb", ""), op(0x06401F, "c784bb", "02000000"),
                op(0x0640AF, "c784bb", "03000000"), op(0x064188, "c78482", "00000000"),
                op(0x0642AF, "c78481", "02000000"), op(0x0643B5, "c7848d", "03000000"),
                op(0x064487, "83bc81", "02"), op(0x06453A, "c78486", "01000000"),
                op(0x06458F, "c78486", "00000000"), op(0x0648A6, "8d86", ""))) {
            checks.add(() -> assertOperand(exe, operand, 588));
        }
        for (Operand operand : List.of(
                op(0x0638FE, "81b9", "00004006"), op(0x06396C, "81be", "00004006"),
                op(0x063A02, "0196", ""), op(0x063A0F, "8b86", ""),
                op(0x063CB8, "0196", ""), op(0x063CC5, "8b86", ""),
                op(0x064371, "0195", ""), op(0x06437E, "8b85", ""),
                op(0x06468D, "298e", ""), op(0x06469A, "8b86", ""),
                op(0x0648A0, "899e", ""))) {
            checks.add(() -> assertOperand(exe, operand, 1044));
        }
        for (Operand operand : List.of(
                op(0x0639A5, "80be", "00"), op(0x0639D3, "c686", "01"),
                op(0x063AA5, "80be", "00"), op(0x063AB6, "c686", "00"),
                op(0x0640DE, "c683", "00"), op(0x06489A, "889e", ""))) {
            checks.add(() -> assertOperand(exe, operand, 1048));
        }
        checks.add(() -> assertOperand(exe, op(0x063A38, "b9", ""), -588));
        checks.add(() -> assertOperand(exe, op(0x0648AB, "b9", "90"), 114));
        checks.add(() -> assertOperand(exe, op(0x0648B2, "8998", "8918"), -456));
        checks.add(() -> assertOperand(exe, op(0x1E5840, "68", ""), 1052));
        checks.add(() -> {
            requireBytes(exe, 0x063A83, "83fb");
            assertEquals(114, Byte.toUnsignedInt(exe[0x063A85]));
        });
        assertEquals(38, checks.size(), "全部布局operand，不重复新增scan owner");
        assertAll("所有队列、扫描、accounting、cleanup位点", checks);
        assertReferenceOperands(exe);
    }

    @Test
    void nativeCrtAndTheOriginalExecutableRemainUnchangedThroughoutTheChain() throws Exception {
        Chain chain = chainTo114();
        byte[] originalHandler = Arrays.copyOfRange(chain.original(), 0x2789F8, 0x278A18);
        assertArrayEquals(new byte[]{(byte) 0x8B, (byte) 0xFF}, Arrays.copyOf(originalHandler, 2));
        assertArrayEquals(originalHandler, Arrays.copyOfRange(chain.retainedV2(), 0x2789F8, 0x278A18));
        for (byte[] stage : chain.bheStages()) {
            assertArrayEquals(originalHandler, Arrays.copyOfRange(stage, 0x2789F8, 0x278A18));
        }
        assertArrayEquals(chain.original(), Files.readAllBytes(ORIGINAL_EXE));
    }

    private static void assertConstructorClears(byte[] exe, Layout expected) {
        Arena arena = executeConstructorClearLoop(exe);
        assertEquals(expected.allocation(), arena.allocation, "实际malloc大小");
        for (int id = 0; id < expected.count(); id++) {
            assertEquals(0, arena.word(132 + 4 * id), "refcount[" + id + "] 未被真实指令清零");
            assertEquals(0, arena.word(expected.state() + 4 * id), "state[" + id + "] 未被真实指令清零");
        }
        assertEquals(0, arena.word(expected.accounted()), "accounting尾字段必须清零");
        assertEquals(0, Byte.toUnsignedInt(arena.bytes[expected.active()]), "active尾字段必须清零");
        for (int offset = 0; offset < 132; offset++) assertEquals((byte) 0x5A, arena.bytes[offset], "循环不能碰前缀");
        for (int offset = expected.active() + 1; offset < expected.allocation(); offset++) {
            assertEquals((byte) 0x5A, arena.bytes[offset], "byte active写入不能覆盖对齐padding");
        }
        arena.assertCanary();
    }

    private static Arena executeConstructorClearLoop(byte[] exe) {
        // 只执行已审计的清零指令块。EBX来自原构造器的xor ebx,ebx。
        requireBytes(exe, 0x06484B, "33db");
        requireBytes(exe, 0x1E583F, "68");
        requireBytes(exe, 0x064898, "889e");
        requireBytes(exe, 0x06489E, "899e");
        requireBytes(exe, 0x0648A4, "8d86");
        requireBytes(exe, 0x0648AA, "b9");
        requireBytes(exe, 0x0648AF, "908998");
        requireBytes(exe, 0x0648B6, "891883c0");
        requireBytes(exe, 0x0648BB, "83e9");
        requireBytes(exe, 0x0648BE, "75");
        assertEquals(0x0648B0, 0x0648C0 + exe[0x0648BF], "JNZ必须回到实际双数组store");
        Arena arena = new Arena(integer(exe, 0x1E5840));
        arena.byteStore(integer(exe, 0x06489A), 0);
        arena.wordStore(integer(exe, 0x0648A0), 0);
        int pointer = integer(exe, 0x0648A6);
        int remaining = integer(exe, 0x0648AB);
        int backDisplacement = integer(exe, 0x0648B2);
        int stride = Byte.toUnsignedInt(exe[0x0648BA]);
        int decrement = Byte.toUnsignedInt(exe[0x0648BD]);
        int iterations = 0;
        do {
            assertTrue(++iterations <= 2048, "native loop不能失控");
            arena.wordStore(pointer + backDisplacement, 0);
            arena.wordStore(pointer, 0);
            pointer += stride;
            remaining -= decrement;
        } while (remaining != 0);
        return arena;
    }

    private static void assertIndependentCells(byte[] exe, int id) {
        Arena arena = executeConstructorClearLoop(exe);
        assertReferenceOperands(exe);
        requireBytes(exe, 0x064537, "c78486");
        int referenceBase = integer(exe, 0x064546); // inc [esi+eax*4+disp32]
        int stateBase = integer(exe, 0x06453A); // mov [esi+eax*4+disp32],1
        int refScale = 1 << (Byte.toUnsignedInt(exe[0x064545]) >>> 6);
        int stateScale = 1 << (Byte.toUnsignedInt(exe[0x064539]) >>> 6);
        assertEquals(4, refScale);
        assertEquals(4, stateScale);
        for (int lowId : new int[]{0, 1, 10}) arena.wordStore(stateBase + stateScale * lowId, 7000 + lowId);
        int accounted = integer(exe, 0x0648A0), active = integer(exe, 0x06489A);
        arena.wordStore(accounted, 0x12345678);
        arena.byteStore(active, 0x6D);
        arena.wordStore(referenceBase + refScale * id, 1000 + id);
        arena.wordStore(stateBase + stateScale * id, 2000 + id);
        assertEquals(1000 + id, arena.word(referenceBase + refScale * id));
        assertEquals(2000 + id, arena.word(stateBase + stateScale * id));
        for (int lowId : new int[]{0, 1, 10}) {
            assertEquals(7000 + lowId, arena.word(stateBase + stateScale * lowId), "高ID ref不得覆盖低ID state");
        }
        assertEquals(0x12345678, arena.word(accounted), "高ID state不得覆盖accounting尾字段");
        assertEquals(0x6D, Byte.toUnsignedInt(arena.bytes[active]), "高ID state不得覆盖active尾字段");
        arena.assertCanary();
    }

    private static void assertReferenceOperands(byte[] exe) {
        assertOperand(exe, op(0x0644DF, "83bc86", "00"), 132);
        assertOperand(exe, op(0x064546, "ff8486", ""), 132);
        assertOperand(exe, op(0x064581, "838486", "ff"), 132);
    }

    private static Operand op(int offset, String prefix, String suffix) {
        return new Operand(offset, prefix, suffix);
    }

    private static void assertOperand(byte[] exe, Operand operand, int expected) {
        int prefixLength = operand.prefix().length() / 2;
        requireBytes(exe, operand.offset() - prefixLength, operand.prefix());
        assertEquals(expected, integer(exe, operand.offset()), "native operand 0x" + Integer.toHexString(operand.offset()));
        requireBytes(exe, operand.offset() + 4, operand.suffix());
    }

    private static void requireBytes(byte[] exe, int offset, String hex) {
        byte[] expected = HexFormat.of().parseHex(hex);
        assertArrayEquals(expected, Arrays.copyOfRange(exe, offset, offset + expected.length),
                "native opcode/context 0x" + Integer.toHexString(offset));
    }

    private static Chain chainTo114() throws Exception {
        byte[] original = Files.readAllBytes(ORIGINAL_EXE);
        byte[] legacy = apply(original,
                com.giga.nexas.transfer.jinki2bsdx.v2.exe.ExePatchProfile.defaultAkaoCompatibilityProfile()
                        .getSites().stream().map(site -> new Patch(site.getOffset(), site.getExpectedBytes(), site.getTargetBytes())).toList());
        byte[] target = apply(legacy, ExePatchProfile.forCapacities(104, 105, 104, 105)
                .getSites().stream().map(site -> new Patch(site.getOffset(), site.getExpectedBytes(), site.getTargetBytes())).toList());
        List<byte[]> stages = new ArrayList<>();
        stages.add(target);
        for (int capacity = 106; capacity <= 114; capacity++) {
            target = apply(target,
                    com.giga.nexas.transfer.bhe2bsdx.meka.followup.exe.ExePatchProfile
                            .forCapacities(capacity - 1, capacity, capacity - 1, capacity)
                            .getSites().stream().map(site -> new Patch(site.getOffset(), site.getExpectedBytes(), site.getTargetBytes())).toList());
            stages.add(target);
        }
        return new Chain(original, legacy, target, List.copyOf(stages));
    }

    private static byte[] apply(byte[] source, List<Patch> sites) {
        byte[] target = source.clone();
        HashSet<Integer> offsets = new HashSet<>();
        for (Patch site : sites) {
            assertTrue(offsets.add(site.offset()), "同一profile不能重复拥有operand: " + Integer.toHexString(site.offset()));
            assertTrue(site.offset() >= 0 && site.offset() + site.target().length <= source.length);
            byte[] actual = Arrays.copyOfRange(source, site.offset(), site.offset() + site.target().length);
            assertTrue(Arrays.equals(actual, site.expected()) || Arrays.equals(actual, site.target()),
                    "真实EXE不匹配expected/target at " + Integer.toHexString(site.offset()));
        }
        for (Patch site : sites) System.arraycopy(site.target(), 0, target, site.offset(), site.target().length);
        return target;
    }

    private static int integer(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private record Patch(int offset, byte[] expected, byte[] target) { }
    private record Chain(byte[] original, byte[] retainedV2, byte[] finalExe, List<byte[]> bheStages) { }
    private record Layout(int count, int state, int accounted, int active, int allocation) { }
    private record Operand(int offset, String prefix, String suffix) { }

    private static final class Arena {
        private final int allocation;
        private final byte[] bytes;

        private Arena(int allocation) {
            assertTrue(allocation > 0 && allocation < 16384, "实际allocation必须是小型loader对象");
            this.allocation = allocation;
            bytes = new byte[allocation + 128];
            Arrays.fill(bytes, 0, allocation, (byte) 0x5A);
            Arrays.fill(bytes, allocation, bytes.length, (byte) 0xA7);
        }

        private void byteStore(int offset, int value) {
            assertTrue(offset >= 0 && offset < allocation, "native byte store超出malloc: " + offset);
            bytes[offset] = (byte) value;
        }

        private void wordStore(int offset, int value) {
            assertTrue(offset >= 0 && offset + 4 <= allocation, "native DWORD store超出malloc: " + offset);
            ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value);
        }

        private int word(int offset) {
            return integer(bytes, offset);
        }

        private void assertCanary() {
            for (int offset = allocation; offset < bytes.length; offset++) {
                assertEquals((byte) 0xA7, bytes[offset], "越过malloc的canary不得被写入");
            }
        }
    }
}
