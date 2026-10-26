package com.giga.nexas.dto.clarias.grp.parser.impl;

import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.dto.clarias.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.clarias.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description
 */
public class MapGroupGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "mapgroup";
    }

    @Override
    public Grp parse(BinaryReader reader) {
        MapGroupGrp result = new MapGroupGrp();

        int groupCount = reader.readInt();
        for (int gi = 0; gi < groupCount; gi++) {
            MapGroupGrp.MapGroup group = new MapGroupGrp.MapGroup();

            // 读取存在标志
            int existFlag = reader.readInt();
            group.setExistFlag(existFlag);

            if (existFlag != 0) {
                // 读取组名
                group.setGroupName(reader.readNullTerminatedString());
                // 读取组代号
                group.setGroupCodeName(reader.readNullTerminatedString());
                // 读取资源名
                group.setGroupResourceName(reader.readNullTerminatedString());
                // 未知整型1
                group.setInt1(reader.readInt());

                int itemCount = reader.readInt();
                List<MapGroupGrp.Item> items = new ArrayList<>(Math.max(itemCount, 0));
                for (int ii = 0; ii < itemCount; ii++) {
                    MapGroupGrp.Item item = new MapGroupGrp.Item();
                    item.setInt1(reader.readInt());
                    item.setInt2(reader.readInt());
                    item.setInt3(reader.readInt());
                    item.setInt4(reader.readInt());
                    items.add(item);
                }
                group.setItems(items);

                // 读取三段
                group.setArray1(readIntArraySegment(reader));
                group.setArray2(readIntArraySegment(reader));
                group.setArray3(readIntArraySegment(reader));
            }

            // 加入结果
            result.getGroupList().add(group);
        }

        return result;
    }

    private List<MapGroupGrp.IntArray> readIntArraySegment(BinaryReader reader) {
        int segCount = reader.readInt();
        List<MapGroupGrp.IntArray> list = new ArrayList<>(Math.max(segCount, 0));
        for (int i = 0; i < segCount; i++) {
            int len = reader.readInt();
            MapGroupGrp.IntArray arr = new MapGroupGrp.IntArray();
            List<Integer> values = arr.getValues();
            for (int k = 0; k < len; k++) {
                values.add(reader.readInt());
            }
            list.add(arr);
        }
        return list;
    }

}

