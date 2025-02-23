package com.giga.nexasdxeditor.dto.bsdx.grp.parser;

import com.giga.nexasdxeditor.dto.bsdx.grp.GroupMap;
import com.giga.nexasdxeditor.exception.BusinessException;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.findNullTerminator;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/2
 * @Description GroupMapParser
 */
public class GroupMapParser {

    public List<GroupMap> parseGrp(byte[] bytes, String charset) throws BusinessException {
        List<GroupMap> groupMapList = new ArrayList<>();

        int offset = 0;
        int grpQuantity = readInt32(bytes, offset);
        offset += 4;
        for (int i = 0; offset < bytes.length - 4; i++) {

            if (readInt32(bytes, offset) == 1) {
                offset += 4;
            } else{
                throw new BusinessException(500, "grp文件解析错误！");
            }

            GroupMap groupMap = new GroupMap();

            groupMap.setItemQuantity(i);

            groupMap.setFileName(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)).replace(".spm",""));
            offset += findNullTerminator(bytes, offset) + 1;

            groupMap.setCodeName(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)));
            offset += findNullTerminator(bytes, offset) + 1;

            groupMapList.add(groupMap);

            while (true) {
                if (readInt32(bytes, offset) == 1) {
                    break;
                }

                if (readInt32(bytes, offset) == 0) {
                    offset += 4;
                } else {
                    break;
                }
            }

        }

        return groupMapList;
    }

}
