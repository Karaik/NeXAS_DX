package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * CEventBlink__Read
 */
@Data
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

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.int1 = readInt32(bytes, offset); offset += 4;
        this.int2 = readInt32(bytes, offset); offset += 4;
        this.int3 = readInt32(bytes, offset); offset += 4;

        this.short1 = readInt16(bytes, offset); offset += 2;

        return offset;
    }
}
