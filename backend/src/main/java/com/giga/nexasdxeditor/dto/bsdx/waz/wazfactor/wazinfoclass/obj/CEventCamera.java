package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazaBlockTypeEntry;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt16;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventCamera__Read
 */
@Data
public class CEventCamera extends WazInfoObject {

    private Short short1;
    private List<CEventCameraCollectionUnit> ceventCameraCollection;

    public CEventCamera() {
        this.ceventCameraCollection = new ArrayList<>();
    }

    @Data
    public static class CEventCameraCollectionUnit {
        private Integer flag;
        private WazInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        setShort1(readInt16(bytes, offset)); offset += 2;

        WazaBlockTypeEntry wazaBlockTypeEntry = null;
        for (int i = 0; i < 5; i++) {
            CEventCameraCollectionUnit unit = new CEventCameraCollectionUnit();

            int flag = readInt32(bytes, offset); offset += 4;
            unit.setFlag(flag);
            if (flag != 0) {
                if (unit.getData() == null) {
                    // TODO *v6 = makeObjectOfType(dword_7F1F7C[2 * v5]);
                    wazaBlockTypeEntry = WazInfoFactory.WAZA_BLOCK_TYPE_ENTRIES[2 * i];
                }
                WazInfoObject ceventObjectByType = WazInfoFactory.createCEventObjectByType(wazaBlockTypeEntry.getType());
                offset = ceventObjectByType.readInfo(bytes, offset);
                unit.setData(ceventObjectByType);
            }
            ceventCameraCollection.add(unit);
        }

        return offset;
    }
}

