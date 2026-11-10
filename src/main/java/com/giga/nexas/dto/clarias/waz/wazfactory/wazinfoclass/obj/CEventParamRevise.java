package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.dto.clarias.waz.wazfactory.SkillInfoFactory.createCEventObjectByTypeClarias;

@Data
@NoArgsConstructor
public class CEventParamRevise extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventParamReviseType {
        private Integer type;
        private String description;
    }

    public static final CEventParamReviseType[] CEVENT_PARAM_REVISE_ENTRIES = {
            new CEventParamReviseType(0xFFFFFFFF, "チェックタイプ"),
            new CEventParamReviseType(0xFFFFFFFF, "チェックパラメータ"),
            new CEventParamReviseType(0x9, "チェック対象"),
            new CEventParamReviseType(0x9, "補正対象OBJ"),
            new CEventParamReviseType(0xFFFFFFFF, "補正タイプ"),
            new CEventParamReviseType(0xFFFFFFFF, "補正パラメータ"),
            new CEventParamReviseType(0xFFFFFFFF, "補正パラメータ(Kingdom)")
    };

    public static final String[] CEVENT_PARAM_REVISE_EXTRA_ENTRIES = {
            "バトルスキル",
            "(強化):メイン_拡大",
            "弾数",
            "移動速度",
            "リロード回復値",
            "リーチ",
            "爆風効果時間",
            "旋回速度",
            "ドローン：HP",
            "ドローン：弾速",
            "ゲージ回復速度",
            "ブースト回復",
            "交代スローLV",
            "汎用スキル:ジャスト回避スローLV",
            "強:速度上昇",
            "キャラスキル:必殺技:相殺魔痕増加",
            "魔法:回復量増加",
            "キャラスキル:魔法:氷結時間延長",
            "魔法:回数増加",
            "キャラスキル:通常攻撃:攻撃範囲拡大",
            "必殺技:ガード強化",
            "キャラスキル:必殺技:魔力非消費",
            "通常:速度上昇",
            "キャラスキル:汎用:スタン時移動速度上昇",
            "強:性能上昇",
            "キャラスキル:魔法:スロー"
    };

    public Integer fieldTableAddress = 0x00C37688;
    public Integer wrapperTableAddress = 0x00B63710;

    // Legacy records stop after four fixed ints. Extended records append the runtime cache
    // sentinel and expose the seventh (Kingdom) wrapper slot.
    private Integer flatInt0;   // 4 bytes
    private Integer flatInt1;   // 4 bytes
    private Integer flatInt2;   // 4 bytes
    private Integer flatInt3;   // 4 bytes
    private Integer runtimeCacheValue;

    @Data
    public static class CEventParamReviseUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventParamReviseUnit> unitList = new ArrayList<>();

    public CEventParamRevise(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.flatInt0 = reader.readInt();
        this.flatInt1 = reader.readInt();
        this.flatInt2 = reader.readInt();
        this.flatInt3 = reader.readInt();

        this.unitList.clear();
        int firstValue = reader.readInt();
        boolean extendedLayout = firstValue == -999;
        this.runtimeCacheValue = extendedLayout ? firstValue : null;
        int wrapperCount = extendedLayout ? 7 : 6;
        for (int i = 0; i < wrapperCount; i++) {
            int buffer = i == 0 && !extendedLayout ? firstValue : reader.readInt();
            CEventParamReviseUnit unit = new CEventParamReviseUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_PARAM_REVISE_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_PARAM_REVISE_ENTRIES[i].getType();
            if (buffer != 0 && innerTypeId != 0xFFFFFFFF) {
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                obj.readInfo(reader);
                unit.setData(obj);
            }
            this.unitList.add(unit);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.flatInt0);
        writer.writeInt(this.flatInt1);
        writer.writeInt(this.flatInt2);
        writer.writeInt(this.flatInt3);
        if (this.runtimeCacheValue != null) {
            writer.writeInt(this.runtimeCacheValue);
        }

        int wrapperCount = this.runtimeCacheValue == null ? 6 : 7;
        for (int i = 0; i < wrapperCount; i++) {
            CEventParamReviseUnit target = null;
            for (CEventParamReviseUnit unit : this.unitList) {
                if (unit.getUnitSlotNum() == i) {
                    target = unit;
                    break;
                }
            }
            writer.writeInt(target.getBuffer());
            if (target.getBuffer() != 0 && target.getData() != null) {
                target.getData().writeInfo(writer);
            }
        }
    }
}
