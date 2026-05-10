package com.giga.nexas.dto.clarias.grp.generator.impl;

import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.dto.clarias.grp.generator.GrpFileGenerator;
import com.giga.nexas.dto.clarias.grp.groupmap.MapGroupGrp;
import com.giga.nexas.io.BinaryWriter;

import java.io.IOException;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/16
 * @Description
 */
public class MapGroupGrpGenerator implements GrpFileGenerator<Grp> {

    @Override
    public String getGeneratorKey() {
        return "mapgroup";
    }

    @Override
    public void generate(BinaryWriter writer, Grp grp) {
        MapGroupGrp obj = (MapGroupGrp) grp;
        try {
            writer.writeInt(obj.getGroupList().size());
            for (MapGroupGrp.MapGroup group : obj.getGroupList()) {
                writer.writeInt(group.getExistFlag());
                if (group.getExistFlag() != 0) {
                    writer.writeNullTerminatedString(group.getGroupName());
                    writer.writeNullTerminatedString(group.getGroupCodeName());
                    writer.writeNullTerminatedString(group.getGroupResourceName());
                    writer.writeInt(group.getInt1());

                    writer.writeInt(group.getItems().size());
                    for (MapGroupGrp.Item item : group.getItems()) {
                        writer.writeInt(item.getInt1());
                        writer.writeInt(item.getInt2());
                        writer.writeInt(item.getInt3());
                        writer.writeInt(item.getInt4());
                    }

                    writeIntArraySegment(writer, group.getArray1());
                    writeIntArraySegment(writer, group.getArray2());
                    writeIntArraySegment(writer, group.getArray3());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate MapGroup.grp", e);
        }
    }

    private void writeIntArraySegment(BinaryWriter writer, java.util.List<MapGroupGrp.IntArray> list) throws IOException {
        writer.writeInt(list.size());
        for (MapGroupGrp.IntArray arr : list) {
            writer.writeInt(arr.getValues().size());
            for (Integer v : arr.getValues()) {
                writer.writeInt(v);
            }
        }
    }

}

