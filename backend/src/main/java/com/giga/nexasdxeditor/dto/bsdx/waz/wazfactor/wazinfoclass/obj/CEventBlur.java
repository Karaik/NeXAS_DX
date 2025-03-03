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
public class CEventBlur extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventBlurType {
        private String description;
        private String value;
    }

    public static final CEventBlurType[] CEVENT_BLUR_ENTRIES = {
            new CEventBlurType("POSITION", "位置"),
            new CEventBlurType("DIRECTION", "方向"),
            new CEventBlurType("DISTANCE", "距離"),
            new CEventBlurType("HEIGHT", "高さ"),
            new CEventBlurType("FLAG", "フラグ"),

            new CEventBlurType("FORMAT_S", "%s"),
            new CEventBlurType("FORMAT_SS", "%s:%s"),
            new CEventBlurType("FORMAT_DISTANCE", "%s:(距離)%s"),
            new CEventBlurType("FORMAT_HEIGHT", "%s:(高度)%s")
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
