package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import com.giga.nexasdxeditor.io.BinaryReader;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description
 * CEventMove__Read
 */
@Data
public class CEventMove extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventMoveType {
        private Integer type;
        private String description;
    }

    public static final CEventMoveType[] CEVENT_MOVE_TYPES = {
            new CEventMoveType(0xFFFFFFFF, "開始位置"),
            new CEventMoveType(0xFFFFFFFF, "終了位置"),
            new CEventMoveType(0xFFFFFFFF, "移動タイプ")
    };

    private List<WazInfoCollection> wazInfoCollectionList1 = new ArrayList<>();
    private List<WazInfoCollection> wazInfoCollectionList2 = new ArrayList<>();

    private Integer int1;

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.wazInfoCollectionList1.clear();
        this.wazInfoCollectionList2.clear();

        WazInfoCollection wazInfoCollection1 = new WazInfoCollection();
        wazInfoCollection1.readCollection(reader);
        this.wazInfoCollectionList1.add(wazInfoCollection1);

        WazInfoCollection wazInfoCollection2 = new WazInfoCollection();
        wazInfoCollection2.readCollection(reader);
        this.wazInfoCollectionList2.add(wazInfoCollection2);

        this.int1 = reader.readInt();
    }
}
