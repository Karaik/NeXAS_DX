package com.giga.nexas.dto.clarias.grp.parser.impl;

import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.dto.clarias.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.clarias.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 杩欎綅鍚屽(Karaik)
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

            // 璇诲彇瀛樺湪鏍囧織
            int existFlag = reader.readInt();
            group.setExistFlag(existFlag);

            if (existFlag != 0) {
                // 璇诲彇缁勫悕
                group.setGroupName(reader.readNullTerminatedString());
                // 璇诲彇缁勪唬鍙?
                group.setGroupCodeName(reader.readNullTerminatedString());
                // 璇诲彇璧勬簮鍚?
                group.setGroupResourceName(reader.readNullTerminatedString());
                // 鏈煡鏁村瀷1
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

                // 璇诲彇涓夋
                group.setArray1(readIntArraySegment(reader));
                group.setArray2(readIntArraySegment(reader));
                group.setArray3(readIntArraySegment(reader));
            }

            // 鍔犲叆缁撴灉
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

