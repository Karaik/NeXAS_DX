package com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description
 * CEventMove__Read
 */
@Data
@NoArgsConstructor
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

    private List<BsdxInfoCollection> bsdxInfoCollectionList1 = new ArrayList<>();
    private List<BsdxInfoCollection> bsdxInfoCollectionList2 = new ArrayList<>();

    private Integer int1;

    public CEventMove(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.bsdxInfoCollectionList1.clear();
        this.bsdxInfoCollectionList2.clear();

        BsdxInfoCollection bsdxInfoCollection1 = new BsdxInfoCollection();
        bsdxInfoCollection1.readCollection(reader);
        this.bsdxInfoCollectionList1.add(bsdxInfoCollection1);

        BsdxInfoCollection bsdxInfoCollection2 = new BsdxInfoCollection();
        bsdxInfoCollection2.readCollection(reader);
        this.bsdxInfoCollectionList2.add(bsdxInfoCollection2);

        this.int1 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        for (BsdxInfoCollection collection : bsdxInfoCollectionList1) {
            collection.writeCollection(writer);
        }
        for (BsdxInfoCollection collection : bsdxInfoCollectionList2) {
            collection.writeCollection(writer);
        }
        writer.writeInt(this.int1);
    }

}
