package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.exe;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ExePatchAudit {

    /**
     * 本次执行中实际从 expectedBytes 写成 targetBytes 的 patch site。
     *
     * <p>如果旧 baseline exe 还没打过这些补丁，这里会记录对应位点。</p>
     */
    private List<String> appliedSites = new ArrayList<>();

    /**
     * 输入 exe 已经处于 targetBytes 状态的 patch site。
     *
     * <p>这是链式成果物场景的正常结果：后一层从 BSDX+TSUKUYOMI 继续 patch 时，
     * 不能把已 patch 字节误判为失败。</p>
     */
    private List<String> alreadyPatchedSites = new ArrayList<>();

    /**
     * EXE patch 过程的辅助说明。
     *
     * <p>用于记录跳过菜单行扩容、输出路径等不属于单个 site 的信息。</p>
     */
    private List<String> notes = new ArrayList<>();

    public void addAppliedSite(String note) {
        add(appliedSites, note);
    }

    public void addAlreadyPatchedSite(String note) {
        add(alreadyPatchedSites, note);
    }

    public void addNote(String note) {
        add(notes, note);
    }

    private void add(List<String> target, String note) {
        if (note == null || note.isBlank()) {
            return;
        }
        target.add(note);
    }
}
