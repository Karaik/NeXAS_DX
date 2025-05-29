package com.giga.nexas.dto.bsdx;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description BsdxInfoCollection
 * 后续逆向发现不单独是针对waz文件的集合，ai部分也有用到
 *
 * 2025/5/29
 * 暂时可下结论，此为用于记录多个数值影响因子或行为参数的集合，
 * 它就像一个通用的“多参数行为描述器”，用来为不同的CEvent类型挂接自定义数值，
 * 实际反序列化样本中，所有SkillInfoObject类型似乎都可以挂接这个集合，这表明它是一个“标准附属结构”，
 * 用于为技能的每一帧或多帧事件CEvent附加可配置的数值数据，
 * 多数情况下，四个list长度一致，说明它们是一组组的平行字段，
 * 但也有例外，不过个人觉得应该是自己写错了导致的，当然这不影响大多数情况，
 * 以及，list34有可能全为空，这是否代表为非必须属性？暂时不得而知
 */
@Data
public class BsdxInfoCollection {

    private Integer int1;

    /**
     * 类型或属性ID（攻击种类、效果种类等）
     */
    private List<Integer> typeList;

    /**
     * 数值（伤害值、倍率、速度等）
     */
    private List<Integer> paramList;

    /**
     *
     */
    private List<Integer> intList3;

    /**
     *
     */
    private List<Integer> intList4;

    private Integer int2;

    public BsdxInfoCollection() {
        typeList = new ArrayList<>();
        paramList = new ArrayList<>();
        intList3 = new ArrayList<>();
        intList4 = new ArrayList<>();
    }

    public void readCollection(BinaryReader reader) {

        setInt1(reader.readInt());

        int count1 = reader.readInt();
        for (int i = 0; i < count1; i++) {
            typeList.add(reader.readInt());
        }

        int count2 = reader.readInt();
        for (int i = 0; i < count2; i++) {
            paramList.add(reader.readInt());
        }

        int count3 = reader.readInt();
        for (int i = 0; i < count3; i++) {
            intList3.add(reader.readInt());
        }

        int count4 = reader.readInt();
        for (int i = 0; i < count4; i++) {
            intList4.add(reader.readInt());
        }

        setInt2(reader.readInt());
    }

    public void writeCollection(BinaryWriter writer) throws  IOException {
        writer.writeInt(this.int1);

        writer.writeInt(typeList.size());
        for (Integer val : typeList) {
            writer.writeInt(val);
        }

        writer.writeInt(paramList.size());
        for (Integer val : paramList) {
            writer.writeInt(val);
        }

        writer.writeInt(intList3.size());
        for (Integer val : intList3) {
            writer.writeInt(val);
        }

        writer.writeInt(intList4.size());
        for (Integer val : intList4) {
            writer.writeInt(val);
        }

        writer.writeInt(this.int2);
    }

}
