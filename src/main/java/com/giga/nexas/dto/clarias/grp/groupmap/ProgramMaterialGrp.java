package com.giga.nexas.dto.clarias.grp.groupmap;

import com.giga.nexas.dto.clarias.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/10
 * @Description ProgramMaterialGrp
 */
@Data
public class ProgramMaterialGrp extends Grp {

    private List<IntArray> array1 = new ArrayList<>();
    private List<IntArray> array2 = new ArrayList<>();
    private List<IntArray> array3 = new ArrayList<>();

    @Data
    public static class IntArray {
        private List<Integer> values = new ArrayList<>();
    }

}

