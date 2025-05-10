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

    public List<SpriteGroupEntry> spriteList = new ArrayList<>();

    @Data
    public static class SpriteGroupEntry {
        public String spriteFileName;
        public String spriteCodeName;
        public int param;
    }

}
