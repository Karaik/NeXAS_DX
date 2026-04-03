package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 记录 step8 静态资源落盘后的输出集合。
 */
@Data
public class ImportedAssetSet {

    private Path outputRootDir;

    private List<Path> generatedDatFiles = new ArrayList<>();
    private List<Path> generatedMekFiles = new ArrayList<>();
    private List<Path> generatedWazFiles = new ArrayList<>();
    private List<Path> copiedSpmFiles = new ArrayList<>();
    private List<Path> copiedAudioFiles = new ArrayList<>();

    private List<String> spmFiles = new ArrayList<>();
    private List<String> wazFiles = new ArrayList<>();
    private List<String> auxiliaryFiles = new ArrayList<>();
    private List<String> missingAssets = new ArrayList<>();
}
