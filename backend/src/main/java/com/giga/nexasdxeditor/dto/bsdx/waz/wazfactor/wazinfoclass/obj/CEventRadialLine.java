package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventRadialLine__Read
 */
@Data
public class CEventRadialLine extends WazInfoObject {

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
    private Integer int12;
    private Integer int13;

    private List<CEventRadialLineCollectionUnit> ceventRadialLineCollection;

    public CEventRadialLine() {
        this.ceventRadialLineCollection = new ArrayList<>();
    }

    @Data
    public static class CEventRadialLineCollectionUnit {
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
        setInt12(readInt32(bytes, offset)); offset += 4;
        setInt13(readInt32(bytes, offset)); offset += 4;

        for (int i = 0; i < 13; i++) {
            if (i == 4) {
                CEventRadialLineCollectionUnit unit = new CEventRadialLineCollectionUnit();

                int flag = readInt32(bytes, offset);
                offset += 4;
                unit.setFlag(flag);
                if (flag != 0) {
                    unit.setData(readInt32(bytes, offset)); offset += 4;
                } else {
                    unit.setData(null);
                }
                ceventRadialLineCollection.add(unit);
            }
        }

        return offset;
    }
}


