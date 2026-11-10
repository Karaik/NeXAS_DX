package com.giga.nexas.dto.bhe;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * @Description BheInfoCollection
 * 后续逆向发现不单独是针对waz文件的集合，ai部分也有用到
 * 与 term.grp 强相关
 */
@Data
public class BheInfoCollection {

    // todo
    // 20260331 5
    // int1=termList[0]，int1为term的序列号（index）
    // 第一个语义数为 typelist[0]，若typelist[0].param2 > 0（n），则跳转到 typelist[n]，然后找到 typelist[n].[param2]
    // 以此类推，直到迭代整个typelist，凑出完整语义
    // 凑出语义后，查看最后一个元素的param2，
    //      若为负数，则语义拼凑结束，将 paramlist内的数，按顺序填入到占位符
    //      若为正数，（不可能出现）
    // 此处运算子要修复运算逻辑，保证bhe和bsdx语义一致，至少要保证语义有含义（无错误） *point！！！！！！！
    private Integer int1;
    private List<Integer> typeList;
    private List<Integer> paramList;

    // todo
    // 20260331 4
    // term.grp 中 オブジェクト位置 的关联data
    // 以下两个list的元素个数都为 2 or null，
    // 元素1为（オブジェクト位置：OBJECTPOS），直接使用bhe中的termItemDescription，去bsdx内寻找key，找不到则重定向为 1（標的：ROCK）
    // 元素2为（オブジェクト位置2：OBJECTPOS2），直接使用bhe中的termItemDescription，去bsdx内寻找key，找不到则重定向为 0（追加処理無し）
    private List<Integer> intList3;
    private List<Integer> intList4;
    // 尚不清楚含义 or 或者 and？？？
    private Integer int2;

    public BheInfoCollection() {
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
