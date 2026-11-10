package com.giga.nexas.transfer.jinki2bsdx.v2.output;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class ResourceOverwriteAudit {

    /**
     * 相对路径到首次来源说明的映射。
     *
     * <p>输出目录允许后置覆盖同名文件，但必须知道这个路径最早是谁写入的，
     * 才能在覆盖发生时记录“从谁覆盖到谁”。</p>
     */
    private Map<String, String> firstSourceByRelativePath = new LinkedHashMap<>();

    /**
     * 同名资源覆盖说明。
     *
     * <p>用于区分“预期覆盖”（例如菜单 DAT/SPM 后置覆写）和意外多次写入。</p>
     */
    private List<String> overwriteNotes = new ArrayList<>();

    public boolean recordOutput(String relativePath, String sourceDescription) {
        if (relativePath == null || relativePath.isBlank()) {
            return false;
        }
        String source = sourceDescription == null || sourceDescription.isBlank()
                ? "unknown"
                : sourceDescription;
        String existing = firstSourceByRelativePath.putIfAbsent(relativePath, source);
        if (existing == null) {
            return false;
        }
        overwriteNotes.add(relativePath + " overwritten: " + existing + " -> " + source);
        firstSourceByRelativePath.put(relativePath, source);
        return true;
    }
}
