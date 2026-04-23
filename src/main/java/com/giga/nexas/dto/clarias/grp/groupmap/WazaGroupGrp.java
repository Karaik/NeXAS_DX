package com.giga.nexas.dto.clarias.grp.groupmap;

import com.giga.nexas.dto.clarias.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/10
 * @Description WazaGroupGrp
 */
@Data
public class WazaGroupGrp extends Grp {

    private List<WazaGroupEntry> wazaList = new ArrayList<>();

    @Data
    public static class WazaGroupEntry {
        public Integer existFlag; // 浠呰褰曠敤
        private String wazaName;
        private String wazaCodeName;
        private String wazaDisplayName;
        private Integer param;
    }
}

