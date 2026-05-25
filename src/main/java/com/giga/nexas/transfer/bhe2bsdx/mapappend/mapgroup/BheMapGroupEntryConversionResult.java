package com.giga.nexas.transfer.bhe2bsdx.mapappend.mapgroup;

import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单条 BHE MapGroup entry 转 BSDX MapGroup entry 的内存结果。
 *
 * <p>本结果只表达 MapGroup entry conversion 的内存产物，不代表已经写入 MapGroup.grp。</p>
 */
@Data
public class BheMapGroupEntryConversionResult {

    private MapGroupGrp.MapGroup targetGroup;
    private List<BheMapGroupItemDowngrade> itemDowngrades = new ArrayList<>();
}
