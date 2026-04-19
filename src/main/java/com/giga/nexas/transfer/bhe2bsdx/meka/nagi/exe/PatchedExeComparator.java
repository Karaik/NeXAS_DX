package com.giga.nexas.transfer.bhe2bsdx.meka.nagi.exe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PatchedExeComparator {

    public boolean byteIdentical(Path left, Path right) {
        if (left == null || right == null || !Files.exists(left) || !Files.exists(right)) {
            return false;
        }
        try {
            if (Files.size(left) != Files.size(right)) {
                return false;
            }
            return Files.mismatch(left, right) < 0;
        } catch (IOException e) {
            throw new IllegalStateException("比较 patched exe 失败", e);
        }
    }
}
