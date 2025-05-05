package com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description SpmCallingInfo
 * CEventSprite__Read
 *  该阶段中的各动作使用哪个spm图层，以及帧数关系
 *  ERROR:CMeka,CPartsじゃないのに自動指定を使った
 */
@Data
@NoArgsConstructor
public class CEventSprite extends SkillInfoObject {

    /**
     * 机体对应spm，但多为FF FF FF FF(-1)
     */
    private Integer spmFileSequence;

    /**
     * 第几号动作组
     */
    private Integer actionGroupNumber;

    /**
     * 该动作组的第几个动作
     */
    private Integer actionNumber;

    public CEventSprite(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.spmFileSequence = reader.readInt();
        this.actionGroupNumber = reader.readInt();
        this.actionNumber = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.spmFileSequence);
        writer.writeInt(this.actionGroupNumber);
        writer.writeInt(this.actionNumber);
    }

}
