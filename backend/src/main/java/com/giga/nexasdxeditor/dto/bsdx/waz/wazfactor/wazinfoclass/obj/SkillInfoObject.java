package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.Data;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description WazInfoObject
 */
@Data
public class SkillInfoObject {

    public Integer offset;
    /**
     * 第几帧开始
     */
    private Integer startFrame;

    /**
     * 第几帧结束
     */
    private Integer endFrame;

    public void readInfo(BinaryReader reader) {

        this.offset = reader.getPosition();

        this.startFrame = reader.readInt();
        this.endFrame = reader.readInt();
    }

    public void writeInfo(BinaryWriter writer) throws IOException {
        writer.writeInt(this.startFrame);
        writer.writeInt(this.endFrame);
    }

}
