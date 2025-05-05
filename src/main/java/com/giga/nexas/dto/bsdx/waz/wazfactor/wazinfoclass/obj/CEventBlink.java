package com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * CEventBlink__Read
 */
@Data
@NoArgsConstructor
public class CEventBlink extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventBlinkType {
        private Integer type;
        private String description;
    }

    public static final CEventBlinkType[] CEVENT_BLINK_ENTRIES = {
            new CEventBlinkType(0xFFFFFFFF, "残像タイプ"),
            new CEventBlinkType(0xFFFFFFFF, "発射間隔"),
            new CEventBlinkType(0xFFFFFFFF, "アニメ時間"),
            new CEventBlinkType(0xFFFFFFFF, "フラグ"),

            // todo unsure
            new CEventBlinkType(0xFFFFFFFF, "加算属性"),
            new CEventBlinkType(0xFFFFFFFF, "ノーマル"),
            new CEventBlinkType(0xFFFFFFFF, "親オブジェクトの拡大率反映"),
            new CEventBlinkType(0xFFFFFFFF, "親オブジェクトの属性反映"),
            new CEventBlinkType(0xFFFFFFFF, "%s (周期:%3d)(ｱﾆﾒ長:%3d)")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Short short1;

    public CEventBlink(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();

        this.short1 = reader.readShort();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeShort(this.short1);
    }

}
