package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory.createCEventObjectByType;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * @Description CEventCpuButton
 * CEventCpuButton__Read
 */
@Data
@NoArgsConstructor
public class CEventCpuButton extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventCpuButtonType {
        private Integer type;
        private String description;
    }

    public static final CEventCpuButtonType[] CEVENT_CPU_BUTTON_ENTRIES = {
            new CEventCpuButtonType(0x9, "チェック方向"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック方向補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック範囲"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック範囲（高さ）"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック高度補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック回数"),
            new CEventCpuButtonType(0xFFFFFFFF, "ボタン入力（範囲内）"),
            new CEventCpuButtonType(0xFFFFFFFF, "ボタン入力（範囲外）")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Short short1;
    private Short short2;

    private List<CEventCpuButtonUnit> ceventCpuButtonUnitList = new ArrayList<>();

    public CEventCpuButton(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventCpuButtonUnit {
        private Integer ceventCpuButtonUnitQuantity;
        private String description;
        private Integer buffer;
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.int5 = reader.readInt();

        this.short1 = reader.readShort();
        this.short2 = reader.readShort();

        this.ceventCpuButtonUnitList.clear();

        for (int i = 0; i < 8; i++) {

            if (i == 0) {
                CEventCpuButtonUnit unit = new CEventCpuButtonUnit();
                int buffer = reader.readInt();
                unit.setCeventCpuButtonUnitQuantity(i);
                unit.setDescription(CEVENT_CPU_BUTTON_ENTRIES[i].getDescription());
                unit.setBuffer(buffer);

                if (buffer != 0) {
                    SkillInfoObject obj = createCEventObjectByType(CEVENT_CPU_BUTTON_ENTRIES[i].getType());
                    if (obj != null) {
                        obj.readInfo(reader);
                        unit.setData(obj);
                    }
                }
                this.ceventCpuButtonUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeInt(this.int5);
        writer.writeShort(this.short1);
        writer.writeShort(this.short2);
        for (CEventCpuButtonUnit unit : ceventCpuButtonUnitList) {
            writer.writeInt(unit.getBuffer());
            if (unit.getBuffer() != 0 && unit.getData() != null) {
                unit.getData().writeInfo(writer);
            }
        }
    }

}
