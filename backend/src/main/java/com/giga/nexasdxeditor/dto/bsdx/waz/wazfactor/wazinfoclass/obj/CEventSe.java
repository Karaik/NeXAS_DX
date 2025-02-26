package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEvent__Read
 */
@Data
public class CEventSe extends WazInfoObject {

    private Integer int1;
    private Integer result;

    private List<byte[]> byteDataList;

    public CEventSe() {
        this.byteDataList = new ArrayList<>();
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        setInt1(readInt32(bytes, offset)); offset += 4;
        int result = readInt32(bytes, offset); offset += 4;
        setResult(result);
        if (result > 0) {
            List<byte[]> byteDataList = getByteDataList();
            int counter = result;
            do {
                // 读取16字节
                byte[] byteData = Arrays.copyOfRange(bytes, offset, offset + 16); offset += 16;
                byteDataList.add(byteData);
                --counter;
            } while (counter > 0);
        }

        return offset;
    }
}
