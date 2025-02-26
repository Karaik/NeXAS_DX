package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * @Description CEventCpuButton
 * CEventCpuButton__Read
 */
@Data
public class CEventCpuButton extends WazInfoObject {

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Short short1;
    private Short short2;
    private List<CEventCpuButtonCollectionUnit> ceventCpuButtonCollection;

    public CEventCpuButton() {
        this.ceventCpuButtonCollection = new ArrayList<>();
    }

    @Data
    public static class CEventCpuButtonCollectionUnit {
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

        setShort1(readInt16(bytes, offset)); offset += 2;
        setShort2(readInt16(bytes, offset)); offset += 2;

        for (int i = 0; i < 8; i++) {
            CEventCpuButtonCollectionUnit unit = new CEventCpuButtonCollectionUnit();

            if (i == 0) {
                int flag = readInt32(bytes, offset); offset += 4;
                unit.setFlag(flag);
                if (flag != 0) {
                    // todo
                    unit.setData(readInt32(bytes, offset)); offset += 4;
                } else {
                    unit.setData(null);
                }
                ceventCpuButtonCollection.add(unit);
            }
        }

        return offset;
    }
}
