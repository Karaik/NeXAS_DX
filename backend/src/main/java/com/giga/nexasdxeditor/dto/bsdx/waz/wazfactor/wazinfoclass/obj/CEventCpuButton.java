package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * @Description CEventCpuButton
 * CEventCpuButton__Read
 */
@Data
public class CEventCpuButton extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventCpuButtonType {
        private Integer type;
        private String description;
    }

    public static final CEventCpuButtonType[] CEVENT_CPU_BUTTON_ENTRIES = {
            // .rdata:007F2290 ~ 007F257F
            new CEventCpuButtonType(0xFFFFFFFF, "エフェクトタイプ1"),
            new CEventCpuButtonType(0xFFFFFFFF, "エフェクトタイプ2"),
            new CEventCpuButtonType(0xFFFFFFFF, "フラグ"),
            new CEventCpuButtonType(0xFFFFFFFF, "エフェクト番号"),
            new CEventCpuButtonType(0xFFFFFFFF, "同時発射数"),
            new CEventCpuButtonType(0xFFFFFFFF, "最大発射数"),
            new CEventCpuButtonType(0xFFFFFFFF, "発射間隔"),
            new CEventCpuButtonType(0xFFFFFFFF, "発射位置"),
            new CEventCpuButtonType(0xFFFFFFFF, "発射位置補正：方向"),
            new CEventCpuButtonType(0xFFFFFFFF, "発射位置補正：方向補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "発射位置補正：距離"),
            new CEventCpuButtonType(0xFFFFFFFF, "発射位置補正：高さ"),
            new CEventCpuButtonType(0xFFFFFFFF, "発射方向：１発ごとの補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "発射方向：同時発射補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "発射方向：全体補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "発射ＳＥ"),
            new CEventCpuButtonType(0xFFFFFFFF, "ｽﾌﾟﾗｲﾄ：拡大縮小"),
            new CEventCpuButtonType(0xFFFFFFFF, "ｽﾌﾟﾗｲﾄ：拡大縮小X"),
            new CEventCpuButtonType(0xFFFFFFFF, "ｽﾌﾟﾗｲﾄ：拡大縮小Y"),
            new CEventCpuButtonType(0xFFFFFFFF, "ｽﾌﾟﾗｲﾄ：拡大縮小Y(ｽﾛｰ影響)"),
            new CEventCpuButtonType(0xFFFFFFFF, "ｽﾌﾟﾗｲﾄ：振動"),
            new CEventCpuButtonType(0xFFFFFFFF, "ｽﾌﾟﾗｲﾄ：属性"),
            new CEventCpuButtonType(0xFFFFFFFF, "ｽﾌﾟﾗｲﾄ：方向"),
            new CEventCpuButtonType(0xFFFFFFFF, "ｽﾌﾟﾗｲﾄ：方向補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "ｽﾌﾟﾗｲﾄ：方向増分"),
            new CEventCpuButtonType(0xFFFFFFFF, "位置"),
            new CEventCpuButtonType(0xFFFFFFFF, "位置：分散率"),
            new CEventCpuButtonType(0xFFFFFFFF, "座標移動"),
            new CEventCpuButtonType(0xFFFFFFFF, "ﾍﾞｸﾄﾙ：方向"),
            new CEventCpuButtonType(0xFFFFFFFF, "ﾍﾞｸﾄﾙ：方向補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "ﾍﾞｸﾄﾙ：方向増分"),
            new CEventCpuButtonType(0xFFFFFFFF, "ﾍﾞｸﾄﾙ：速度"),
            new CEventCpuButtonType(0xFFFFFFFF, "ﾍﾞｸﾄﾙ：高さ"),
            new CEventCpuButtonType(0xFFFFFFFF, "ﾍﾞｸﾄﾙ：重力"),
            new CEventCpuButtonType(0xFFFFFFFF, "ﾍﾞｸﾄﾙ：最低高度"),
            new CEventCpuButtonType(0xFFFFFFFF, "ﾍﾞｸﾄﾙ：慣性"),
            new CEventCpuButtonType(0xFFFFFFFF, "汎用変数"),
            new CEventCpuButtonType(0xFFFFFFFF, "溜め攻撃力"),
            new CEventCpuButtonType(0xFFFFFFFF, "影の濃さ"),
            new CEventCpuButtonType(0xFFFFFFFF, "優先順位補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "耐久力"),
            new CEventCpuButtonType(0xFFFFFFFF, "記憶ＯＢＪの最大数"),
            new CEventCpuButtonType(0xFFFFFFFF, "指定OBJ数以下なら生成"),
            new CEventCpuButtonType(0xFFFFFFFF, "指定画面範囲内なら生成"),
            new CEventCpuButtonType(0xFFFFFFFF, "指定画面範囲外なら削除")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Short short1;
    private Short short2;

    private List<CEventCpuButtonUnit> ceventCpuButtonUnitList = new ArrayList<>();

    @Data
    public static class CEventCpuButtonUnit {
        private Integer buffer;
        private WazInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.int1 = readInt32(bytes, offset); offset += 4;
        this.int2 = readInt32(bytes, offset); offset += 4;
        this.int3 = readInt32(bytes, offset); offset += 4;
        this.int4 = readInt32(bytes, offset); offset += 4;
        this.int5 = readInt32(bytes, offset); offset += 4;

        this.short1 = readInt16(bytes, offset); offset += 2;
        this.short2 = readInt16(bytes, offset); offset += 2;

        this.ceventCpuButtonUnitList.clear();

        for (int i = 0; i < 8; i++) {
            CEventCpuButtonUnit unit = new CEventCpuButtonUnit();
            if (i == 0) {
                int buffer = readInt32(bytes, offset); offset += 4;
                unit.setBuffer(buffer);

                if (buffer != 0) {
                    WazInfoObject obj = createCEventObjectByType(CEVENT_CPU_BUTTON_ENTRIES[i].getType());
                    if (obj != null) {
                        offset = obj.readInfo(bytes, offset);
                        unit.setData(obj);
                    }
                }

            } else {
                unit.setBuffer(null);
                unit.setData(null);
            }
            ceventCpuButtonUnitList.add(unit);
        }

        return offset;
    }
}
