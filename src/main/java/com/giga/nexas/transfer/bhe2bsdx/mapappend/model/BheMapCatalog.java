package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE MapGroup.grp 的轻量目录。
 *
 * <p>totalGroupCount 保留原始顶层条目数，entries 只放 existFlag 非 0 且带 groupResourceName 的迁移候选。</p>
 */
@Data
public class BheMapCatalog {

    private int totalGroupCount;
    private List<BheMapSourceEntry> entries = new ArrayList<>();
}
