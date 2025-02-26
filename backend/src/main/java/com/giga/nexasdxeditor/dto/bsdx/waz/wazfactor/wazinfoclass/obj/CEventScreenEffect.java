package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventScreenEffect__Read
 */
@Data
public class CEventScreenEffect extends WazInfoObject {

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    private List<CEventScreenEffectCollectionUnit> ceventScreenEffectCollection;

    public CEventScreenEffect() {
        this.ceventScreenEffectCollection = new ArrayList<>();
    }

    @Data
    public static class CEventScreenEffectCollectionUnit {
        private Integer flag;
        private Integer data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        setInt1(readInt32(bytes, offset)); offset += 4;
        setInt2(readInt32(bytes, offset)); offset += 4;
        setInt3(readInt32(bytes, offset)); offset += 4;
        setInt4(readInt32(bytes, offset)); offset += 4;

        for (int i = 0; i < 5; i++) {
            if (i == 4) {
                CEventScreenEffectCollectionUnit unit = new CEventScreenEffectCollectionUnit();

                int flag = readInt32(bytes, offset); offset += 4;
                unit.setFlag(flag);
                if (flag != 0) {
                    unit.setData(readInt32(bytes, offset)); offset += 4;
                } else {
                    unit.setData(null);
                }
                ceventScreenEffectCollection.add(unit);
            }
        }

        return offset;
    }
}
