package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
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
            new CEventBlinkType(0xFFFFFFFF, "描画優先順位(相対)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:描画タイプ"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:開始描画値"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:開始描画値(R)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:開始描画値(G)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:開始描画値(B)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:終了描画値"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:終了描画値(R)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:終了描画値(G)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:終了描画値(B)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:開始拡大率(0未満無視)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:終了拡大率(0未満無視)"),
            new CEventBlinkType(0xFFFFFFFF, "フラグ")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Short short1;
    private Integer int5;
    private Integer int6;
    private Byte byte1;
    private Byte byte2;
    private Byte byte3;
    private Integer int7;
    private Byte byte4;
    private Byte byte5;
    private Byte byte6;
    private Integer int8;
    private Integer int9;

    public CEventBlink(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.short1 = reader.readShort();
        this.int5 = reader.readInt();
        this.int6 = reader.readInt();
        this.byte1 = reader.readByte();
        this.byte2 = reader.readByte();
        this.byte3 = reader.readByte();
        this.int7 = reader.readInt();
        this.byte4 = reader.readByte();
        this.byte5 = reader.readByte();
        this.byte6 = reader.readByte();
        this.int8 = reader.readInt();
        this.int9 = reader.readInt();
    }
}
