package com.giga.nexas.dto.bsdx.spm.generator;

import cn.hutool.core.io.FileUtil;
import com.giga.nexas.dto.bsdx.BsdxGenerator;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.io.BinaryWriter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/13
 * @Description SpmGenerator
 */
public class SpmGenerator implements BsdxGenerator<Spm> {
    private static final String SPM_VERSION_202 = "SPM VER-2.02";
    private static String currentVersion;

    @Override
    public String supportExtension() {
        return "spm";
    }

    @Override
    public void generate(String path, Spm spm, String charset) throws IOException {
        FileUtil.mkdir(FileUtil.getParent(path, 1));

        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(path));
             BinaryWriter writer = new BinaryWriter(os, charset)) {

            // 写入版本
            writer.writeNullTerminatedString(spm.getSpmVersion());
            currentVersion = spm.getSpmVersion();

            // 写入页面数据
            writer.writeInt(spm.getNumPageData());
            for (Spm.SPMPageData page : spm.getPageData()) {
                writePageData(writer, page);
            }

            writer.writeInt(spm.getNumImageData());
            for (Spm.SPMImageData image : spm.getImageData()) {
                writer.writeNullTerminatedString(image.getImageName());
            }

            writer.writeInt(spm.getPatPageNum());
            writer.writeInt(spm.getNumAnimData());
            for (Spm.SPMAnimData anim : spm.getAnimData()) {
                writeAnimData(writer, anim, spm.getPatPageNum());
            }
        }
    }

    private void writePageData(BinaryWriter writer, Spm.SPMPageData page) throws IOException {
        writer.writeInt(page.getNumChipData());
        writer.writeInt(page.getPageWidth());
        writer.writeInt(page.getPageHeight());
        writeRect(writer, page.getPageRect());
        writer.writeInt(page.getPageOption().intValue());
        writer.writeInt(page.getRotateCenterX());
        writer.writeInt(page.getRotateCenterY());
        writer.writeInt(page.getHitFlag().intValue());

        for (Spm.SPMHitArea hitArea : page.getHitRects()) {
            writeHitArea(writer, hitArea);
        }

        for (Spm.SPMChipData chip : page.getChipData()) {
            writeChipData(writer, chip);
        }
    }

    private void writeRect(BinaryWriter writer, Spm.SPMRect rect) throws IOException {
        writer.writeInt(rect.getLeft());
        writer.writeInt(rect.getTop());
        writer.writeInt(rect.getRight());
        writer.writeInt(rect.getBottom());
    }

    private void writeHitArea(BinaryWriter writer, Spm.SPMHitArea hitArea) throws IOException {
        writeRect(writer, hitArea.getHitRect());
        writer.writeInt(hitArea.getUnk0());
        writer.writeInt(hitArea.getUnk1());
        writer.writeInt(hitArea.getUnk2());
    }

    private void writeChipData(BinaryWriter writer, Spm.SPMChipData chip) throws IOException {
        writer.writeInt(chip.getImageNo());
        writeRect(writer, chip.getDstRect());
        writer.writeInt(chip.getChipWidth());
        writer.writeInt(chip.getChipHeight());
        writeRect(writer, chip.getSrcRect());
        writer.writeInt(chip.getDrawOption().intValue());
        writer.writeInt(chip.getDrawOptionValue().intValue());
        writer.writeInt(chip.getOption());

        // SPM 2.02
        if (SPM_VERSION_202.equals(currentVersion)) {
            writer.writeByte(chip.getUnk5());
        }
    }

    private void writeAnimData(BinaryWriter writer, Spm.SPMAnimData anim, int patPageNum) throws IOException {
        writer.writeNullTerminatedString(anim.getAnimName());
        writer.writeInt(anim.getPatData().size());
        writer.writeInt(anim.getAnimRotateDirection());
        writer.writeInt(anim.getAnimReverseDirection());

        for (Spm.SPMPatData pat : anim.getPatData()) {
            writePatData(writer, pat, patPageNum);
        }
    }

    private void writePatData(BinaryWriter writer, Spm.SPMPatData pat, int patPageNum) throws IOException {
        writer.writeInt(pat.getWaitFrame());
        for (int i = 0; i < patPageNum; i++) {
            writer.writeInt(pat.getPageNo().get(i));
        }
    }
}