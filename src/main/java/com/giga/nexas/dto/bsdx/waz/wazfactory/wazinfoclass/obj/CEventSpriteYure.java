package com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description
 * CEventSpriteYure__Read
 */
@Data
@NoArgsConstructor
public class CEventSpriteYure extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventSpriteYureType {
        private Integer type;
        private String description;
    }

    /**
     * 此为第一个属性的枚举，直接逆向所得
     */
    public static final CEventSpriteYureType[] CEVENT_SPRITE_YURE_TYPES = {
            new CEventSpriteYureType(0xFFFFFFFF, "角度指定"),
            new CEventSpriteYureType(0xFFFFFFFF, "左回転"),
            new CEventSpriteYureType(0xFFFFFFFF, "右回転"),
            new CEventSpriteYureType(0xFFFFFFFF, "下"),
            new CEventSpriteYureType(0xFFFFFFFF, "上"),
            new CEventSpriteYureType(0xFFFFFFFF, "左"),
            new CEventSpriteYureType(0xFFFFFFFF, "右"),
            new CEventSpriteYureType(0xFFFFFFFF, "ランダム")
    };

    /**
     * 表示精灵振动的方向区域，根据测试后所得
     */
    public static final CEventSpriteYureType[] VIBRATION_AREA_TYPES = {
            new CEventSpriteYureType(0, "以自机为中心圆形振动"),
            new CEventSpriteYureType(1, "自机左右两侧振动"),
            new CEventSpriteYureType(2, "自机左右两侧振动"),
            new CEventSpriteYureType(3, "自机上下两侧振动"),
            new CEventSpriteYureType(4, "自机上下两侧振动"),
            new CEventSpriteYureType(5, "自机后方振动"),
            new CEventSpriteYureType(6, "自机前方振动"),
            new CEventSpriteYureType(7, "自机前后两侧振动") // 7及以上视为前后两侧
    };


    public static final String[] CEVENT_SPRITE_YURE_FORMATS = {
            "%s : %4d (周期 : %4d)",
            "%s : %4d ",
            "⇒ %4d (周期 : %4d)"
    };


    private List<BsdxInfoCollection> bsdxInfoCollectionList = new ArrayList<>();

    /**
     * 7及以上为自机前后两侧进行振动
     * 6为只在自机前方进行振动
     * 5为只在自机后方进行振动
     * 4和3为自机上下两侧进行振动（无论自机朝向方向为何都是上下）
     * 2和1为自机左右两侧进行振动（无论自机朝向方向为何都是左右）
     * 0为以自机为中心的圆为范围进行振动
     *
     * 振动区域类型（如：左右/上下/前后/中心等）
     *
     * 参考上方数组VIBRATION_AREA_TYPES
     */
    private Integer vibrationAreaType;

    /**
     * 精灵图进行振动时所振动的范围大小
     * 震动方向为从外向内
     *
     * 振动幅度：从外向内收缩的位移量
     */
    private Integer vibrationAmplitudeOutToIn;

    /**
     * 精灵图进行振动时所振动的范围大小
     * 震动方向为从内向外
     *
     * 振动幅度：从内向外扩张的位移量
     */
    private Integer vibrationAmplitudeInToOut;

    /**
     * 数值越高精灵图所振动的范围越小
     * 
     * 振幅衰减因子：值越高，实际振动越小
     */
    private Integer vibrationDampingFactor;

    public CEventSpriteYure(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.bsdxInfoCollectionList.clear();
        BsdxInfoCollection bsdxInfoCollection = new BsdxInfoCollection();
        bsdxInfoCollection.readCollection(reader);
        this.bsdxInfoCollectionList.add(bsdxInfoCollection);

        this.vibrationAreaType = reader.readInt();
        this.vibrationAmplitudeOutToIn = reader.readInt();
        this.vibrationAmplitudeInToOut = reader.readInt();
        this.vibrationDampingFactor = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        for (BsdxInfoCollection collection : bsdxInfoCollectionList) {
            collection.writeCollection(writer);
        }
        writer.writeInt(this.vibrationAreaType);
        writer.writeInt(this.vibrationAmplitudeOutToIn);
        writer.writeInt(this.vibrationAmplitudeInToOut);
        writer.writeInt(this.vibrationDampingFactor);
    }

}
