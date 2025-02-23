package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazInfoObject;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description CEventHeight
 * CEventHeight__Read
 * 攻撃グループ 10h
 * 攻撃グループ2 10h
 * ヒットカウンタ初期化
 * フラグ 10h
 * ヒット数
 * ヒット間隔
 * 攻撃力 10h
 * 攻撃力最低値 10h
 * 攻撃力溜め反映率
 * 装甲攻撃力 10h
 * 基底コンボ補正値
 * 攻撃力補正：技のみ
 * 攻撃力補正
 * 攻撃力補正：技終了時
 * 自分停止時間
 * 消滅時間
 * ヒットエフェクト
 *
 * 以下对齐为0？
 * のけぞり（地上→地上）
 * のけぞり（空中）
 * のけぞり（空中→地上）
 * のけぞり（ダウン）
 * ヒットストップ
 * ヒットストップ（自機）
 * スロー反映率
 * 画面：拡大縮小時間
 * 画面：拡大縮小
 * 画面：振動時間
 * 画面：振動
 * ヒットＳＥ
 * 鉄ヒットＳＥ
 * ダメージ色時間
 * キャンセルフラグ
 */
@Data
public class CEventHeight extends WazInfoObject {

    private Integer int1;

    private List<Integer> typeCollection;

    public CEventHeight() {
        this.typeCollection = new ArrayList<>();
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset += super.readInfo(bytes, offset);

        setInt1(readInt32(bytes, offset));
        offset += 4;

        for (int i = 0; i < 3; i++) {
            int flag = readInt32(bytes, offset);
            offset += 4;
            if (flag != 0) {
                typeCollection.add(readInt32(bytes, offset));
                offset += 4;
            }
        }

        return offset;
    }
}
