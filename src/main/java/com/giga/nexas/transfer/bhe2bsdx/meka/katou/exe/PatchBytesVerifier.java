package com.giga.nexas.transfer.bhe2bsdx.meka.katou.exe;

public class PatchBytesVerifier {

    public boolean matches(byte[] bytes, int offset, byte[] expected) {
        if (bytes == null || expected == null || offset < 0 || offset + expected.length > bytes.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (bytes[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }

    public void verifySiteInRange(byte[] bytes, ExePatchSite site) {
        if (bytes == null || site == null || site.getTargetBytes() == null) {
            throw new IllegalStateException("exe patch site 为空");
        }
        if (site.getOffset() < 0 || site.getOffset() + site.getTargetBytes().length > bytes.length) {
            throw new IllegalStateException(String.format("exe patch 偏移越界: 0x%06X", site.getOffset()));
        }
    }
}
