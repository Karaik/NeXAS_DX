package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

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
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        List<WazInfoCollection> wazInfoCollectionList = new ArrayList<>();

        WazInfoCollection wazInfoCollection = new WazInfoCollection();
        offset = wazInfoCollection.readCollection(bytes, offset);
        wazInfoCollectionList.add(wazInfoCollection);
        setWazInfoCollectionList(wazInfoCollectionList);

        setInt1(readInt32(bytes, offset)); offset += 4;
        setInt2(readInt32(bytes, offset)); offset += 4;
        setInt3(readInt32(bytes, offset)); offset += 4;
        setInt4(readInt32(bytes, offset)); offset += 4;
        setInt5(readInt32(bytes, offset)); offset += 4;
        setInt6(readInt32(bytes, offset)); offset += 4;

        return offset;
    }
}
