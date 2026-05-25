package com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata;

import com.giga.nexas.dto.bsdx.map.MapData;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * BHE MapData 转 BSDX MapData 的内存结果。
 */
@Data
public class BheMapDataConversionResult {

    private MapData convertedMapData;
    private BheMapDataConversionAudit audit = new BheMapDataConversionAudit();
    private List<BheMapDataConversionIssue> blockingIssues = new ArrayList<>();
    private List<BheMapDataConversionIssue> downgradeIssues = new ArrayList<>();

    public boolean isConverted() {
        return convertedMapData != null && blockingIssues.isEmpty();
    }

    public void addBlockingIssue(BheMapDataConversionIssue issue) {
        if (issue != null) {
            blockingIssues.add(issue);
        }
    }
}
