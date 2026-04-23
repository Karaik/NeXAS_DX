package com.giga.nexas.dto.clarias.grp.groupmap;

import com.giga.nexas.dto.clarias.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/10
 * @Description MapGroupGrp
 */
@Data
public class MapGroupGrp extends Grp {

    private List<MapGroup> groupList = new ArrayList<>();

    /**
     * existFlag, 3涓瓧绗︿覆, 1涓湭鐭nt(int1), N涓狪tem(姣忛」4涓猧nt), 涓夋 鏁扮粍鐨勬暟缁?
     */
    @Data
    public static class MapGroup {

        private int existFlag;
        private String groupName;
        private String groupCodeName;
        private String groupResourceName;
        private int int1;

        // 姣忛」4涓猧nt
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

