package com.giga.nexas.dto.bsdx.grp.groupmap;

import com.giga.nexas.dto.bsdx.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description WazaGroupGrp
 */
@Data
public class WazaGroupGrp extends Grp {

    private List<WazaGroupEntry> wazaList = new ArrayList<>();

    @Data
    public static class WazaGroupEntry {
        public Integer existFlag; // 仅记录用
        private String wazaName;
        private String wazaCodeName;
        private String wazaDisplayName;
        private Integer param;
    }
}
