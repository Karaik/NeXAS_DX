package com.giga.nexas.dto.bsdx.grp.groupmap;

import com.giga.nexas.dto.bsdx.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description SpriteGroupGrp
 */
@Data
public class SpriteGroupGrp extends Grp {

    private List<SpriteGroupEntry> spriteList = new ArrayList<>();

    @Data
    public static class SpriteGroupEntry {
        public Integer existFlag; // 仅记录用
        private String spriteFileName;
        private String spriteCodeName;
        private Integer param;
    }

}
