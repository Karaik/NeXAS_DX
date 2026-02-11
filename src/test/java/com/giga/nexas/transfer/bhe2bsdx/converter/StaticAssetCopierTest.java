package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.spm.Spm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticAssetCopierTest {

    @TempDir
    Path tempDir;

    @Test
    void copyAssets_shouldCopyOnlyReferencedImages() throws Exception {
        Path assetRoot = tempDir.resolve("assets");
        Path outputDir = tempDir.resolve("out");
        Files.createDirectories(assetRoot);
        Files.writeString(assetRoot.resolve("a.png"), "a");
        Files.writeString(assetRoot.resolve("b.png"), "b");
        Files.writeString(assetRoot.resolve("c.png"), "c");

        Spm spm = new Spm();
        spm.setImageData(List.of(image("a.png"), image("b.png"), image("c.png")));
        spm.setPageData(List.of(pageWithImageNo(1)));
        spm.setPatPageNum(1);
        spm.setAnimData(List.of(animWithPageNo(0)));

        StaticAssetCopier copier = new StaticAssetCopier();
        copier.copyAssets(outputDir, assetRoot, Map.of("demo", spm), Map.of());

        assertFalse(Files.exists(outputDir.resolve("a.png")));
        assertTrue(Files.exists(outputDir.resolve("b.png")));
        assertFalse(Files.exists(outputDir.resolve("c.png")));
    }

    @Test
    void copyAssets_shouldFallbackToFullImageListWhenImageNoOutOfRange() throws Exception {
        Path assetRoot = tempDir.resolve("assets2");
        Path outputDir = tempDir.resolve("out2");
        Files.createDirectories(assetRoot);
        Files.writeString(assetRoot.resolve("a.png"), "a");
        Files.writeString(assetRoot.resolve("b.png"), "b");
        Files.writeString(assetRoot.resolve("c.png"), "c");

        Spm spm = new Spm();
        spm.setImageData(List.of(image("a.png"), image("b.png"), image("c.png")));
        spm.setPageData(List.of(pageWithImageNo(5)));
        spm.setPatPageNum(1);
        spm.setAnimData(List.of(animWithPageNo(0)));

        StaticAssetCopier copier = new StaticAssetCopier();
        copier.copyAssets(outputDir, assetRoot, Map.of("demo", spm), Map.of());

        assertTrue(Files.exists(outputDir.resolve("a.png")));
        assertTrue(Files.exists(outputDir.resolve("b.png")));
        assertTrue(Files.exists(outputDir.resolve("c.png")));
    }

    private Spm.SPMImageData image(String name) {
        Spm.SPMImageData image = new Spm.SPMImageData();
        image.setImageName(name);
        return image;
    }

    private Spm.SPMPageData pageWithImageNo(int imageNo) {
        Spm.SPMChipData chipData = new Spm.SPMChipData();
        chipData.setImageNo(imageNo);
        Spm.SPMPageData pageData = new Spm.SPMPageData();
        pageData.setChipData(List.of(chipData));
        return pageData;
    }

    private Spm.SPMAnimData animWithPageNo(int pageNo) {
        Spm.SPMPatData patData = new Spm.SPMPatData();
        patData.setPageNo(List.of(pageNo));
        Spm.SPMAnimData animData = new Spm.SPMAnimData();
        animData.setPatData(List.of(patData));
        return animData;
    }
}
