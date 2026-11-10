package com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bhe.grp.groupmap.MapGroupGrp;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapGroupEntrySnapshot;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapSourceEntry;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 读取 BHE MapGroup.grp 并提取地图源目录。
 *
 * <p>MapGroup 下标就是外部表的 mapId，因此 loader 只跳过 existFlag=0 的空槽，不重新排序有效地图。</p>
 */
public class BheMapCatalogLoader {

    private final BheBinService bheBinService;

    public BheMapCatalogLoader() {
        this(new BheBinService());
    }

    public BheMapCatalogLoader(BheBinService bheBinService) {
        this.bheBinService = bheBinService;
    }

    public BheMapCatalog load(Path mapGroupPath, String charset) {
        try {
            ResponseDTO<?> response = bheBinService.parse(mapGroupPath.toString(), charset);
            MapGroupGrp mapGroup = (MapGroupGrp) response.getData();
            return toCatalog(mapGroup);
        } catch (IOException e) {
            throw new IllegalStateException("读取 BHE MapGroup.grp 失败: " + mapGroupPath, e);
        }
    }

    public BheMapCatalog toCatalog(MapGroupGrp mapGroup) {
        BheMapCatalog catalog = new BheMapCatalog();
        if (mapGroup == null || mapGroup.getGroupList() == null) {
            return catalog;
        }
        catalog.setTotalGroupCount(mapGroup.getGroupList().size());
        catalog.setEntries(toSourceEntries(mapGroup));
        return catalog;
    }

    public List<BheMapSourceEntry> toSourceEntries(MapGroupGrp mapGroup) {
        List<BheMapSourceEntry> entries = new ArrayList<>();
        if (mapGroup == null || mapGroup.getGroupList() == null) {
            return entries;
        }

        for (int index = 0; index < mapGroup.getGroupList().size(); index++) {
            MapGroupGrp.MapGroup group = mapGroup.getGroupList().get(index);
            if (group == null || group.getExistFlag() == 0 || group.getGroupResourceName() == null || group.getGroupResourceName().isBlank()) {
                continue;
            }

            BheMapSourceEntry entry = new BheMapSourceEntry();
            entry.setSourceMapIndex(index);
            entry.setGroupName(group.getGroupName());
            entry.setGroupCodeName(group.getGroupCodeName());
            entry.setGroupResourceName(group.getGroupResourceName());
            entry.setMapGroupInt1(group.getInt1());
            entry.setItemCount(group.getItems().size());
            entry.setPairArray1Count(group.getPairArray1().size());
            entry.setArray2Count(group.getArray2().size());
            entry.setArray3Count(group.getArray3().size());
            entry.setMapGroupSnapshot(toSnapshot(group));
            entries.add(entry);
        }
        return entries;
    }

    private BheMapGroupEntrySnapshot toSnapshot(MapGroupGrp.MapGroup group) {
        BheMapGroupEntrySnapshot snapshot = new BheMapGroupEntrySnapshot();
        snapshot.setExistFlag(group.getExistFlag());
        snapshot.setGroupName(group.getGroupName());
        snapshot.setGroupCodeName(group.getGroupCodeName());
        snapshot.setGroupResourceName(group.getGroupResourceName());
        snapshot.setInt1(group.getInt1());
        for (MapGroupGrp.Item item : group.getItems()) {
            snapshot.getItems().add(List.of(item.getInt1(), item.getInt2(), item.getInt3(), item.getInt4(), item.getInt5()));
        }
        for (MapGroupGrp.PairArray pairArray : group.getPairArray1()) {
            List<Integer> values = new ArrayList<>();
            for (MapGroupGrp.Pair pair : pairArray.getValues()) {
                values.add(pair.getInt1());
                values.add(pair.getInt2());
            }
            snapshot.getPairArray1().add(values);
        }
        for (MapGroupGrp.IntArray intArray : group.getArray2()) {
            snapshot.getArray2().add(new ArrayList<>(intArray.getValues()));
        }
        for (MapGroupGrp.IntArray intArray : group.getArray3()) {
            snapshot.getArray3().add(new ArrayList<>(intArray.getValues()));
        }
        return snapshot;
    }
}
