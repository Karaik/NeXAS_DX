package com.giga.nexas.transfer.bhe2bsdx.mapappend.mapgroup;

import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapGroupEntrySnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BheMapGroupEntryConverterTest {

    @TempDir
    Path tempDir;

    @Test
    void convertsBheSnapshotToBsdxMapGroupEntryInMemoryOnly() throws Exception {
        BheMapEntryPlan entryPlan = new BheMapEntryPlan();
        entryPlan.setSourceMapFileName("map_source.map");
        entryPlan.setTargetGroupResourceName("bhe_map_source");
        BheMapGroupEntrySnapshot snapshot = new BheMapGroupEntrySnapshot();
        snapshot.setExistFlag(1);
        snapshot.setGroupName("source group");
        snapshot.setGroupCodeName("source code");
        snapshot.setGroupResourceName("map_source");
        snapshot.setInt1(77);
        snapshot.getItems().add(List.of(1, 2, 3, 4, 500));
        snapshot.getItems().add(List.of(5, 6, 7, 8, 900));
        snapshot.getPairArray1().add(List.of(10, 11, 12, 13));
        snapshot.getPairArray1().add(List.of(20, 21));
        snapshot.getArray2().add(List.of(30, 31, 32));
        snapshot.getArray3().add(List.of(40, 41));
        entryPlan.setSourceMapGroupSnapshot(snapshot);

        BheMapGroupEntryConversionResult result = new BheMapGroupEntryConverter().convert(entryPlan);
        MapGroupGrp.MapGroup target = result.getTargetGroup();

        // 转换只消费内存 plan，不读写 outputRoot 或任何目标文件。
        assertFalse(Files.exists(tempDir.resolve("MapGroup.grp")));
        assertEquals(1, target.getExistFlag());
        assertEquals("source group", target.getGroupName());
        assertEquals("source code", target.getGroupCodeName());
        assertEquals("bhe_map_source", target.getGroupResourceName());
        assertEquals(77, target.getInt1());

        assertEquals(2, target.getItems().size());
        assertBsdxItem(target.getItems().get(0), 1, 2, 3, 4);
        assertBsdxItem(target.getItems().get(1), 5, 6, 7, 8);
        assertEquals(2, result.getItemDowngrades().size());
        assertEquals(500, result.getItemDowngrades().get(0).getDroppedInt5());
        assertEquals(900, result.getItemDowngrades().get(1).getDroppedInt5());
        assertEquals("map_source.map", result.getItemDowngrades().get(0).getSourceMapFileName());

        assertEquals(List.of(10, 11, 12, 13), target.getArray1().get(0).getValues());
        assertEquals(List.of(20, 21), target.getArray1().get(1).getValues());
        assertEquals(List.of(30, 31, 32), target.getArray2().get(0).getValues());
        assertEquals(List.of(40, 41), target.getArray3().get(0).getValues());
    }

    private void assertBsdxItem(MapGroupGrp.Item item, int int1, int int2, int int3, int int4) {
        assertEquals(int1, item.getInt1());
        assertEquals(int2, item.getInt2());
        assertEquals(int3, item.getInt3());
        assertEquals(int4, item.getInt4());
    }
}
