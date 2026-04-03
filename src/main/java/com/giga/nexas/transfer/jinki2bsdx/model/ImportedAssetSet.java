package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * graft 输出阶段需要复制或导入的静态资源集合。
 */
@Data
public class ImportedAssetSet {

    private List<String> spmFiles = new ArrayList<>();
    private List<String> wazFiles = new ArrayList<>();
    private List<String> auxiliaryFiles = new ArrayList<>();
}
