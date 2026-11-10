package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单 override 的审计载体。
 *
 * <p>这个类不是业务输入，也不参与生成 bytes。它的作用是把 当前 做过的关键决定记录下来：
 * slot 如何推导、DAT/SPM 改了什么、写出了哪些文件、复制了哪些 PNG、哪些图片缺失。</p>
 *
 * <p>最终 byte parity 失败时，优先看这个 audit，再看二进制 diff。
 * 这样定位会从“哪个文件不同”进一步缩小到“哪个菜单阶段产生了不同”。</p>
 */
@Data
public class MenuOverrideAudit {

    /**
     * 本轮菜单覆盖最终采用的槽位映射。
     *
     * <p>它是排查菜单差异时最先看的对象：如果行号或 anim index 错了，
     * 后续 DAT/SPM 即使算法正确也会写到错误位置。</p>
     */
    private MenuSlotMapping slotMapping;

    /**
     * 槽位推导说明。
     *
     * <p>记录从哪一行读出 source meka、anim、state，以及 pilot 行如何反查得到。</p>
     */
    private List<String> slotMappingNotes = new ArrayList<>();

    /**
     * 菜单 DAT patch 说明。
     *
     * <p>用于记录 `Meka.dat`、`MekaPilot.dat`、`SelectMekaMenu.dat` 分别覆写了哪一行或哪个 target id。</p>
     */
    private List<String> datPatchNotes = new ArrayList<>();

    /**
     * 菜单 SPM patch 说明。
     *
     * <p>用于记录两个菜单 SPM 分别重建了哪个 anim，便于 byte diff 时快速缩小范围。</p>
     */
    private List<String> spmPatchNotes = new ArrayList<>();

    /**
     * 菜单 pipeline 实际写出的 DAT/SPM 文件路径。
     *
     * <p>这里记录的是最终输出目录里的文件，不是临时对象名。</p>
     */
    private List<Path> writtenFiles = new ArrayList<>();

    /**
     * patched 菜单 SPM 实际引用并成功复制的 PNG。
     *
     * <p>最终输出集合不能多图或少图；这组路径可以和旧 pipeline 输出图集做集合对比。</p>
     */
    private List<Path> copiedImages = new ArrayList<>();

    /**
     * patched 菜单 SPM 引用但外部资源目录中找不到的 PNG 文件名。
     *
     * <p>正常 parity 场景下这里应为空；非空时通常说明 spec 文件名或资源目录配置错了。</p>
     */
    private List<String> missingImages = new ArrayList<>();

    public void addSlotMappingNote(String note) {
        addNote(slotMappingNotes, note);
    }

    public void addDatPatchNote(String note) {
        addNote(datPatchNotes, note);
    }

    public void addSpmPatchNote(String note) {
        addNote(spmPatchNotes, note);
    }

    public void addWrittenFile(Path path) {
        addPath(writtenFiles, path);
    }

    public void addCopiedImage(Path path) {
        addPath(copiedImages, path);
    }

    public void addMissingImage(String imageName) {
        addNote(missingImages, imageName);
    }

    private void addNote(List<String> notes, String note) {
        if (note == null || note.isBlank()) {
            return;
        }
        notes.add(note);
    }

    private void addPath(List<Path> paths, Path path) {
        if (path == null) {
            return;
        }
        paths.add(path);
    }
}
