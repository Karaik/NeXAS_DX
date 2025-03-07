package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * CEventTouch__Read
 */
@Data
public class CEventTouch extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventTouchType {
        private Integer type;
        private String description;
    }

    public static final CEventTouchType[] CEVENT_TOUCH_TYPES = {
            new CEventTouchType(0xFFFFFFFF, "水平：メカ（敵）"),
            new CEventTouchType(0xFFFFFFFF, "水平：メカ（味方）"),
            new CEventTouchType(0xFFFFFFFF, "水平：弾（敵）"),
            new CEventTouchType(0xFFFFFFFF, "水平：弾（味方）"),
            new CEventTouchType(0xFFFFFFFF, "水平：マップOBJ"),
            new CEventTouchType(0xFFFFFFFF, "進禁（メカ）"),
            new CEventTouchType(0xFFFFFFFF, "進禁（弾）"),
            new CEventTouchType(0xFFFFFFFF, "進禁（穴）"),
            new CEventTouchType(0xFFFFFFFF, "画面端"),
            new CEventTouchType(0xFFFFFFFF, "垂直：メカ（敵）"),
            new CEventTouchType(0xFFFFFFFF, "垂直：メカ（味方）"),
            new CEventTouchType(0xFFFFFFFF, "垂直：弾（敵）"),
            new CEventTouchType(0xFFFFFFFF, "垂直：弾（味方）"),
            new CEventTouchType(0xFFFFFFFF, "垂直：障害物")
    };

    private byte[] byteData1;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        // 读取0x38u字节
        this.byteData1 = Arrays.copyOfRange(bytes, offset, offset + 56); offset += 56;

        return offset;
    }
}
