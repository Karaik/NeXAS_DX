package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.exe;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.ExePatchPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NativeErrorHandlerPreservationTest {

    @TempDir
    Path output;

    @Test
    void capacityChainWritesNewExecutablesWithoutDisablingNativeInvalidParameterHandling() throws Exception {
        Path baseline = Path.of("src/main/resources/ida-reverse/BaldrSky.exe");
        byte[] original = Files.readAllBytes(baseline);
        byte[] handler = Arrays.copyOfRange(original, 0x2789F8, 0x2789F8 + 32);
        assertArrayEquals(new byte[]{(byte) 0x8B, (byte) 0xFF}, Arrays.copyOf(handler, 2));

        AkaoGraftRequest akao = new AkaoGraftRequest();
        akao.setExeOutputDir(output.resolve("akao"));
        ExePatchPlan akaoPlan = new ExePatchPlan();
        akaoPlan.setSourceExePath(baseline);
        new com.giga.nexas.transfer.jinki2bsdx.v2.exe.ApplyExePatchStep().apply(
                akao, akaoPlan,
                com.giga.nexas.transfer.jinki2bsdx.v2.exe.ExePatchProfile.defaultAkaoCompatibilityProfile());
        byte[] akaoBytes = Files.readAllBytes(akaoPlan.getOutputExePath());
        assertEquals(104, Byte.toUnsignedInt(akaoBytes[0x1E3F40]));
        assertArrayEquals(handler, Arrays.copyOfRange(akaoBytes, 0x2789F8, 0x2789F8 + 32));

        TsukuyomiGraftRequest tsukuyomi = request("tsukuyomi");
        TsukuyomiExePatchPlan tsukuyomiPlan = plan(akaoPlan.getOutputExePath());
        new ApplyExePatchStep().apply(tsukuyomi, tsukuyomiPlan,
                ExePatchProfile.forCapacities(104, 105, 104, 105));
        byte[] tsukuyomiBytes = Files.readAllBytes(tsukuyomiPlan.getOutputExePath());
        assertEquals(105, Byte.toUnsignedInt(tsukuyomiBytes[0x1E3F40]));
        assertArrayEquals(handler, Arrays.copyOfRange(tsukuyomiBytes, 0x2789F8, 0x2789F8 + 32));

        TsukuyomiExePatchPlan followupPlan = plan(tsukuyomiPlan.getOutputExePath());
        new com.giga.nexas.transfer.bhe2bsdx.meka.followup.exe.ApplyExePatchStep().apply(
                request("followup"), followupPlan,
                com.giga.nexas.transfer.bhe2bsdx.meka.followup.exe.ExePatchProfile.forCapacities(105, 106, 105, 106));
        byte[] followupBytes = Files.readAllBytes(followupPlan.getOutputExePath());
        assertEquals(106, Byte.toUnsignedInt(followupBytes[0x1E3F40]));
        assertArrayEquals(handler, Arrays.copyOfRange(followupBytes, 0x2789F8, 0x2789F8 + 32));
        assertArrayEquals(original, Files.readAllBytes(baseline), "原版EXE不得被原地改写");
    }

    private TsukuyomiGraftRequest request(String name) {
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setExeOutputDir(output.resolve(name));
        return request;
    }

    private static TsukuyomiExePatchPlan plan(Path source) {
        TsukuyomiExePatchPlan plan = new TsukuyomiExePatchPlan();
        plan.setSourceExePath(source);
        return plan;
    }
}
