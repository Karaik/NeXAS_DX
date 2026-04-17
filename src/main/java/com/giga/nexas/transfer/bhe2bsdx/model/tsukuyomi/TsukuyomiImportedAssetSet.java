package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 记录静态资源落盘后的输出集合。
 */
@Data
public class TsukuyomiImportedAssetSet {

    private Path outputRootDir;

    private List<Path> generatedDatFiles = new ArrayList<>();
    private List<Path> generatedGrpFiles = new ArrayList<>();
    private List<Path> generatedMekFiles = new ArrayList<>();
    private List<Path> generatedWazFiles = new ArrayList<>();
    private List<Path> copiedSpmFiles = new ArrayList<>();
    private List<Path> copiedImageFiles = new ArrayList<>();
    private List<Path> copiedAudioFiles = new ArrayList<>();

    private List<String> spmFiles = new ArrayList<>();
    private List<String> wazFiles = new ArrayList<>();
    private List<String> auxiliaryFiles = new ArrayList<>();
    private List<String> missingAssets = new ArrayList<>();
}
