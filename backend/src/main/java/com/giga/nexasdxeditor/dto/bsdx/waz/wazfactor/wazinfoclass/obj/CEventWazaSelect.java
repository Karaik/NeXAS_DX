package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.Data;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/28
 * CEventWazaSelect__Read
 */
@Data
public class CEventWazaSelect extends SkillInfoObject {

    private Integer int1;
    private Integer int2;

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

