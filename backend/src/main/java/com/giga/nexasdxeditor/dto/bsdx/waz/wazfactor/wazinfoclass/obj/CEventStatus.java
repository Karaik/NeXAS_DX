package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description CEventStatus
 * CEventStatus__Read
 */
@Data
public class CEventStatus extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventStatusType {
        private Integer type;
        private String description;
    }

    public static final CEventStatusType[] CEVENT_STATUS_TYPES = {
            new CEventStatusType(0xFFFFFFFF, "追加タイプ"),
            new CEventStatusType(0xFFFFFFFF, "対象タイプ"),
            new CEventStatusType(0xFFFFFFFF, "最大HP"),
            new CEventStatusType(0xFFFFFFFF, "ブースター数"),
            new CEventStatusType(0xFFFFFFFF, "FCゲージ数"),
            new CEventStatusType(0xFFFFFFFF, "打撃攻撃力"),
            new CEventStatusType(0xFFFFFFFF, "射撃攻撃力"),
            new CEventStatusType(0xFFFFFFFF, "打撃防御力"),
            new CEventStatusType(0xFFFFFFFF, "射撃防御力"),
            new CEventStatusType(0xFFFFFFFF, "移動速度：歩行"),
            new CEventStatusType(0xFFFFFFFF, "移動速度：ダッシュ"),
            new CEventStatusType(0xFFFFFFFF, "移動速度：ブーストダッシュ"),
            new CEventStatusType(0xFFFFFFFF, "移動速度：サーチダッシュ"),
            new CEventStatusType(0xFFFFFFFF, "攻撃速度：打撃"),
            new CEventStatusType(0xFFFFFFFF, "攻撃速度：射撃"),
            new CEventStatusType(0xFFFFFFFF, "熱冷却速度"),
            new CEventStatusType(0xFFFFFFFF, "根性値"),
            new CEventStatusType(0xFFFFFFFF, "データタイプ"),
            new CEventStatusType(0xFFFFFFFF, "終了タイプ"),
            new CEventStatusType(0xFFFFFFFF, "終了カウント"),
            new CEventStatusType(0xFFFFFFFF, "開始時間"),
            new CEventStatusType(0xFFFFFFFF, "実行間隔"),
            new CEventStatusType(0xFFFFFFFF, "優先順位"),
            new CEventStatusType(0x4, "重複タイプ"),
            new CEventStatusType(0xFFFFFFFF, "エフェクト")
    };

    public static final String[] CEVENT_STATUS_FORMATS = {
            "%s : %4d (周期 : %4d)",
            "%s : %4d ",
            "⇒ %4d (周期 : %4d)"
    };

    private byte[] byteData1;
    private byte[] byteData2;
    private byte byte1;
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;

    private List<CEventStatusUnit> ceventStatusUnitList = new ArrayList<>();

    @Data
    public static class CEventStatusUnit {
        private Integer ceventStatusUnitQuantity;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        // 读取0xFu字节
        byteData1 = Arrays.copyOfRange(bytes, offset, offset + 15); offset += 15;
        // 读取0x3Cu字节
        byteData2 = Arrays.copyOfRange(bytes, offset, offset + 60); offset += 60;

        this.byte1 = readInt8(bytes, offset); offset += 1;

        this.int1 = readInt32(bytes, offset); offset += 4;
        this.int2 = readInt32(bytes, offset); offset += 4;
        this.int3 = readInt32(bytes, offset); offset += 4;
        this.int4 = readInt32(bytes, offset); offset += 4;
        this.int5 = readInt32(bytes, offset); offset += 4;
        this.int6 = readInt32(bytes, offset); offset += 4;
        this.int7 = readInt32(bytes, offset); offset += 4;
        this.int8 = readInt32(bytes, offset); offset += 4;
        this.int9 = readInt32(bytes, offset); offset += 4;

        this.ceventStatusUnitList.clear();

        for (int i = 0; i < 25; i++) {
            int buffer = readInt32(bytes, offset); offset += 4;
            CEventStatusUnit unit = new CEventStatusUnit();
            unit.setCeventStatusUnitQuantity(i);
            unit.setDescription(CEVENT_STATUS_TYPES[i].getDescription());
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEVENT_STATUS_TYPES[i].getType();
                SkillInfoObject obj = createCEventObjectByType(typeId);
                if (obj != null) {
                    offset = obj.readInfo(bytes, offset);
                    unit.setData(obj);
                }
                this.ceventStatusUnitList.add(unit);
            }
        }

        return offset;
    }
}
