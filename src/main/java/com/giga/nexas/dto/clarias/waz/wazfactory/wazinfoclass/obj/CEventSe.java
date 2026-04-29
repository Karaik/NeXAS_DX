package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CEventSe extends SkillInfoObject {

    private Integer count;
    private List<byte[]> byteDataList = new ArrayList<>();

    public CEventSe(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.count = reader.readInt();

        if (this.count > 0) {
            this.byteDataList.clear();
            int counter = this.count;
            while (counter > 0) {
                this.byteDataList.add(reader.readBytes(16));
                counter--;
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.count);
        for (byte[] byteData : this.byteDataList) {
            writer.writeBytes(byteData);
        }
    }
}
