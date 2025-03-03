package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventScreenEffect__Read
 */
@Data
public class CEventScreenEffect extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventScreenEffectType {
        private Integer type;
        private String description;
    }

    public static final CEventScreenEffectType[] CEVENT_SCREEN_EFFECT_TYPES = {
            new CEventScreenEffectType(0xFFFFFFFF, "持続カウンタ"),
            new CEventScreenEffectType(0xFFFFFFFF, "変化フレーム数"),
            new CEventScreenEffectType(0xFFFFFFFF, "数"),
            new CEventScreenEffectType(0x9, "角度"),
            new CEventScreenEffectType(0xFFFFFFFF, "速度")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    private List<CEventScreenEffectUnit> ceventScreenEffectUnitList = new ArrayList<>();

    @Data
    public static class CEventScreenEffectUnit {
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

        this.ceventScreenEffectUnitList.clear();

        for (int i = 0; i < 5; i++) {
            if (i == 4) {
                int buffer = readInt32(bytes, offset); offset += 4;
                CEventScreenEffectUnit unit = new CEventScreenEffectUnit();
                unit.setBuffer(buffer);

                if (buffer != 0) {
                    int typeId = CEVENT_SCREEN_EFFECT_TYPES[i].getType();
                    WazInfoObject obj = createCEventObjectByType(typeId);
                    if (obj != null) {
                        offset = obj.readInfo(bytes, offset);
                        unit.setData(obj);
                    }
                }
                this.ceventScreenEffectUnitList.add(unit);
            }
        }

        return offset;
    }
}
