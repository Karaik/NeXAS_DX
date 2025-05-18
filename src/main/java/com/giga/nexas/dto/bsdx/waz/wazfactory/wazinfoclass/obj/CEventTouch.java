package com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * CEventTouch__Read
 */
@Data
@NoArgsConstructor
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

    public CEventTouch(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        // 读取 0x38 字节
        this.byteData1 = reader.readBytes(56);
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeBytes(this.byteData1);
    }

}
