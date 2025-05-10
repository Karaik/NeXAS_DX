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

    public List<WazaGroupEntry> wazaList = new ArrayList<>();

    @Data
    public static class WazaGroupEntry {
        public String wazaName;
        public String wazaCodeName;
        public String wazaDisplayName;
        public int param;
    }
}
