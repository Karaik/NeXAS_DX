package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * @Description CEventAttr
 */
@Data
@NoArgsConstructor
public class CEventAttr extends SkillInfoObject {

    private Short short1;

    public CEventAttr(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.short1 = reader.readShort();
    }
}
