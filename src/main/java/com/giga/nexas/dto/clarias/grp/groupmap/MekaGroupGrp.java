package com.giga.nexas.dto.clarias.grp.groupmap;

import com.giga.nexas.dto.clarias.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/10
 * @Description MekaGroupGrp
 */
@Data
public class MekaGroupGrp extends Grp {

    private List<MekaGroup> mekaList = new ArrayList<>();

    @Data
    public static class MekaGroup {
        public Integer existFlag; // 浠呰褰曠敤
        private String mekaName;
        private String mekaCodeName;
    }
}

