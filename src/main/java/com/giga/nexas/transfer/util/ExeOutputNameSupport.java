package com.giga.nexas.transfer.util;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ExeOutputNameSupport {

    private static final String TIMESTAMP_SUFFIX_REGEX = "_\\d{8}_\\d{6}_\\d{3}$";

    private ExeOutputNameSupport() {
    }

    public static String buildTimestampedExeName(Path sourceExe, DateTimeFormatter formatter) {
        String fileName = sourceExe.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot >= 0 ? fileName.substring(dot) : ".exe";
        return stripTrailingTimestamps(baseName) + "_" + LocalDateTime.now().format(formatter) + ext;
    }

    private static String stripTrailingTimestamps(String baseName) {
        String stripped = baseName;
        while (stripped.matches(".*" + TIMESTAMP_SUFFIX_REGEX)) {
            stripped = stripped.replaceFirst(TIMESTAMP_SUFFIX_REGEX, "");
        }
        return stripped;
    }
}
