package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description CEventStatus
 * CEventStatus__Read
 */
@Data
public class CEventStatus extends WazInfoObject {

    private byte[] byteData1;
    private byte[] byteData2;
    private byte byte1;
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;

    private List<CEventStatusCollectionUnit> ceventStatusCollection; // 不确定是否全为int，因为调用了虚函数的read

    public CEventStatus() {
        this.ceventStatusCollection = new ArrayList<>();
    }

    @Data
    public static class CEventStatusCollectionUnit {
        private Integer flag;
        private Integer data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        // 读取0xFu字节
        byteData1 = Arrays.copyOfRange(bytes, offset, offset + 15); offset += 15;
        // 读取0x3Cu字节
        byteData2 = Arrays.copyOfRange(bytes, offset, offset + 60); offset += 60;

        setByte1(readInt8(bytes, offset)); offset += 1;

        setInt1(readInt32(bytes, offset)); offset += 4;
        setInt2(readInt32(bytes, offset)); offset += 4;
        setInt3(readInt32(bytes, offset)); offset += 4;
        setInt4(readInt32(bytes, offset)); offset += 4;
        setInt5(readInt32(bytes, offset)); offset += 4;
        setInt6(readInt32(bytes, offset)); offset += 4;
        setInt7(readInt32(bytes, offset)); offset += 4;
        setInt8(readInt32(bytes, offset)); offset += 4;
        setInt9(readInt32(bytes, offset)); offset += 4;

        for (int i = 0; i < 25; i++) {
            CEventStatusCollectionUnit unit = new CEventStatusCollectionUnit();

            int flag = readInt32(bytes, offset); offset += 4;
            unit.setFlag(flag);
            if (flag != 0) {
                // TODO 极有可能出错
                unit.setData(readInt32(bytes, offset)); offset += 4;
            } else {
                unit.setData(null);
            }
            ceventStatusCollection.add(unit);
        }

        return offset;
    }
}
