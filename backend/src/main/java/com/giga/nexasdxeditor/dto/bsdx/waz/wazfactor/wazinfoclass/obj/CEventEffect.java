package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventEffect__Read
 */
@Data
public class CEventEffect extends WazInfoObject {

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;
    private Integer int10;
    private Integer int11;

    private List<CEventEffectCollectionUnit> ceventEffectCollection;

    public CEventEffect() {
        this.ceventEffectCollection = new ArrayList<>();
    }

    @Data
    public static class CEventEffectCollectionUnit {
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
        setInt5(readInt32(bytes, offset)); offset += 4;
        setInt6(readInt32(bytes, offset)); offset += 4;
        setInt7(readInt32(bytes, offset)); offset += 4;
        setInt8(readInt32(bytes, offset)); offset += 4;
        setInt9(readInt32(bytes, offset)); offset += 4;
        setInt10(readInt32(bytes, offset)); offset += 4;
        setInt11(readInt32(bytes, offset)); offset += 4;

        for (int i = 0; i < 45; i++) {
            CEventEffectCollectionUnit unit = new CEventEffectCollectionUnit();

            int flag = readInt32(bytes, offset); offset += 4;
            unit.setFlag(flag);
            if (flag != 0) {
                unit.setData(readInt32(bytes, offset)); offset += 4;
            } else {
                unit.setData(null);
            }
            ceventEffectCollection.add(unit);
        }

        return offset;
    }
}

