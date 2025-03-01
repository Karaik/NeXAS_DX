package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description
 * CEventMove__Read
 */
@Data
public class CEventMove extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventMoveType {
        private Integer type;
        private String description;
    }

    public static final CEventMoveType[] CEVENT_MOVE_TYPES = {
            new CEventMoveType(0xFFFFFFFF, "移動タイプ"),
            new CEventMoveType(0xFFFFFFFF, "開始位置"),
            new CEventMoveType(0xFFFFFFFF, "終了位置"),
            new CEventMoveType(0xFFFFFFFF, "処理タイプ"),
            new CEventMoveType(0xFFFFFFFF, "フラグ"),
            new CEventMoveType(0xFFFFFFFF, "停止時間"),
            new CEventMoveType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ"),
            new CEventMoveType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ(のけぞり時)"),
            new CEventMoveType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ:自機補正"),
            new CEventMoveType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ:自機補正(のけぞり時)"),
            new CEventMoveType(0xFFFFFFFF, "ダウン時間"),
            new CEventMoveType(0xFFFFFFFF, "のけぞり方向"),
            new CEventMoveType(0xFFFFFFFF, "のけぞり速度タイプ"),
            new CEventMoveType(0xFFFFFFFF, "のけぞり速度"),
            new CEventMoveType(0xFFFFFFFF, "速度減速率"),
            new CEventMoveType(0xFFFFFFFF, "吹き飛び規模タイプ"),
            new CEventMoveType(0xFFFFFFFF, "吹き飛び規模"),
            new CEventMoveType(0xFFFFFFFF, "吹き飛び重力"),
            new CEventMoveType(0xFFFFFFFF, "バウンドLv"),
            new CEventMoveType(0xFFFFFFFF, "バウンド速度")
    };

    private List<WazInfoCollection> wazInfoCollectionList1;
    private List<WazInfoCollection> wazInfoCollectionList2;

    private Integer int1;

    @Override
    public int readInfo(byte[] bytes, int offset) {

        offset = super.readInfo(bytes, offset);

        List<WazInfoCollection> wazInfoCollectionList1 = new ArrayList<>();
        WazInfoCollection wazInfoCollection1 = new WazInfoCollection();
        offset = wazInfoCollection1.readCollection(bytes, offset);
        wazInfoCollectionList1.add(wazInfoCollection1);
        setWazInfoCollectionList1(wazInfoCollectionList1);

        List<WazInfoCollection> wazInfoCollectionList2 = new ArrayList<>();
        WazInfoCollection wazInfoCollection2 = new WazInfoCollection();
        offset = wazInfoCollection2.readCollection(bytes, offset);
        wazInfoCollectionList2.add(wazInfoCollection2);
        setWazInfoCollectionList2(wazInfoCollectionList2);

        setInt1(readInt32(bytes, offset)); offset += 4;

        return offset;
    }
}
