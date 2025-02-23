package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazInfoObject;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description 逆向时该虚函数调用下的字符串信息，一并记录
 * CEventSpriteYure__Read
 * 10h
 * 追加タイプ
 * 対象タイプ
 * 最大HP
 * ブースター数
 * FCゲージ数
 * 打撃攻撃力
 * 射撃攻撃力
 * 打撃防御力
 * 射撃防御力
 * 移動速度：歩行
 * 移動速度：ダッシュ
 * 移動速度：ブーストダッシュ
 * 移動速度：サーチダッシュ
 * 攻撃速度：打撃
 * 攻撃速度：射撃
 * 熱冷却速度
 * 根性値
 * データタイプ
 * 終了タイプ
 * 終了カウント
 * 開始時間
 * 実行間隔
 * 優先順位
 * 重複タイプ
 * エフェクト
 */
@Data
public class CEventSpriteYure extends WazInfoObject {

    private List<WazInfoCollection> wazInfoCollectionList;

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    public CEventSpriteYure() {
        wazInfoCollectionList = new ArrayList<>();
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {

        offset += super.readInfo(bytes, offset);

        WazInfoCollection wazInfoCollection = new WazInfoCollection();
        offset += wazInfoCollection.readCollection(bytes, offset);
        wazInfoCollectionList.add(wazInfoCollection);

        setInt1(readInt32(bytes, offset));
        offset += 4;
        setInt2(readInt32(bytes, offset));
        offset += 4;
        setInt3(readInt32(bytes, offset));
        offset += 4;
        setInt4(readInt32(bytes, offset));
        offset += 4;

        return offset;
    }
}
