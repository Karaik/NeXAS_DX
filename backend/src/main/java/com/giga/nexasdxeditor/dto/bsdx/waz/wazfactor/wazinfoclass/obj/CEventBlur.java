package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import com.giga.nexasdxeditor.io.BinaryReader;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * CEventBlur__Read
 */
@Data
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

    private List<WazInfoCollection> wazInfoCollectionList;

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;

    public CEventBlur() {
        wazInfoCollectionList = new ArrayList<>();
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        List<WazInfoCollection> wazInfoCollectionList = new ArrayList<>();

        WazInfoCollection wazInfoCollection = new WazInfoCollection();
        wazInfoCollection.readCollection(reader);
        wazInfoCollectionList.add(wazInfoCollection);
        setWazInfoCollectionList(wazInfoCollectionList);

        setInt1(reader.readInt());
        setInt2(reader.readInt());
        setInt3(reader.readInt());
        setInt4(reader.readInt());
        setInt5(reader.readInt());
        setInt6(reader.readInt());
    }

}
