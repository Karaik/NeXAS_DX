package com.giga.nexas.dto.clarias.grp.groupmap;

import com.giga.nexas.dto.clarias.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/10
 * @Description SpriteGroupGrp
 */
@Data
public class SpriteGroupGrp extends Grp {

    private List<SpriteGroupEntry> spriteList = new ArrayList<>();

    @Data
    public static class SpriteGroupEntry {
        public Integer existFlag; // 浠呰褰曠敤
        private String spriteFileName;
        private String spriteCodeName;
        private Integer param;
    }

}

