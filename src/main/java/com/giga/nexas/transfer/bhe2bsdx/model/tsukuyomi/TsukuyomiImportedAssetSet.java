package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import lombok.Data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import com.giga.nexas.transfer.bhe2bsdx.meka.shared.AssetDeliveryValidationContext;

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
    /** SPM声明或静态动画引用的缺图记录；不代表已证明运行必需，不作为交付阻断条件。 */
    private Map<String, String> unresolvedImageReferences = new LinkedHashMap<>();
    /** MEK语音表中原生会静默不播放的越界语音号；保留引用并单独审计。 */
    private Map<String, String> unresolvedMekVoiceReferences = new LinkedHashMap<>();
    private Map<String, String> resolvedMediaSources = new LinkedHashMap<>();
    @com.fasterxml.jackson.annotation.JsonIgnore
    private transient AssetDeliveryValidationContext validationContext;
}
