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
 * CEventBlur__Read
 */
@Data
@NoArgsConstructor
public class CEventBlur extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventBlurType {
        private Integer type;
        private String description;
    }

    public static final CEventBlurType[] CEVENT_BLUR_ENTRIES = {

            // todo unsure
            new CEventBlurType(0xFFFFFFFF, "%3d(%3d,%3d)"),
            new CEventBlurType(0xFFFFFFFF, "%3d(%3d,%3d)"),
            new CEventBlurType(0xFFFFFFFF, "⇒ %3d(%3d,%3d)"),
    };

    private List<BheInfoCollection> bheInfoCollectionList = new ArrayList<>();

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;

    public CEventBlur(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        List<BheInfoCollection> bheInfoCollectionList = new ArrayList<>();

        BheInfoCollection bheInfoCollection = new BheInfoCollection();
        bheInfoCollection.readCollection(reader);
        bheInfoCollectionList.add(bheInfoCollection);
        setBheInfoCollectionList(bheInfoCollectionList);

        setInt1(reader.readInt());
        setInt2(reader.readInt());
        setInt3(reader.readInt());
        setInt4(reader.readInt());
        setInt5(reader.readInt());
        setInt6(reader.readInt());
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        for (BheInfoCollection collection : bheInfoCollectionList) {
            collection.writeCollection(writer);
        }
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeInt(this.int5);
        writer.writeInt(this.int6);
    }


}
