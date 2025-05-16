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

    private List<TermGroup> termList = new ArrayList<>();

    @Data
    public static class TermGroup {
        private String termGroupName;
        private String termGroupCodeName;
        private List<TermItem> termItemList = new ArrayList<>();
    }

    @Data
    public static class TermItem {
        private String termItemName;
        private String termItemCodeName;
        private String termItemDescription;
        private Integer param1;
        private Integer param2;
    }
}
