package com.giga.nexas.dto.bsdx.grp.groupmap;

import com.giga.nexas.dto.bsdx.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description MekaGroupGrp
 */
@Data
public class MekaGroupGrp extends Grp {

    public List<MekaGroup> mekaList = new ArrayList<>();

    @Data
    public static class MekaGroup {
        public String mekaName;
        public String mekaCodeName;
    }
}
