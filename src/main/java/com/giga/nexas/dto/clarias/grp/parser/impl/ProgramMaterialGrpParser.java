package com.giga.nexas.dto.clarias.grp.parser.impl;

import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.dto.clarias.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.clarias.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description
 */
public class ProgramMaterialGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "programmaterial";
    }

    @Override
    public Grp parse(BinaryReader reader) {
        // 创建结果对象
        ProgramMaterialGrp result = new ProgramMaterialGrp();

        // 读取三段数组的数组
        result.setArray1(readIntArraySegment(reader));
        result.setArray2(readIntArraySegment(reader));
        result.setArray3(readIntArraySegment(reader));

        return result;
    }

    /**
     * 读取一段“数组的数组”：int32 count -> repeat count: int32 len -> len * int32
     */
    private List<ProgramMaterialGrp.IntArray> readIntArraySegment(BinaryReader reader) {
        int segCount = reader.readInt();
        List<ProgramMaterialGrp.IntArray> list = new ArrayList<>(Math.max(segCount, 0));
        for (int i = 0; i < segCount; i++) {
            int len = reader.readInt();
            ProgramMaterialGrp.IntArray arr = new ProgramMaterialGrp.IntArray();
            List<Integer> values = arr.getValues();
            for (int k = 0; k < len; k++) {
                values.add(reader.readInt());
            }
            list.add(arr);
        }
        return list;
    }
}

