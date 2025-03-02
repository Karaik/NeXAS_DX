package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventRadialLine__Read
 */
@Data
public class CEventRadialLine extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventRadialLineType {
        private Integer type;
        private String description;
    }

    public static final CEventRadialLineType[] CEVENT_RADIAL_LINE_TYPES = {
            new CEventRadialLineType(0xFFFFFFFF, "フェード描画"),
            new CEventRadialLineType(0xFFFFFFFF, "フラッシュ描画"),
            new CEventRadialLineType(0xFFFFFFFF, "フェード色指定描画"),
            new CEventRadialLineType(0xFFFFFFFF, "フラッシュ色指定描画"),
            new CEventRadialLineType(0xFFFFFFFF, "ネガティブ描画"),
            new CEventRadialLineType(0xFFFFFFFF, "アルファ塗りつぶし描画"),
            new CEventRadialLineType(0xFFFFFFFF, "加算塗りつぶし描画"),
            new CEventRadialLineType(0xFFFFFFFF, "減算塗りつぶし描画")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;
    private Integer int10;
    private Integer int11;
    private Integer int12;

    private List<CEventRadialLineUnit> ceventRadialLineUnitList;

    @Data
    public static class CEventRadialLineUnit {
        private Integer buffer;
        private WazInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        setInt1(readInt32(bytes, offset)); offset += 4;
        setInt2(readInt32(bytes, offset)); offset += 4;
        setInt3(readInt32(bytes, offset)); offset += 4;
        setInt4(readInt32(bytes, offset)); offset += 4;
        setInt5(readInt32(bytes, offset)); offset += 4;
        setInt6(readInt32(bytes, offset)); offset += 4;
        setInt7(readInt32(bytes, offset)); offset += 4;
        setInt8(readInt32(bytes, offset)); offset += 4;
        setInt9(readInt32(bytes, offset)); offset += 4;
        setInt10(readInt32(bytes, offset)); offset += 4;
        setInt11(readInt32(bytes, offset)); offset += 4;
        setInt12(readInt32(bytes, offset)); offset += 4;

        List<CEventRadialLineUnit> ceventRadialLineUnitList = new ArrayList<>();

        for (int i = 0; i < 13; i++) {
            if (i == 4) {
                int buffer = readInt32(bytes, offset); offset += 4;
                CEventRadialLineUnit unit = new CEventRadialLineUnit();
                unit.setBuffer(buffer);

                if (buffer != 0) {
                    WazInfoObject obj = createCEventObjectByType(5);
                    if (obj != null) {
                        offset = obj.readInfo(bytes, offset);
                        unit.setData(obj);
                    }
                }
                ceventRadialLineUnitList.add(unit);
            }
        }

        setCeventRadialLineUnitList(ceventRadialLineUnitList);

        return offset;
    }
}


