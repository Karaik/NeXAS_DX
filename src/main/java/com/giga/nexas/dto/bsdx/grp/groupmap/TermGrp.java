package com.giga.nexas.dto.bsdx.grp.groupmap;

import com.giga.nexas.dto.bsdx.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description TermGrp
 */
@Data
public class TermGrp extends Grp {

    public List<TermGroup> termList = new ArrayList<>();

    @Data
    public static class TermGroup {
        public String termGroupName;
        public String termGroupCodeName;
        public List<TermItem> termItemList = new ArrayList<>();
    }

    @Data
    public static class TermItem {
        public String termItemName;
        public String termItemCodeName;
        public String termItemDescription;
        public int param1;
        public int param2;
    }
}
