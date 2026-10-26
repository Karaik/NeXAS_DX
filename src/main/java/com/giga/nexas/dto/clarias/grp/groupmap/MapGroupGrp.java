package com.giga.nexas.dto.clarias.grp.groupmap;

import com.giga.nexas.dto.clarias.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description MapGroupGrp
 */
@Data
public class MapGroupGrp extends Grp {

    private List<MapGroup> groupList = new ArrayList<>();

    /**
     * existFlag, 3个字符串, 1个未知int(int1), N个Item(每项4个int), 三段 数组的数组
     */
    @Data
    public static class MapGroup {

        private int existFlag;
        private String groupName;
        private String groupCodeName;
        private String groupResourceName;
        private int int1;

        // 每项4个int
        private List<Item> items = new ArrayList<>();

        private List<IntArray> array1 = new ArrayList<>();
        private List<IntArray> array2 = new ArrayList<>();
        private List<IntArray> array3 = new ArrayList<>();
    }

    @Data
    public static class Item {
        private int int1;
        private int int2;
        private int int3;
        private int int4;
    }

    @Data
    public static class IntArray {
        private List<Integer> values = new ArrayList<>();
    }

}

