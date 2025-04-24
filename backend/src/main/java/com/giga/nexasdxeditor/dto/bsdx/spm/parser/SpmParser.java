package com.giga.nexasdxeditor.dto.bsdx.spm.parser;

import com.giga.nexasdxeditor.dto.bsdx.BsdxParser;
import com.giga.nexasdxeditor.dto.bsdx.spm.Spm;
import com.giga.nexasdxeditor.io.BinaryReader;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/4/21
 * @Description SpmParser
 */
public class SpmParser implements BsdxParser<Spm> {

    @Override
    public String supportExtension() {
        return "spm";
    }

    @Override
    public Spm parse(byte[] data, String filename, String charset) {
        BinaryReader reader = new BinaryReader(data, charset);
        Spm spm = new Spm();

        spm.setSpmVersion(reader.readNullTerminatedString());

        spm.setNumPageData(reader.readInt());

        List<Spm.SPMPageData> pageDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumPageData(); i++) {
            pageDataList.add(parsePageData(reader));
        }
        spm.setPageData(pageDataList);

        spm.setNumImageData(reader.readInt());

        List<Spm.SPMImageData> imageDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumImageData(); i++) {
            imageDataList.add(parseImageData(reader));
        }
        spm.setImageData(imageDataList);

        spm.setPatPageNum(reader.readInt());
        spm.setNumAnimData(reader.readInt());

        List<Spm.SPMAnimData> animDataList = new ArrayList<>();
        for (int i = 0; i < spm.getNumAnimData(); i++) {
            animDataList.add(parseAnimData(reader, spm.getPatPageNum()));
        }
        spm.setAnimData(animDataList);

        return spm;
    }

    private Spm.SPMPageData parsePageData(BinaryReader reader) {
        Spm.SPMPageData page = new Spm.SPMPageData();
        page.setNumChipData(reader.readInt());
        page.setPageWidth(reader.readInt());
        page.setPageHeight(reader.readInt());
        page.setPageRect(parseRect(reader));
        page.setPageOption(reader.readInt() & 0xFFFFFFFFL);
        page.setRotateCenterX(reader.readInt());
        page.setRotateCenterY(reader.readInt());
        page.setHitFlag(reader.readInt() & 0xFFFFFFFFL);

        List<Spm.SPMHitArea> hitRects = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            if (((1 << (i & 31)) & page.getHitFlag()) != 0) {
                hitRects.add(parseHitArea(reader));
            }
        }
        page.setHitRects(hitRects);

        List<Spm.SPMChipData> chipData = new ArrayList<>();
        for (int i = 0; i < page.getNumChipData(); i++) {
            chipData.add(parseChipData(reader));
        }
        page.setChipData(chipData);

        return page;
    }

    private Spm.SPMRect parseRect(BinaryReader reader) {
        Spm.SPMRect rect = new Spm.SPMRect();
        rect.setLeft(reader.readInt());
        rect.setTop(reader.readInt());
        rect.setRight(reader.readInt());
        rect.setBottom(reader.readInt());
        return rect;
    }

    private Spm.SPMHitArea parseHitArea(BinaryReader reader) {
        Spm.SPMHitArea hitArea = new Spm.SPMHitArea();
        hitArea.setHitRect(parseRect(reader));
        hitArea.setUnk0(reader.readInt());
        hitArea.setUnk1(reader.readInt());
        hitArea.setUnk2(reader.readInt());
        return hitArea;
    }

    private Spm.SPMChipData parseChipData(BinaryReader reader) {
        Spm.SPMChipData chipData = new Spm.SPMChipData();
        chipData.setImageNo(reader.readInt());
        chipData.setDstRect(parseRect(reader));
        chipData.setChipWidth(reader.readInt());
        chipData.setChipHeight(reader.readInt());
        chipData.setSrcRect(parseRect(reader));
        chipData.setDrawOption(reader.readInt() & 0xFFFFFFFFL);
        chipData.setDrawOptionValue(reader.readInt() & 0xFFFFFFFFL);
        chipData.setOption(reader.readInt());
        return chipData;
    }

    // 解析 `SPMImageData`
    private Spm.SPMImageData parseImageData(BinaryReader reader) {
        Spm.SPMImageData imageData = new Spm.SPMImageData();
        imageData.setImageName(reader.readNullTerminatedString());
        return imageData;
    }

    // 解析 `SPMAnimData`
    private Spm.SPMAnimData parseAnimData(BinaryReader reader, int patPageNum) {
        Spm.SPMAnimData animData = new Spm.SPMAnimData();
        animData.setAnimName(reader.readNullTerminatedString());
        animData.setNumPat(reader.readInt());
        animData.setAnimRotateDirection(reader.readInt());
        animData.setAnimReverseDirection(reader.readInt());

        int numPat = animData.getNumPat() & 0xFFFF;  // 65535
        List<Spm.SPMPatData> patDataList = new ArrayList<>();
        for (int i = 0; i < numPat; i++) {
            patDataList.add(parsePatData(reader, patPageNum));
        }
        animData.setPatData(patDataList);

        return animData;
    }

    private Spm.SPMPatData parsePatData(BinaryReader reader, int patPageNum) {
        Spm.SPMPatData patData = new Spm.SPMPatData();
        patData.setWaitFrame(reader.readInt());
        List<Integer> pageNos = new ArrayList<>();
        for (int i = 0; i < patPageNum; i++) {
            pageNos.add(reader.readInt());
        }
        patData.setPageNo(pageNos);
        return patData;
    }
}
