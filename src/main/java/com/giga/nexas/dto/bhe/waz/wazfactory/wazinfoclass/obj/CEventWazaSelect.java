package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * CEventWazaSelect__Read
 */
@Data
@NoArgsConstructor
public class CEventWazaSelect extends SkillInfoObject {

    private Integer int1;
    private Integer int2;

    public CEventWazaSelect(Integer typeId) {
        super(typeId);
    }

    public static final String[] CEVENT_WAZA_SELECT_FORMATS = {
            " ： "
    };

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
    }

}

