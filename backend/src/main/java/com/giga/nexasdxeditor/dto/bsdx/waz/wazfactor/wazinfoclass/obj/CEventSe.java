package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEvent__Read
 */
@Data
public class CEventSe extends SkillInfoObject {

    private Integer count;

    private List<byte[]> byteDataList = new ArrayList<>();

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        int count = reader.readInt();
        setCount(count);

        if (count > 0) {
            this.byteDataList.clear();
            while (count > 0) {
                byte[] byteData = reader.readBytes(16); // 读取 16 字节
                this.byteDataList.add(byteData);
                count--;
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.count);
        for (byte[] byteData : byteDataList) {
            writer.writeBytes(byteData);
        }
    }

}
