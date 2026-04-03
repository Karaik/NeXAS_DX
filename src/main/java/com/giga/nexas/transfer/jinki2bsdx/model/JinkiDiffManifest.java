package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 记录“哪些同名资源复用、哪些导入”的稳定清单对象。
 */
@Data
public class JinkiDiffManifest {

    private List<String> reusedAssets = new ArrayList<>();
    private List<String> importedAssets = new ArrayList<>();
    private Map<Integer, Integer> spriteIndexMap = new LinkedHashMap<>();
    private Map<Integer, Integer> wazIndexMap = new LinkedHashMap<>();
}
