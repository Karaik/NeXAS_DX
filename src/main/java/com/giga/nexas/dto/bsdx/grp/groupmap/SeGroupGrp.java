package com.giga.nexas.dto.bsdx.grp.groupmap;

import com.giga.nexas.dto.bsdx.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description SeGroupGrp
 */
@Data
public class SeGroupGrp extends Grp {

    public List<SeGroupGroup> seList = new ArrayList<>();

    @Data
    public static class SeGroupGroup {
        public String seType;
        public String seTypeCodeName;
        public List<SeGroupItem> seItems = new ArrayList<>();
    }

    @Data
    public static class SeGroupItem {
        public String seItemName;
        public String seItemCodeName;
        public String seFileName;
    }
}
