package com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog;

import com.giga.nexas.dto.bhe.grp.groupmap.MapGroupGrp;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapSourceEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MapCatalogLoaderTest {

    @Test
    void bheLoaderKeepsOriginalMapGroupIndexAfterSkippingEmptySlots() {
        MapGroupGrp mapGroup = new MapGroupGrp();
        mapGroup.getGroupList().add(group(0, null));
        MapGroupGrp.MapGroup sourceGroup = group(1, "mapb06_amagahara01");
        sourceGroup.getItems().add(item(1, 2, 3, 4, 5));
        sourceGroup.getPairArray1().add(pairArray(6, 7));
        sourceGroup.getArray2().add(intArray(8, 9));
        sourceGroup.getArray3().add(intArray(10, 11));
        mapGroup.getGroupList().add(sourceGroup);
        mapGroup.getGroupList().add(group(1, "mapBlack_S01"));

        List<BheMapSourceEntry> entries = new BheMapCatalogLoader().toSourceEntries(mapGroup);

        assertEquals(2, entries.size());
        assertEquals(1, entries.get(0).getSourceMapIndex());
        assertEquals("mapb06_amagahara01", entries.get(0).getGroupResourceName());
        assertEquals(List.of(1, 2, 3, 4, 5), entries.get(0).getMapGroupSnapshot().getItems().get(0));
        assertEquals(List.of(6, 7), entries.get(0).getMapGroupSnapshot().getPairArray1().get(0));
        assertEquals(List.of(8, 9), entries.get(0).getMapGroupSnapshot().getArray2().get(0));
        assertEquals(List.of(10, 11), entries.get(0).getMapGroupSnapshot().getArray3().get(0));
        assertEquals(2, entries.get(1).getSourceMapIndex());
        assertEquals("mapBlack_S01", entries.get(1).getGroupResourceName());
    }

    private MapGroupGrp.MapGroup group(int existFlag, String resourceName) {
        MapGroupGrp.MapGroup group = new MapGroupGrp.MapGroup();
        group.setExistFlag(existFlag);
        group.setGroupResourceName(resourceName);
        return group;
    }

    private MapGroupGrp.Item item(int int1, int int2, int int3, int int4, int int5) {
        MapGroupGrp.Item item = new MapGroupGrp.Item();
        item.setInt1(int1);
        item.setInt2(int2);
        item.setInt3(int3);
        item.setInt4(int4);
        item.setInt5(int5);
        return item;
    }

    private MapGroupGrp.PairArray pairArray(int int1, int int2) {
        MapGroupGrp.Pair pair = new MapGroupGrp.Pair();
        pair.setInt1(int1);
        pair.setInt2(int2);
        MapGroupGrp.PairArray pairArray = new MapGroupGrp.PairArray();
        pairArray.getValues().add(pair);
        return pairArray;
    }

    private MapGroupGrp.IntArray intArray(int... values) {
        MapGroupGrp.IntArray intArray = new MapGroupGrp.IntArray();
        for (int value : values) {
            intArray.getValues().add(value);
        }
        return intArray;
    }
}
