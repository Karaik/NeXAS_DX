package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

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
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.int1 = readInt32(bytes, offset); offset += 4;
        this.int2 = readInt32(bytes, offset); offset += 4;

        return offset;
    }
}

