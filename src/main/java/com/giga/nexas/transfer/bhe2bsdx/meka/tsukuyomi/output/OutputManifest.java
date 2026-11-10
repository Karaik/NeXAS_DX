package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.output;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class OutputManifest {

    /**
     * manifest 对应的输出根目录。
     *
     * <p>记录绝对规范路径，便于测试失败时回到真实文件位置排查。</p>
     */
    private String outputRoot;

    /**
     * 输出目录内每个受审计资源的记录。
     *
     * <p>最终比较使用相对路径集合和 bytes；entries 用于解释“这个文件为什么在输出里”。</p>
     */
    private List<OutputResourceEntry> entries = new ArrayList<>();

    /**
     * 输出阶段的辅助说明。
     *
     * <p>记录缺失 sidecar、外部输入存在/不存在等不属于单个文件 entry 的信息。</p>
     */
    private List<String> notes = new ArrayList<>();

    public void addEntry(OutputResourceEntry entry) {
        if (entry == null) {
            return;
        }
        entries.add(entry);
    }

    public void addNote(String note) {
        if (note == null || note.isBlank()) {
            return;
        }
        notes.add(note);
    }
}
