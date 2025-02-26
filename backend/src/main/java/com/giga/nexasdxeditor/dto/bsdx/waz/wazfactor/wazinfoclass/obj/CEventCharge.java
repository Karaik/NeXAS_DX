package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventCharge__Read
 */
@Data
public class CEventCharge extends WazInfoObject {

    private List<CEventChargeCollectionUnit> ceventChargeCollection;

    public CEventCharge() {
        this.ceventChargeCollection = new ArrayList<>();
    }

    @Data
    public static class CEventChargeCollectionUnit {
        private Integer flag;
        private Integer data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        for (int i = 0; i < 2; i++) {
            CEventChargeCollectionUnit unit = new CEventChargeCollectionUnit();

            int flag = readInt32(bytes, offset); offset += 4;
            unit.setFlag(flag);
            if (flag != 0) {
                // TODO
                unit.setData(readInt32(bytes, offset)); offset += 4;
            } else {
                unit.setData(null);
            }
            ceventChargeCollection.add(unit);
        }

        return offset;
    }
}

