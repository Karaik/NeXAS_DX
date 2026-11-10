package com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;

import java.io.IOException;
import java.nio.file.Path;

/**
 * 读取 BSDX MapGroup.grp 的基线资源名集合。
 *
 * <p>import plan construction 只需要知道目标 groupResourceName 是否撞名，不提前修改 BSDX MapGroup DTO。</p>
 */
public class BsdxMapBaselineLoader {

    private final BsdxBinService bsdxBinService;

    public BsdxMapBaselineLoader() {
        this(new BsdxBinService());
    }

    public BsdxMapBaselineLoader(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService;
    }

    public BsdxMapBaseline load(Path mapGroupPath, String charset) {
        try {
            ResponseDTO<?> response = bsdxBinService.parse(mapGroupPath.toString(), charset);
            MapGroupGrp mapGroup = (MapGroupGrp) response.getData();
            return toBaseline(mapGroup);
        } catch (IOException e) {
            throw new IllegalStateException("读取 BSDX MapGroup.grp 失败: " + mapGroupPath, e);
        }
    }

    public BsdxMapBaseline toBaseline(MapGroupGrp mapGroup) {
        BsdxMapBaseline baseline = new BsdxMapBaseline();
        if (mapGroup == null || mapGroup.getGroupList() == null) {
            return baseline;
        }

        for (MapGroupGrp.MapGroup group : mapGroup.getGroupList()) {
            if (group == null || group.getExistFlag() == 0 || group.getGroupResourceName() == null || group.getGroupResourceName().isBlank()) {
                continue;
            }
            baseline.getExistingGroupResourceNames().add(group.getGroupResourceName());
        }
        return baseline;
    }
}
