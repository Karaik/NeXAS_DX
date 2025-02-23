package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazInfoObject;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description 逆向时该虚函数调用下的字符串信息，一并记录
 * CEventMove__Read
 * (10h)
 * 処理タイプ
 * フラグ
 * 停止時間
 * ﾋｯﾄﾊﾞｯｸ
 * ﾋｯﾄﾊﾞｯｸ(のけぞり時)
 * ﾋｯﾄﾊﾞｯｸ:自機補正
 * ﾋｯﾄﾊﾞｯｸ:自機補正(のけぞり時)
 * ダウン時間
 * のけぞり方向
 * のけぞり速度タイプ
 * のけぞり速度
 * 速度減速率
 * 吹き飛び規模タイプ
 * 吹き飛び規模
 * 吹き飛び重力
 * バウンドLv
 * バウンド速度
 */
@Data
public class CEventMove extends WazInfoObject {

    private List<WazInfoCollection> wazInfoCollectionList;

    private Integer int1;

    public CEventMove() {
        wazInfoCollectionList = new ArrayList<>();
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {

        offset += super.readInfo(bytes, offset);

        WazInfoCollection wazInfoCollection1 = new WazInfoCollection();
        offset += wazInfoCollection1.readCollection(bytes, offset);
        wazInfoCollectionList.add(wazInfoCollection1);

        WazInfoCollection wazInfoCollection2 = new WazInfoCollection();
        offset += wazInfoCollection2.readCollection(bytes, offset);
        wazInfoCollectionList.add(wazInfoCollection2);

        setInt1(readInt32(bytes, offset));
        offset += 4;

        return offset;
    }
}
