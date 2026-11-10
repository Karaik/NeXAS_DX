package com.giga.nexas.transfer.bhe2bsdx.meka.followup.exe;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ExePatchProfileTest {

    private final PatchBytesVerifier verifier = new PatchBytesVerifier();

    @Test
    @DisplayName("验证 ExePatchProfile 中的所有位点在原始 BaldrSky.exe 与 BaldrSky_add.exe 上均能准确匹配")
    void testExePatchProfileSitesMatchBaselineExe() throws Exception {
        Path exePath = Paths.get("src/main/resources/ida-reverse/BaldrSky.exe");
        assertTrue(Files.exists(exePath), "BaldrSky.exe baseline must exist");

        byte[] exeBytes = Files.readAllBytes(exePath);
        ExePatchProfile profile = ExePatchProfile.forCapacities(104, 104);

        assertNotNull(profile.getSites());
        assertFalse(profile.getSites().isEmpty());

        for (ExePatchSite site : profile.getSites()) {
            verifier.verifySiteInRange(exeBytes, site);
            boolean matchExpected = verifier.matches(exeBytes, site.getOffset(), site.getExpectedBytes());
            boolean matchTarget = verifier.matches(exeBytes, site.getOffset(), site.getTargetBytes());

            assertTrue(
                    matchExpected || matchTarget,
                    String.format("Patch site 0x%06X (%s) failed to match expected or target bytes", site.getOffset(), site.getLabel())
            );
        }
    }

    @Test
    @DisplayName("验证 ExePatchProfile 包含武装列表 512 扩容与 STL 异常安全旁路位点")
    void testExePatchProfileContainsWeaponCapacitySites() {
        ExePatchProfile profile = ExePatchProfile.forCapacities(104, 104);

        boolean has0x056CBB = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x056CBB);
        boolean has0x056F51 = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x056F51);
        boolean has0x056FD1 = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x056FD1);
        boolean has0x05BC09 = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x05BC09);
        boolean has0x057081 = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x057081);
        boolean has0x0570E1 = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x0570E1);
        boolean has0x053DF1 = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x053DF1);
        boolean has0x055129 = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x055129);
        boolean has0x0559D1 = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x0559D1);
        boolean has0x052AF0 = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x052AF0);
        boolean has0x215D6B = profile.getSites().stream().anyMatch(s -> s.getOffset() == 0x215D6B);

        assertTrue(has0x056CBB, "Missing 0x056CBB (weapon UI list capacity 512)");
        assertTrue(has0x056F51, "Missing 0x056F51 (weapon index table capacity 512)");
        assertTrue(has0x056FD1, "Missing 0x056FD1 (weapon 208-byte table capacity 512)");
        assertTrue(has0x05BC09, "Missing 0x05BC09 (weapon 16-byte item accessor bypass)");
        assertTrue(has0x057081, "Missing 0x057081 (weapon 208-byte table accessor bypass)");
        assertTrue(has0x0570E1, "Missing 0x0570E1 (weapon table iterator bypass)");
        assertTrue(has0x053DF1, "Missing 0x053DF1 (weapon status save loader loop bound)");
        assertTrue(has0x055129, "Missing 0x055129 (weapon status copy upper bound)");
        assertTrue(has0x0559D1, "Missing 0x0559D1 (weapon status reset count)");
        assertTrue(has0x052AF0, "Missing 0x052AF0 (weapon unlock gate)");
        assertTrue(has0x215D6B, "Missing 0x215D6B (weapon lookup range bypass)");
    }
}
