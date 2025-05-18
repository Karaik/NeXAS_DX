package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bhe.BheInfoCollection;
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
 * @Date 2025/5/18
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

    private List<BheInfoCollection> bheInfoCollectionList1 = new ArrayList<>();
    private List<BheInfoCollection> bheInfoCollectionList2 = new ArrayList<>();

    private Integer int1;

    public CEventMove(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.bheInfoCollectionList1.clear();
        this.bheInfoCollectionList2.clear();

        BheInfoCollection bheInfoCollection1 = new BheInfoCollection();
        bheInfoCollection1.readCollection(reader);
        this.bheInfoCollectionList1.add(bheInfoCollection1);

        BheInfoCollection bheInfoCollection2 = new BheInfoCollection();
        bheInfoCollection2.readCollection(reader);
        this.bheInfoCollectionList2.add(bheInfoCollection2);

        this.int1 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        for (BheInfoCollection collection : bheInfoCollectionList1) {
            collection.writeCollection(writer);
        }
        for (BheInfoCollection collection : bheInfoCollectionList2) {
            collection.writeCollection(writer);
        }
        writer.writeInt(this.int1);
    }

}
